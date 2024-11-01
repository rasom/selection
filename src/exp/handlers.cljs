(ns exp.handlers
  (:require [clojure.edn :as clojure.edn]
            [re-frame.alpha :as re-frame]
            [clojure.string :as string]
            [cljs.test :as c.test]
            [exp.algo :as algo]
            ))

(def db-interceptor
  (re-frame/after
   (fn [db]
     (js/localStorage.setItem "db" (prn-str db)))))

(def all-interceptors [db-interceptor])

(defn handler [n h]
  (re-frame/reg-event-db
   n
   all-interceptors
   (fn [db [_ & params]]
     (apply h db params))))

(defn handler-fx [n h]
  (re-frame/reg-event-fx
   n
   all-interceptors
   (fn [{:keys [db] :as cofx} [_ & params]]
     (apply h cofx db params))))

(handler
 :init
 (fn [_ db]
   (clojure.edn/read-string db)))

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
  :inputs {:players-list [:players-list]}
  :output (fn [{:keys [players-list]}]
            {:best
             (-> players-list
                 vals
                 (algo/from-top
                  {:white {:players [], :rating 0},
                   :black {:players [], :rating 0}}
                  #{})
                 vals)
             :worst
             (-> players-list
                 vals
                 (algo/from-bottom
                  {:white {:players [], :rating 0},
                   :black {:players [], :rating 0}}
                  #{})
                 vals)
             :avg
             (-> players-list
                 vals
                 (algo/avg-vs-rest
                  {:white {:players [], :rating 0},
                   :black {:players [], :rating 0}}
                  #{})
                 vals)})
  :path   [:suggestions]})

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
