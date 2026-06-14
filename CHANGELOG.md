# Change Log

All notable changes to this project are documented here. This change log follows
the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.3.3] - 2026-06-13

### Changed
- Bump default/dev Clojure 1.12.0 -> 1.12.5 (latest patch). Provided scope, no
  consumer impact; the 1.12 CI cell now exercises the current release.

## [0.3.2] - 2026-06-11

### Added
- Docstrings for the public `bubble-chart` and `normalize-categories`.

## [0.3.1] - 2026-06-11

### Fixed
- Restore `:java-source-paths`, which had been dropped in 0.3.0. The
  `com.hypirion.clj-xchart.opt` namespace imports the bundled `ListMapping`
  Java helper, so the 0.3.0 jar was missing that class and cljdoc analysis
  failed. The core namespace was unaffected. Added a regression test that
  exercises the `opt` namespace end to end.

## [0.3.0] - 2026-06-11

Maintenance fork, published to Clojars as `net.clojars.savya/clj-xchart`. The
Clojure namespace is unchanged (`com.hypirion.clj-xchart`), as is the
keyword-based chart API.

### Changed
- Port from XChart 3.2.1 to **XChart 4.0.1**. XChart 4.x was a major API
  overhaul; the library no longer compiled or ran against it.
  - Vector export (SVG/PDF/EPS) now uses XChart's own `VectorGraphicsEncoder`
    instead of the dropped `de.erichseifert.vectorgraphics2d` classes.
  - Styling setters that moved from the base `Styler` onto the per-chart styler
    subclasses are dispatched correctly again.
  - Pie "annotations" became "labels" upstream; the `:annotations?` /
    `:annotations-font` keys now drive the pie/category label styling, and
    `:annotation-type` maps to the new `LabelType`.
- Default to **Clojure 1.12**; test across Clojure 1.10/1.11/1.12.
- Enable `*warn-on-reflection*`; only the (intentional) polymorphic `addSeries`
  calls remain reflective.

### Added
- `:value` and `:name-and-value` pie annotation types (newly available in
  XChart 4.x).
- Characterization tests that render every chart type to every output format.
- GitHub Actions CI (JDK x Clojure matrix).

### Removed
- The empty `:java-source-paths` build configuration.
- Axis titles on pie charts (XChart 4.x removed them; pie charts have no axes).

[0.3.0]: https://github.com/jsavyasachi/clj-xchart/releases/tag/0.3.0
