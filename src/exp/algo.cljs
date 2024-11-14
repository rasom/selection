(ns exp.algo
  (:require
   [cljs.math :as math]
   [clojure.set :as clojure.set]
   [clojure.test :as c.test]))

(def rand-base 97)
(defn gen-seed! []
  (->> (range 1 8)
       (map (fn [r]
              [r (rand-int rand-base)]))
       (into {})))

(defn update-teams [teams peaked-team players]
  (update teams peaked-team
          (fn [team]
            (reduce
             (fn [team peaked-p]
               (->  team
                    (update :players conj peaked-p)
                    (update :rating + (:rating peaked-p))))
             team
             players))))

(defn- iterative-peaking [peak-players-fn peak-team-fn]
  (fn [players teams best-friends]
    (let [peaked-ps    (peak-players-fn players)
          with-besties (->> best-friends
                            (remove #(empty? (clojure.set/intersection % peaked-ps)))
                            (reduce clojure.set/union peaked-ps))
          next-ps      (remove with-besties players)
          peaked-team  (peak-team-fn teams)
          teams        (update-teams teams peaked-team with-besties)]
      (if (empty? next-ps)
        teams
        (recur next-ps teams best-friends)))))

(defn- peak-player-by-rating [seeds p-fn]
  (fn [players]
    (let [selected-rating (->> players
                               (sort-by :rating p-fn)
                               first
                               :rating)
          players-pool    (filter (fn [{:keys [rating]}]
                                    (= rating selected-rating))
                                  players)
          seed            (get seeds selected-rating 0)
          shift           (rem seed (count players-pool))]
      (set [(nth players-pool shift)]))))

(defn- peak-team-by-rating [p-fn]
  (fn [teams] (ffirst (sort-by (fn [[_ {:keys [rating]}]] rating) p-fn teams))))

(defn from-top [players teams best-friends seeds]
  ((iterative-peaking
    (peak-player-by-rating seeds >)
    (peak-team-by-rating <))
   players teams best-friends))

(defn from-bottom [players teams best-friends seeds]
  ((iterative-peaking
    (peak-player-by-rating seeds <)
    (peak-team-by-rating <))
   players teams best-friends))

(defn avg-vs-rest [players teams best-friends seeds]
  (let [half           (math/floor (/ (count players) 2))
        sorted-players (sort-by :rating > players)
        head-cnt       (math/ceil (/ half 2))
        tail-cnt       (- half head-cnt)
        t1-players     (concat (take head-cnt sorted-players)
                               (take-last tail-cnt sorted-players))
        t2-players     (take (- (count players) half)
                             (drop head-cnt sorted-players))]
    (-> teams
        (update-teams :white t1-players)
        (update-teams :black t2-players))))

(c.test/deftest test-from-top
  (c.test/is
   (= (from-top
       [{:name "Adam", :rating 10}
        {:name "Wojtek M", :rating 4}
        {:name "Sikor", :rating 6}
        {:name "Dawid", :rating 5}
        {:name "K fb", :rating 3}
        {:name "Damian", :rating 4}
        {:name "Rico", :rating 6}
        {:name "Marian", :rating 7}
        {:name "Pan Dariusz Rudnik", :rating 10}
        {:name "Hamid", :rating 8}]
       {:white {:players [], :rating 0} ;
        :black {:players [], :rating 0}}
       [#{{:name "Adam", :rating 10}
          {:name "Pan Dariusz Rudnik", :rating 10}}
        #{{:name "Wojtek M", :rating 4}
          {:name "Damian", :rating 4}}]
       {1 0, 2 0, 3 0, 4 0, 5 0, 6 0, 7 0})
      {:white
       {:players
        [{:name "Adam", :rating 10}
         {:name "Pan Dariusz Rudnik", :rating 10}
         {:name "Rico", :rating 6}
         {:name "Damian", :rating 4}
         {:name "Wojtek M", :rating 4}],
        :rating 34},
       :black
       {:players
        [{:name "Hamid", :rating 8}
         {:name "Marian", :rating 7}
         {:name "Sikor", :rating 6}
         {:name "Dawid", :rating 5}
         {:name "K fb", :rating 3}],
        :rating 29}})))

(c.test/deftest test-avg-vs-rest
  (c.test/is
   (= (avg-vs-rest
       [{:name "Adam", :rating 7}
        {:name "Roman", :rating 6}
        {:name "Sikor", :rating 5}
        {:name "Karol", :rating 2}
        {:name "Franek", :rating 4}
        {:name "Darek R", :rating 6}
        {:name "Dima", :rating 7}
        {:name "Dawid", :rating 3}
        {:name "Kamil", :rating 5}
        {:name "Hamid", :rating 6}]
       {:white {:players [], :rating 0} ;
        :black {:players [], :rating 0}}
       []
       {})
      {:white
       {:players
        [{:name "Adam", :rating 7}
         {:name "Dima", :rating 7}
         {:name "Roman", :rating 6}
         {:name "Dawid", :rating 3}
         {:name "Karol", :rating 2}],
        :rating 25},
       :black
       {:players
        [{:name "Darek R", :rating 6}
         {:name "Hamid", :rating 6}
         {:name "Sikor", :rating 5}
         {:name "Kamil", :rating 5}
         {:name "Franek", :rating 4}],
        :rating 26}})))
