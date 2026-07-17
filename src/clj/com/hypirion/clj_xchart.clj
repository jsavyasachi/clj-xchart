(ns com.hypirion.clj-xchart
  (:refer-clojure :exclude [spit])
  (:require [clojure.set :as set]
            [clojure.string :as s])
  (:import (org.knowm.xchart AnnotationImage
                             AnnotationLine
                             AnnotationText
                             AnnotationTextPanel
                             BoxChart
                             BoxSeries
                             BubbleChart
                             XYChart
                             PieChart
                             CategoryChart
                             DialChart
                             DialSeries
                             HeatMapChart
                             HeatMapSeries
                             HorizontalBarChart
                             HorizontalBarSeries
                             OHLCChart
                             OHLCSeries
                             OHLCSeries$OHLCSeriesRenderStyle
                             RadarChart
                             RadarSeries
                             ToolTipType
                             XYSeries
                             CategorySeries
                             BubbleSeries
                             BubbleSeries$BubbleSeriesRenderStyle
                             CategorySeries$CategorySeriesRenderStyle
                             PieSeries$PieSeriesRenderStyle
                             XYSeries$XYSeriesRenderStyle
                             XChartPanel
                             BitmapEncoder
                             BitmapEncoder$BitmapFormat
                             VectorGraphicsEncoder
                             VectorGraphicsEncoder$VectorGraphicsFormat)
           (org.knowm.xchart.style Styler
                                   AxesChartStyler
                                   AxesChartStyler$TextAlignment
                                   BoxStyler
                                   BoxStyler$BoxplotCalCulationMethod
                                   DialStyler
                                   HeatMapStyler
                                   HorizontalBarStyler
                                   OHLCStyler
                                   RadarStyler
                                   RadarStyler$RadarRenderStyle
                                   Styler$LegendLayout
                                   Styler$LegendPosition
                                   Styler$YAxisPosition
                                   XYStyler
                                   CategoryStyler
                                   PieStyler
                                   PieStyler$ClockwiseDirectionType
                                   PieStyler$LabelType
                                   BubbleStyler)
           (org.knowm.xchart.style.colors ColorBlindFriendlySeriesColors
                                          PrinterFriendlySeriesColors)
           (org.knowm.xchart.style.theme GGPlot2Theme
                                         MatlabTheme
                                         XChartTheme)
           (org.knowm.xchart.style.markers Circle
                                           Cross
                                           Diamond
                                           None
                                           Oval
                                           Plus
                                           Rectangle
                                           Square
                                           Trapezoid
                                           TriangleDown
                                           TriangleUp)
           (org.knowm.xchart.style.lines SeriesLines)
           (org.knowm.xchart.internal.series Series)
           (java.io ByteArrayOutputStream FileOutputStream)
           (java.awt Color
                     GridLayout)
           (java.util.function Function)
           (javax.swing JPanel
                        JFrame
                        SwingUtilities)))

;; reduce-map + map-vals is taken from the Medley utility library:
;; https://github.com/weavejester/medley
;; Medley is under the same license (EPL1.0) as clj-xchart.
(defn- reduce-map [f coll]
  (if (instance? clojure.lang.IEditableCollection coll)
    (persistent! (reduce-kv (f assoc!) (transient (empty coll)) coll))
    (reduce-kv (f assoc) (empty coll) coll)))

(defn- map-vals
  "Maps a function over the values of an associative collection."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m k (f v)))) coll))

(defprotocol Chart
  "Protocol for charts, which extends the XChart charts with
  additional polymorphic Clojure functions."
  (add-series! [chart series-name data]
    "A method to add new series to the provided chart"))

(def colors
  "All the default java.awt colors as keywords. You can use this map
  to iterate over the keys, in case you'd like to compare different
  colors. Or you could use java.awt.Color directly to use the exact
  color you want."
  {:blue Color/BLUE
   :black Color/BLACK
   :cyan Color/CYAN
   :dark-gray Color/DARK_GRAY
   :gray Color/GRAY
   :green Color/GREEN
   :light-gray Color/LIGHT_GRAY
   :magenta Color/MAGENTA
   :orange Color/ORANGE
   :pink Color/PINK
   :red Color/RED
   :white Color/WHITE
   :yellow Color/YELLOW})

(def strokes
  "The default stroke types provided by XChart. You can also use a self-made
  stroke if you're not happy with any of the predefined ones."
  {:none SeriesLines/NONE
   :solid SeriesLines/SOLID
   :dash-dot SeriesLines/DASH_DOT
   :dash-dash SeriesLines/DASH_DASH
   :dot-dot SeriesLines/DOT_DOT})

(def markers
  "All the default XChart markers as keywords. To create your own marker, you
  must _subclass_ the org.knowm.xchart.style.markers.Marker class, so it's often
  better to use the default ones."
  {:circle (Circle.)
   :diamond (Diamond.)
   :none (None.)
   :cross (Cross.)
   :oval (Oval.)
   :plus (Plus.)
   :rectangle (Rectangle.)
   :square (Square.)
   :trapezoid (Trapezoid.)
   :triangle-up (TriangleUp.)
   :triangle-down (TriangleDown.)})

(def xy-render-styles
  "The different xy-render styles: :area, :scatter and :line."
  {:area XYSeries$XYSeriesRenderStyle/Area
   :polygon-area XYSeries$XYSeriesRenderStyle/PolygonArea
   :scatter XYSeries$XYSeriesRenderStyle/Scatter
   :step XYSeries$XYSeriesRenderStyle/Step
   :step-area XYSeries$XYSeriesRenderStyle/StepArea
   :line XYSeries$XYSeriesRenderStyle/Line})

(def pie-render-styles
  "The different pie render styles. It is :pie by default."
  {:pie PieSeries$PieSeriesRenderStyle/Pie
   :donut PieSeries$PieSeriesRenderStyle/Donut})

(def pie-annotation-types
  "The different annotation types you can use to annotate pie charts.
  By default, this is :percentage. (XChart renamed AnnotationType to
  LabelType in 4.x; :label maps to the slice name.)"
  {:label PieStyler$LabelType/Name
   :label-and-percentage PieStyler$LabelType/NameAndPercentage
   :percentage PieStyler$LabelType/Percentage
   :value PieStyler$LabelType/Value
   :name-and-value PieStyler$LabelType/NameAndValue})

(def category-render-styles
  "The different styles you can use for category series."
  {:area CategorySeries$CategorySeriesRenderStyle/Area
   :bar CategorySeries$CategorySeriesRenderStyle/Bar
   :line CategorySeries$CategorySeriesRenderStyle/Line
   :scatter CategorySeries$CategorySeriesRenderStyle/Scatter
   :stepped-bar CategorySeries$CategorySeriesRenderStyle/SteppedBar
   :stick CategorySeries$CategorySeriesRenderStyle/Stick})

(def ohlc-render-styles
  "The different styles you can use for OHLC series."
  {:candle OHLCSeries$OHLCSeriesRenderStyle/Candle
   :hilo OHLCSeries$OHLCSeriesRenderStyle/HiLo
   :line OHLCSeries$OHLCSeriesRenderStyle/Line})

(def radar-render-styles
  "The different styles you can use to render radar charts."
  {:polygon RadarStyler$RadarRenderStyle/Polygon
   :circle RadarStyler$RadarRenderStyle/Circle})

(def bubble-render-styles
  "Different render styles for bubble series. For now this is useless, as you
  can only use :round. Apparently :box is around the corner though."
  {:round BubbleSeries$BubbleSeriesRenderStyle/Round})

(def text-alignments
  "The different kinds of text alignments you can use."
  {:centre AxesChartStyler$TextAlignment/Centre
   :left AxesChartStyler$TextAlignment/Left
   :right AxesChartStyler$TextAlignment/Right})

(def legend-positions
  "The different legend positions. Note that xchart implements only a
  subset of inside/outside for the different positions."
  {:inside-n  Styler$LegendPosition/InsideN
   :inside-ne Styler$LegendPosition/InsideNE
   :inside-nw Styler$LegendPosition/InsideNW
   :inside-s Styler$LegendPosition/InsideS
   :inside-se Styler$LegendPosition/InsideSE
   :inside-sw Styler$LegendPosition/InsideSW
   :outside-e Styler$LegendPosition/OutsideE
   :outside-s Styler$LegendPosition/OutsideS})

(def series-color-presets
  "XChart's built-in accessible and print-friendly series color palettes."
  {:color-blind-friendly (.getSeriesColors (ColorBlindFriendlySeriesColors.))
   :printer-friendly (.getSeriesColors (PrinterFriendlySeriesColors.))})

(def legend-layouts
  "The vertical and horizontal legend layouts."
  {:vertical Styler$LegendLayout/Vertical
   :horizontal Styler$LegendLayout/Horizontal})

(def tooltip-types
  "The label combinations shown in XChart tooltips."
  {:x-and-y-labels ToolTipType/xAndYLabels
   :x-labels ToolTipType/xLabels
   :y-labels ToolTipType/yLabels})

(def y-axis-positions
  "The positions available for grouped y axes."
  {:left Styler$YAxisPosition/Left
   :right Styler$YAxisPosition/Right})

(def clockwise-directions
  "The directions in which pie slices can be drawn."
  {:clockwise PieStyler$ClockwiseDirectionType/CLOCKWISE
   :counter-clockwise PieStyler$ClockwiseDirectionType/COUNTER_CLOCKWISE})

(def themes
  "The different default themes you can use with xchart."
  {:ggplot2 (GGPlot2Theme.)
   :matlab (MatlabTheme.)
   :xchart (XChartTheme.)})

(defmacro ^:private doto-cond
  "Example:
  (doto-cond expr
    cond1 (my call)
    cond2 (my2 call2))
  =>
  (let [e# expr]
    (when cond1 (my #e call))
    (when cond2 (my2 #2 call2)))"
  [expr & clauses]
  (let [pairs (partition 2 clauses)
        ;; carry any ^Tag hint on expr onto the binding so the doto-style
        ;; method calls resolve without reflection
        expr-sym (with-meta (gensym "expr") (meta expr))]
    `(let [~expr-sym ~expr]
       ~@(map (fn [[cond clause]]
                `(when ~cond
                   (~(first clause) ~expr-sym ~@(rest clause))))
              pairs)
       ~expr-sym)))

(defn- set-legend!
  [^Styler styler
   {:keys [background-color border-color font padding
           position layout series-line-length visible?]}]
  (doto-cond
   styler
   background-color (.setLegendBackgroundColor (colors background-color background-color))
   border-color (.setLegendBorderColor (colors border-color border-color))
   font (.setLegendFont font)
   layout (.setLegendLayout (legend-layouts layout layout))
   padding (.setLegendPadding (int padding))
   position (.setLegendPosition (legend-positions position))
   series-line-length (.setLegendSeriesLineLength (int series-line-length))
   (not (nil? visible?)) (.setLegendVisible (boolean visible?))))

(defn- set-chart-title-style!
  [^Styler styler
   {:keys [box font font-color padding visible?]}]
  (let [{box-background-color :background-color
         box-border-color :color
         box-visible? :visible?} box]
    (doto-cond
     styler
     box-background-color (.setChartTitleBoxBackgroundColor (colors box-background-color box-background-color))
     box-border-color (.setChartTitleBoxBorderColor (colors box-border-color box-border-color))
     (not (nil? box-visible?)) (.setChartTitleBoxVisible (boolean box-visible?))
     font (.setChartTitleFont font)
     font-color (.setChartTitleFontColor (colors font-color font-color))
     padding (.setChartTitlePadding (int padding))
     (not (nil? visible?)) (.setChartTitleVisible (boolean visible?)))))

(defn- set-chart-style!
  [^Styler styler
   {:keys [background-color font-color padding title]}]
  (doto-cond
   styler
   background-color (.setChartBackgroundColor (colors background-color background-color))
   font-color (.setChartFontColor (colors font-color font-color))
   padding (.setChartPadding (int padding))
   title (set-chart-title-style! title)))

(defn- set-plot-style!
  [^Styler styler
   {:keys [background-color border-color border-visible? content-size]}]
  (doto-cond
   styler
   background-color (.setPlotBackgroundColor (colors background-color background-color))
   border-color (.setPlotBorderColor (colors border-color border-color))
   (not (nil? border-visible?)) (.setPlotBorderVisible (boolean border-visible?))
   content-size (.setPlotContentSize (double content-size))))

(defn- set-series-style!
  [^Styler styler
   series]
  ;; All of these are arrays, so we mutate them and set them back in.
  (let [series-colors (.getSeriesColors styler)
        series-lines (.getSeriesLines styler)
        series-markers (.getSeriesMarkers styler)
        series (vec series)]
    (dotimes [i (count series)]
      ;; TODO: nth instead mayhaps
      (let [{:keys [color stroke marker]} (series i)]
        (when color
          (aset series-colors i (colors color color)))
        (when stroke
          (aset series-lines i (strokes stroke stroke)))
        (when marker
          (aset series-markers i (markers marker marker)))))
    (doto styler
      (.setSeriesColors series-colors)
      (.setSeriesLines series-lines)
      (.setSeriesMarkers series-markers))))

(defn- set-series-colors!
  [^Styler styler series-colors]
  (.setSeriesColors
   styler
   (if (keyword? series-colors)
     (series-color-presets series-colors)
     (into-array Color (map #(colors % %) series-colors)))))

(defn- set-tooltips!
  [^Styler styler
   {:keys [type background-color border-color font highlight-color
           always-visible?]}]
  (doto-cond
   styler
   type (.setToolTipType (tooltip-types type type))
   background-color (.setToolTipBackgroundColor (colors background-color background-color))
   border-color (.setToolTipBorderColor (colors border-color border-color))
   font (.setToolTipFont font)
   highlight-color (.setToolTipHighlightColor (colors highlight-color highlight-color))
   (not (nil? always-visible?)) (.setToolTipsAlwaysVisible (boolean always-visible?))))

(defn- set-annotation-style!
  [^Styler styler {:keys [text line panel]}]
  (let [{text-font :font text-font-color :font-color} text
        {line-color :color line-stroke :stroke} line
        {panel-background-color :background-color
         panel-border-color :border-color
         panel-font :font panel-font-color :font-color
         panel-padding :padding} panel]
    (doto-cond
     styler
     text-font (.setAnnotationTextFont text-font)
     text-font-color (.setAnnotationTextFontColor (colors text-font-color text-font-color))
     line-color (.setAnnotationLineColor (colors line-color line-color))
     line-stroke (.setAnnotationLineStroke (strokes line-stroke line-stroke))
     panel-background-color (.setAnnotationTextPanelBackgroundColor
                             (colors panel-background-color panel-background-color))
     panel-border-color (.setAnnotationTextPanelBorderColor
                         (colors panel-border-color panel-border-color))
     panel-font (.setAnnotationTextPanelFont panel-font)
     panel-font-color (.setAnnotationTextPanelFontColor
                       (colors panel-font-color panel-font-color))
     panel-padding (.setAnnotationTextPanelPadding (int panel-padding)))))

(defn- set-default-style!
  [^Styler styler
   {:keys [annotations-font annotations? annotation anti-alias? base-font chart
           plot legend series series-colors text-anti-alias? tooltips
           y-axis-group-positions]}]
  ;; XChart 4.x renamed "annotations" to "labels" and moved them off the base
  ;; Styler onto the pie/category stylers, so route there when applicable.
  (when (instance? PieStyler styler)
    (doto-cond ^PieStyler styler
      annotations-font (.setLabelsFont annotations-font)
      (not (nil? annotations?)) (.setLabelsVisible (boolean annotations?))))
  (when (instance? CategoryStyler styler)
    (doto-cond ^CategoryStyler styler
      annotations-font (.setLabelsFont annotations-font)
      (not (nil? annotations?)) (.setLabelsVisible (boolean annotations?))))
  (doseq [[group position] y-axis-group-positions]
    (.setYAxisGroupPosition styler (int group) (y-axis-positions position position)))
  (doto-cond
   styler
   annotation (set-annotation-style! annotation)
   (not (nil? anti-alias?)) (.setAntiAlias (boolean anti-alias?))
   base-font (.setBaseFont base-font)
   chart (set-chart-style! chart)
   legend (set-legend! legend)
   plot (set-plot-style! plot)
   series-colors (set-series-colors! series-colors)
   series (set-series-style! series)
   (not (nil? text-anti-alias?)) (.setTextAntiAlias (boolean text-anti-alias?))
   tooltips (set-tooltips! tooltips)))

(defn- set-axis-ticks!
  [^AxesChartStyler styler
   {:keys [labels marks padding visible? line-visible?]}]
  (let [{:keys [color font]} labels]
    (doto-cond
     styler
     color (.setAxisTickLabelsColor (colors color color))
     font (.setAxisTickLabelsFont font)))
  (let [{:keys [length color stroke visible?]} marks]
    (doto-cond
     styler
     length (.setAxisTickMarkLength (int length))
     color (.setAxisTickMarksColor (colors color color))
     stroke (.setAxisTickMarksStroke (strokes stroke stroke))
     (not (nil? visible?)) (.setAxisTicksMarksVisible (boolean visible?))))
  (doto-cond
   styler
   padding (.setAxisTickPadding (int padding))
   (not (nil? line-visible?)) (.setAxisTicksLineVisible (boolean line-visible?))
   (not (nil? visible?)) (.setAxisTicksVisible (boolean visible?))))

(defn- set-axis-title!
  [^AxesChartStyler styler
   {:keys [font visible? padding]}]
  (doto-cond
   styler
   font (.setAxisTitleFont font)
   padding (.setAxisTitlePadding (int padding))
   (not (nil? visible?)) (.setAxisTitlesVisible (boolean visible?))))

(defn- set-axis-plot!
  [^AxesChartStyler styler
   {:keys [grid-lines margin tick-marks?]}]
  (let [{:keys [horizontal? vertical? visible? color stroke]} grid-lines]
    (doto-cond
     styler
     (not (nil? visible?)) (.setPlotGridLinesVisible (boolean visible?))
     color (.setPlotGridLinesColor (colors color color))
     stroke (.setPlotGridLinesStroke (strokes stroke stroke))
     (not (nil? horizontal?)) (.setPlotGridHorizontalLinesVisible (boolean horizontal?))
     (not (nil? vertical?)) (.setPlotGridVerticalLinesVisible (boolean vertical?))))
  (doto-cond
   styler
   margin (.setPlotMargin (int margin))
   (not (nil? tick-marks?)) (.setPlotTicksMarksVisible (boolean tick-marks?))))

(defn- ^Function as-java-function
  [f]
  (reify Function
    (apply [_ value] (f value))))

(defn- set-x-axis-style!
  [^AxesChartStyler styler
   {:keys [label logarithmic? max min decimal-pattern
           logarithmic-decade-only? max-label-count tick-label-color
           tick-label-formatter tick-mark-color tick-mark-spacing-hint
           ticks-visible? title-visible?]}]
  (let [{:keys [alignment rotation vertical-alignment]} label]
    (doto-cond
     styler
     alignment (.setXAxisLabelAlignment (text-alignments alignment alignment))
     vertical-alignment (.setXAxisLabelAlignmentVertical
                         (text-alignments vertical-alignment vertical-alignment))
     rotation (.setXAxisLabelRotation (int rotation))))
  (doto-cond
   styler
   decimal-pattern (.setXAxisDecimalPattern decimal-pattern)
   (not (nil? logarithmic?)) (.setXAxisLogarithmic (boolean logarithmic?))
   (not (nil? logarithmic-decade-only?))
   (.setXAxisLogarithmicDecadeOnly (boolean logarithmic-decade-only?))
   max (.setXAxisMax (double max))
   max-label-count (.setXAxisMaxLabelCount (int max-label-count))
   min (.setXAxisMin (double min))
   tick-label-color (.setXAxisTickLabelsColor (colors tick-label-color tick-label-color))
   tick-label-formatter (.setXAxisTickLabelsFormattingFunction
                         (as-java-function tick-label-formatter))
   tick-mark-color (.setXAxisTickMarksColor (colors tick-mark-color tick-mark-color))
   tick-mark-spacing-hint (.setXAxisTickMarkSpacingHint (int tick-mark-spacing-hint))
   (not (nil? ticks-visible?)) (.setXAxisTicksVisible (boolean ticks-visible?))
   (not (nil? title-visible?)) (.setXAxisTitleVisible (boolean title-visible?))))

(defn- set-y-axis-style!
  [^AxesChartStyler styler
   {:keys [label logarithmic? max min decimal-pattern
           logarithmic-decade-only? tick-label-color tick-label-formatter
           tick-mark-color tick-mark-spacing-hint ticks-visible? title-visible?]}]
  (let [{:keys [alignment rotation]} label]
    (doto-cond
     styler
     alignment (.setYAxisLabelAlignment (text-alignments alignment alignment))))
  (doto-cond
   styler
   decimal-pattern (.setYAxisDecimalPattern decimal-pattern)
   (not (nil? logarithmic?)) (.setYAxisLogarithmic (boolean logarithmic?))
   (not (nil? logarithmic-decade-only?))
   (.setYAxisLogarithmicDecadeOnly (boolean logarithmic-decade-only?))
   max (.setYAxisMax (double max))
   min (.setYAxisMin (double min))
   tick-label-color (.setYAxisTickLabelsColor (colors tick-label-color tick-label-color))
   tick-label-formatter (.setYAxisTickLabelsFormattingFunction
                         (as-java-function tick-label-formatter))
   tick-mark-color (.setYAxisTickMarksColor (colors tick-mark-color tick-mark-color))
   tick-mark-spacing-hint (.setYAxisTickMarkSpacingHint (int tick-mark-spacing-hint))
   (not (nil? ticks-visible?)) (.setYAxisTicksVisible (boolean ticks-visible?))
   (not (nil? title-visible?)) (.setYAxisTitleVisible (boolean title-visible?))))

(defn- set-axes-style!
  [^AxesChartStyler styler
   {:keys [axis error-bars-color plot x-axis y-axis
           date-pattern decimal-pattern locale marker timezone]}]
  (let [ebc error-bars-color ;; error-bars-color is too long to be readable in these expressions
        {axis-ticks :ticks axis-title :title} axis
        {marker-size :size} marker]
    (doto-cond
     styler
     axis-ticks (set-axis-ticks! axis-ticks)
     axis-title (set-axis-title! axis-title)
     date-pattern (.setDatePattern date-pattern)
     decimal-pattern (.setDecimalPattern decimal-pattern)
     ;; The logic here is as follows: You can specify a colour for the error
     ;; bars. If the colour is :match-series, then the colour matches the series
     ;; colour, but if you specify something else, you cannot match the series!
     (and ebc (not= ebc :match-series)) (.setErrorBarsColor (colors ebc ebc))
     (and ebc (not= ebc :match-series)) (.setErrorBarsColorSeriesColor false)
     (= ebc :match-series) (.setErrorBarsColorSeriesColor true)
     locale (.setLocale locale)
     marker-size (.setMarkerSize marker-size)
     plot (set-axis-plot! plot)
     timezone (.setTimezone timezone)
     x-axis (set-x-axis-style! x-axis)
     y-axis (set-y-axis-style! y-axis))))

(defn- set-xy-style!
  [^XYStyler styler {:keys [cursor]}]
  (let [{:keys [color line-width font font-color background-color
                x-formatter y-formatter]} cursor]
    (doto-cond
     styler
     color (.setCursorColor (colors color color))
     line-width (.setCursorLineWidth (float line-width))
     font (.setCursorFont font)
     font-color (.setCursorFontColor (colors font-color font-color))
     background-color (.setCursorBackgroundColor (colors background-color background-color))
     x-formatter (.setCustomCursorXDataFormattingFunction (as-java-function x-formatter))
     y-formatter (.setCustomCursorYDataFormattingFunction (as-java-function y-formatter)))))

(defn- set-category-style!
  [^CategoryStyler styler
   {:keys [show-stack-sum? bar-label-color bar-label-rotation
           bar-label-position overlapped? bar-label-automatic-contrast?
           bar-label-automatic-light-color bar-label-automatic-dark-color]}]
  (doto-cond
   styler
   (not (nil? show-stack-sum?)) (.setShowStackSum (boolean show-stack-sum?))
   bar-label-color (.setLabelsFontColor (colors bar-label-color bar-label-color))
   bar-label-rotation (.setLabelsRotation (int bar-label-rotation))
   bar-label-position (.setLabelsPosition (double bar-label-position))
   (not (nil? overlapped?)) (.setOverlapped (boolean overlapped?))
   (not (nil? bar-label-automatic-contrast?))
   (.setLabelsFontColorAutomaticEnabled (boolean bar-label-automatic-contrast?))
   bar-label-automatic-light-color
   (.setLabelsFontColorAutomaticLight
    (colors bar-label-automatic-light-color bar-label-automatic-light-color))
   bar-label-automatic-dark-color
   (.setLabelsFontColorAutomaticDark
    (colors bar-label-automatic-dark-color bar-label-automatic-dark-color))))

(defn- set-pie-style!
  [^PieStyler styler
   {:keys [sum-visible? sum-format sum-font series-label-fn label-color
           label-automatic-contrast? label-automatic-light-color
           label-automatic-dark-color clockwise-direction slice-border-width]}]
  (doto-cond
   styler
   (not (nil? sum-visible?)) (.setSumVisible (boolean sum-visible?))
   sum-format (.setSumFormat sum-format)
   sum-font (.setSumFont sum-font)
   series-label-fn (.setCustomSeriesLabelFunction (as-java-function series-label-fn))
   label-color (.setLabelsFontColor (colors label-color label-color))
   (not (nil? label-automatic-contrast?))
   (.setLabelsFontColorAutomaticEnabled (boolean label-automatic-contrast?))
   label-automatic-light-color
   (.setLabelsFontColorAutomaticLight
    (colors label-automatic-light-color label-automatic-light-color))
   label-automatic-dark-color
   (.setLabelsFontColorAutomaticDark
    (colors label-automatic-dark-color label-automatic-dark-color))
   clockwise-direction
   (.setClockwiseDirectionType (clockwise-directions clockwise-direction clockwise-direction))
   slice-border-width (.setSliceBorderWidth (double slice-border-width))))

(defn- set-common-series-style!
  [^Series series
   {:keys [label enabled? y-axis-group y-axis-decimal-pattern]}]
  (doto-cond
   series
   label (.setLabel label)
   (not (nil? enabled?)) (.setEnabled (boolean enabled?))
   (not (nil? y-axis-group)) (.setYAxisGroup (int y-axis-group))
   y-axis-decimal-pattern (.setYAxisDecimalPattern y-axis-decimal-pattern)))

(defn- add-raw-series
  ;; addSeries is intentionally reflective: chart is one of several types and
  ;; XChart exposes ~12 addSeries overloads (double[]/float[]/int[]/List), so we
  ;; rely on runtime dispatch to accept whatever numeric collection is passed.
  ([chart s-name x-data y-data]
   (clojure.lang.Reflector/invokeInstanceMethod
    chart "addSeries" (to-array [s-name x-data y-data])))
  ([chart s-name x-data y-data error-bars]
   (clojure.lang.Reflector/invokeInstanceMethod
    chart "addSeries" (to-array [s-name x-data y-data error-bars]))))

(defn- assoc-in-nonexisting
  "Like assoc-in, but will only add fields not found. Nil may be found, in which
  case it is NOT updated."
  [m ks v]
  (cond->
      m
      (identical? (get-in m ks ::not-found) ::not-found)
      (assoc-in ks v)))

(defn- attach-default-font
  "Sets the font type on all the provided "
  [style-map]
  (if-let [font (:font style-map)]
    (-> style-map
        (dissoc :font)
        (assoc :base-font font)
        (assoc-in-nonexisting [:axis :ticks :labels :font] font)
        (assoc-in-nonexisting [:axis :title :font] font)
        (assoc-in-nonexisting [:legend :font] font)
        (assoc-in-nonexisting [:annotations-font] font)
        (assoc-in-nonexisting [:chart :title :font] font))
    style-map))

(extend-type XYChart
  Chart
  (add-series! [chart s-name data]
    (if (sequential? data)
      (apply add-raw-series chart s-name data)
      (let [{:keys [x y error-bars style]} data
            {:keys [marker-color marker-type
                    line-color line-style line-width
                    fill-color show-in-legend? render-style smooth?]} style]
        (doto-cond
         ^XYSeries
         (if error-bars
           (add-raw-series chart s-name x y error-bars)
           (add-raw-series chart s-name x y))
         style (set-common-series-style! style)
         render-style (.setXYSeriesRenderStyle (xy-render-styles render-style))
         marker-color (.setMarkerColor (colors marker-color marker-color))
         marker-type (.setMarker (markers marker-type marker-type))
         line-color (.setLineColor (colors line-color line-color))
         line-style (.setLineStyle (strokes line-style line-style))
         line-width (.setLineWidth (float line-width))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? smooth?)) (.setSmooth (boolean smooth?))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn xy-chart
  "Returns an xy-chart. See the tutorial for more information about
  how to create an xy-chart, and see the render-styles documentation
  for styling options."
  ([series]
   (xy-chart series {}))
  ([series
    {:keys [width height title theme render-style]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (XYChart. width height)
         styling (attach-default-font styling)]
     (doto-cond
      ^XYStyler (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (xy-render-styles render-style))
      styling (set-xy-style! styling))
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto (.getStyler chart)
       (set-default-style! styling)
       (set-axes-style! styling))
     (doto-cond
      chart
      title (.setTitle title)
      (-> styling :x-axis :title) (.setXAxisTitle (-> styling :x-axis :title))
      (-> styling :y-axis :title) (.setYAxisTitle (-> styling :y-axis :title))))))

(extend-type CategoryChart
  Chart
  (add-series! [chart s-name data]
    (if (sequential? data)
      (apply add-raw-series chart s-name data)
      (let [{:keys [x y error-bars style]} data
            {:keys [marker-color marker-type
                    line-color line-style line-width
                    fill-color show-in-legend? render-style smooth? overlapped?]} style]
        (doto-cond
         ^CategorySeries
         (if error-bars
           (add-raw-series chart s-name x y error-bars)
           (add-raw-series chart s-name x y))
         style (set-common-series-style! style)
         render-style (.setChartCategorySeriesRenderStyle (category-render-styles render-style))
         marker-color (.setMarkerColor (colors marker-color marker-color))
         marker-type (.setMarker (markers marker-type marker-type))
         line-color (.setLineColor (colors line-color line-color))
         line-style (.setLineStyle (strokes line-style line-style))
         line-width (.setLineWidth (float line-width))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? smooth?)) (.setSmooth (boolean smooth?))
         (not (nil? overlapped?)) (.setOverlapped (boolean overlapped?))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn category-chart*
  "Returns a raw category chart. Prefer `category-chart` unless you
  run into performance issues. See the tutorial for more information
  about how to create category charts, and see the render-styles
  documentation for styling options."
  ([series]
   (category-chart* series {}))
  ([series
    {:keys [width height title theme render-style available-space-fill overlap?
            stacked?]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (CategoryChart. width height)
         styling (attach-default-font styling)]
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto-cond
      ^CategoryStyler (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (category-render-styles render-style))
      available-space-fill (.setAvailableSpaceFill (double available-space-fill))
      (not (nil? overlap?)) (.setOverlapped (boolean overlap?))
      (not (nil? stacked?)) (.setStacked (boolean stacked?))
      styling (set-category-style! styling))
     (doto (.getStyler chart)
       (set-default-style! styling)
       (set-axes-style! styling))
     (doto-cond
      chart
      title (.setTitle title)
      (-> styling :x-axis :title) (.setXAxisTitle (-> styling :x-axis :title))
      (-> styling :y-axis :title) (.setYAxisTitle (-> styling :y-axis :title))))))

;; Utility functions

(defn- normalize-category-series
  "Returns the content of a category series on the shape {:x ... :y ...} with
  styling data retained."
  [series-data]
  (cond (and (map? series-data)
             (contains? series-data :x)
             (contains? series-data :y)) series-data
        (and (map? series-data)
             (contains? series-data :content)) (-> (:content series-data)
                                                   (normalize-category-series)
                                                   ;; retain styling data:
                                                   (merge (dissoc series-data :content)))
        ;; Assuming keys are strings/vals
        (and (map? series-data)
             (every? (comp not keyword?)
                     (keys series-data))) {:x (keys series-data)
                                           :y (vals series-data)}
        (sequential? series-data) {:x (first series-data)
                                   :y (second series-data)}))

(defn- category-series-xs
  "Given a map of series, return all the unique x-elements as a set."
  [series]
  (->> (vals series)
       (mapcat :x)
       set))

(defn- reorder-series
  "Reorders a normalized series content to the assigned ordering"
  [{:keys [x y] :as series} x-order]
  ;; Here we may unfortunately recompute an input value. If perfomance is an
  ;; issue, we may attach the mapping onto the series.
  (let [mapping (zipmap x y)]
    (assoc series
           :x x-order
           :y (mapv (fn [x] (get mapping x 0.0)) x-order))))

;; I do have some issues differing between a single series and multiple series.
;; I'll call a map of series a series-map for now.
(defn- normalize-category-series-map
  "Given a series map, normalize the series to contain all x values with the
  order specified in x-order. If the x value does not exist in a series, the
  value 0.0 is inserted. If there are other x values not in x-order, they are
  attached at the end in sorted order."
  [series-map x-order]
  (let [series-map (map-vals normalize-category-series series-map)
        x-order (vec x-order)
        extra-xs (sort (set/difference (category-series-xs series-map)
                                       (set x-order)))
        x-order (into x-order extra-xs)]
    (map-vals #(reorder-series % x-order) series-map)))

(defn category-chart
  "Returns a category chart. See the tutorial for more information
  about how to create category charts, and see the render-styles
  documentation for styling options."
  ([series]
   (category-chart series {}))
  ([series {:keys [x-axis series-order] :as styling}]
   (let [x-order (:order x-axis)
         normalized-map (normalize-category-series-map series x-order)
         extra-categories (->> (apply dissoc normalized-map series-order)
                               (sort-by key))
         normalized-seq (concat (keep #(find normalized-map %) series-order)
                                extra-categories)]
     (category-chart* normalized-seq styling))))

(extend-type BubbleChart
  Chart
  (add-series! [chart s-name data]
    (if (sequential? data)
      (apply add-raw-series chart s-name data)
      (let [{:keys [x y bubble style]} data
            {:keys [marker-color marker-type
                    line-color line-style line-width
                    fill-color show-in-legend? render-style]} style]
        (doto-cond
         ^BubbleSeries (add-raw-series chart s-name x y bubble)
         style (set-common-series-style! style)
         ;; NOTE: Add render style when squares are added to the impl?
         render-style (.setBubbleSeriesRenderStyle (bubble-render-styles render-style))
         line-color (.setLineColor (colors line-color line-color))
         line-style (.setLineStyle (strokes line-style line-style))
         line-width (.setLineWidth (float line-width))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn bubble-chart*
  "Returns a raw bubble chart. Bubble charts are hard to make right,
  so please see the tutorial for more information about how to create
  one. The render-styles page will give you information about styling
  options."
  ([series]
   (bubble-chart* series {}))
  ([series
    {:keys [width height title theme render-style]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (BubbleChart. width height)
         styling (attach-default-font styling)]
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto-cond
      ^BubbleStyler (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (bubble-render-styles render-style)))
     (doto (.getStyler chart)
       (set-default-style! styling)
       (set-axes-style! styling))
     (doto-cond
      chart
      title (.setTitle title)
      (-> styling :x-axis :title) (.setXAxisTitle (-> styling :x-axis :title))
      (-> styling :y-axis :title) (.setYAxisTitle (-> styling :y-axis :title))))))

(defn- max-bubble-value [series]
  (reduce max
          (mapcat :bubble (vals series))))

(defn- scale-bubbles
  "Scales the bubbles such that a bubble with size `in-val` has `out-val`
  diameter in pixels."
  [series in-val out-val]
  (let [bubble-fn #(* out-val (Math/sqrt (/ % in-val)))]
    (map-vals
     (fn [data]
       (update data :bubble #(map bubble-fn %)))
     series)))

(defn bubble-chart
  "Returns a bubble chart for `series`, scaling bubble diameters per `size`.

  `size` is a map {:in <number or :max> :out [<value> <unit>]} where the input
  bubble value `:in` (or :max, the largest value in the series) maps to an
  output diameter `:out` given in :px/:pixels or :%/:percent of the chart's
  larger dimension. `styling` accepts the same keys as `bubble-chart*`."
  ([series size]
   (bubble-chart series size {}))
  ([series {in :in [out-val out-type] :out :as bubble-size}
    {:keys [width height]
     :or {width 640 height 500}
     :as styling}]
   (let [ot ({:% :percent
              :percent :percent
              :px :pixels
              :pixels :pixels} out-type)
         out-size (if (identical? :percent ot)
                    (/ (* out-val (max width height))
                       100.0)
                    out-val)
         in (if (identical? in :max)
              (max-bubble-value series)
              in)]
     (when-not (and (number? in) (number? out-val) ot)
       (throw (ex-info "bubble-size is not on the correct format"
                       {:input bubble-size
                        :expected-example {:in 100 ;; or :max
                                           :out [100 :px]}})))
     (bubble-chart* (scale-bubbles series in out-size) styling))))

(defn- attach-default-annotation-distance
  "Attaches a default annotation distance if the donut thickness"
  [styling]
  (if-not (and (identical? :donut (:render-style styling))
               (not (:annotation-distance styling)))
    styling
    (assoc styling :annotation-distance
           (- 1.0 (/ (:donut-thickness styling 0.33) 2)))))

(extend-type PieChart
  Chart
  (add-series! [chart s-name data]
    (if (number? data)
      (.addSeries chart s-name data)
      (let [{:keys [render-style fill-color show-in-legend?]} (:style data)
            style (:style data)
            val (:value data)]
        (doto-cond
         (.addSeries chart s-name val)
         style (set-common-series-style! style)
         render-style (.setChartPieSeriesRenderStyle (pie-render-styles render-style))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn pie-chart
  "Returns a pie chart. The series map is in this case just a mapping
  from string to number. For styling information, see the
  render-styles page.

  Example:
  (c/pie-chart {\"Red\" 54
                \"Green\" 34})"
  ([series]
   (pie-chart series {}))
  ([series
    {:keys [width height title circular? theme render-style annotation-distance
            start-angle draw-all-annotations? donut-thickness annotation-type]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (PieChart. width height)
         styling (-> styling
                     attach-default-font
                     attach-default-annotation-distance)
         ;; Need to rebind this one. We could probably omit it from the keys
         ;; entry at the top, if it's not used for documentation purposes.
         annotation-distance (:annotation-distance styling)]
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto-cond
      ^PieStyler (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (pie-render-styles render-style))
      (not (nil? circular?)) (.setCircular (boolean circular?))
      (not (nil? draw-all-annotations?)) (.setForceAllLabelsVisible (boolean draw-all-annotations?))
      annotation-distance (.setLabelsDistance (double annotation-distance))
      donut-thickness (.setDonutThickness (double donut-thickness))
      start-angle (.setStartAngleInDegrees (double start-angle))
      annotation-type (.setLabelType (pie-annotation-types annotation-type))
      styling (set-pie-style! styling))
     (set-default-style! (.getStyler chart) styling)
     ;; Pie charts have no axes in XChart 4.x, so they take only a title.
     (doto-cond
      chart
      title (.setTitle title)))))

(defn as-buffered-image
  "Converts a chart into a java.awt.image.BufferedImage."
  [chart]
  (BitmapEncoder/getBufferedImage chart))

(def ^:private bitmap-formats
  {:png BitmapEncoder$BitmapFormat/PNG
   :gif BitmapEncoder$BitmapFormat/GIF
   :bmp BitmapEncoder$BitmapFormat/BMP
   :jpg BitmapEncoder$BitmapFormat/JPG
   :jpeg BitmapEncoder$BitmapFormat/JPG})

(def ^:private vector-formats
  {:pdf VectorGraphicsEncoder$VectorGraphicsFormat/PDF
   :svg VectorGraphicsEncoder$VectorGraphicsFormat/SVG
   :eps VectorGraphicsEncoder$VectorGraphicsFormat/EPS})

(defn to-bytes
  "Converts a chart into a byte array."
  ([chart type]
   (if-let [bitmap-format (bitmap-formats type)]
     (BitmapEncoder/getBitmapBytes chart bitmap-format)
     (if-let [vector-format (vector-formats type)]
       (let [out (ByteArrayOutputStream.)]
         (VectorGraphicsEncoder/saveVectorGraphic
          ^org.knowm.xchart.internal.chartpart.Chart chart out
          ^VectorGraphicsEncoder$VectorGraphicsFormat vector-format)
         (.toByteArray out))
       (throw (IllegalArgumentException. (str "Unknown format: " type)))))))

(defn view
  "Utility function to render one or more charts in a swing frame."
  [& charts]
  (let [num-rows (int (+ (Math/sqrt (count charts)) 0.5))
        num-cols (inc (/ (count charts)
                         (double num-rows)))
        frame (JFrame. "XChart")]
    (SwingUtilities/invokeLater
     #(do (.. frame (getContentPane) (setLayout (GridLayout. num-rows num-cols)))
          (doseq [chart charts]
            (if chart
              (.add frame (XChartPanel. chart))
              (.add frame (JPanel.))))
          (.pack frame)
          (.setVisible frame true)))
    frame))


(defn- guess-extension
  [fname]
  (if-let [last-dot (s/last-index-of fname ".")]
    (let [extension (s/lower-case (subs fname (inc last-dot)))]
      (keyword extension))))

(defn spit
  "Spits the chart to the given filename. If no type is provided, the type is
  guessed by the filename extension. If no extension is found, an error is
  raised."
  ([chart fname]
   (spit chart fname (guess-extension fname)))
  ([chart fname type]
   (with-open [fos (FileOutputStream. ^String fname)]
     (.write fos ^bytes (to-bytes chart type)))))

(defn- transpose-single
  [acc k1 v1]
  (reduce-kv (fn [m k2 v2]
               (assoc-in m [k2 k1] v2))
             acc v1))

(defn transpose-map
  "Transforms a map of maps such that the inner keys and outer keys are flipped.
  That is, `(get-in m [k1 k2])` = `(get-in (transpose-map m) [k2 k1])`. The
  inner values remain the same."
  [series]
  (reduce-kv transpose-single {} series))

(defn extract-series
  "Transforms coll into a series map by using the values in the provided keymap.
  There's no requirement to provide :x or :y (or any key at all, for that
  matter), although that's common.

  Example: (extract-series {:x f, :y g, :bubble bubble} coll)
        == {:x (map f coll), :y (map g coll), :bubble (map bubble coll)}"
  [keymap coll]
  (map-vals #(map % coll) keymap))

(defn- normalize-group
  [m]
  (let [sum (reduce + (vals m))]
    (map-vals #(/ % sum) m)))

(defn normalize-categories
  "Normalizes a category-series map `m` so that every series shares the same set
  of x-values, filling any missing entries. Useful before handing data to
  `category-chart`, which expects aligned categories across series."
  [m]
  (->> (transpose-map m)
       (map-vals normalize-group)
       transpose-map))
