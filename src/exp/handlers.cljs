(ns exp.handlers
  (:require [clojure.edn :as clojure.edn]
            [re-frame.alpha :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [clojure.string :as string]
            [cljs.test :as c.test]
            [exp.algo :as algo]))

(def default-db {:teams-number 2})

(def db-interceptor
  (re-frame/after
   (fn [db]
     (js/localStorage.setItem "db" (prn-str db)))))

(def all-interceptors [db-interceptor])

(defn handler [n h]
  (re-frame/reg-event-db
   n
   all-interceptors
   (fn-traced [db [_ & params]]
     (apply h db params))))

(defn handler-fx [n h]
  (re-frame/reg-event-fx
   n
   all-interceptors
   (fn-traced [{:keys [db] :as cofx} [_ & params]]
              (apply h cofx db params))))

(handler
 :init
 (fn [_ str-db]
   (if (empty? str-db)
     default-db
     (clojure.edn/read-string str-db))))

(handler
 :reset-db
 (fn [_] {}))

(defn list->names [string-list]
  (->> string-list
       string/split-lines
       (keep #(second (re-find #"^\d[^\.]*\.[\s]*([^$]*)" %)))))

(defn parse-list [string-list]
  (->> string-list
       list->names
       (map-indexed
        #(hash-map :id %1 :name %2))))

(handler
 :set-list
 (fn [db list]
   (assoc db :raw-list list)))

(def default-rating 4)

(defn add-ratings
  [ratings players]
  (map
   (fn [{:keys [name] :as player}]
     (assoc player :rating (get ratings name default-rating)))
   players))

(handler
 :save-list
 (fn [{:keys [raw-list ratings] :as db}]
   (->> raw-list
        parse-list
        (add-ratings ratings)
        (map #(vector (:id %) %))
        (into {})
        (assoc db :players-list))))

(handler
 :clear-list
 (fn [db]
   (dissoc db :players-list)))

(handler
 :set-rating
 (fn [db id str-rating]
   (let [rating (js/parseInt str-rating)]
     (-> db
         (assoc-in [:players-list id :rating] rating)
         (assoc-in [:ratings (get-in db [:players-list id :name])] rating)))))

(handler
 :reset-ratings
 (fn [{:keys [players-list] :as db}]
   (-> db
       (assoc :players-list
              (reduce
               (fn [players [id]]
                 (assoc-in players [id :rating] default-rating))
               players-list
               players-list))
       (assoc :ratings {}))))

(re-frame/reg-flow
 {:id     :suggestions
  :inputs {:players-list [:players-list]
           :teams-number [:teams-number]}
  :output (fn [{:keys [players-list teams-number]}]
            (let [teams (cond->
                         {:white {:players [], :rating 0},
                          :black {:players [], :rating 0}
                          :green {:players [], :rating 0}}
                          (= 2 teams-number)
                          (dissoc :green))]
              {:best
               (-> players-list
                   vals
                   (algo/from-top teams #{})
                   vals)
               :worst
               (-> players-list
                   vals
                   (algo/from-top teams #{})
                   vals)
               :avg
               (when (= 2 teams-number)
                 (-> players-list
                     vals
                     (algo/avg-vs-rest
                      {:white {:players [], :rating 0},
                       :black {:players [], :rating 0}}
                      #{})
                     vals))}))
  :path   [:suggestions]})

(handler
 :set-teams-number
 (fn [db str-number]
   (assoc db :teams-number (js/parseInt str-number))))

(c.test/deftest test-parse-list
  (c.test/is (= (parse-list
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
                [{:id 0, :name "Adam"}
                 {:id 1, :name "Roman"}
                 {:id 2, :name "Sikor"}
                 {:id 3, :name "Karol"}
                 {:id 4, :name "Franek"}
                 {:id 5, :name "Darek R"}
                 {:id 6, :name "Dima"}
                 {:id 7, :name "Dawid"}
                 {:id 8, :name "Kamil"}
                 {:id 9, :name "Hamid"}])))
