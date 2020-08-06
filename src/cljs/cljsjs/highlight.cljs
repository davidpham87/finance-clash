(ns cljsjs.highlight
  (:require ["highlight" :as highlight]))

(js/goog.exportSymbol "highlight" highlight)
(js/goog.exportSymbol "DevcardsSyntaxHighlighter" highlight)
