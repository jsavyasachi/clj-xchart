# Advanced Examples

## Sparklines

[According to Wikipedia](https://en.wikipedia.org/wiki/Sparkline):

> A sparkline is a very small line chart, typically drawn without axes or
> coordinates. It presents the general shape of the variation (typically over
> time) in some measurement, such as temperature or stock market price, in a
> simple and highly condensed way.

Use sparklines in status dashboards for your servers. At
[status.github.com](https://status.github.com), charts resemble sparklines:

![Mean response time over at GitHub](imgs/gh-mean-response.png)

If you have hundreds of servers, large charts are hard to compare. A chart with
all series is difficult to read. Sparklines can make comparison easier.

You can create sparklines in clj-xchart. Change the default styles. The default
line width and marker size are too large for small charts, so this example
reduces them.

This example marks the current value. You can remove the marker.

```clj
(let [ys (repeatedly 100 #(* (- 0.5 (rand)) (rand)))]
  (c/spit
   (c/xy-chart {"line" {:x (range 100)
                        :y ys
                        :style {:marker-type :none
                                :line-color :black
                                :line-width 0.5}}
                "marker" {:x [99]
                          :y [(last ys)]
                          :style {:marker-color :red
                                  :marker-style :diamond}}}
               {:width 150
                :height 24
                :marker {:size 3}
                :legend {:visible? false}
                :axis {:ticks {:visible? false}}
                :chart {:padding 0}
                :plot {:margin 0
                       :border-visible? false
                       :background-color :white
                       :grid-lines {:visible? false
                                    :content-size 1.0}}})
   "sparkline.png"))
```

The code above creates this sparkline:
![standard looking sparkline](imgs/default-sparkline.png). If you prefer a more
terminal-like color scheme, replace the line's `:line-color` with `:green`.
Change the marker's `:marker-color` to `:magenta`. Change the plot's
`:background-color` to `:black`. The sparkline then looks like this:
![sparkline with terminal colors](imgs/hacker-sparkline.png).

## GitHub System Status

GitHub uses D3 to display its status pages. The result is noninteractive, so
clj-xchart can render similar pages. Server performance can differ.

To render this example, open
[GitHub System Status](https://status.github.com/) and inspect the source. It
contains divs with a `data-string` attribute. The attribute is a JSON array of
values. For example: `[[1477049100.0, 32.40260003567365],
[1477049400.0, 35.22429803658118] ...]`). This is actually valid Clojure as
well. Copy one of these values and run

```clj
(def data ... ) ;; your long array of data here
```

to use the example below.

The GitHub data has the number of seconds since the epoch (January 1, 1970,
00:00:00 GMT) and the value at that time. Multiply the number of seconds by
1000. Then pass it to java.util.Date:

```clj
(import '(java.util Date))

;; in the xy-chart series generation:
(c/extract-series
 {:x (comp #(Date. (* 1000 %)) long first)
  :y second}
 data)
```

Change the font size with the Java Font class. This example uses this font:

```clj
(import '(java.awt Font))

(def small-plain (Font. Font/SANS_SERIF Font/PLAIN 10))
```

The default grid lines are dashed. `:solid` makes the grid lines too large.
XChart has no option to set the grid line size. Use a BasicStroke:

```clj
(import '(java.awt BasicStroke))

(def small-stroke (BasicStroke. 0.2))
```

The `:light-gray` color makes the font too light. Get the light gray color from
the clj-xchart color map and use the
[`.darker`](https://docs.oracle.com/javase/7/docs/api/java/awt/Color.html#darker\(\))
method

```clj
(def font-color (.darker (c/colors :light-gray)))
```

Use this code:

```clj
(import '(java.util Date)
        '(java.awt Font BasicStroke))

(def small-plain (Font. Font/SANS_SERIF Font/PLAIN 10))

(def small-stroke (BasicStroke. 0.2))

(def font-color (.darker (c/colors :light-gray)))

(c/spit
 (c/xy-chart {"line" (merge
                      (c/extract-series
                       {:x (comp #(Date. (* 1000 %)) long first)
                        :y second}
                       data)
                      {:style {:marker-type :none
                               :line-color :black}})}
             {:width 250
              :height 100
              :legend {:visible? false}
              :chart {:background-color :white}
              :axis {:ticks {:labels {:color font-color
                                      :font small-plain}
                             :marks {:color :light-gray}}}
              :y-axis {:decimal-pattern "## ms"}
              :x-axis {:tick-mark-spacing-hint 1}
              :date-pattern "HH:mm"
              :plot {:border-visible? false
                     :grid-lines {:vertical? false
                                  :horizontal? true
                                  :color :light-gray
                                  :stroke small-stroke}}})
 "gh-status.png")
```

The result is similar but not identical. The original is on the left. The code
above produces the chart on the right:

![Mean response time over at GitHub](imgs/gh-mean-response.png "Original") ![clj-xchart output](imgs/gh-copycat-mean-response.png "Copycat")

XChart/clj-xchart has limited tick-mark control. Set `:tick-mark-spacing-hint`
on `:y-axis` or `:x-axis`. This option is a hint. Here, it removes all tick
marks or displays the tick marks shown.

You cannot remove the y-axis tick-mark line without removing the x-axis
tick-mark line. `[:axis :ticks :line-visible?]` removes both lines.

## Newer chart types cookbook

These small examples are designed to be copied into a REPL. Each expression
returns a chart, so it can be passed to `view`, `to-bytes`, or `spit`.

```clj
(c/box-chart {"latency" [12 18 21 27 31]})

(c/horizontal-bar-chart {"sales" [[10 20 15] ["Q1" "Q2" "Q3"]]})

(c/ohlc-chart {"price" {:x [1 2 3]
                         :open [10 12 11] :high [13 14 15]
                         :low [9 10 10] :close [12 11 14]}})

(c/dial-chart {"cpu" {:value 0.72 :label "72%"}}
              {:legend {:visible? false}})

(c/radar-chart {"host-a" [0.8 0.5 0.9]
                "host-b" [0.6 0.7 0.5]}
               {:radii-labels ["speed" "reliability" "cost"]})

(c/heat-map-chart
 {"load" {:x-labels ["Mon" "Tue"] :y-labels ["AM" "PM"]
           :heat-data [[1 2] [3 4]]}}
 {:range-colors [:blue :white :red]
  :locale java.util.Locale/US})
```

Annotations are maps added after construction. Headless HTTP or notebook
exports can use bytes directly:

```clj
(c/add-annotations!
 (c/xy-chart {"trend" {:x [1 2] :y [2 3]}})
 [{:type :horizontal-line :value 2.5}
  {:type :text :x 1.2 :y 3.0 :text "target"}])

(def png-bytes (c/to-bytes my-chart :png {:dpi 300}))
```
