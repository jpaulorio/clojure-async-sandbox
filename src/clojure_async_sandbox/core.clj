(ns clojure-async-sandbox.core
  (:require [clojure-async-sandbox.single-handler :as sh])
  (:require [clojure-async-sandbox.multiple_handlers :as mh])
  (:gen-class))


(defn -main [& args]
  (sh/run-simulation args)
  (mh/run-simulation args))