(ns exp.index
  (:require [hiccup.core :as hiccup]))

(defn page []
  [:html
   {:lang :en}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name   "viewport"
            :content "width=device-width, initial-scale=1"}]
    [:title "Players Selection"]
    [:link
     {:href "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
      :rel "stylesheet"
      :integrity "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
      :crossorigin "anonymous"}]
    [:link
     {:rel "stylesheet"
      :href "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"}]
    [:style ".max-width {max-width: 30;}"]]
   [:body
    [:div#selection-frame]
    [:script {:src "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
              :integrity "sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"
              :crossorigin "anonymous"}]
    [:script {:src "js/compiled/main.js"}]]])


(defn generate-index [_]
  (spit "public/index.html"
        (hiccup/html (page))))
