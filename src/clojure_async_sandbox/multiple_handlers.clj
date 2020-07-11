(ns clojure-async-sandbox.multiple_handlers
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:require [java-time :as jt])
  (:require [clojure-async-sandbox.common :refer :all])
  (:require [clojure-async-sandbox.event-handlers :refer :all])
  (:gen-class))

(defn input-event-handler [event-handler-fn product-channels]
  (let [input (async/chan 15000)]
    (async/go
      (while-let [message (async/<! input)]
                 (let [product (find-product message product-channels)
                       output-channel (:input-channel product)
                       result (event-handler-fn message)]
                   (async/>! output-channel result))))
    input))

(defn price-computation-handler [input-channel output-channel total-processing-time]
  (let [product (atom {:price 0.00})]
    (fn []
      (async/go-loop []
        (let [message (async/<! input-channel)]
          (when message
            (let [t (jt/instant)]
              (println (str "Computing price for product with id: " (:product-id message)))
              (let [old-price (:price @product)
                    new-price (compute-price)
                    price (round-places (+ new-price old-price) 2)
                    elapsed (jt/time-between t (jt/instant) :millis)
                    output-message (assoc message :price price)]
                (async/>! output-channel (str "Old price " old-price " plus computed price " new-price " equals new price " price ". Product id: " (:product-id output-message) " took " elapsed " miliseconds"))
                (swap! product #(merge % output-message))
                (swap! total-processing-time #(+ % elapsed))))
            (recur)))))))

(defn generate-products [product-count]
  (map #(-> {:product-id % :price 0.00 :input-channel (async/chan 2) :output-channel (async/chan 2)}) (range product-count)))

(defn pick-random-event-channel [event-types event-handler-map]
  (let [event-type (nth event-types (rand-int (count event-types)))]
    (event-type event-handler-map)))

(defn pick-random-product [products]
  (nth products (rand-int (count products))))

(defn run-simulation [number-of-products number-of-events]
  (let [products (generate-products number-of-products)
        total-processing-time (atom 0)
        new-product-input (input-event-handler new-product-handler products)
        cost-change-input (input-event-handler cost-change-handler products)
        event-types [:new-product :cost-change]
        event-channel-map  {:new-product new-product-input :cost-change cost-change-input}
        event-count (atom 0)]

    (doseq [product products]
      ((price-computation-handler (:input-channel product) (:output-channel product) total-processing-time)))
    
    (doseq [n (range number-of-events)]
      (async/go (async/>! (pick-random-event-channel event-types event-channel-map) (pick-random-product products))))

    (println (str "Multiple Handlers - Processing new prices for " number-of-products " products ..."))
    (async/go (while-let [[message _] (async/alts! (map #(:output-channel %) products))]
                         (swap! event-count inc)
                         (println (str message " - " @event-count " of " number-of-events))))

    (while (not= @event-count number-of-events))

    (async/close! new-product-input)
    (async/close! cost-change-input)
    (doseq [input-channel (:input-channel products)
            output-channel (:output-channel products)]
      (async/close! input-channel)
      (async/close! output-channel))

    (let [avg-processing-time (float (/ @total-processing-time number-of-products))]
      (println (str "Avg price computation time: " avg-processing-time " ms")))))