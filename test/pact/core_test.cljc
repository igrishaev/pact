(ns pact.core-test
  (:require
   [pact.core
    :refer [then error then-fn error-fn failure]
    :include-macros true]

   ;; extenders
   #?(:clj
      [pact.comp-future :as future])

   #?(:clj
      [pact.manifold])

   [pact.core-async]

   #?(:clj [clojure.core.async :as a]
      :cljs [cljs.core.async :as a])

   #?(:cljs
      [cljs.core.async.interop :refer-macros [<p!]])

   #?(:clj
      [manifold.deferred :as d])

   [clojure.string :as str]

   #?(:clj [clojure.test :refer [deftest testing is]]
      :cljs [cljs.test :as t :refer [deftest testing is async]])))


;;
;; A hack for proper indentation of cljs/async macro in Emacs/Cider
;;
#?(:clj
   (defmacro async
     {:style/indent 1}
     [_ & body]))


#?(:cljs

   (do

     (deftest test-core-async-cljs-ok

       (async done
         (let [in (a/chan)
               out (-> in
                       (then [x]
                         (+ 1 x))
                       (then [x]
                         (+ 1 x))
                       (then [x]
                         (str "+" x "+")))]

           (a/go
             (a/>! in 1)
             (is (= "+3+" (a/<! out))))

           (done))))

     (deftest test-core-async-cljs-recovering-from-error

       (async done

         (let [in (a/chan)
               out (-> in
                       (then [x]
                         (throw (ex-info "Divide by zero" {})))
                       (then [x]
                         42)
                       (error [e]
                         (ex-message e))
                       (then [message]
                         (str "<<< " message " >>>")))]

           (a/go
             (a/>! in 1)
             (is (= "<<< Divide by zero >>>" (a/<! out))))

           (done))))

     (deftest test-core-async-cljs-error-in-error

       (async done

         (let [in (a/chan)
               out (-> in
                       (then [x]
                         (throw (new js/Error "err 1")))
                       (then [x]
                         42)
                       (error [e]
                         (throw (new js/Error "err 2")))
                       (error [e]
                         (ex-message e))
                       (then [message]
                         (str "<<< " message " >>>")))]

           (a/go
             (a/>! in 1)
             (is (= "<<< err 2 >>>" (a/<! out))))

           (done))))))


#?(:clj

   (deftest test-core-async-clj-ok

     (testing "simple cases"

       (let [in (a/chan)
             out (-> in
                     (then [x]
                       (+ 1 x))
                     (then [x]
                       (+ 1 x))
                     (then [x]
                       (str "+" x "+")))]

         (a/>!! in 1)
         (is (= "+3+" (a/<!! out)))
         (a/close! in)))

     (testing "recovering from an error"

       (let [in (a/chan)
             out (-> in
                     (then [x]
                       (/ x 0))
                     (then [x]
                       42)
                     (error [e]
                       (ex-message e))
                     (then [message]
                       (str "<<< " message " >>>")))]

         (a/put! in 1)
         (is (= "<<< Divide by zero >>>" (a/<!! out)))
         (a/close! in)))

     (testing "error in error"

       (let [in (a/chan)
             out (-> in
                     (then [x]
                       (/ x 0))
                     (then [x]
                       42)
                     (error [e]
                       (+ 1 (ex-message e)))
                     (error [e]
                       (ex-message e))
                     (then [message]
                       (str "<<< " message " >>>")))]

         (a/put! in 1)
         (is (str/starts-with? (a/<!! out)
                               "<<< class java.lang.String cannot be cast to class java.lang.Number" ))
         (a/close! in)))))


(deftest test-simple-ok

  #?(:cljs

     (testing "js Date"

       (is (= "Sun, 09 Sep 2001 01:46:40 GMT"
              (-> (new js/Date 1000000000000)
                  (then [date]
                    (.toGMTString date)))))))

  (testing "test nil"

    (is (= ""
           (-> nil
               (error-fn str)
               (then-fn str)))))

  (testing "boolean"

    (is (= "false"
           (-> true
               (then-fn not)
               (then-fn str)))))

  (testing "string"

    (is (= "42/hello"
           (-> 42
               (then [x]
                 (-> x int str))
               (then [x]
                 (str x "/hello"))))))

  (is (= "Divide by zero"
         (-> 42
             (then [x]
               (throw (ex-info "Divide by zero" {})))
             (then [x]
               (+ x 1000))
             (error [e]
               (ex-message e)))))

  #?(:clj
     (testing "future"

       (is (= 103
              (-> (future (+ 1 2))
                  (then [x]
                    (+ x 100)))))))

  (testing "error in error handler"

    (is (= "class java.lang.String cannot be cast to class java.lang.Number"
           (-> 42
               (then [x]
                 (throw (ex-info "Divide by zero" {})))
               (error [e]
                 (throw (ex-info "class java.lang.String cannot be cast to class java.lang.Number" {})))
               (error [e]
                 (ex-message e))
               (then [message]
                 (subs message 0 63))))))

  (testing "stop on error"

    (let [e
          (-> 0
              inc
              inc
              (then [x]
                (throw (ex-info "Divide by zero" {})))
              (then [x]
                (inc x))
              (then [x]
                (throw (ex-info "foobar" {})))
              (then [x]
                (inc x))
              (then [x]
                (inc x))
              (then [x]
                (inc x)))]

      (is (= "Divide by zero" (ex-message e))))))


(deftest test-failure-ok

  (let [res
        (-> 1
            (then [x]
              (inc x))
            (then [x]
              (failure {:foo 42}))
            (error [e]
              (ex-data e)))]

    (is (= {:foo 42 :ex/type :pact.core/failure}
           res))))


(deftest test-mapping-ok

  (is (= {:a 1 :b 2 :c 3 :d 6}

         (-> {:a 1 :b 2}

             (then [{:as scope :keys [a b]}]
               (assoc scope :c (+ a b)))

             (then [{:as scope :keys [a b c]}]
               (assoc scope :d (+ a b c)))))))


#?(:clj

   (deftest test-comp-future-ok

     (testing "simple"

       (let [fut
             (future/future 1)

             res
             (-> fut
                 (then [x]
                   (inc x)))]

         (is (= 2 @res))))

     (testing "ex type for comp future"

       (let [fut
             (future/future 1)

             res
             (-> fut
                 (then [x]
                   (inc x))
                 (then [x]
                   (/ 0 0))
                 (error [e]
                   e))]

         (is (= java.lang.ArithmeticException
                (-> res deref class)))

         (is (= "Divide by zero"
                (-> res deref ex-message))))

       (testing "recover"

         (let [fut
               (future/future 1)

               res
               (-> fut
                   (then [x]
                     (inc x))
                   (then [x]
                     (/ 0 0))
                   (error [e]
                     (ex-message e))
                   (then [message]
                     (str "<<< " message " >>>")))]

           (is (= "<<< Divide by zero >>>"
                  @res))))

       (testing "error in error"

         (let [fut
               (future/future 1)

               res
               (-> fut
                   (then [x]
                     (inc x))
                   (then [x]
                     (/ 0 0))
                   (error [e]
                     (+ 1 (ex-message e)))
                   (error [e]
                     (ex-message e))
                   (then [message]
                     (str "<<< " message " >>>")))]

           (is (str/starts-with?
                @res "<<< class java.lang.String cannot be cast to class java.lang.Number")))))))


#?(:clj

   (deftest test-manifold-ok

     (testing "simple"

       (let [res
             (-> (d/future 1)
                 (then [x]
                   (inc x))
                 (then-fn inc))]

         (d/deferred? res)

         (is (= 3 @res))))

     (testing "recovery"

       (let [res
             (-> (d/future 1)
                 (then [x]
                   (/ x 0))
                 (error [e]
                   (ex-message e)))]

         (d/deferred? res)

         (is (= "Divide by zero" @res))))

     (testing "error in error"

       (let [res
             (-> (d/future 1)
                 (then [x]
                   (/ x 0))
                 (error [e]
                   (+ 1 (ex-message e)))
                 (error [e]
                   (ex-message e)))]

         (d/deferred? res)

         (is (str/starts-with?
              @res "class java.lang.String cannot be cast to class java.lang.Number"))))))



#?(:cljs

   (do

     (deftest test-promise-ok

       (async done

         (let [p
               (-> (js/Promise.resolve 1)
                   (then-fn inc)
                   (then [x]
                     (str "<<< " x " >>>")))]

           (a/go
             (is (= "<<< 2 >>>" (<p! p))))

           (done))))

     (deftest test-promise-error

       (async done

         (let [p
               (-> (js/Promise.resolve 1)
                   (then [x]
                     (throw (ex-info "error" {})))
                   (error [e]
                     (ex-message e)))]

           (a/go
             (is (= "error" (<p! p))))

           (done))))

     (deftest test-promise-error-in-error

       (async done

         (let [p
               (-> (js/Promise.resolve 1)
                   (then [x]
                     (throw (ex-info "error1" {})))
                   (error [e]
                     (throw (ex-info "error2" {})))
                   (error [e]
                     (ex-message e)))]

           (a/go
             (is (= "error2" (<p! p))))

           (done))))))




#?(:cljs

   (do

     (defn -main [& _]
       (enable-console-print!)
       (t/run-tests))

     (set! *main-cli-fn* -main)))
