(ns clojure-async-sandbox.bank-account
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn bank-account
  ([] (bank-account 0))
  ([current-balance] (bank-account current-balance []))
  ([current-balance transaction-history]
   (let [input (atom (async/chan))]
     (defmulti bank-account-behavior (fn [message] (:type message)))
     (defmethod bank-account-behavior :credit [message]
       (bank-account (+ current-balance (:amount message)) (conj transaction-history message)))
     (defmethod bank-account-behavior :debit [message]
       (bank-account (- current-balance (:amount message)) (conj transaction-history message)))
     (defmethod bank-account-behavior :current-balance [message]
       (println "Current balance is:" current-balance)
       (bank-account current-balance transaction-history))
     (defmethod bank-account-behavior :list-transactions [message]
       (doseq [transaction transaction-history] (println "Transaction:" (:type transaction) (:amount transaction)))
       (bank-account current-balance transaction-history))
     (async/go
       (let [current-input @input
             message (async/<! @input)]
                  (swap! input (fn update-input [_] (bank-account-behavior message)))
                  (while-let [m (async/<! current-input)]
                             (async/>! @input m))))
     @input)))


(defn -main [& args]
  (let [bank-account (bank-account)]
    (async/go
      (async/>! bank-account {:type :credit :amount 1000})
      (async/>! bank-account {:type :debit :amount 300})
      (async/>! bank-account {:type :current-balance})
      (async/>! bank-account {:type :list-transactions})))
  (read-line))