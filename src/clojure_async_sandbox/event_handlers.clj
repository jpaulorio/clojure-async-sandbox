(ns clojure-async-sandbox.event-handlers  
  (:gen-class))

(defn new-product-handler [message]
  (println (str "Processing new product: " message))
  message)

(defn cost-change-handler [message]
  (println (str "Processing cost change: " message))
  message)