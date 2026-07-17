(ns com.hypirion.clj-xchart-box-hbar-test
  (:require [clojure.test :refer :all]
            [com.hypirion.clj-xchart :as c])
  (:import (java.awt Font)
           (org.knowm.xchart.style BoxStyler$BoxplotCalCulationMethod)))

(deftest box-chart-series-and-styling
  (let [chart (c/box-chart
               {"plain" [1 2 3]
                "styled" {:values [4 5 6]
                          :style {:label "quartiles"
                                  :enabled? false
                                  :fill-color :red
                                  :show-in-legend? true}}}
               {:width 720
                :height 480
                :title "Boxes"
                :box-plot-calculation-method :n-less-1-plus-1
                :box-width-fraction 0.65
                :chart {:background-color :white}
                :x-axis {:title "sample"}
                :y-axis {:title "value" :max 10}
                :annotations [{:type :horizontal-line :value 3}]})
        styler (.getStyler chart)
        plain (.getSeries chart "plain")
        styled (.getSeries chart "styled")]
    (is (= 720 (.getWidth chart)))
    (is (= 480 (.getHeight chart)))
    (is (= #{"plain" "styled"}
           (set (map #(.getName %) (.getSeriesCollection chart)))))
    (is (= [1.0 2.0 3.0] (vec (.getYData plain))))
    (is (= BoxStyler$BoxplotCalCulationMethod/N_LESS_1_PLUS_1
           (.getBoxplotCalCulationMethod styler)))
    (is (= 0.65 (.getBoxWidthFraction styler)))
    (is (= (c/colors :white) (.getChartBackgroundColor styler)))
    (is (= 10.0 (.getYAxisMax styler)))
    (is (= "quartiles" (.getLabel styled)))
    (is (false? (.isEnabled styled)))
    (is (= (c/colors :red) (.getFillColor styled)))
    (is (.isShowInLegend styled))))

(deftest horizontal-bar-chart-preserves-upstream-orientation
  (let [font (Font. Font/SANS_SERIF Font/BOLD 13)
        chart (c/horizontal-bar-chart
               {"plain" [[10 20] ["a" "b"]]
                "styled" {:x [30 40]
                          :y ["c" "d"]
                          :style {:label "horizontal"
                                  :fill-color :blue
                                  :show-in-legend? false}}}
               {:available-space-fill 0.8
                :bar-label-visible? true
                :bar-label-font font
                :bar-label-color :red
                :bar-label-rotation 15
                :bar-label-position 0.4
                :bar-label-automatic-contrast? true
                :annotations [{:type :vertical-line :value 25}]})
        styler (.getStyler chart)
        plain (.getSeries chart "plain")
        styled (.getSeries chart "styled")]
    (is (= [10 20] (vec (.getXData plain))))
    (is (= ["a" "b"] (vec (.getYData plain))))
    (is (= 0.8 (.getAvailableSpaceFill styler)))
    (is (.isLabelsVisible styler))
    (is (= font (.getLabelsFont styler)))
    (is (= (c/colors :red) (.getLabelsFontColor styler)))
    (is (= 15 (.getLabelsRotation styler)))
    (is (= 0.4 (.getLabelsPosition styler)))
    (is (.isLabelsFontColorAutomaticEnabled styler))
    (is (= "horizontal" (.getLabel styled)))
    (is (= (c/colors :blue) (.getFillColor styled)))
    (is (false? (.isShowInLegend styled)))))
