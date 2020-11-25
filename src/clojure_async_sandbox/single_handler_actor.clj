(ns clojure-async-sandbox.single-handler-actor
  (:require [clojure-async-sandbox.common :refer :all]))

(defn new-product-handler [store-products-count online-products-count]
  (defmulti product-handler-behavior (fn [message] (:channel message)))
  (defmethod product-handler-behavior :store [message]
    (println "Processing new store product with id:" (:product-id message))
    (new-product-handler (inc store-products-count) online-products-count))
  (defmethod product-handler-behavior :online [message]
    (println "Processing new online product with id:" (:product-id message))
    (new-product-handler store-products-count (inc online-products-count)))
  product-handler-behavior)

(defn cost-change-handler [store-products-count online-products-count]
  (defmulti cost-handler-behavior (fn [message] (:channel message)))
  (defmethod cost-handler-behavior :store [message]
    (println "Processing cost change for store product with id:" (:product-id message))
    (cost-change-handler (inc store-products-count) online-products-count))
  (defmethod cost-handler-behavior :online [message]
    (println "Processing cost change for online product with id:" (:product-id message))
    (cost-change-handler store-products-count (inc online-products-count)))
  cost-handler-behavior)

(defn price-computation-handler [product-list]
  (defmulti price-computation-handler-behavior (fn [message] (:channel message)))
  (defmethod price-computation-handler-behavior :store [message]
    (let [product-id (:product-id message)]
      (println "Computing price for store product with id:" product-id)
      (price-computation-handler (assoc-in product-list [product-id :price] (compute-price)))))
  (defmethod price-computation-handler-behavior :online [message]
    (let [product-id (:product-id message)]
      (println "Computing price for online product with id:" product-id)
      (price-computation-handler (assoc-in product-list [product-id :price] (compute-price)))))
  price-computation-handler-behavior)
