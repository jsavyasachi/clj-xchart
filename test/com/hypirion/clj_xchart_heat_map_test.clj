(ns com.hypirion.clj-xchart-heat-map-test
  (:require [clojure.test :refer :all]
            [com.hypirion.clj-xchart :as c])
  (:import (java.awt Font)
           (org.knowm.xchart HeatMapChart HeatMapSeries)
           (org.knowm.xchart.style HeatMapStyler)))

(set! *warn-on-reflection* true)

(deftest heat-map-chart-coerces-matrix-rows-and-styles
  (let [constructor (ns-resolve 'com.hypirion.clj-xchart 'heat-map-chart)]
    (is (some? constructor))
    (when constructor
      (let [font (Font. Font/SANS_SERIF Font/BOLD 13)
            ^HeatMapChart chart
            (constructor
             {"temperature" {:x-labels ["Mon" "Tue"]
                             :y-labels ["AM" "PM"]
                             :heat-data [(int-array [1 2]) [3 4]]}}
             {:piecewise? true
              :piecewise-ranged? true
              :split-count 4
              :range-colors [:blue :white :red]
              :draw-border? false
              :show-value? true
              :value-font font
              :value-font-color :black
              :value-decimal-pattern "0.0"
              :value-formatter #(str "v=" %)
              :min 1
              :max 4
              :gradient-legend-width 24
              :gradient-legend-height 180})
            ^HeatMapSeries series (.getSeries chart "temperature")
            ^HeatMapStyler styler (.getStyler chart)]
        (is (some? series))
        (is (= ["Mon" "Tue"] (vec (.getXData series))))
        (is (= ["AM" "PM"] (vec (.getYData series))))
        (is (= [[0 0 1] [0 1 2] [1 0 3] [1 1 4]]
               (mapv vec (.getHeatData series))))
        (is (.isPiecewise styler))
        (is (.isPiecewiseRanged styler))
        (is (= 4 (.getSplitNumber styler)))
        (is (= [(c/colors :blue) (c/colors :white) (c/colors :red)]
               (vec (.getRangeColors styler))))
        (is (false? (.isDrawBorder styler)))
        (is (.isShowValue styler))
        (is (= font (.getValueFont styler)))
        (is (= (c/colors :black) (.getValueFontColor styler)))
        (is (= "0.0" (.getHeatMapValueDecimalPattern styler)))
        (is (= "v=2.5"
               (.apply (.getHeatMapDecimalValueFormatter styler) 2.5)))
        (is (= 1.0 (.getMin styler)))
        (is (= 4.0 (.getMax styler)))
        (is (= 24 (.getGradientColorColumnWeight styler)))
        (is (= 180 (.getGradientColorColumnHeight styler)))))))

(deftest heat-map-chart-coerces-number-rows
  (let [constructor (ns-resolve 'com.hypirion.clj-xchart 'heat-map-chart)]
    (is (some? constructor))
    (when constructor
      (let [^HeatMapChart chart
            (constructor
             {"measurements" [["a" "b"]
                              ["low" "high"]
                              [(into-array Number [0 0 1.5])
                               [1 1 2.75]]]})
            ^HeatMapSeries series (.getSeries chart "measurements")]
        (is (= [[0 0 1.5] [1 1 2.75]]
               (mapv vec (.getHeatData series))))))))

(deftest heat-map-chart-rejects-additional-series-early
  (let [constructor (ns-resolve 'com.hypirion.clj-xchart 'heat-map-chart)]
    (is (some? constructor))
    (when constructor
      (let [data {:x-labels ["x"] :y-labels ["y"] :heat-data [[1]]}
            ^HeatMapChart chart (constructor {"first" data})]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"exactly one series"
                              (constructor {"first" data "second" data})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"already has a series"
                              (c/add-series! chart "second" data)))))))
