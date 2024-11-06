(ns exp.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :players-list
  (fn [db]
    (:players-list db)))

(re-frame/reg-sub
 :raw-suggestion
 (fn [db]
   (:suggestions db)))

(re-frame/reg-sub
 :teams-number
 (fn [db]
   (:teams-number db)))

(re-frame/reg-sub
 :suggestion
 :<- [:raw-suggestion]
 (fn [raw-suggestion [_ label]]
   (when-let [suggestion (label raw-suggestion)]
     (map (fn [{:keys [players rating] :as team}]
            (let [ratings (map :rating players)]
              (assoc team
                     :max (- rating (apply min ratings))
                     :min (- rating (apply max ratings)))))
          suggestion))))

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
