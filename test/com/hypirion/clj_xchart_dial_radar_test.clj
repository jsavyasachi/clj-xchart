(ns com.hypirion.clj-xchart-dial-radar-test
  (:require [clojure.test :refer :all]
            [com.hypirion.clj-xchart :as c])
  (:import (java.awt Font)
           (org.knowm.xchart DialChart DialSeries RadarChart RadarSeries)
           (org.knowm.xchart.style DialStyler RadarStyler)))

(deftest dial-chart-series-and-styling
  (let [font (Font. Font/SANS_SERIF Font/BOLD 13)
        ^DialChart chart (c/dial-chart
                   {"speed" {:value 0.72
                              :label "72 km/h"
                              :style {:fill-color :cyan
                                      :show-in-legend? false
                                      :enabled? true}}}
                   {:width 720
                    :height 480
                    :title "Speed"
                    :circular? false
                    :axis-tick-values [0.0 0.5 1.0]
                    :axis-tick-labels ["0" "50" "100"]
                    :axis-tick-labels-visible? false
                    :axis-tick-marks-visible? false
                    :axis-tick-marks-color :black
                    :axis-tick-marks-stroke :dash-dash
                    :axis-title-visible? false
                    :axis-title-font font
                    :axis-title-padding 9
                    :lower-from 0.0
                    :lower-to 0.3
                    :lower-color :green
                    :middle-from 0.3
                    :middle-to 0.7
                    :middle-color :yellow
                    :upper-from 0.7
                    :upper-to 1.0
                    :upper-color :red
                    :arc-angle 250.0
                    :donut-thickness 0.25
                    :arrow-length-percentage 0.8
                    :arrow-arc-angle 18.0
                    :arrow-arc-percentage 0.06
                    :arrow-color :blue
                    :label-visible? true
                    :label-font font})
        ^DialSeries series (.getSeries chart "speed")
        ^DialStyler styler (.getStyler chart)]
        (is (= 720 (.getWidth chart)))
        (is (= 480 (.getHeight chart)))
        (is (= "Speed" (.getTitle chart)))
        (is (= 0.72 (.getValue series)))
        (is (= "72 km/h" (.getLabel series)))
        (is (= (c/colors :cyan) (.getFillColor series)))
        (is (false? (.isShowInLegend series)))
        (is (.isEnabled series))
        (is (false? (.isCircular styler)))
        (is (= [0.0 0.5 1.0] (vec (.getAxisTickValues styler))))
        (is (= ["0" "50" "100"] (vec (.getAxisTickLabels styler))))
        (is (false? (.isAxisTickLabelsVisible styler)))
        (is (false? (.isAxisTicksMarksVisible styler)))
        (is (= (c/colors :black) (.getAxisTickMarksColor styler)))
        (is (= (c/strokes :dash-dash) (.getAxisTickMarksStroke styler)))
        (is (false? (.isAxisTitleVisible styler)))
        (is (= font (.getAxisTitleFont styler)))
        (is (= 9 (.getAxisTitlePadding styler)))
        (is (= 0.0 (.getLowerFrom styler)))
        (is (= 0.3 (.getLowerTo styler)))
        (is (= (c/colors :green) (.getLowerColor styler)))
        (is (= 0.3 (.getMiddleFrom styler)))
        (is (= 0.7 (.getMiddleTo styler)))
        (is (= (c/colors :yellow) (.getMiddleColor styler)))
        (is (= 0.7 (.getUpperFrom styler)))
        (is (= 1.0 (.getUpperTo styler)))
        (is (= (c/colors :red) (.getUpperColor styler)))
        (is (= 250.0 (.getArcAngle styler)))
        (is (= 0.25 (.getDonutThickness styler)))
        (is (= 0.8 (.getArrowLengthPercentage styler)))
        (is (= 18.0 (.getArrowArcAngle styler)))
        (is (= 0.06 (.getArrowArcPercentage styler)))
        (is (= (c/colors :blue) (.getArrowColor styler)))
    (is (.isLabelsVisible styler))
    (is (= font (.getLabelsFont styler)))))

(deftest radar-chart-series-and-styling
  (let [font (Font. Font/SERIF Font/ITALIC 12)
        ^RadarChart chart (c/radar-chart
                   {"plain" [0.2 0.4 0.6]
                    "styled" {:values [0.8 0.5 0.9]
                              :tooltips ["fast" "steady" "strong"]
                              :style {:fill-color :cyan
                                      :line-color :red
                                      :line-style :dash-dot
                                      :line-width 3.5
                                      :marker-color :blue
                                      :marker-type :diamond
                                      :show-in-legend? false
                                      :enabled? false}}}
                   {:width 700
                    :height 450
                    :title "Profile"
                    :radii-labels ["Speed" "Control" "Power"]
                    :render-style :circle
                    :circular? false
                    :start-angle 45.0
                    :marker-size 11
                    :radii-tick-marks-visible? false
                    :radii-tick-marks-color :green
                    :radii-tick-marks-stroke :dot-dot
                    :radii-tick-marks-count 7
                    :radii-title-visible? false
                    :radii-title-font font
                    :radii-title-padding 8
                    :series-filled? true})
        ^RadarSeries plain (.getSeries chart "plain")
        ^RadarSeries styled (.getSeries chart "styled")
        ^RadarStyler styler (.getStyler chart)]
        (is (= ["Speed" "Control" "Power"] (vec (.getRadiiLabels chart))))
        (is (= [0.2 0.4 0.6] (vec (.getValues plain))))
        (is (= [0.8 0.5 0.9] (vec (.getValues styled))))
        (is (= ["fast" "steady" "strong"]
               (vec (.getTooltipOverrides styled))))
        (is (= (c/colors :cyan) (.getFillColor styled)))
        (is (= (c/colors :red) (.getLineColor styled)))
        (is (= (c/strokes :dash-dot) (.getLineStyle styled)))
        (is (= 3.5 (.getLineWidth styled)))
        (is (= (c/colors :blue) (.getMarkerColor styled)))
        (is (= (c/markers :diamond) (.getMarker styled)))
        (is (false? (.isShowInLegend styled)))
        (is (false? (.isEnabled styled)))
        (is (= (c/radar-render-styles :circle) (.getRadarRenderStyle styler)))
        (is (false? (.isCircular styler)))
        (is (= 45.0 (.getStartAngleInDegrees styler)))
        (is (= 11 (.getMarkerSize styler)))
        (is (false? (.isRadiiTicksMarksVisible styler)))
        (is (= (c/colors :green) (.getRadiiTickMarksColor styler)))
        (is (= (c/strokes :dot-dot) (.getRadiiTickMarksStroke styler)))
        (is (= 7 (.getRadiiTickMarksCount styler)))
        (is (false? (.isRadiiTitleVisible styler)))
    (is (= font (.getRadiiTitleFont styler)))
    (is (= 8 (.getRadiiTitlePadding styler)))
    (is (.isSeriesFilled styler))))

(deftest dial-and-radar-constructor-contracts
  (let [^DialChart dial (c/dial-chart {"plain" 0.42})
        ^RadarChart radar (c/radar-chart {"plain" [0.1 0.2]}
                                         {:radii-labels ["A" "B"]})]
    (is (= 640 (.getWidth dial)))
    (is (= 500 (.getHeight dial)))
    (is (= 0.42 (.getValue ^DialSeries (.getSeries dial "plain"))))
    (is (= 640 (.getWidth radar)))
    (is (= 500 (.getHeight radar)))
    (is (= ["A" "B"] (vec (.getRadiiLabels radar)))))
  (is (thrown? AssertionError
               (c/radar-chart {"plain" [0.1 0.2]} {}))))
