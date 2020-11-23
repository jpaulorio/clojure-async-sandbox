(ns clojure-async-sandbox.actors
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn bank-account
  ([] (bank-account 0))
  ([current-balance] (bank-account current-balance []))
  ([current-balance transaction-history]
   (let [input (atom (async/chan))]
     (defmulti bank-account-behavior (fn [state message] (:type message)))
     (defmethod bank-account-behavior :credit [state message]
       (bank-account (+ (:current-balance state) (:amount message)) (conj (:transaction-history state) message)))
     (defmethod bank-account-behavior :debit [state message]
       (bank-account (- (:current-balance state) (:amount message)) (conj (:transaction-history state) message)))
     (defmethod bank-account-behavior :current-balance [state message]
       (println "Current balance is:" current-balance)
       (bank-account (:current-balance state) (:transaction-history state)))
     (defmethod bank-account-behavior :list-transactions [state message]
       (doseq [transaction (:transaction-history state)] (println "Transaction:" (:type transaction) (:amount transaction)))
       (bank-account (:current-balance state) (:transaction-history state)))
     (async/go
       (let [current-input @input
             message (async/<! @input)]
                  (swap! input (fn update-input [_] (bank-account-behavior {:current-balance current-balance
                                                                            :transaction-history transaction-history} message)))
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
  (while true))