(ns pact.core-async
  (:require
   [pact.core :as p]
   [clojure.core.async :as a])
  (:import
   clojure.core.async.impl.protocols.Channel))


(defn throwable? [e]
  (instance? Throwable e))


(extend-protocol p/IPact

  Channel

  (-then [this func]
    (let [out
          (a/promise-chan (map (fn [x]
                                 (if (throwable? x)
                                   x
                                   (func x))))
                          identity)]

      (a/pipe this out)

      out))

  (-error [this func]
    (let [out
          (a/promise-chan (map (fn [x]
                                 (if (throwable? x)
                                   (func x)
                                   x)))
                          identity)]

      (a/pipe this out)

      out)))
