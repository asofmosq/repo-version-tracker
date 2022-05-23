;;todo: need to rewrite this file
(ns user
    (:require
      [shadow.cljs.devtools.api :as shadow]
      [ring.middleware.resource :as middleware]))

(defn cljs []
      (shadow/repl :app))

(def dev-handler
  (middleware/wrap-resource identity "public"))

(def prod-handler
  (middleware/wrap-resource identity "public"))

