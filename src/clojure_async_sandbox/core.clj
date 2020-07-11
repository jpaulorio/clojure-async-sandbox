(ns clojure-async-sandbox.core
  (:require [clojure-async-sandbox.single-handler :as sh])
  (:require [clojure-async-sandbox.multiple_handlers :as mh])
  (:gen-class))


(defn -main [& args]
  (let [mode (if-let [arguments args] (keyword (first arguments)) :both)
        number-of-products (if-let [arguments args] (read-string (second arguments)) 50)
        number-of-events (if-let [arguments args] (read-string (nth arguments 2)) 100)]
    (case mode
      :sh (sh/run-simulation number-of-products number-of-events)
      :mh (mh/run-simulation number-of-products number-of-events)
      :both (do
              (sh/run-simulation number-of-products number-of-events)
              (mh/run-simulation number-of-products number-of-events)))))