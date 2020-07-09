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
              (println (str "Computing price for: " message))
              (let [old-price (:price @product)
                    new-price (compute-price)
                    price (+ new-price old-price)
                    elapsed (jt/time-between t (jt/instant) :millis)
                    output-message (assoc message :price price)]
                (async/>! output-channel (str "Old price for product was " old-price ". New price is old price plus " new-price " equals " price ". " output-message " took " elapsed " miliseconds"))
                (swap! product #(merge % output-message))
                (swap! total-processing-time #(+ % elapsed))))
            (recur)))))))

(defn run-simulation [number-of-products]
  (let [product-channels (map #(-> {:product-id % :input-channel (async/chan 2) :output-channel (async/chan 2)}) (range number-of-products))
        total-processing-time (atom 0)
        new-product-input (input-event-handler new-product-handler product-channels)
        cost-change-input (input-event-handler cost-change-handler product-channels)
        events [new-product-input cost-change-input]
        products ["bananas" "apples" "grapes" "oranges" "papaya"]
        product-count (atom 0)]

    (doseq [channel product-channels]
      ((price-computation-handler (:input-channel channel) (:output-channel channel) total-processing-time)))

    (doseq [n (range number-of-products)]
      (async/go (async/>! (nth events (rand-int (count events))) {:product-id n :name (nth products (rand-int (count products)))})))

    (println (str "Multiple Handlers - Processing new prices for " number-of-products " products ..."))
    (async/go (while-let [[message _] (async/alts! (map #(:output-channel %) product-channels))]
                         (swap! product-count inc)
                         (println (str message " - " @product-count " of " number-of-products))))

    (while (not= @product-count number-of-products))

    (async/close! new-product-input)
    (async/close! cost-change-input)
    (doseq [input-channel (:input-channel product-channels)
            output-channel (:output-channel product-channels)]
      (async/close! input-channel)
      (async/close! output-channel))

    (let [avg-processing-time (float (/ @total-processing-time number-of-products))]
      (println (str "Avg price computation time: " avg-processing-time " ms")))))