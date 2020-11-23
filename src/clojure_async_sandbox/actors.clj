(ns clojure-async-sandbox.actors
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn actor [behavior]
  (let [input (atom (async/chan))]
    (async/go
      (let [current-input @input
            message (async/<! current-input)]
        (swap! input (fn update-input [_] (behavior message)))
        (while-let [m (async/<! current-input)]
                   (async/>! @input m))))
    @input))