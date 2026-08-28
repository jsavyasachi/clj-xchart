# Tutorial for clj-xchart

clj-xchart is a Clojure wrapper over the Java library
[XChart](http://knowm.org/open-source/xchart/). XChart is a small library that
plots data. If Incanter is too large for your plotting task, clj-xchart can be
an alternative.

clj-xchart has a small set of functions, but about 1 million render-style options.
This page does not describe them. See the
[render-options](render-options.md) page for the options that you can and cannot
configure.

To try clj-xchart, you can use
[lein-try](https://github.com/rkneufeld/lein-try):

```shell
$ lein try com.hypirion/clj-xchart
```

or [inlein](http://inlein.org/):

```clj
#!/usr/bin/env inlein

'{:dependencies [[org.clojure/clojure "1.12.0"]
                 [net.clojars.savya/clj-xchart "0.4.0"]]}

(require '[com.hypirion.clj-xchart :as c])

;; your code here
```

The code below assumes that you require the namespace `com.hypirion.clj-xchart`
and give it the alias `c`. Do this as in the inlein example above, or in a `ns`
form.

## Visualising

First, look at the different ways to save a chart and to show it on the screen.

`view` takes one or more charts and renders them in a swing frame. Use it during
development to make sure that the chart is correct. You can also use it to
compare the styling of two charts and find the best one.

```clj
(c/view my-chart)

(c/view my-chart1 my-chart2)
```

`to-bytes` takes a single chart and a format type. It returns a byte array of
the output. The format type is one of `:png`, `:gif`, `:bmp`,
`:jpg`/`:jpeg`, `:pdf`, `:svg`, and `:eps`.

```clj
(c/to-bytes my-chart :png)

Bitmap exports also accept `:dpi` and JPEG `:quality` options. Multiple charts
can be combined into one bitmap, and `to-output-stream` writes directly to an
open stream without closing it:

```clj
(c/to-bytes my-chart :png {:dpi 300})
(c/to-bytes my-chart :jpg {:quality 0.85})
(c/to-bytes [chart-a chart-b] :png {:rows 1})
(c/to-output-stream my-chart response-output-stream :png)
```

;; Example

(import '(java.io ByteArrayInputStream))

(defn svg-stats [request]
  (let [stat-chart (make-chart (:body request))]
    {:status-code 200
     :headers {"Content-Type" "image/svg+xml"}
     :body (ByteArrayInputStream.
            (c/to-bytes stat-chart :svg))}))
```

`spit` is a utility function and a chart variant of Clojure's own `spit`. It
takes a chart, a filename, and an optional format type, and writes the chart to
disk. If you do not give the format type, `spit` gets it from the filename
extension.

```clj
(c/spit my-chart "results.pdf")

(c/spit my-chart "no-suffix" :jpg)
```

For low-level use, you can use `as-buffered-image` to get a
`java.awt.image.BufferedImage` from the chart.

## XY-Charts

The simplest chart type is the XY-chart. It plots line plots. To create an
XY-chart, use the `xy-chart` function:

```clj
user=> (def chart
         (c/xy-chart {"Expected rate" [(range 10) (range 10)]
                      "Actual rate" [(range 10) (map #(+ % (rand-int 5) -2) (range 10))]}))
#'user/chart

user=> (c/view chart)
```

This shows a chart similar to this one:

![A basic XY-chart](imgs/basic-xy.png)

All functions that create charts start with the series they contain. The series
is a map from strings to the content of the series. The content depends on the
chart type that you want. For a simple xy-chart, the content is a vector of 2 or
3 sequences of numbers. The first sequence is the x values, the second is the y
values, and the optional last one is the error bars.

```clj
user=> (def series {"The Prediction" [[1 2 3] ;; X
                                      [2 4 6] ;; Y
                                      [0.2 0.9 0.6]]}) ;; error-bars (optional)
#'user/series
user=> (def error-bars (c/xy-chart series))
#'user/error-bars
user=> (c/view error-bars)
```

![A basic XY-chart with error bars](imgs/xy-error-bars.png)

The `view` function, which this page uses two times above, is a utility function
that renders the chart in a window. It is variadic. You can view more than one
chart in the same command and compare them. Use this to find the chart that
looks best:

```clj
user=> (c/view chart error-bars)
```

### Verbose

### Canonical Form

All series values are in a shorthand form or in a canonical form. Look again at
the content of the error-bars example:

```clj
{"The Prediction" [[1 2 3] ;; X
                   [2 4 6] ;; Y
                   [0.2 0.9 0.6]]})
```

You can write the same data like this:

```clj
{"The Prediction" {:x [1 2 3]
                   :y [2 4 6]
                   :error-bars [0.2 0.9 0.6]}}
```

The two forms are identical, but the second form is more self-describing. Use
the form that agrees with how you extract your data.

The canonical form can attach styling. The shorthand form cannot do this:

```clj
{"The Prediction" {:x [1 2 3]
                   :y [2 4 6]
                   :error-bars [0.2 0.9 0.6]
                   :style {:marker-type :triangle-up
                           :line-color :red}}}
```

This renders as follows:

![A basic XY-chart with styled error bars](imgs/styled-error-bars.png)

You _can_ attach styling for the full chart with
[render-options](render-options.md). In some conditions you can also attach a
style that is based on the input order. Your choice depends on your use case:
keep the styling with the data, or keep it separate.

## Category Charts

clj-xchart can also render category charts. Use `category-chart*` for them. The
most common type of category chart is the bar chart, but other types exist.

XY-charts and category charts have different inputs. The X-axis of a category
chart can be numbers, dates, or strings. The X-axis of an XY-chart can only be
numbers or dates.

The X-axis of a category chart is also not "sorted", and it does not show the
deltas. If the X-axis is `[100 -20]`, the chart renders 100 first, then -20. If
the X-axis is `[-20 -21 100]`, the distance between -20 and -21 is as large as
the distance between -21 and 100.

Look at an example:

```clj
user=> (def expected [["Food" "Savings" "Rent"]
                      [5.2 3.5 13.4]])
#'user/expected

user=> (def actual [["Food" "Savings" "Rent" "Unexpected"]
                    [5.5 2.5 13.4 1.0]])
#'user/actual

user=> (def chart (c/category-chart* {"Expected" expected
                                      "Actual" actual}))
#'user/chart

user=> (c/view chart)
```

![Image showing erroneous usage of category charts](imgs/category-chart-star.png)


This shows one of the many problems of the category chart: the chart does not
print "Unexpected". XChart appears to use only the rows in the first input
series. The example uses a map, thus you cannot be sure which series goes to
XChart first.

Another problem with the category chart is that data is often in a map with this form:

```clj
{"Food" 5.2
 "Savings" 3.5
 "Rent" 13.4}
```

instead of a vector of keys and a vector of vals. This form does not work with
the canonical form.

`category-chart` (without the `*`) is a convenience wrapper. It detects content
in the shape described above. It transforms the content into input that
`category-chart*` can handle.

Maps do not usually contain an order. Use the 2-arity version to set the order.
You can order the series and the x values:

```clj
user=> (def expected {"Food" 5.2
                      "Savings" 3.5
                      "Rent" 13.4})
#'user/expected

user=> (def actual {"Food" 5.5
                    "Savings" 2.5
                    "Rent" 13.4
                    "Unexpected" 1.0})

#'user/actual

user=> (def chart (c/category-chart {"Expected" expected
                                     "Actual" actual}
                                    {:series-order ["Expected" "Actual"]}))
#'user/chart

user=> (c/view chart)
```

![Image showing series order usage of category charts](imgs/category-chart-series-order.png)

Rows not included in the order are printed in alphanumeric order. If you give
no order, all rows are sorted alphanumerically.

```clj
user=> (def chart (c/category-chart {"Expected" expected
                                     "Actual" actual}
                                    {:x-axis {:order ["Rent" "Food"]}}))
#'user/chart

user=> (c/view chart)
```

![Image showing x axis order usage of category charts](imgs/category-chart-x-axis-order.png)

In this example, the series order is alphanumeric. The additional x-axis values
Savings and Unexpected are also sorted alphanumerically.

### Overlapping category charts

Use `:overlap?` to overlap the same data. Transpose the data to use this option:

```clj
(def rent {"Expected" 13.4, "Actual" 13.4})
(def food {"Expected" 5.2, "Actual" 5.5})
(def savings {"Expected" 3.5, "Actual" 2.5})
(def unexpected {"Actual" 1.0})

user=> (def chart (c/category-chart {"Food" food
                                     "Rent" rent
                                     "Savings" savings
                                     "Unexpected" unexpected}
                                     {:overlap? true
                                      :x-axis {:order ["Expected" "Actual"]}
                                      :series-order ["Rent" "Food" "Savings" "Unexpected"]}))
#'user/chart

user=> (c/view chart)
```

![Image showing an overlapped category chart](imgs/category-chart-overlap.png)

Overlap is not the same as a stacked chart. An overlap can fully paint over
another series. Reorder the series to ``["Food" "Rent" "Savings" "Unexpected"]`
to see this result:

![Image showing a badly overlapped category chart](imgs/category-chart-overlap-bad.png)

Use overlap only when you know the data and set the correct order.

### Stacked category charts

Stacked makes the "categorical" series stack on top of eachother instead of
stacking beside eachother:

```clj
(c/view
 (c/category-chart
  {"Bananas" {"Mon" 6, "Tue" 2, "Fri" 3, "Wed" 1, "Thur" 3}
   "Apples" {"Tue" 3, "Wed" 5, "Fri" 1, "Mon" 1}
   "Pears" {"Thur" 1, "Mon" 3, "Fri" 4, "Wed" 1}}
  {:title "Weekly Fruit Sales"
   :width 640
   :height 500
   :stacked? true
   :x-axis {:order ["Mon" "Tue" "Wed" "Thur" "Fri"]}}))
```

![Basic stacked category chart](imgs/stacked-category-chart.png)


## Bubble Chart

Create bubble charts with `bubble-chart*`. It works like an XY chart, but it
uses required bubble data instead of error bars.

`bubble-chart*` is for low-level use. There is no high-level function named
`bubble-chart` because its input and output format is not yet defined.

The bubble data for `bubble-chart*` is the rendered bubble _diameter in pixels_.
Bubble size does not scale with the width or height of the chart.

People often compare bubbles by total _area_, not _diameter_. In
`bubble-chart*`, 20.0 has four times the area of 10.0. Map bubble data through
`Math/sqrt` before you use it in the chart.

Bubble size can be very large or very small, based on the input values. Set the
largest bubble value, _b-max_, to a selected bubble diameter, _max-diameter_, in
pixels. Find _b-max_ for all series. Then scale every bubble value with this
expression:

```clj
(fn [b] (* max-diameter (Math/sqrt (/ b b-max))))
```

Do not use this as a general rule. Bubble size is relative to the chart content.
If the data changes over time, the changing scale can confuse readers. For sales
data, use a constant that you select when you make the charts.

Use bubble charts carefully with the current implementation.

This example uses bubble charts.

This example has two heuristics for an NP-complete task scheduling algorithm.
One uses taboo search. The other uses simulated annealing. For different input
sizes, the data has total task cost and task completion time. The chart shows
both values.

```clj
(def taboo
  {50 {:cost 0.5
       :duration 567}
   2500 {:cost 23.4
         :duration 24291}
   125000 {:cost 1281
           :duration 1299568}
   6250000 {:cost 70102
            :duration 54653212}})

(def simulated-annealing
  {50 {:cost 0.51
       :duration 560}
   2500 {:cost 26.4
         :duration 23102}
   125000 {:cost 1821
           :duration 1182343}
   6250000 {:cost 83613
            :duration 47293720}})
```

The input sizes are the keys. Divide total cost and duration by the total number
of tasks. The y-axis shows task completion time. Bubble size shows cost. A
smaller bubble is better. The example selected the constant 500 by trial and
error.

```clj
(defn bubblify
  [m]
  {:x (keys m)
   :y (map (fn [input prop] (/ (:duration prop) input))
           (keys m) (vals m))
   :bubble (map (fn [input prop] (* 500 (Math/sqrt (/ (:cost prop) input))))
                (keys m) (vals m))})
```

Set `[:x-axis :logarithmic?]` to true in the style map to use a logarithmic
x-axis:

```clj
(c/view
 (c/bubble-chart*
  {"Taboo" (bubblify taboo)
   "Simulated Annealing" (bubblify simulated-annealing)}
  {:title "Heuristic comparison"
   :legend {:position :inside-ne}
   :y-axis {:title "Task completion time (s/task)"}
   :x-axis {:title "Number of tasks"
            :logarithmic? true}}))
```

![Image of a sample bubble chart](imgs/np-bubble.png)

This example puts the legend inside the plot.

## Pie Charts

Pass a map of strings to numbers to create a pie chart:

```clj
(c/view
 (c/pie-chart {"Spaces" 400
               "Tabs" 310
               "A mix of both" 2}))
```

![Image of a sample pie chart](imgs/basic-pie-chart.png)

When an entry is very small, its percentage is not shown. You can disable this
behavior. See
[render-options](render-options.md).

## Utility Functions

clj-xchart has two data transformation functions for conforming series.

### `extract-series`

Data can be grouped in a form that is awkward for clj-xchart. The `:x`, `:y`,
and `:error-bar`/`:bubble` content is separate.

Keep the data in pairs or maps, for example:

```clj
(def pairs
  [[1 1]
   [2 2]
   [3 3]])

(def maps
  [{:cpu-usage 55.0, :time #inst "2016-10-11T22:22:18.771-00:00"}
   {:cpu-usage 68.0, :time #inst "2016-10-11T22:22:19.753-00:00"}
   ...])
```

Separate these values to use clj-xchart. `extract-series` does this. Its first
argument is a map of extraction functions. Its second argument is a collection
of values:

```clj
(c/extract-series
  {:x first
   :y second}
  pairs)
=> {:x (1 2 3)
    :y (1 2 3)}

(c/extract-series
  {:x :cpu-usage
   :y :time}
  maps)
=> {:x (55.0 68.0 ...)
    :y (#inst "2016-10-11T22:22:18.771-00:00" #inst "2016-10-11T22:22:19.753-00:00" ...)}
```

Provide the keys you need. Add an entry to compute `:bubble`. Do not add `:x`
when you compute it by another method.

### `transpose-map`

Use `transpose-map` for surveys or other nested maps when you want to invert the
y- and x-axes. It switches the outer and inner keys. This example uses part of
the Clojure survey:

```clj
(c/view
 (c/category-chart
  (c/transpose-map
   {"Easy to find?" {"True" 1329,
                     "False" 47,
                     "Mixed bag" 830},
    "Active maintainers?" {"True" 1049,
                           "False" 32,
                           "Mixed bag" 1015},
    "Accurate + good docs?" {"True" 435,
                             "False" 295,
                             "Mixed bag" 1463},
    "Good quality?" {"True" 1221,
                     "False" 36,
                     "Mixed bag" 910}})
  {:title "Excerpt from the State of Clojure Survey 2015"
   :render-style :stick
   :y-axis {:ticks-visible? false}
   :x-axis {:label {:rotation 30}}}))
```

![Excerpt from the State of Clojure Survey 2015](imgs/excerpt-clojure-survey-2015.png)


## Gotchas

### PDF Support

PDF support can be slow and can fail on Java 1.6. Check performance before you
use PDF in production. The other vector formats work correctly.

### View and Mutable Size

`view` can change chart dimensions. If you view a chart, resize its window, and
write it to a file, the size can differ from the selected size.

### Line Chart and X/Y ordering

Line charts are effectively polylines, so x- and y-value order matters. The
values are not sorted before the chart renders. This example shows the result:

```clj
(defn log-spiral-x [a b t]
  (* a (Math/exp (* b t)) (Math/cos t)))
(defn log-spiral-y [a b t]
  (* a (Math/exp (* b t)) (Math/sin t)))

(c/view (c/xy-chart
         {"curve" {:x (cons 0 (map #(+ 2 (log-spiral-x -0.2 0.2 %))
                                   (range 10.5 0 -0.1)))
                   :y (cons 0 (map #(+ 4 (log-spiral-y 0.2 0.2 %))
                                   (range 10.5 0 -0.1)))
                   :style {:marker-type :none}}}
         {:title "Learning Curve for Emacs"
          :width 640
          :height 500
          :legend {:visible? false}
          :axis {:ticks {:visible? false}}}))
```

![Emacs Learning Curve](imgs/emacs-learning-curve.png)

This behavior can be useful for charts with parametric forms. Select the order
for other line charts. Scatter and bubble charts do not have this issue.

### Many Datapoints

#### The Opt Namespace

Use `com.hypirion.clj-xchart.opt` to create immutable views of lists and vectors
without duplicating data. Unlike `map` and `mapv`, these views do not duplicate
data.

Use `extract-series` from this namespace:

```clj
(ns ...
  (:require [com.hypirion.clj-xchart :as c]
            [com.hypirion.clj-xchart.opt :as c-opt]))

(c/spit
  (c/xy-chart {"foo" (c-opt/extract-series
                       {:x :foo :y :bar}
                       my-data)})
  my-filename)
```

You can also use `extract-field`. It works like `map` but is immutable.

These views do not duplicate data. They recompute a value when a user looks it
up again. This is usually not a problem when you map fields in a structure. If
you compute values, check performance.

#### Shrinking the Dataset

More data points use more XChart memory. If data points are evenly spaced, use
at most 2000 data points. Group more points. This library has no grouped-value
option because the required value, such as max, min, or average, differs.

```clj
(defn avg [coll]
  (double (/ (reduce + coll) (count coll))))

(defn chunkify
  [coll chunk-size]
  (map avg (partition-all chunk-size coll)))

(defn shrink-series
  "Assumes the series is on the form {:x [] :y [], ...} and x values
  are ordered."
  [series]
  (let [goal 2000
        current (count (:x series))]
    (if (<= current goal)
      series
      (let [chunk-size (int (Math/ceil (/ current goal)))]
        (-> series
            (update :x chunkify chunk-size)
            (update :y chunkify chunk-size))))))
```

This function is not yet part of the library.
