(ns pact.core-async
  (:require
   [pact.core :as p]
   [clojure.core.async :as a])
  (:import
   clojure.core.async.impl.protocols.Channel))


(extend-protocol core/IPact

  Channel

  (-then [this func]
    (let [out
          (a/promise-chan (map func) identity)]

      (a/pipe this out)

      out))

  (-error [this func]
    (let [out
          (a/promise-chan (map identity) (fn [e]
                                           (try
                                             (func e)
                                             (catch Throwable e
                                               e))))]

      (a/pipe this out)

      out)))
