(ns com.hypirion.clj-xchart-test
  (:require [clojure.test :refer :all]
            [com.hypirion.clj-xchart :as c]
            [com.hypirion.clj-xchart.opt :as opt]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]])
  (:import (java.awt Font)
           (java.awt.image BufferedImage)))

(def category-series-gen
  (gen/map gen/string-ascii (gen/double* {:infinite? false
                                          :NaN? false})
           {:max-elements 100}))

(def category-series-map-gen
  (gen/map gen/string-ascii category-series-gen {:max-elements 10}))

(defspec normalization-doesnt-shuffle-values
  (prop/for-all [category-map category-series-gen]
    (let [normalized-map (#'c/normalize-category-series category-map)]
      (= category-map
         (zipmap (:x normalized-map) (:y normalized-map))))))

(defspec normalization-extends-x-values
  (prop/for-all [series-map category-series-map-gen]
    (let [normalized-categories (#'c/normalize-category-series-map series-map nil)
          x-values (set (mapcat keys (vals series-map)))]
      (every? #(= (set (:x (val %))) x-values)
              normalized-categories))))

(def nonempty-category-series-gen
  (gen/not-empty category-series-gen))

(def nonempty-category-series-map-gen
  "Where the series itself is nonempty, not the map containing series"
  (gen/map gen/string-ascii nonempty-category-series-gen {:max-elements 10}))

(defspec transpose-map-is-involutory-ish
  (prop/for-all [series-map nonempty-category-series-map-gen]
    (= series-map (c/transpose-map (c/transpose-map series-map)))))

;; Characterization tests for the XChart 4.x interop. The reflective styler /
;; series / export calls only fail at runtime, so these build one of every
;; chart type with rich styling and render every output format, asserting we
;; get non-trivial bytes back. This is the regression net for the 3.2 -> 4.0.1
;; port.

(defn- renders-all-formats? [chart]
  (every? (fn [fmt] (pos? (count (c/to-bytes chart fmt))))
          [:png :gif :bmp :jpg :svg :pdf :eps]))

(deftest xy-chart-renders
  (is (renders-all-formats?
       (c/xy-chart {"series" {:x [1 2 3] :y [4 5 6]
                              :style {:marker-type :circle :marker-color :blue
                                      :line-style :dash-dash :line-width 2
                                      :render-style :line :show-in-legend? true}}}
                   {:title "xy" :theme :matlab :render-style :line
                    :annotations? true
                    :x-axis {:title "x" :title-style {:visible? true}}
                    :y-axis {:title "y"}}))))

(deftest category-chart-renders
  (is (renders-all-formats?
       (c/category-chart {"a" {"x" 1.0 "y" 2.0} "b" {"x" 3.0 "y" 4.0}}
                         {:title "cat" :theme :ggplot2 :render-style :bar
                          :stacked? true :available-space-fill 0.9
                          :annotations? true}))))

(deftest pie-chart-renders
  (doseq [atype [:label :label-and-percentage :percentage :value :name-and-value]]
    (is (renders-all-formats?
         (c/pie-chart {"a" 30 "b" 50 "c" 20}
                      {:title "pie" :circular? true :annotation-type atype
                       :draw-all-annotations? true}))
        (str "pie annotation-type " atype))))

(deftest pie-chart-map-style-series
  (let [chart (c/pie-chart {"styled" {:value 7
                                       :style {:render-style :donut
                                               :fill-color :red
                                               :show-in-legend? false}}})
        series (.getSeries chart "styled")]
    (is (= 7 (.getValue series)))
    (is (= (c/pie-render-styles :donut)
           (.getChartPieSeriesRenderStyle series)))
    (is (= (c/colors :red) (.getFillColor series)))
    (is (false? (.isShowInLegend series)))))

(deftest bubble-chart-renders
  (is (renders-all-formats?
       (c/bubble-chart {"b" {:x [1 2 3] :y [4 5 6] :bubble [10 20 30]}}
                       {:in :max :out [20 :px]}
                       {:title "bubble" :theme :xchart}))))

(deftest opt-namespace-loads-and-maps
  ;; Guards the src/java ListMapping class: the opt namespace imports it, so a
  ;; missing :java-source-paths build (or an uncompiled jar) breaks here and in
  ;; cljdoc, even though the core namespace is unaffected.
  (let [coll [{:x 1 :y 10} {:x 2 :y 20} {:x 3 :y 30}]
        series (opt/extract-series {:x :x :y :y} coll)]
    (is (= [1 2 3] (vec (:x series))))
    (is (= [10 20 30] (vec (:y series))))
    ;; the lazy view must render through a real chart end to end
    (is (pos? (count (c/to-bytes (c/xy-chart {"s" series}) :png))))))

(deftest legend-position-and-alignment
  (is (renders-all-formats?
       (c/xy-chart {"s" {:x [1 2] :y [3 4]}}
                   {:legend {:position :inside-nw}
                    :x-axis {:title "x" :title-style {:alignment :right}}}))))

(deftest shared-parity-imports-and-lookups
  (let [imports (ns-imports 'com.hypirion.clj-xchart)]
    (doseq [class-name '[BoxChart OHLCChart DialChart RadarChart HeatMapChart
                         HorizontalBarChart BoxSeries OHLCSeries DialSeries
                         RadarSeries HeatMapSeries HorizontalBarSeries BoxStyler
                         OHLCStyler DialStyler RadarStyler HeatMapStyler
                         HorizontalBarStyler AnnotationText AnnotationLine
                         AnnotationImage AnnotationTextPanel Cross Plus Trapezoid
                         Oval Rectangle ColorBlindFriendlySeriesColors
                         PrinterFriendlySeriesColors]]
      (is (contains? imports class-name) (str "import " class-name))))
  (is (= #{:cross :plus :trapezoid :oval :rectangle}
         (set (filter c/markers [:cross :plus :trapezoid :oval :rectangle]))))
  (is (= #{:step :step-area :polygon-area}
         (set (filter c/xy-render-styles [:step :step-area :polygon-area]))))
  (is (contains? c/category-render-styles :stepped-bar))
  (is (= #{:inside-s :outside-s}
         (set (filter c/legend-positions [:inside-s :outside-s]))))
  (let [ohlc-var (ns-resolve 'com.hypirion.clj-xchart 'ohlc-render-styles)
        radar-var (ns-resolve 'com.hypirion.clj-xchart 'radar-render-styles)
        presets-var (ns-resolve 'com.hypirion.clj-xchart 'series-color-presets)]
    (is (some? ohlc-var))
    (is (some? radar-var))
    (is (some? presets-var))
    (when (and ohlc-var radar-var presets-var)
      (is (= #{:candle :hilo :line} (set (keys (var-get ohlc-var)))))
      (is (= #{:polygon :circle} (set (keys (var-get radar-var)))))
      (is (= #{:color-blind-friendly :printer-friendly}
             (set (keys (var-get presets-var))))))))

(deftest common-and-axes-styler-options
  (let [font (Font. Font/SANS_SERIF Font/PLAIN 12)
        chart (c/xy-chart
               {"s" {:x [1 2] :y [3 4]
                     :style {:label "label" :enabled? false :y-axis-group 1
                             :y-axis-decimal-pattern "0.00" :smooth? true}}}
               {:font font
                :chart {:title {:font-color :red}}
                :legend {:layout :horizontal}
                :series-colors :printer-friendly
                :anti-alias? false
                :text-anti-alias? false
                :tooltips {:type :x-labels :background-color :white
                           :border-color :black :font font
                           :highlight-color :yellow :always-visible? true}
                :y-axis-group-positions {1 :right}
                :annotation {:text {:font font :font-color :blue}
                             :line {:color :red :stroke :dash-dash}
                             :panel {:background-color :white :border-color :black
                                     :font font :font-color :green :padding 7}}
                :x-axis {:max-label-count 3 :tick-label-color :red
                         :tick-mark-color :blue :tick-label-formatter #(str "x" %)
                         :logarithmic-decade-only? true
                         :label {:vertical-alignment :left}}
                :y-axis {:tick-label-color :green :tick-mark-color :orange
                         :tick-label-formatter #(str "y" %)
                         :logarithmic-decade-only? true}
                :cursor {:color :magenta :line-width 2.5 :font font
                         :font-color :white :background-color :black
                         :x-formatter #(str "cx" %) :y-formatter #(str "cy" %)}})
        styler (.getStyler chart)
        series (.getSeries chart "s")]
    (is (= font (.getBaseFont styler)))
    (is (= (c/colors :red) (.getChartTitleFontColor styler)))
    (is (= "Horizontal" (str (.getLegendLayout styler))))
    (is (false? (.getAntiAlias styler)))
    (is (false? (.getTextAntiAlias styler)))
    (is (= "xLabels" (str (.getToolTipType styler))))
    (is (= (c/colors :white) (.getToolTipBackgroundColor styler)))
    (is (= (c/colors :black) (.getToolTipBorderColor styler)))
    (is (= font (.getToolTipFont styler)))
    (is (= (c/colors :yellow) (.getToolTipHighlightColor styler)))
    (is (.isToolTipsAlwaysVisible styler))
    (is (= "Right" (str (.getYAxisGroupPosistion styler 1))))
    (is (= font (.getAnnotationTextFont styler)))
    (is (= (c/colors :blue) (.getAnnotationTextFontColor styler)))
    (is (= (c/colors :red) (.getAnnotationLineColor styler)))
    (is (= (c/strokes :dash-dash) (.getAnnotationLineStroke styler)))
    (is (= (c/colors :white) (.getAnnotationTextPanelBackgroundColor styler)))
    (is (= (c/colors :black) (.getAnnotationTextPanelBorderColor styler)))
    (is (= font (.getAnnotationTextPanelFont styler)))
    (is (= (c/colors :green) (.getAnnotationTextPanelFontColor styler)))
    (is (= 7 (.getAnnotationTextPanelPadding styler)))
    (is (= (seq (c/series-color-presets :printer-friendly))
           (seq (.getSeriesColors styler))))
    (is (= 3 (.getXAxisMaxLabelCount styler)))
    (is (= (c/colors :red) (.getXAxisTickLabelsColor styler)))
    (is (= (c/colors :blue) (.getXAxisTickMarksColor styler)))
    (is (= "x2.0" (.apply (.getXAxisTickLabelsFormattingFunction styler) 2.0)))
    (is (.isXAxisLogarithmicDecadeOnly styler))
    (is (= "Left" (str (.getXAxisLabelAlignmentVertical styler))))
    (is (= (c/colors :green) (.getYAxisTickLabelsColor styler)))
    (is (= (c/colors :orange) (.getYAxisTickMarksColor styler)))
    (is (= "y3.0" (.apply (.getYAxisTickLabelsFormattingFunction styler) 3.0)))
    (is (.isYAxisLogarithmicDecadeOnly styler))
    (is (= (c/colors :magenta) (.getCursorColor styler)))
    (is (= 2.5 (.getCursorLineWidth styler)))
    (is (= font (.getCursorFont styler)))
    (is (= (c/colors :white) (.getCursorFontColor styler)))
    (is (= (c/colors :black) (.getCursorBackgroundColor styler)))
    (is (= "cx1.0" (.apply (.getCustomCursorXDataFormattingFunction styler) 1.0)))
    (is (= "cy4.0" (.apply (.getCustomCursorYDataFormattingFunction styler) 4.0)))
    (is (= "label" (.getLabel series)))
    (is (false? (.isEnabled series)))
    (is (= 1 (.getYAxisGroup series)))
    (is (= "0.00" (.getYAxisDecimalPattern series)))
    (is (.isSmooth series))))

(deftest explicit-series-colors
  (let [styler (.getStyler (c/xy-chart {"s" [[1] [2]]}
                                       {:series-colors [:red :blue]}))]
    (is (= [(c/colors :red) (c/colors :blue)]
           (vec (.getSeriesColors styler))))))

(deftest category-and-series-styler-options
  (let [chart (c/category-chart*
               [["s" {:x ["a" "b"] :y [1 2]
                       :style {:label "category" :enabled? false
                               :y-axis-group 2 :y-axis-decimal-pattern "0.0"
                               :smooth? true :overlapped? true}}]]
               {:show-stack-sum? true :bar-label-color :red
                :bar-label-rotation 30 :bar-label-position 0.75
                :overlapped? true :bar-label-automatic-contrast? true
                :bar-label-automatic-light-color :white
                :bar-label-automatic-dark-color :black})
        styler (.getStyler chart)
        series (.getSeries chart "s")]
    (is (.isShowStackSum styler))
    (is (= (c/colors :red) (.getLabelsFontColor styler)))
    (is (= 30 (.getLabelsRotation styler)))
    (is (= 0.75 (.getLabelsPosition styler)))
    (is (.isOverlapped styler))
    (is (.isLabelsFontColorAutomaticEnabled styler))
    (is (= (c/colors :white) (.getLabelsFontColorAutomaticLight styler)))
    (is (= (c/colors :black) (.getLabelsFontColorAutomaticDark styler)))
    (is (= "category" (.getLabel series)))
    (is (false? (.isEnabled series)))
    (is (= 2 (.getYAxisGroup series)))
    (is (= "0.0" (.getYAxisDecimalPattern series)))
    (is (.isSmooth series))
    (is (.isOverlapped series))))

(deftest bubble-series-common-options
  (let [chart (c/bubble-chart* {"s" {:x [1] :y [2] :bubble [3]
                                      :style {:label "bubble" :enabled? false
                                              :y-axis-group 1
                                              :y-axis-decimal-pattern "0.000"}}})
        series (.getSeries chart "s")]
    (is (= "bubble" (.getLabel series)))
    (is (false? (.isEnabled series)))
    (is (= 1 (.getYAxisGroup series)))
    (is (= "0.000" (.getYAxisDecimalPattern series)))))

(deftest pie-styler-options
  (let [font (Font. Font/SERIF Font/BOLD 14)
        chart (c/pie-chart
               {"s" {:value 4 :style {:label "slice" :enabled? false}}}
               {:sum-visible? true :sum-format "%.1f" :sum-font font
                :series-label-fn #(.getName %)
                :label-color :blue :label-automatic-contrast? true
                :label-automatic-light-color :white
                :label-automatic-dark-color :black
                :clockwise-direction :counter-clockwise
                :slice-border-width 2.5})
        styler (.getStyler chart)
        series (.getSeries chart "s")]
    (is (.isSumVisible styler))
    (is (= "%.1f" (.getSumFormat styler)))
    (is (= font (.getSumFont styler)))
    (is (= "s" (.apply (.getCustomSeriesLabelFunction styler) series)))
    (is (= (c/colors :blue) (.getLabelsFontColor styler)))
    (is (.isLabelsFontColorAutomaticEnabled styler))
    (is (= (c/colors :white) (.getLabelsFontColorAutomaticLight styler)))
    (is (= (c/colors :black) (.getLabelsFontColorAutomaticDark styler)))
    (is (= "COUNTER_CLOCKWISE" (str (.getClockwiseDirectionType styler))))
    (is (= 2.5 (.getSliceBorderWidth styler)))
    (is (= "slice" (.getLabel series)))
    (is (false? (.isEnabled series)))))

(defn- field-value
  [object field-name]
  (letfn [(lookup [class]
            (if class
              (try
                (let [field (.getDeclaredField class field-name)]
                  (.setAccessible field true)
                  (.get field object))
                (catch NoSuchFieldException _
                  (lookup (.getSuperclass class))))
              (throw (IllegalArgumentException. (str "No field " field-name)))))]
    (lookup (class object))))

(deftest annotation-coercion-and-helpers
  (let [add-one (ns-resolve 'com.hypirion.clj-xchart 'add-annotation!)
        add-many (ns-resolve 'com.hypirion.clj-xchart 'add-annotations!)]
    (is (some? add-one))
    (is (some? add-many))
    (when (and add-one add-many)
      (let [image (BufferedImage. 2 2 BufferedImage/TYPE_INT_ARGB)
            chart (c/xy-chart {"s" [[0 1] [1 2]]})
            text {:type :text :text "point" :x 1 :y 2}
            vertical {:type :line :value 3 :orientation :vertical
                      :coordinate-space :screen}
            horizontal {:type :horizontal-line :value 4}
            image-ann {:type :image :image image :x 5 :y 6 :screen-space? true}
            panel {:type :text-panel :lines ["one" "two"] :x 7 :y 8}
            returned-one (add-one chart text)
            returned-many (add-many chart [vertical horizontal image-ann panel])
            annotations (vec (field-value chart "annotations"))
            [text-object vertical-object horizontal-object image-object panel-object]
            annotations]
        (is (identical? chart returned-one))
        (is (identical? chart returned-many))
        (is (= 5 (count annotations)))
        (is (= "org.knowm.xchart.AnnotationText" (.getName (class text-object))))
        (is (= "point" (field-value text-object "text")))
        (is (= 1.0 (field-value text-object "x")))
        (is (= 2.0 (field-value text-object "y")))
        (is (false? (field-value text-object "isValueInScreenSpace")))
        (is (= "org.knowm.xchart.AnnotationLine" (.getName (class vertical-object))))
        (is (true? (field-value vertical-object "isVertical")))
        (is (= 3.0 (field-value vertical-object "value")))
        (is (true? (field-value vertical-object "isValueInScreenSpace")))
        (is (false? (field-value horizontal-object "isVertical")))
        (is (= "org.knowm.xchart.AnnotationImage" (.getName (class image-object))))
        (is (identical? image (field-value image-object "image")))
        (is (true? (field-value image-object "isValueInScreenSpace")))
        (is (= "org.knowm.xchart.AnnotationTextPanel" (.getName (class panel-object))))
        (is (= ["one" "two"] (vec (field-value panel-object "lines"))))))))

(deftest axes-constructors-add-annotations
  (let [spec [{:type :text :text "a" :x 0 :y 0}]
        charts [(c/xy-chart {"s" [[0] [1]]} {:annotations spec})
                (c/category-chart* [["s" [["x"] [1]]]] {:annotations spec})
                (c/bubble-chart* {"s" {:x [0] :y [1] :bubble [2]}}
                                 {:annotations spec})]]
    (doseq [chart charts]
      (is (= 1 (count (field-value chart "annotations")))))))
