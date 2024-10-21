(ns exp.subs
  (:require [re-frame.core :as re-frame]
            [clojure.string :as clojure.string]))

(re-frame/reg-sub
  :players-list
  (fn [db]
    (:players-list db)))

(re-frame/reg-sub
 :raw-suggestion
 (fn [db]
   (:suggestions db)))

(defn team-list [{:keys [players]}]
  (->> players
       (map :name)
       sort
       (clojure.string/join "\n")))

(re-frame/reg-sub
 :str-suggestion
 (fn [[_ label]]
   [(re-frame/subscribe [:suggestion label])])
 (fn [suggestion]
   (clojure.string/join
    [(team-list (first suggestion))
     "\n\nvs\n\n"
     (team-list (second suggestion))])))

(re-frame/reg-sub
 :suggestion
 :<- [:raw-suggestion]
 (fn [raw-suggestion [_ label]]
   (map (fn [{:keys [players rating] :as team}]
          (let [ratings (map :rating players)]
            (assoc team
                   :max (- rating (apply min ratings))
                   :min (- rating (apply max ratings)))))
        (label raw-suggestion))))

(re-frame/reg-sub
 :players
 :<- [:players-list]
 (fn [players-list]
   (vals players-list)))

(re-frame/reg-sub
 :empty-list?
 :<- [:players-list]
 (fn [players-list]
   (empty? players-list)))
