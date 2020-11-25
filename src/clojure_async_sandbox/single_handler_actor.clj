(ns clojure-async-sandbox.single-handler-actor
  (:require [clojure-async-sandbox.common :refer :all])
  (:require [clojure-async-sandbox.actors :refer :all])
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn new-product-handler [store-products-count online-products-count price-calculation-actor]
  (defmulti product-handler-behavior (fn [message] (:channel message)))
  (defmethod product-handler-behavior :store [message]
    (println "Processing new store product with id:" (:product-id message))
    (new-product-handler (inc store-products-count) online-products-count price-calculation-actor))
  (defmethod product-handler-behavior :online [message]
    (println "Processing new online product with id:" (:product-id message))
    (new-product-handler store-products-count (inc online-products-count) price-calculation-actor))
  product-handler-behavior)

(defn cost-change-handler [store-products-count online-products-count price-calculation-actor]
  (defmulti cost-handler-behavior (fn [message] (:channel message)))
  (defmethod cost-handler-behavior :store [message]
    (println "Processing cost change for store product with id:" (:product-id message))
    (async/go (async/>! price-calculation-actor message))
    (cost-change-handler (inc store-products-count) online-products-count price-calculation-actor))
  (defmethod cost-handler-behavior :online [message]
    (println "Processing cost change for online product with id:" (:product-id message))
    (async/go (async/>! price-calculation-actor message))
    (cost-change-handler store-products-count (inc online-products-count) price-calculation-actor))
  cost-handler-behavior)

(defn price-computation-handler [product-list output-channel]
  (defmulti price-computation-handler-behavior (fn [message] (:channel message)))
  (defmethod price-computation-handler-behavior :store [message]
    (let [product-id (:product-id message)
          updated-product (assoc message :price (compute-price))]
      (println "Computing price for store product with id:" product-id)
      (async/go (async/>! output-channel updated-product))
      (price-computation-handler (assoc product-list product-id updated-product) output-channel)))
  (defmethod price-computation-handler-behavior :online [message]
    (let [product-id (:product-id message)
          updated-product (assoc message :price (compute-price))]
      (println "Computing price for online product with id:" product-id)
      (async/go (async/>! output-channel updated-product))
      (price-computation-handler (assoc product-list product-id updated-product) output-channel)))
  price-computation-handler-behavior)

(defn -main [& args]
  (let [number-of-events (if (first args) (first args) 100)
        number-of-products (if (second args) (second args) 100)
        products (vec (generate-products number-of-products))
        new-price-output (async/chan)
        price-computation-handler-actor (build-actor price-computation-handler products new-price-output)
        new-product-handler-actor (build-actor new-product-handler 0 0 price-computation-handler-actor)
        cost-change-handler-actor (build-actor cost-change-handler 0 0 price-computation-handler-actor)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-handler-actor :cost-change cost-change-handler-actor}
        event-count (atom 0)]
    ;randomly sends events/messages to actors
    (doseq [n (range number-of-events)]
      (async/go (async/>! (pick-random-event-channel event-types event-actor-map) (pick-random-product products))))
    (println (str "Single Handler Actor - Processing " number-of-events " events for " number-of-products " products ..."))

    ;consolidate computed prices from the new price channel
    (async/go (while-let [message (async/<! new-price-output)]
                         (swap! event-count inc)
                         (println (str (dissoc message :input-channel :output-channel) " - " @event-count " of " number-of-events))))

    ;waits until all events are processed
    (while (not= @event-count number-of-products)
      ;(println "Prices computed so far:" @event-count)
      )))
