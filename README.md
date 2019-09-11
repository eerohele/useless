Useless
=======

Make the Clojure code blocks in Markdown documents interactive.

## Quick start

You must have the [Clojure CLI tools](https://www.clojure.org/guides/getting_started#_clojure_installer_and_cli_tools) installed.

1. Run:

    ```bash
    $ clj -Sdeps '{:deps {me.flowthing/useless {:git/url "http://github.com/eerohele/useless" :sha "ff009f40aeeedb4dcc282440cca26171fc1e967c"}}}' -m useless.cli
    Listening on http://[::1]:1234
    ```

1. Open [http://localhost:1234/d/readme/github/eerohele/useless](http://localhost:1234/d/readme/github/eerohele/useless) in your favorite browser and start evaluating things.

    ```clojure
    ;; Press Cmd-Enter (macOS) or Ctrl-Enter to evaluate things.

    (require '[clojure.string :as string])

    (string/upper-case "Hello, world!")
    ```

## Alias

To make launching Useless easier, you can add an alias like this in your `~/.clojure/deps.edn`:

```clojure
:aliases {:useless {:extra-deps {me.flowthing/useless {:git/url "http://github.com/eerohele/useless"
                                                       :sha "<INSERT SHA HERE>"}}
                    :main-opts  ["-m" "useless.cli"]}}
```

Then, you can run Useless like this:

```bash
$ clj -A:useless
```
   
## Sources

Useless can work on Markdown documents in one of these sources:

- File: [http://localhost:1234/d/file/README.md](http://localhost:1234/d/file/README.md)
- Classpath resource: [http://localhost:1234/d/classpath/vendor/example.markdown](http://localhost:1234/d/classpath/vendor/example.markdown)
- Gist: [http://localhost:1234/d/gist/e1dca953548bfdfb9844](http://localhost:1234/d/gist/e1dca953548bfdfb9844)
- GitHub README: [http://localhost:1234/d/readme/github/ztellman/manifold](http://localhost:1234/d/readme/github/ztellman/manifold)
- GitHub file: [http://localhost:1234/d/github/file/adambard/learnxinyminutes-docs/clojure.html.markdown](http://localhost:1234/d/github/file/adambard/learnxinyminutes-docs/clojure.html.markdown)

## Evaluating forms

Here's how code evaluation works for interactive code blocks:

- If your cursor is at a bracket, the form delimited by that bracket and its
matching bracket is evaluated.
- If you select a form, that form is evaluated.
- If you don't select a form and your cursor is not at a bracket, the whole code
block is evaluated.

Interactive code blocks are powered by [Parinfer](https://shaunlebron.github.io/parinfer/).

## How it works

Useless establishes a WebSocket connection that mediates your code between an
nREPL server and your browser.

By default, Useless starts an nREPL server and connects to it. That means that
you can evaluate any Clojure code and any code that uses whatever dependencies
Useless might have at that point in time.

## Using dependencies

You can also tell Useless to connect to another nREPL server. For example, if
you want to use Useless to work through the examples in the README for Renzo
Borgatti's [`parallel`](https://github.com/reborg/parallel) library, you can
first start an nREPL server that includes it as a dependency:

```bash
# You can make this invocation a bit shorter if you add an alias for nREPL
# in your `~/.clojure/deps.edn`: https://nrepl.org/nrepl/0.6.0/usage/server.html
$ clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "0.6.0"} parallel {:mvn/version "0.10"}}}' -m nrepl.cmdline --port 31337
```

Then, you can start Useless and tell it to use the nREPL server you started:

```bash
# If you have the appropriate alias in your `~/.clojure/deps.edn`:
$ clj -A:useless --nrepl-port 31337
Listening on http://[::1]:1234

$ open http://[::1]:1234/d/readme/github/reborg/parallel
```

Liberal use of aliases can make things rather more succinct when it comes to
using Useless.

You can also change the port number of the nREPL server to connect to by
clicking on the port number in the upper left-hand corner.

## Security

Nope. If you expose Useless to the public internet, you're going to have bad
time. I haven't made any sort of attempt to make Useless secure in any
particular way, except to prevent it from listening on all IP addresses
(0.0.0.0).

## Why?

I don't know. You tell me. Tutorials? Interactive documentation? Onboarding a
new developer by acquainting them with a library or microservice via an
interactive Markdown document? Shitty literate programming? Embed an nREPL
server into your application and create an interactive Markdown document
to annotate some aspect of it?

Please let me know whether this library is worth its name.

## TODO

- Support `println` etc.
- Look for easier ways to use dependencies
- Add caching for GitHub content
- Encode nREPL messages as Transit over the wire?
- Improve styles
- Support images
- This README is quite bad
