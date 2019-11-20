Useless
=======

**I'm still working on this. Things might change drastically.** Feel free to give it a try, but everything remains subject to change at this point.

Make the Clojure code blocks in Markdown documents interactive.

![Screenshot of Useless](https://gist.githubusercontent.com/eerohele/b4c84928cc9cdb664cb97b39eaf14688/raw/5c79a1a068c74ac1c0046a6c1cf1e375c600efde/useless.png)

## Quick start

You must have the [Clojure CLI tools](https://www.clojure.org/guides/getting_started#_clojure_installer_and_cli_tools) installed.

1. Run:

    ```bash
    clj -Sdeps '{:deps {me.flowthing/useless {:git/url "http://github.com/eerohele/useless" :sha "2d12c68b160ceec0c59c92c72b738bae0bd7c9e7"}}}' \
        -m useless.cli \
        --uri https://gist.github.com/john2x/e1dca953548bfdfb9844
    ```

1. Start evaluating forms with <kbd>Cmd</kbd> + <kbd>Enter</kbd> (macOS) or <kbd>Ctrl</kbd> + <kbd>Enter</kbd> (Windows).

## Alias

To make launching Useless easier, you can add an alias like this in your `~/.clojure/deps.edn`:

```clojure
:aliases {:useless {:extra-deps {me.flowthing/useless {:git/url "http://github.com/eerohele/useless"
                                                       :sha "<INSERT SHA HERE>"}}
                    :main-opts  ["-m" "useless.cli"]}}
```

Then, you can run Useless like this:

```bash
clj -A:useless --uri https://gist.github.com/john2x/e1dca953548bfdfb9844
```
   
## Sources

Useless can handle Markdown documents from one of these sources:

| Source | URI |
|------|-------|
| File | [http://localhost:1234/file/README.md](http://localhost:1234/file/README.md) |
| Classpath | [http://localhost:1234/classpath/vendor/example.markdown](http://localhost:1234/classpath/vendor/example.markdown) |
| Gist | [http://localhost:1234/gist/e1dca953548bfdfb9844](http://localhost:1234/gist/e1dca953548bfdfb9844) |
| GitHub README | [http://localhost:1234/github/readme/ztellman/manifold](http://localhost:1234/github/readme/ztellman/manifold) |
| GitHub file | [http://localhost:1234/github/file/adambard/learnxinyminutes-docs/clojure.html.markdown](http://localhost:1234/github/file/adambard/learnxinyminutes-docs/clojure.html.markdown) |

## Evaluating forms

Here's how code evaluation works for interactive code blocks:

- If your cursor is at a bracket, the form delimited by that bracket and its
matching bracket is evaluated.
- If you select a form, that form is evaluated.
- If you don't select a form and your cursor is not at a bracket, the whole code
block is evaluated.

Interactive code blocks are powered by [Parinfer](https://shaunlebron.github.io/parinfer/).

## How it works

Useless establishes a WebSocket connection that mediates your code between a
prepl server and your browser.

By default, Useless starts an prepl server and connects to it. That means that
you can evaluate any Clojure code and any code that uses whatever dependencies
Useless might have at that point in time.

## Using dependencies

If you're not afraid of conflicts, you can add dependencies with `-Sdeps`. If
you want an interactive version of the
[Meander](https://github.com/noprompt/meander) README, for example, you can do
this:

```bash
# If you have the appropriate alias in your `~/.clojure/deps.edn`:  
clj -A:useless \
    -Sdeps '{:deps {meander/epsilon {:mvn/version "RELEASE"}}}' \
    --uri https://github.com/noprompt/meander/blob/epsilon/README.md
```

However, there's a chance Useless uses a different version of the library you
add a dependency to. If you want to avoid conflicts, you can also tell Useless
to connect to another prepl server. For example, if you want to use Useless to
work through the examples in the README for Renzo Borgatti's
[`parallel`](https://github.com/reborg/parallel) library, you can first start an
prepl server that includes it as a dependency:

```bash
clj -J-Dclojure.server.jvm="{:port 31337 :accept clojure.core.server/io-prepl}" \
    -Sdeps '{:deps {parallel {:mvn/version "RELEASE"}}}'
```

Or, to make the invocation a bit shorter, you can first add an alias like this
in your `~/.clojure/deps.edn`:

```clojure
:prepl {:jvm-opts ["-Dclojure.server.repl={:port,31337,:accept,clojure.core.server/io-prepl}"]}
```

Then, to run a prepl server that can use `parallel`, run:

```bash
clj -O:prepl -Sdeps '{:deps {parallel {:mvn/version "RELEASE"}}}'
```

Next, you can start Useless and tell it to use the prepl server you started:

```bash
clj -A:useless \
    --prepl-port 31337 \
    --uri https://github.com/reborg/parallel/blob/master/README.md
```

Liberal use of aliases can make things rather more succinct when it comes to
using Useless.

You can also change the port number of the prepl server to connect to by
clicking on the port number in the upper left-hand corner.

## Security

Nope. If you expose Useless to the public internet, you're going to have bad
time. I haven't made any sort of attempt to make Useless secure in any
particular way, except to prevent it from listening on all IP addresses
(0.0.0.0).

## Why?

I don't know. You tell me. Tutorials? Interactive documentation? Onboarding a
new developer by acquainting them with a library or microservice via an
interactive Markdown document? Shitty literate programming? Embed a prepl
server into your application and create an interactive Markdown document
to annotate some aspect of it?

Please let me know whether this library is worth its name.
