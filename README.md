# clj-xchart

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.savya/clj-xchart.svg)](https://clojars.org/net.clojars.savya/clj-xchart)
[![cljdoc](https://cljdoc.org/badge/net.clojars.savya/clj-xchart)](https://cljdoc.org/d/net.clojars.savya/clj-xchart/CURRENT)
[![CI](https://github.com/jsavyasachi/clj-xchart/actions/workflows/ci.yml/badge.svg)](https://github.com/jsavyasachi/clj-xchart/actions/workflows/ci.yml)

Clojure wrapper around [XChart](https://knowm.org/open-source/xchart/), a small
library for rendering charts/plots.

![Rosling chart](rosling.png)

> Maintenance fork of [hyPiRion/clj-xchart](https://github.com/hyPiRion/clj-xchart),
> ported to XChart 4.x / Clojure 1.12 and published to Clojars as
> `net.clojars.savya/clj-xchart`.

## Installation

Leiningen/Boot:

```clj
[net.clojars.savya/clj-xchart "0.3.3"]
```

deps.edn:

```clj
net.clojars.savya/clj-xchart {:mvn/version "0.3.3"}
```

## What

clj-xchart wraps all the different charts you can generate in XChart:

* Line charts
* Scatter charts
* Area charts
* Bar charts
* Histogram charts
* Pie charts
* Donut charts
* Bubble charts
* Stick charts

## Usage

There are a lot of examples on the
[examples page](https://hypirion.github.io/clj-xchart/examples), and
[the tutorial](https://github.com/jsavyasachi/clj-xchart/blob/main/docs/tutorial.md)
covers basic usage. For more advanced customisation, have a look at the
[render options](https://github.com/jsavyasachi/clj-xchart/blob/main/docs/render-options.md)
page.

```clj
(require '[com.hypirion.clj-xchart :as c])

(-> (c/xy-chart {"y = x" {:x [1 2 3] :y [1 2 3]}
                 "y = 2x" {:x [1 2 3] :y [2 4 6]}})
    (c/spit "chart.png"))
```

The Clojure namespace is still `com.hypirion.clj-xchart`, so existing `require`
forms keep working; only the Clojars coordinate changed.

## Compatibility

XChart 4.x reorganised its API (the vector-graphics export, the styler
hierarchy, and the pie "annotations" which are now "labels"). This fork tracks
those changes while keeping clj-xchart's own keyword-based API unchanged, so
charts written against earlier `com.hypirion/clj-xchart` releases keep
rendering. See `CHANGELOG.md` for details.

## License

Copyright © 2016 Jean Niklas L'orange

Maintenance fork © 2026 Savyasachi. Original:
https://github.com/hyPiRion/clj-xchart

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
