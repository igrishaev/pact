(ns pact.core-test
  (:require
   [pact.core :refer [then error]]
   [pact.comp-future :as future]

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


(deftest test-comp-future-ok


  #_
  (let [fut
        (future/future 1)

        res
        (-> fut
            (then [x]
              (inc x)))]

    (is (= 2 @res)))

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
                e)
              #_
              (then [x]
                (+ 1 x))
              #_
              (then [message]
                (str "<<< " message " >>>")))]

      (is (= java.lang.ArithmeticException
             (-> res deref class)

             ))

      (is (= "Divide by zero"
             (-> res deref ex-message
                 )

             )))

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
                (then [message]
                  (str "<<< " message " >>>")))]

        (is (str/starts-with? @res "<<< java.lang.ClassCastException"))))



    )


  )
