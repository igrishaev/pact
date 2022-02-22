(ns pact.core
  #?(:clj
     (:import java.util.concurrent.Future)))


(defprotocol IPact

  (-then [this func])

  (-error [this func]))


(defn failure

  ([data]
   (failure "Failure" data))

  ([message data]
   (throw (ex-info message (-> (or data {})
                               (merge {:ex/type ::failure}))))))


#?(:cljs

   (extend-protocol IPact

     default

     (-then [this func]
       (try
         (func this)
         (catch :default e
           e)))

     (-error [this func]
       this)

     js/Error

     (-then [this func]
       this)

     (-error [this func]
       (try
         (func this)
         (catch :default e
           e)))

     js/Promise

     (-then [this func]
       (.then this func))

     (-error [this func]
       (.catch this func))))


#?(:clj
   (extend-protocol IPact

     nil

     (-then [this func]
       (try
         (func this)
         (catch Throwable e
           e)))

     (-error [this func]
       this)

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

     Future

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
       this)))


(defn then-fn [p func]
  (-then p func))


(defn error-fn [p func]
  (-error p func))


(defmacro then
  {:style/indent 1}
  [p [x] & body]
  `(then-fn ~p (^{:once true} fn [~x] ~@body)))


(defmacro error
  {:style/indent 1}
  [p [e] & body]
  `(error-fn ~p (^{:once true} fn [~e] ~@body)))
