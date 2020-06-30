(ns clojure-async-sandbox.core
  (:require [clojure.core.async :as async])
  (:gen-class))

(defn new-product-handler []
  (let [input (async/chan)
        output (async/chan)]
    (async/go (loop []
                (when-some [message (async/<! input)]
                  (println (str "Processing new product: " message))
                  (async/>! output message)
                  (recur))))
    [input output]))

(defn cost-change-handler []
  (let [input (async/chan)
        output (async/chan)]
    (async/go (loop []
                (when-some [message (async/<! input)]
                  (println (str "Processing cost change: " message))
                  (async/>! output message)
                  (recur))))
    [input output]))

(defn price-computation-handler [input-channels]
  (let [output (async/chan)]
    (async/go (loop []
                (let [[message channel] (async/alts! input-channels)]
                  (when message
                    (println (str "Computing price for: " message))
                    (async/go (let [t (+ 50 (rand-int 100))]
                                (Thread/sleep t)
                                (async/>! output (str "New price for " message " took " t " miliseconds"))))
                    (recur)))))
    output))

(defn -main [& args]
  (let [[new-product-input new-product-output] (new-product-handler)
        [cost-change-input cost-change-output] (cost-change-handler)
        new-price-output (price-computation-handler [new-product-output cost-change-output])
        events [new-product-input cost-change-input]
        products ["bananas" "apples" "grapes" "oranges" "papaya"]
        number-of-products 1000
        product-count (atom 0)]

    ; (async/>!! new-product-input {:name "bananas"})
    ; (async/>!! cost-change-input {:name "apples" :cost 0.4})
    ; (async/>!! cost-change-input {:name "grapes" :cost 0.6})
    ; (async/>!! cost-change-input {:name "orange" :cost 0.7})
    ; (async/>!! new-product-input {:name "papaya"})

    (doseq [n (range number-of-products)]
      (async/go (async/>! (nth events (rand-int (count events))) {:event_id n :name (nth products (rand-int (count products)))})))

    (async/thread
      (while (not= @product-count number-of-products))
      (do (async/close! new-product-input)
          (async/close! new-product-output)
          (async/close! cost-change-input)
          (async/close! cost-change-output)
          (async/close! new-price-output)))

    (loop []
      (when-some [message (async/<!! new-price-output)]
        (swap! product-count inc)
        (println (str message " - " @product-count " of " number-of-products))
        (recur)))))

;1. try single handler for all products vs one handler per product
; 
;2. replace loop/recur with
; (go-loop []
;    (when-some [v (<! chan)]
;    ;; do something with v
;      (recur)))
