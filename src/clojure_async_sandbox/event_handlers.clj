(ns clojure-async-sandbox.event-handlers  
  (:gen-class))

(defn new-product-handler [message]
  (println (str "Processing new product with id: " (:product-id message)))
  message)

(defn cost-change-handler [message]
  (println (str "Processing cost change with id: " (:product-id message)))
  message)