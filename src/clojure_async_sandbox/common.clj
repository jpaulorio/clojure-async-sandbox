(ns clojure-async-sandbox.common  
  (:require [clojure.core.matrix :as m])
  (:gen-class))

(defn round-places [number decimals]
  (let [factor (Math/pow 10 decimals)]
    (double (/ (Math/round (* factor number)) factor))))

(defn compute-price []
  (let [matrix-size 2000
        A [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]
        B [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]]
    (round-places (/ (apply + (map (partial reduce +) (m/mul A B))) (rand 100000)) 2)))

(defn find-product [message product-list]
  (first (filter #(= (:product-id %) (:product-id message)) product-list)))