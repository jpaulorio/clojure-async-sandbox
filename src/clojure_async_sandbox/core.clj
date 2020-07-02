(ns clojure-async-sandbox.core
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:require [clojure.core.matrix :as m])
  (:gen-class))

(defn transpose
  [s]
  (apply map vector s))

(defn nested-for
  [f x y]
  (map (fn [a]
         (map (fn [b]
                (f a b)) y))
       x))

(defn matrix-mult
  [a b]
  (nested-for (fn [x y] (reduce + (map * x y))) a (transpose b)))

(defn heavy-fn []
  (let [matrix-size 200000
        A [(vec (repeatedly matrix-size #(rand 10))) (vec (repeatedly matrix-size #(rand 10)))]
        B [(vec (repeatedly matrix-size #(rand 10))) (vec (repeatedly matrix-size #(rand 10)))]]
    (apply + (map (partial reduce +) (matrix-mult A B)))))

(defn new-product-handler []
  (let [input (async/chan)
        output (async/chan)]
    (async/thread
      (while-let [message (async/<!! input)]
                 (println (str "Processing new product: " message))
                 (async/>!! output message)))
    [input output]))

(defn cost-change-handler []
  (let [input (async/chan)
        output (async/chan)]
    (async/thread
      (while-let [message (async/<!! input)]
                 (println (str "Processing cost change: " message))
                 (async/>!! output message)))
    [input output]))

(defn price-computation-handler [input-channels]
  (let [output (async/chan)]
    (async/thread (loop []
                    (let [[message channel] (async/alts!! input-channels)]
                      (when message                        
                        (async/thread (let [t (+ 50 (rand-int 100))]
                                        (println (str "Computing price for: " message))
                                        (heavy-fn)
                                        (async/>!! output (str "New price for " message " took " t " miliseconds"))))
                        (recur)))))
    output))

(defn -main [& args]
  (let [[new-product-input new-product-output] (new-product-handler)
        [cost-change-input cost-change-output] (cost-change-handler)
        new-price-output (price-computation-handler [new-product-output cost-change-output])
        events [new-product-input cost-change-input]
        products ["bananas" "apples" "grapes" "oranges" "papaya"]
        number-of-products 50
        product-count (atom 0)]

    (doseq [n (range number-of-products)]
      (async/thread (async/>!! (nth events (rand-int (count events))) {:event_id n :name (nth products (rand-int (count products)))})))

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
