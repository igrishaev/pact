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
{com.github.igrishaev/pact "0.1.0"}
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

### Complatable Future

### Manifold

### Core.async

### Fast fail

## Testing

&copy; 2022 Ivan Grishaev
