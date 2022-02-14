(ns pact.manifold
  (:require
   [pact.core :as p]
   [manifold.deferred :as d]))


(extend-protocol p/IPact

  manifold.deferred.IDeferred

  (-then [this func]
    (d/chain this func))

  (-error [this func]
    (d/catch this func)))
