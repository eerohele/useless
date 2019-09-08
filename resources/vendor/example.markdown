# Examples

A single successful function call:

```clojure
(+ 1 2)
```

A largish response map:

```clojure
(into (sorted-map)
      (zipmap (map (comp keyword str char) (range 97 123))
              (range 1 26)))
```

A form that returns an error:

```clojure
(throw (ex-info "Boom!" {:foo :bar}))
```

A form that fails to evaluate:

```clojure
;; missing right paren
(map inc (range 5)
```

Changing namespaces:

```clojure
(ns foo.bar)

(defn sum
  [xs]
  (reduce + xs))
  
(sum (repeatedly 5 rand))
```

One page comprises a single nREPL session.

Code block #1:

```clojure
(require '[clojure.string :as string])
```

Code block #2:

```clojure
(string/upper-case "hello, world!")
```

A non-Clojure code block:

```java
// import static java.util.stream.Collectors.*;
Double ev = Stream.of(1, 2, 3, 4, 5, 6) // dice roll
  .collect(teeing(summingDouble(i -> i), counting(), (sum, n) -> sum / n));
```