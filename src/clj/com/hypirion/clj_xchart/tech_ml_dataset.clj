(ns com.hypirion.clj-xchart.tech-ml-dataset
  "Optional tech.ml.dataset adapter. No dataset dependency is required by clj-xchart.")

(defn from-columns
  "Builds a clj-xchart series map from a tech.ml.dataset-compatible dataset.
  A map of columns is accepted for dependency-free tests and simple pipelines."
  [dataset keymap]
  (if (map? dataset)
    (into {} (map (fn [[series-key column-key]]
                    [series-key (get dataset column-key)]) keymap))
    (let [column (requiring-resolve 'tech.v3.dataset/column)]
      (into {} (map (fn [[series-key column-key]]
                      [series-key (column dataset column-key)]) keymap)))))
