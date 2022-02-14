(ns pact.core-test
  (:require
   [pact.core :refer [then error then-fn error-fn failure]]

   ;; extenders
   [pact.comp-future :as future]
   [pact.manifold]
   [pact.core-async]

   [clojure.core.async :as a]
   [manifold.deferred :as d]

   [clojure.string :as str]
   [clojure.test :refer [deftest testing is]]))


(deftest test-simple-ok

  (is (= "42/hello"
         (-> 42
             (then [x]
               (-> x int str))
             (then [x]
               (str x "/hello")))))

  (is (= "Divide by zero"
         (-> 42
             (then [x]
               (/ x 0))
             (then [x]
               (+ x 1000))
             (error [e]
               (ex-message e)))))

  (testing "future"

    (is (= 103
           (-> (future (+ 1 2))
               (then [x]
                 (+ x 100))))))

  (testing "error in error handler"

    (is (= "class java.lang.String cannot be cast to class java.lang.Number"
           (-> 42
               (then [x]
                 (/ x 0))
               (error [e]
                 (+ 1 (ex-message e)))
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
                (/ x 0))
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
             @res "<<< class java.lang.String cannot be cast to class java.lang.Number"))))))


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
           @res "class java.lang.String cannot be cast to class java.lang.Number")))))


(deftest test-core-async-ok

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

      (a/close! in))))
