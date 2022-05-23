(ns repo-version-tracker.common
  (:require
    [cljs.reader :as reader])
  (:import [goog.async Debouncer]))

(defn set-item!
      "Set key in browser's localStorage to val"
      [key val]
      (.setItem js/localStorage key val))

(defn get-item
      "Returns value of key from browser's localStorage."
      [key]
      (when-let [val (.getItem js/localStorage key)]
                (reader/read-string val)))

(defn remove-item!
      "Remove the browser's localStorage value for the given key"
      [key]
      (.removeItem js/localStorage key))

;;todo: diferente? is this the best way to debounce?
(defn debounce [f interval]
      "Source: https://martinklepsch.org/posts/simple-debouncing-in-clojurescript.html"
      (let [dbnc (Debouncer. f interval)]
           (fn [& args]
               (.apply (.-fire dbnc) dbnc (to-array args)))))