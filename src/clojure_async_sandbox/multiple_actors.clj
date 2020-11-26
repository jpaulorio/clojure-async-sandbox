(ns clojure-async-sandbox.multiple-actors
  (:require [clojure-async-sandbox.common :refer :all])
  (:require [clojure-async-sandbox.actors :refer :all])
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn new-product-handler [store-products-count online-products-count]
  (defmulti product-handler-behavior (fn [message] (:channel message)))
  (defmethod product-handler-behavior :store [message]
    (let [price-calculation-actor (:price-calculation-actor message)]
      (println "Processing new store product with id:" (:product-id message))
      (send-async price-calculation-actor message)
      (new-product-handler (inc store-products-count) online-products-count)))
  (defmethod product-handler-behavior :online [message]
    (let [price-calculation-actor (:price-calculation-actor message)]
      (println "Processing new online product with id:" (:product-id message))
      (send-async price-calculation-actor message)
      (new-product-handler store-products-count (inc online-products-count))))
  product-handler-behavior)

(defn cost-change-handler [store-products-count online-products-count]
  (defmulti cost-handler-behavior (fn [message] (:channel message)))
  (defmethod cost-handler-behavior :store [message]
    (let [price-calculation-actor (:price-calculation-actor message)]
      (println "Processing cost change for store product with id:" (:product-id message))
      (send-async price-calculation-actor message)
      (cost-change-handler (inc store-products-count) online-products-count)))
  (defmethod cost-handler-behavior :online [message]
    (let [price-calculation-actor (:price-calculation-actor message)]
      (println "Processing cost change for online product with id:" (:product-id message))
      (send-async price-calculation-actor message)
      (cost-change-handler store-products-count (inc online-products-count))))
  cost-handler-behavior)

(defn price-computation-handler [current-product output-channel]
  (defmulti price-computation-handler-behavior (fn [message] (:channel message)))
  (defmethod price-computation-handler-behavior :store [message]
    (let [updated-product (assoc current-product :price (compute-price))]
      (println "Computing price for store product:" (dissoc current-product :price-calculation-actor))
      (send-async output-channel updated-product)
      (price-computation-handler updated-product output-channel)))
  (defmethod price-computation-handler-behavior :online [message]
    (let [updated-product (assoc current-product :price (compute-price))]
      (println "Computing price for online product:" (dissoc current-product :price-calculation-actor))
      (send-async output-channel updated-product)
      (price-computation-handler updated-product output-channel)))
  price-computation-handler-behavior)

(defn -main [& args]
  (let [number-of-events (if (first args) (first args) 10)
        number-of-products (if (second args) (second args) 2)
        products (vec (generate-products-without-channels number-of-products))
        new-price-output (async/chan)
        new-product-handler-actor (build-actor new-product-handler 0 0)
        cost-change-handler-actor (build-actor cost-change-handler 0 0)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-handler-actor :cost-change cost-change-handler-actor}
        products-actors-map (map #(assoc % :price-calculation-actor (build-actor price-computation-handler % new-price-output)) products)
        event-count (atom 0)]
    ;randomly sends events/messages to actors
    (doseq [n (range number-of-events)]
      (let [product (pick-random-product products-actors-map)
            event-actor (pick-random-event-channel event-types event-actor-map)]
        (send-async event-actor product)))

    (println (str "Single Handler Actor - Processing " number-of-events " events for " number-of-products " products ..."))

    ;consolidate computed prices from the new price channel
    (async/go (while-let [message (async/<! new-price-output)]
                         (swap! event-count inc)
                         (println (str (dissoc message :price-calculation-actor) " - " @event-count " of " number-of-events))))

    ;waits until all events are processed
    (while (not= @event-count number-of-events))
    (async/close! new-price-output)))