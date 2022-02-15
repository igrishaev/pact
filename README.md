# Pact

A small library for chaining values through forms. It's like a promise but much
simipler.

## Installation

Lein:

```clojure
[com.github.igrishaev/pact "0.1.0"]
```

Deps.edn

```clojure
{com.github.igrishaev/pact {:mvn/version "0.1.0"}}
```

## How it works

The library declares two universe handlers: `then` and `error`. When you apply
`then` to the "good" values, you propagate furter. Applying `error` for them
does nothing. And vice versa: `then` for the "bad" values does nothing. Calling
`error` on the "bad" values gives you a chance to recover the pipeline.

By default, there is only one "bad" value which is an instance of
`Throwable`. Other types are considered as positive ones. The library carries
extensions for async data types such as `CompletableFuture`, Manifold and
`core.async`. You only need to require their modules so they extend the `IPact`
protocol.

## Examples

Import `then` and `error` macros, then chain a value with the standard `->`
threading macro. Both `then` and `error` accept a binding vector and an
arbitrary body.

```clojure
(ns foobar
  (:require
   [pact.core :refer [then error]]))


(-> 42
    (then [x]
      (-> x int str))
    (then [x]
      (str x "/hello")))

"42/hello"
```

If any exception pops up, the sequence of `then` handlers gets interrupted, and
the `error` handler gets into play:

```clojure
(-> 1
    (then [x]
      (/ x 0))
    (then [x]
      (str x "/hello")) ;; won't be executed
    (error [e]
      (ex-message e)))

"Divide by zero"
```

The `error` handler gives you a chance to recover from the exception. If you
return a non-exceptional data in `error`, the execution will proceed from the
next `then` handler:

```clojure
(-> 1
    (then [x]
      (/ x 0))
    (error [e]
      (ex-message e))
    (then [message]
      (log/info message)))

;; nil
```

The `->` macro can be nested. This is useful to capture the context for a
possible exception:

```clojure
(-> 1
    (then [x]
      (+ x 1))
    (then [x]
      (-> x
          (then [x]
            (/ x 0))
          (error [e]
            (println "The x was" x)
            nil))))

;; The x was 2
;; nil
```

Besides `then` and `error` macros, the library provides the `then-fn` and
`error-fn` functions. They are useful when you have a ready function that
processes the value:

```clojure
(ns foobar
  (:require
   [pact.core :refer [then-fn error-fn]]))

(-> 1
    (then-fn inc)
    (then-fn str))

;; "2"

(-> 1
    (then [x]
      (/ x 0))
    (error-fn ex-message))

;; "Divide by zero"
```

Chaining with `then` and `error` is especially good for maps as allowing
destructuring:

```clojure
(-> {:db {...} :cassandra {...}}

    ;; Get a user from the database and attach it to the scope.
    (then [{:as scope :keys [db]}]
      (let [user (jdbc/get-by-id db :users 42)]
        (assoc scope :user user)))

    ;; Having a user, get their last items from Cassandra cluster
    ;; and attach them to the scope.
    (then [{:as scope :keys [cassandra user]}]
      (let [items (get-user-items cassandra user)]
        (assoc scope :items items)))

    ;; Do something more...
    (then [...]
      ...))
```

TODO Fast fail

## Supported types

The `core` namespace declares the `then` and `error` handlers for the `Object`,
`Throwable`, and `java.util.concurrent.Future` types. The `Future` values get
dereferenced when passing to `then`.

The following modules extend the `IPact` protocol for asynchronous types.

### Complatable Future

The module `pact.comp-future` handles the `CompletableFuture` class availabe
since Java 11. The module also provides its own `future` macro to build an
instance of `CompletableFuture`:

```clojure
(-> (future/future 1)
    (then [x]
      (inc x))
    (then [x]
      (/ 0 0))
    (error [e]
      (ex-message e))
    (deref))

"Divide by zero"
```

Pay attention: if you fed an instance of `CompletableFuture` to the threading
macro, the result will always be of this type. Thus, there is a `deref` call at
the end.

Infernally, the `then` handler calls for the `.thenApply` method if a future,
and the `error` handler boils down to `..exceptionally`.

### Manifold

The `pact.manifold` module makes the handlers work with the amazing Manifold
library and its types. The Pact library doesn't have Manifold dependency: you've
got to add it by your own.

```clojure
[manifold "0.1.9-alpha3"]
```

```clojure
(-> (d/future 1)
    (then [x]
      (/ x 0))
    (error [e]
      (ex-message e))
    (deref))

"Divide by zero"
```

Under the hood, `then` and `error` call the `d/chain` and `d/catch` macros
respectively.

Once you've put an instance of Manifold deferred, the result will always be a
deferred.

### Core.async

## Testing

To run the tests, do `lein test` or just `make test`.

&copy; 2022 Ivan Grishaev
