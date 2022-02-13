(ns pact.core)


(defprotocol IPact

  (-then [this func])

  (-error [this func]))


(defn failure

  ([data]
   (failure "Failure" data))

  ([message data]
   (ex-info message (-> (or data {})
                        (merge {:ex/type ::failure})))))


(extend-protocol IPact

  Object

  (-then [this func]
    (try
      (func this)
      (catch Throwable e
        e)))

  (-error [this func]
    this)

  Throwable

  (-then [this func]
    this)

  (-error [this func]
    (try
      (func this)
      (catch Throwable e
        e)))

  clojure.lang.IDeref

  (-then [this func]

    (let [[result e]
          (try
            [@this nil]
            (catch Throwable e
              [nil e]))]

      (if e
        e
        (-then result func))))

  (-error [this func]
    this))


(defn then-fn [p func]
  (-then p func))


(defn error-fn [p func]
  (-error p func))


(defmacro then
  {:style/indent 1}
  [p [x] & body]
  `(then-fn ~p (fn [~x] ~@body)))


(defmacro error
  {:style/indent 1}
  [p [e] & body]
  `(error-fn ~p (fn [~e] ~@body)))
