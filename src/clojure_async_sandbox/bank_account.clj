(ns clojure-async-sandbox.bank-account
  (:require [clojure.core.async :as async])
  (:require [clojure-async-sandbox.actors :refer :all])
  (:gen-class))

(defn bank-account
  ([]
   (bank-account 0))
  ([current-balance]
   (bank-account current-balance []))
  ([current-balance transaction-history]
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
   bank-account-behavior))


(defn -main [& args]
  (let [bank-account
        (build-actor bank-account)]
    (send-sync bank-account {:type :credit :amount 1000})
    (send-sync bank-account {:type :debit :amount 300})
    (send-sync bank-account {:type :current-balance})
    (send-sync bank-account {:type :list-transactions}))
  (read-line))