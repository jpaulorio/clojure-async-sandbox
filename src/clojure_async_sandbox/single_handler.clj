(ns clojure-async-sandbox.single-handler
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:require [java-time :as jt])
  (:require [clojure-async-sandbox.common :refer :all])
  (:require [clojure-async-sandbox.event-handlers :refer :all])
  (:gen-class))

(defn input-event-handler [event-handler-fn]
  (let [input (async/chan 15000)
        output (async/chan 15000)]
    (async/go
      (while-let [message (async/<! input)]
                 (let [result (event-handler-fn message)]
                   (async/>! output result))))
    [input output]))

(defn price-computation-handler [input-channels total-processing-time]
  (let [output (async/chan 20000)]
    (async/go-loop []
      (let [[message channel] (async/alts! input-channels)]
        (when message
          (async/go (let [t (jt/instant)]
                      (println (str "Computing price for product with id: " (:product-id message)))
                      (let [price (compute-price)
                            elapsed (jt/time-between t (jt/instant) :millis)
                            output-message (assoc message :price price)]
                        (async/>! output (str "New price " price " for product with id " (:product-id output-message) " took " elapsed " miliseconds"))
                        (swap! total-processing-time #(+ % elapsed)))))
          (recur))))
    output))

(defn run-simulation [number-of-products number-of-events]
  (let [products (generate-products-without-channels number-of-products)
        total-processing-time (atom 0)
        [new-product-event-channel new-product-output-channel] (input-event-handler new-product-handler)
        [cost-change-event-channel cost-change-output-channel] (input-event-handler cost-change-handler)
        new-price-output (price-computation-handler [new-product-output-channel cost-change-output-channel] total-processing-time)
        event-types [:new-product :cost-change]
        event-channel-map  {:new-product new-product-event-channel :cost-change cost-change-event-channel}
        event-count (atom 0)]

    ;randomly sends events to the events' input channels
    (doseq [n (range number-of-events)]
      (async/go (async/>! (pick-random-event-channel event-types event-channel-map) (pick-random-product products))))

    (println (str "Single Handler - Processing " number-of-events " events for " number-of-products " products ..."))

    ;consolidate computed prices from the new price channel
    (async/go (while-let [message (async/<! new-price-output)]
                         (swap! event-count inc)
                         (println (str message " - " @event-count " of " number-of-events))))

    ;waits until all events are processed
    (while (not= @event-count number-of-events))

    (close-channels [new-product-event-channel new-product-output-channel cost-change-event-channel cost-change-output-channel new-price-output])
    
    (let [avg-processing-time (float (/ @total-processing-time number-of-products))]
      (println (str "Avg price computation time: " avg-processing-time " ms")))))