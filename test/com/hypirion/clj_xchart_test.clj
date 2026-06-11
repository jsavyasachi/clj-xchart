(ns com.hypirion.clj-xchart-test
  (:require [clojure.test :refer :all]
            [com.hypirion.clj-xchart :as c]
            [com.hypirion.clj-xchart.opt :as opt]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

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
