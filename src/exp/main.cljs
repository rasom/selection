(ns exp.main
  (:require [reagent.dom :as reagent.dom]
            [re-frame.core :as re-frame]
            [cljs.test :as c.test]
            [goog.string :as gstring]
            [goog.string.format]
            exp.handlers
            exp.subs))

(defn init-db []
  (re-frame/dispatch-sync [:init (js/localStorage.getItem "db")]))

(defn timestamp []
  (js/Date.now))

(defn get-value [obj]
  (.-value (.-target obj)))

(defn render [root-view]
  (reagent.dom/render
   [root-view]
   (js/document.getElementById "selection-frame")))

(defn get-sub [params]
  @(re-frame/subscribe params))

(defn nav []
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-light
   [:div.container-fluid
    [:a.navbar-brand
     {:href "#"}
     "Players Selection"]]])

(defn primbut [on-click text]
  [:button.btn.btn-primary.btn-sm
   {:type :submit
    :on-click on-click}
   text])

(defn players-table []
  [:table.table.table-sm
   [:thead
    [:tr
     [:th {:scope :col
           :style {:width "70%"}}
      "Name"]
     [:th {:scope :col
           :style {:width "70%"}}
      "Rating"]]]
   [:tbody
    (for [{:keys [name rating id]} (get-sub [:players])]
      ^{:key id}
      [:tr
       [:td name]
       [:td
        [:select.form-select.form-select-sm
         {:style {:width 100
                  :height 29}
          :aria-label "Rating"
          :default-value rating
          :on-change #(re-frame/dispatch [:set-rating id (get-value %)])}
         (for [r (range 1 8)]
           ^{:key r}
           [:option
            {:value r}
            (str r)])]]])]])

(defn suggested-team [{:keys [players rating min max]}]
  [:table.table.table-sm
   [:tbody
    (concat
     (map
      (fn [{:keys [name rating id]}]
        ^{:key id}
        [:tr
         [:td name]
         [:td rating]])
      players)
     [^{:key :rating}
      [:tr
       [:td
        {:col-span 2}
        [:b (gstring/format "%d (%d - %d)" rating min max)]]]])]])

(defn copy-to-clipboard [val]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) val)
    (.appendChild js/document.body el)
    (.select el)
    (js/document.execCommand "copy")
    (.removeChild js/document.body el)))

(defn suggestion [title label]
  (let [[team1 team2] (get-sub [:suggestion label])]
    [:div.row-sm
     [:table.table.table-sm
      [:thead
       [:tr
        [:th {:scope :col}
         title]
        [:th
         [primbut (let [str-list (get-sub [:str-suggestion label])]
                    #(do (.stopPropagation %)
                         (copy-to-clipboard str-list)))
          "Copy selection"]]]]
      [:tbody
       [:tr
        [:td
         [suggested-team team1]]
        [:td
         [suggested-team team2]]]]]]))

(defn list-view []
  (if (get-sub [:empty-list?])
    [:div.row
     [:div.row-sm
      [:div.input-group
       [:span.input-group-text "Players list"]
       [:textarea.form-control
        {:aria-label "Players list"
         :rows 10
         :on-change #(re-frame/dispatch [:set-list (get-value %)])}]]]
     [:div.row-sm
      [primbut #(re-frame/dispatch [:save-list]) "Save list"]]]
    [:div.row
     [:div.row-sm
      [primbut #(re-frame/dispatch [:clear-list]) "Clear list"]]
     [:div.row-sm
      [players-table]]
     [suggestion "Best first" :best]
     [suggestion "Worst first" :worst]
     [suggestion "Average vs rest" :avg]]))

(defn main-div []
  [:div
   [nav]
   [:div.container-fluid
    [list-view]]])

(defn ^:dev/after-load tests []
  (c.test/run-all-tests #"exp.*"))

(defn ^:dev/after-load init []
  (init-db)
  (render main-div))
