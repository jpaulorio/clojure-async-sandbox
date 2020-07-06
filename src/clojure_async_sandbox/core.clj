(ns clojure-async-sandbox.core
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:require [clojure.core.matrix :as m])
  (:require [java-time :as jt])
  (:gen-class))

(defn round-places [number decimals]
  (let [factor (Math/pow 10 decimals)]
    (double (/ (Math/round (* factor number)) factor))))

(defn compute-price []
  (let [matrix-size 2000
        A [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]
        B [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]]
    (round-places (/ (apply + (map (partial reduce +) (m/mul A B))) (rand 100000)) 2)))

(defn input-event-handler [event-handler-fn]  
  (let [input (async/chan 15000)
        output (async/chan 15000)]
    (async/go
      (while-let [message (async/<! input)]
                 (let [result (event-handler-fn message)]
                   (async/>! output result))))
    [input output]))

(defn new-product-handler [message]
  (println (str "Processing new product: " message))
  message)

(defn cost-change-handler [message]
  (println (str "Processing cost change: " message))
  message)

(defn price-computation-handler [input-channels total-processing-time]
  (let [output (async/chan 20000)]
    (async/go (loop []
                (let [[message channel] (async/alts! input-channels)]
                  (when message
                    (async/go (let [t (jt/instant)]
                                (println (str "Computing price for: " message))
                                (let [price (compute-price)
                                      elapsed (jt/time-between t (jt/instant) :millis)
                                      output-message (assoc message :price price)]
                                  (async/>! output (str "New price for " output-message " took " elapsed " miliseconds"))
                                  (swap! total-processing-time #(+ % elapsed))
                                  (println (str "Price computed for: " output-message)))))
                    (recur)))))
    output))

(defn -main [& args]
  (let [total-processing-time (atom 0)
        [new-product-input new-product-output] (input-event-handler new-product-handler)
        [cost-change-input cost-change-output] (input-event-handler cost-change-handler)
        new-price-output (price-computation-handler [new-product-output cost-change-output] total-processing-time)
        events [new-product-input cost-change-input]
        products ["bananas" "apples" "grapes" "oranges" "papaya"]
        number-of-products (if-let [arguments args] (read-string (first arguments)) 50)
        product-count (atom 0)]

    (doseq [n (range number-of-products)]
      (async/go (async/>! (nth events (rand-int (count events))) {:event_id n :name (nth products (rand-int (count products)))})))

    (println (str "Processing new prices for " number-of-products " products ..."))
    (async/go (while-let [message (async/<! new-price-output)]
                         (swap! product-count inc)
                         (println (str message " - " @product-count " of " number-of-products))))

    (while (not= @product-count number-of-products))

    (async/close! new-product-input)
    (async/close! new-product-output)
    (async/close! cost-change-input)
    (async/close! cost-change-output)
    (async/close! new-price-output)

    (let [avg-processing-time (float (/ @total-processing-time number-of-products))]
      (println (str "Avg price computation time: " avg-processing-time " ms")))))

;1. try single handler for all products vs one handler per product
