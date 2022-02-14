(ns pact.manifold
  (:require
   [pact.core :as p]
   [manifold.deferred :as d]))


(extend-protocol p/IPact

  manifold.deferred.IDeferred

  (-then [this func]
    (d/chain this
             (fn [x]
               (func x))))

  (-error [this func]
    (d/catch this (fn [e]
                    (func e)))))
