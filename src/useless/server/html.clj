(ns useless.server.html
  (:require [hiccup.page :as page]
            [useless.server.renderer :as renderer]
            [useless.server.markdown]
            [useless.server.asciidoc]))


(defn render
  [document]
  (let [content (renderer/render document)]
    (page/html5
      [:head
       [:meta {:charset "utf-8"}]
       [:title "Useless"]
       [:link {:rel "stylesheet" :href "/assets/css/vendor/codemirror.min.css"}]
       [:link {:rel "stylesheet" :href "/assets/css/vendor/dracula.min.css"}]
       [:link {:rel "stylesheet" :href "/assets/css/vendor/normalize.min.css"}]
       [:link {:rel "stylesheet" :href "/assets/css/main.css"}]]
      [:body
       [:header]
       [:main content]
       [:aside]
       [:script {:src "/assets/js/main.js"}]])))
