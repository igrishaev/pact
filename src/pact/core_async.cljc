(ns pact.core-async
  (:require
   [pact.core :as p]
   #?(:clj [clojure.core.async :as a])
   #?(:cljs [cljs.core.async :as a])))


(defn throwable? [e]
  (instance? #?(:clj Throwable :cljs js/Error) e))


(extend-protocol p/IPact

  #?(:clj clojure.core.async.impl.channels.ManyToManyChannel
     :cljs cljs.core.async.impl.channels.ManyToManyChannel)

  (-then [this func]
    (let [out
          (a/promise-chan (map (fn [x]
                                 (if (throwable? x)
                                   x
                                   (func x))))
                          identity)]

      (a/take! this (fn [x]
                      (a/put! out x)) )

      out))

  (-error [this func]
    (let [out
          (a/promise-chan (map (fn [x]
                                 (if (throwable? x)
                                   (func x)
                                   x)))
                          identity)]

      (a/take! this (fn [x]
                      (a/put! out x)) )

      out)))
