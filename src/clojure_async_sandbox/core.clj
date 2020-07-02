(ns clojure-async-sandbox.core
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:require [clojure.core.matrix :as m])
  (:require [java-time :as jt])
  (:gen-class))

(defn round-places [number decimals]
  (let [factor (Math/pow 10 decimals)]
    (double (/ (Math/round (* factor number)) factor))))

(defn heavy-fn []  
  (let [matrix-size 10000
        A [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]
        B [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]]
    (/ (round-places (apply + (map (partial reduce +) (m/mul A B))) 2) (rand 100000))))

(defn new-product-handler []
  (let [input (async/chan 2048)
        output (async/chan 2048)]
    (async/go
      (while-let [message (async/<! input)]
                 (println (str "Processing new product: " message))
                 (async/>! output message)))
    [input output]))

(defn cost-change-handler []
  (let [input (async/chan 2048)
        output (async/chan 2048)]
    (async/go
      (while-let [message (async/<! input)]
                 (println (str "Processing cost change: " message))
                 (async/>! output message)))
    [input output]))

(defn price-computation-handler [input-channels]
  (let [output (async/chan 4096)]
    (async/go (loop []
                    (let [[message channel] (async/alts! input-channels)]
                      (when message                        
                        (async/go (let [t (jt/instant)]
                                        (println (str "Computing price for: " message))
                                        (let [price (heavy-fn)
                                              elapsed (jt/time-between t (jt/instant) :millis)]                                          
                                          (async/>! output (str "New price for " (assoc message :price price) " took " elapsed " miliseconds"))
                                          (println (str "Price computed for: " message)))))
                        (recur)))))
    output))

(defn -main [& args]
  (let [[new-product-input new-product-output] (new-product-handler)
        [cost-change-input cost-change-output] (cost-change-handler)
        new-price-output (price-computation-handler [new-product-output cost-change-output])
        events [new-product-input cost-change-input]
        products ["bananas" "apples" "grapes" "oranges" "papaya"]
        number-of-products 6000
        product-count (atom 0)]

    (doseq [n (range number-of-products)]
      (async/go (async/>! (nth events (rand-int (count events))) {:event_id n :name (nth products (rand-int (count products)))})))

    (async/thread
      (while (not= @product-count number-of-products))
      (do (async/close! new-product-input)
          (async/close! new-product-output)
          (async/close! cost-change-input)
          (async/close! cost-change-output)
          (async/close! new-price-output)))


    (while-let [message (async/<!! new-price-output)]
               (swap! product-count inc)
               (println (str message " - " @product-count " of " number-of-products)))))

;1. try single handler for all products vs one handler per product
