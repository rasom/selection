(ns exp.main
  (:require [reagent.dom :as reagent.dom]
            [reagent.core :as reagent.core]
            [re-frame.core :as re-frame]
            [cljs.test :as c.test]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.string :as clojure.string]
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

(def dev-mode? js/goog.DEBUG)

(defn nav []
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-light
   [:div.container-fluid
    [:a.navbar-brand
     {:href "#"}
     "Players Selection"]]])

(defn primbut [on-click text]
  [:button.btn.btn-primary.btn-sm
   {:type     :submit
    :on-click on-click}
   text])

(defn nobut [on-click text]
  [:button.btn.btn-sm
   {:type     :submit
    :on-click on-click}
   text])

(defn players-table []
  [:table.table.table-sm
   [:thead
    [:tr
     [:th {:scope :col
           :style {:width "90%"}}
      "Name"]
     [:th {:scope :col}
      "Rating"]]]
   [:tbody
    (for [{:keys [name rating id]} (get-sub [:players])]
      ^{:key id}
      [:tr
       [:td name]
       [:td
        [:select.form-select.form-select-sm
         {:style      {:width  100
                       :height 29}
          :aria-label "Rating"
          :value      rating
          :on-change  #(re-frame/dispatch [:set-rating id (get-value %)])}
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
         [:td {:style {:width "90%"}} name]
         [:td {:style {:width "10%"}} rating]])
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

(defn team-list [{:keys [players]}]
  (->> players
       (map :name)
       sort
       (clojure.string/join "\n")))

(defn players-list->string [teams]
  (->> teams
       (map team-list)
       (interpose "\n\nvs\n\n")
       clojure.string/join))

(defn copy-label [just-copied?]
  [:label "Copy selection "
   (when @just-copied?
     [:i.bi.bi-check])])

(defn suggestion [title label]
  (let [teams        (get-sub [:suggestion label])
        just-copied? (reagent.core/atom false)
        teams-number (get-sub [:teams-number])]
    (when teams
      [:div.row-sm
       [:table.table.table-sm
        [:thead
         [:tr
          [:th
           (cond-> {:scope :col}
             (= 3 teams-number)
             (assoc :col-span 2))
           title]
          [:th
           [primbut #(do (reset! just-copied? true)
                         (js/setInterval
                          (fn [] (reset! just-copied? false))
                          1000)
                         (.stopPropagation %)
                         (copy-to-clipboard
                          (players-list->string teams)))
            [copy-label just-copied?]]
           [nobut
            #(re-frame/dispatch [:reset-seeds label])
            [:label "Refresh " [:i.bi.bi-arrow-clockwise]]]]]]
        [:tbody
         [:tr
          (for [team teams]
            ^{:key (:name team)}
            [:td {:style {:width (goog.string/format "%d%" (/ 100 teams-number))}}
             [suggested-team team]])]]]])))

(def default-list
  "1. Adam
2. Roman
3. Sikor
4. Karol
5. Franek
6. Darek R
7. Dima
8. Dawid
9. Kamil
10. Hamid")

(defn number-of-teams []
  [:select.form-select.form-select-sm
   {:style      {:width  100
                 :height 29}
    :aria-label "Number of teams"
    :value      (get-sub [:teams-number])
    :on-change  #(re-frame/dispatch [:set-teams-number (get-value %)])}
   (for [r (range 2 4)]
     ^{:key r}
     [:option
      {:value r}
      (gstring/format "%d teams" r)])])

(defn list-view []
  (if (get-sub [:empty-list?])
    [:div.row.justify-content-md-center
     [:div.row-sm
      [:div.input-group
       [:span.input-group-text "Players list"]
       [:textarea.form-control
        {:aria-label    "Players list"
         :rows          10
         :default-value (when dev-mode? default-list)
         :on-change     #(re-frame/dispatch [:set-list (get-value %)])}]]]
     [:div.row-sm
      [primbut #(re-frame/dispatch [:save-list]) "Save list"]]]
    [:div.row.justify-content-md-center
     [:div.row.row-sm
      [:div.col.col-sm.d-flex.justify-content-between
       [primbut #(re-frame/dispatch [:clear-list]) "Clear list"]
       [number-of-teams]
       [primbut #(re-frame/dispatch [:reset-ratings]) "Reset ratings"]]] ;
     [:div.row-sm
      [players-table]]
     [suggestion "Best first" :best]
     [suggestion "Worst first" :worst]
     [suggestion "Average vs rest" :avg]]))

(defn main-div []
  [:div
   [nav]
   [:div.container-fluid
    {:style {:max-width 600}}
    [list-view]]])

(defn ^:dev/after-load tests []
  (c.test/run-all-tests #"exp.*"))

(defn ^:dev/after-load init []
  (init-db)
  (render main-div))
