(ns com.hypirion.clj-xchart.tablecloth
  "Optional tablecloth adapter. No tablecloth dependency is required by clj-xchart.")

(defn from-columns
  "Builds a clj-xchart series map from a tablecloth-compatible column map.
  With tablecloth installed, TABLE may also be a dataset accepted by
  `tablecloth.api/column`."
  [table keymap]
  (if (map? table)
    (into {} (map (fn [[series-key column-key]]
                    [series-key (get table column-key)]) keymap))
    (let [column (requiring-resolve 'tablecloth.api/column)]
      (into {} (map (fn [[series-key column-key]]
                      [series-key (column table column-key)]) keymap)))))
