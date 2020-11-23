(ns clojure-async-sandbox.actors
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn actor [behavior]
  (let [current-input (async/chan)]
    (async/go
      (let [message (async/<! current-input)]
        (let [new-input (actor (behavior message))]
          (while-let [m (async/<! current-input)]
                     (async/>! new-input m)))))
    current-input))