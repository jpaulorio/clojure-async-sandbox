(ns clojure-async-sandbox.core
  (:require [clojure.core.async :as async :refer [>! <! >!! <!! go chan buffer close! thread
                                                  alts! alts!! timeout]])
  (:gen-class))

(defn new-product-handler []
  (let [input (async/chan)
        output (async/chan)]
    (async/go (loop []
         (let [message (async/<! input)]
           (println (str "Processing new product: " message))
           (async/>! output (str "Compute price for " message))
           (recur))))
    [input output]))

(defn cost-change-handler []
  (let [input (async/chan)
        output (async/chan)]
    (async/go (loop []
                (let [message (async/<! input)]
                  (println (str "Processing cost change: " message))
                  (async/>! output (str "Compute price for " message))
                  (recur))))
    [input output]))

(defn -main [& args]
  (let [[new-product-input new-product-output] (new-product-handler)
        [cost-change-input cost-change-output] (cost-change-handler)]
    (async/>!! new-product-input {:name "bananas"})
    (println (async/<!! new-product-output))
    (async/>!! cost-change-input {:name "bananas" :cost 0.4})
    (println (async/<!! cost-change-output))))

;1. try single handler for all products vs one handler per product
; 
;2. replace loop/recur with
; (go-loop []
;    (when-some [v (<! chan)]
;    ;; do something with v
;      (recur)))
