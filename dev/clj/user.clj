(ns user
  (:require
   [shadow.cljs.devtools.server :as server]))


(defn init! []
  (server/start!))


(comment
  (init!))
