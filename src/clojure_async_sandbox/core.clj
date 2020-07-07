(ns clojure-async-sandbox.core
  (:require [clojure-async-sandbox.single-handler :as sh])
  (:require [clojure-async-sandbox.multiple_handlers :as mh])
  (:gen-class))


(defn -main [& args]
  (let [mode (if-let [arguments args] (keyword (first arguments)) :both)
        number-of-products (if-let [arguments args] (read-string (second arguments)) 50)]
    (case mode
      :sh (sh/run-simulation number-of-products)
      :mh (mh/run-simulation number-of-products)
      :both (do
              (sh/run-simulation number-of-products)
              (mh/run-simulation number-of-products)))))