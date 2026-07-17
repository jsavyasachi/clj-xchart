(ns com.hypirion.clj-xchart-ohlc-test
  (:require [clojure.test :refer :all]
            [com.hypirion.clj-xchart :as c])
  (:import (org.knowm.xchart OHLCChart OHLCSeries)
           (org.knowm.xchart.style OHLCStyler)))

(defn- field-value
  [object field-name]
  (letfn [(lookup [^Class class]
            (if class
              (try
                (let [^java.lang.reflect.Field field
                      (.getDeclaredField class field-name)]
                  (.setAccessible field true)
                  (.get field object))
                (catch NoSuchFieldException _
                  (lookup (.getSuperclass class))))
              (throw (IllegalArgumentException. (str "No field " field-name)))))]
    (lookup (class object))))

(deftest ohlc-chart-adds-x-and-ohlc-series-with-styling
  (let [ohlc-chart-var (ns-resolve 'com.hypirion.clj-xchart 'ohlc-chart)]
    (is (some? ohlc-chart-var))
    (when ohlc-chart-var
      (let [^OHLCChart chart
            (ohlc-chart-var
             {"prices" {:x [1 2 3]
                        :open [10 12 11]
                        :high [13 14 15]
                        :low [9 10 10]
                        :close [12 11 14]
                        :volume [100 200 150]
                        :style {:render-style :hilo
                                :up-color :green
                                :down-color :red
                                :marker-type :diamond
                                :marker-color :blue
                                :line-style :dash-dash
                                :line-color :black
                                :line-width 2.5
                                :show-in-legend? false
                                :label "Price"
                                :y-axis-group 1}}
              "array-prices" {:x (double-array [1 2])
                              :open (double-array [20 21])
                              :high (double-array [22 23])
                              :low (double-array [19 20])
                              :close (double-array [21 22])
                              :volume (long-array [300 400])}}
             {:width 800
              :height 600
              :title "OHLC"
              :render-style :candle
              :legend {:visible? false}
              :x-axis {:title "Time" :max 4}
              :y-axis {:title "Price"}
              :annotations [{:type :horizontal-line :value 12}]})
            ^OHLCSeries series (.getSeries chart "prices")
            ^OHLCSeries array-series (.getSeries chart "array-prices")
            ^OHLCStyler styler (.getStyler chart)]
        (is (= 800 (.getWidth chart)))
        (is (= 600 (.getHeight chart)))
        (is (= "OHLC" (.getTitle chart)))
        (is (= "Time" (.getXAxisTitle chart)))
        (is (= "Price" (.getYAxisTitle chart)))
        (is (= 4.0 (.getXAxisMax styler)))
        (is (false? (.isLegendVisible styler)))
        (is (= (c/ohlc-render-styles :candle)
               (.getDefaultSeriesRenderStyle styler)))
        (is (= [1.0 2.0 3.0] (vec (.getXData series))))
        (is (= [10.0 12.0 11.0] (vec (.getOpenData series))))
        (is (= [100 200 150] (vec (.getVolumeData series))))
        (is (= [300 400] (vec (.getVolumeData array-series))))
        (is (= (c/ohlc-render-styles :hilo)
               (.getOhlcSeriesRenderStyle series)))
        (is (= (c/colors :green) (.getUpColor series)))
        (is (= (c/colors :red) (.getDownColor series)))
        (is (= (c/markers :diamond) (.getMarker series)))
        (is (= (c/colors :blue) (.getMarkerColor series)))
        (is (= (c/strokes :dash-dash) (.getLineStyle series)))
        (is (= (c/colors :black) (.getLineColor series)))
        (is (= 2.5 (.getLineWidth series)))
        (is (false? (.isShowInLegend series)))
        (is (= "Price" (.getLabel series)))
        (is (= 1 (.getYAxisGroup series)))
        (is (= 1 (count (field-value chart "annotations"))))))))

(deftest ohlc-chart-supports-ohlc-without-x-and-line-data
  (let [ohlc-chart-var (ns-resolve 'com.hypirion.clj-xchart 'ohlc-chart)]
    (is (some? ohlc-chart-var))
    (when ohlc-chart-var
      (let [^OHLCChart chart
            (ohlc-chart-var
             {"implicit-x" {:open [10 11]
                            :high [12 13]
                            :low [9 10]
                            :close [11 12]}
              "average" {:x [1 2] :y [10.5 11.5]
                         :style {:render-style :line}}})
            ^OHLCSeries implicit-x (.getSeries chart "implicit-x")
            ^OHLCSeries average (.getSeries chart "average")]
        (is (= [1.0 2.0] (vec (.getXData implicit-x))))
        (is (= [10.0 11.0] (vec (.getOpenData implicit-x))))
        (is (= [1.0 2.0] (vec (.getXData average))))
        (is (= [10.5 11.5] (vec (.getYData average))))
        (is (= (c/ohlc-render-styles :line)
               (.getOhlcSeriesRenderStyle average)))))))

(deftest ohlc-chart-rejects-unsupported-series-shapes
  (let [ohlc-chart-var (ns-resolve 'com.hypirion.clj-xchart 'ohlc-chart)]
    (is (some? ohlc-chart-var))
    (when ohlc-chart-var
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Unsupported OHLC series shape"
           (ohlc-chart-var {"missing-low" {:x [1]
                                           :open [1]
                                           :high [2]
                                           :close [1.5]}})))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Unsupported OHLC series shape"
           (ohlc-chart-var {"volume-without-x" {:open [1]
                                                :high [2]
                                                :low [0]
                                                :close [1.5]
                                                :volume [10]}}))))))
