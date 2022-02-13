(-then [c-in func]
       (let [c-out (a/chan 1)]
         (a/take! c-in
                  (fn [x]
                    (a/put! c-out
                            (if (exception? x)
                              x
                              (try
                                (func x)
                                (catch Exception e
                                  e))))))
         c-out))


(-error [c-in func]
        (let [c-out (a/chan 1)]
          (a/take! c-in
                   (fn [x]
                     (a/put! c-out
                             (if (exception? x)
                               (try
                                 (func x)
                                 (catch Exception e
                                   e))
                               x))))
          c-out))
