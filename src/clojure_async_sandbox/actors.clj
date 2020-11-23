(ns clojure-async-sandbox.actors
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn build-actor [actor]
  (letfn [(behave [behavior]
            (let [current-input (async/chan)]
              (async/go
                (let [message (async/<! current-input)]
                  (let [new-input (behave (behavior message))]
                    (while-let [m (async/<! current-input)]
                               (async/>! new-input m)))))
              current-input))]
    (behave (actor))))