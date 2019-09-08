Useless
=======

Make the Clojure code blocks in Markdown documents interactive.

## Quick start

```bash
$ clj -Sdeps '{:deps {me.flowthing/useless {:git/url "http://github.com/eerohele/useless" :sha "ffa3f7feaf1ffccf1fd06a54e303f23f53705732"}}}' -m useless.cliREA
```

Open [http://[::1]:1234/readme/github/eerohele/useless](http://[::1]:1234/readme/github/eerohele/useless) in your favorite browser.

```clojure
;; Press Cmd-Enter (macOS) or Ctrl-Enter to evaluate things.

(require '[clojure.string :as string])

(string/upper-case "Hello, world!")
```

## Evaluating forms

Here's how code evaluation works for interactive code blocks:

- If your cursor is at a bracket, the form delimited by that bracket and its
matching bracket is evaluated.
- If you select a form, that form is evaluated.
- If you don't select a form and your cursor is not at a bracket, the whole code
block is evaluated.

Interactive code blocks are powered by [Parinfer](https://shaunlebron.github.io/parinfer/).

## How it works

Useless establishes a WebSocket connection that relays your code to an nREPL
server and sends the result back to your browser.

By default, Useless starts an nREPL server and connects to it. That means that
you can evaluate any Clojure code and any code that uses whatever dependencies
Useless might have at that point in time.

## Using dependencies

You can also tell Useless to connect to another nREPL server. For example, if
you want to use Useless to work through the examples in the README for Renzo
Borgatti's [`parallel`](https://github.com/reborg/parallel) library, you can
first start an nREPL server that includes it as a dependency:

```bash
;; You can make this invocation a lot shorter if you add an alias for nREPL
;; in your `~/.clojure/deps.edn`.
$ clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "0.6.0"} parallel {:mvn/version "0.10"}}}' -m nrepl.cmdline --port 31337
```

Then, you can start Useless and tell it to use the nREPL server you started:

```bash
$ clj -m useless --nrepl-port 31337
Listening on http://[::1]:1234

$ open http://[::1]:1234/d/readme/github/reborg/parallel
```

Liberal use of aliases can make things rather more succinct when it comes to
using Useless.

## Sources

Useless can work on Markdown documents in one of these sources:

- File: [http://[::1]:1234/d/file/README.md](http://[::1]:1234/d/file/README.md)
- Classpath resource: [http://[::1]:1234/d/classpath/vendor/example.markdown](http://[::1]:1234/d/classpath/vendor/example.markdown)
- Gist: [http://[::1]:1234/d/gist/e1dca953548bfdfb9844](http://[::1]:1234/d/gist/e1dca953548bfdfb9844)
- GitHub README: [http://[::1]:1234/d/readme/github/ztellman/manifold](http://[::1]:1234/d/readme/github/ztellman/manifold)

## Security

Nope. If you expose Useless to the public internet, you're going to have bad
time. I haven't made any sort of attempt to make Useless secure in any
particular way, except to prevent it from listening on all IP addresses
(0.0.0.0).

## Why?

I don't know. You tell me. Tutorials? Interactive documentation? Onboarding a
new developer by acquainting them with a library or microservice via an
interactive Markdown document? Shitty literate programming?

Please let me know whether this library is worth its name.

## TODO

- Support `println` etc.
- Look for easier ways to use dependencies
- Add caching for GitHub content
- Encode nREPL messages as Transit over the wire?
- This README is quite bad
