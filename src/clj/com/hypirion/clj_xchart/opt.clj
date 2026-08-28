(ns com.hypirion.clj-xchart.opt
  "A namespace for clj-xchart with optimizations for large datasets."
  (:import (com.hypirion.clj_xchart ArrayMapping ListMapping)))

(defn extract-field
  "Returns an immutable view of a sequence mapped by field. Field can be a
  function, but is usually a keyword. The immutable view uses no additional
  memory. It calls field more than once for the same key.

  Here, immutable means that it cannot be updated, even persistently."
  [field list]
  (ListMapping. list field))

(defn extract-series
  "Transforms coll into a series map. It uses the values in keymap with
  extract-field. You do not need to provide :x, :y, or any key.

  Example: (extract-series {:x f, :y g, :bubble bubble} coll)
        == {:x (extract-field f coll),
            :y (extract-field g coll),
            :bubble (extract-field bubble coll)}"
  [keymap coll]
  (into {}
        (for [[k v] keymap]
          [k (extract-field v coll)])))

(defn extract-array-field
  "Returns a lazy List view over a Java array, including primitive arrays."
  [field array]
  (ArrayMapping. array field))

(defn extract-array-series
  "Builds a series map from an array of records without copying the array or
  its fields before XChart consumes them."
  [keymap array]
  (into {}
        (for [[k field] keymap]
          [k (extract-array-field field array)])))

(defn extract-reducible-series
  "Builds lazy field sequences from any reducible collection. This adapter
  deliberately returns reducible lazy sequences and does not materialize the
  source collection; use `extract-series` when XChart requires indexed Lists."
  [keymap reducible]
  (into {}
        (for [[k field] keymap]
          [k (eduction (map field) reducible)])))
