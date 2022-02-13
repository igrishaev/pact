(ns pact.comp-future
  (:refer-clojure :exclude [future])
  (:require
   [pact.core :as p])
  (:import
   java.util.function.Function
   java.util.function.Supplier
   java.util.concurrent.CompletableFuture))


(extend-protocol p/IPact

  CompletableFuture

  (-then [this func]
    (.thenApply this (reify Function
                       (apply [_ x]
                         (func x)))))

  (-error [this func]
    (.exceptionally this (reify Function
                           (apply [_ e]
                             (p/-error (ex-cause e) func))))))


(defmacro future [& body]
  `(CompletableFuture/supplyAsync (reify Supplier
                                    (get [_]
                                      ~@body))))
