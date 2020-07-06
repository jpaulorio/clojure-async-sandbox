(ns clojure-async-sandbox.core
  (:require [clojure-async-sandbox.single-handler :as sh])
  (:require [clojure-async-sandbox.multiple_handlers :as mh])
  (:gen-class))


(defn -main [& args]
  (sh/run-simulation args)
  (mh/run-simulation args))

;1. try single handler for all products vs one handler per product
