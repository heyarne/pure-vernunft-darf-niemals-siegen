(ns ai-for-games.minimax-test
  (:require [clojure.test :refer :all]
            [ai-for-games.minimax :refer :all]
            [ai-for-games.core :as game]
            [ai-for-games.helpers :refer [set-cell]]))

(def fixtures
  {:without-red (map #(if (= [:r] %) [] %) @game/board)
   :only-green (map #(if (= [:g] %) % []) @game/board)})

(deftest players-present-on-field
  (testing "The initial configuration"
    (is (= #{:r :g :b} (players-on-field @game/board))))
  (testing "A board with one player missing"
    (is (= #{:g :b} (players-on-field (fixtures :without-red)))))
  (testing "Only one player left on the field"
    (is (= #{:g} (players-on-field (fixtures :only-green))))))

(deftest scoring
  (testing "Should be -Infinity when player is not on the field"
    (is (= Double/NEGATIVE_INFINITY
           (score (fixtures :without-red) :r))))
  (testing "Should be +Infinity when player has won"
    (is (= Double/POSITIVE_INFINITY
           (score (fixtures :only-green) :g))))
  (testing "Should have a higher score when there are more own stones close by"
    (is (> (score-by-neighboring-friends (neighbor-cells (-> @game/board
                                                             (set-cell [4 0] nil)
                                                             (set-cell [3 0] nil)
                                                             (set-cell [2 1] [:r])
                                                             (set-cell [1 1] [:r])) :r) :r)
           (score-by-neighboring-friends (neighbor-cells @game/board :r) :r)
           (score-by-neighboring-friends (neighbor-cells (fixtures :without-red) :r) :r))))
  (testing "Should have a lower score when there are more enemies next to us"
    (let [enemies #{:r :g}]
      (is (> (score-by-neighboring-enemies (neighbor-cells @game/board :b)
                                           enemies)
             (score-by-neighboring-enemies (neighbor-cells (-> @game/board
                                                               (set-cell [7 8] [:g])) :b)
                                           enemies)
             (score-by-neighboring-enemies (neighbor-cells (-> @game/board
                                                               (set-cell [7 4] [:g])
                                                               (set-cell [7 5] [:r])) :b)
                                           enemies))))))

(deftest game-tree-structure
  (testing "A game tree of depth 0"
    (is (= {:board @game/board
            :player :r
            :next nil}
           (game-tree @game/board [:r :g :b] 0))))
  (testing "A game tree of depth 1"
    (let [depth-one (game-tree @game/board [:r :g :b] 1)
          next-level (first (:next depth-one))]
      (is (= :g (:player next-level)))
      (is (not= @game/board (:board next-level)))
      (is (= 0 (count (:next next-level))))))
  (testing "A game tree of depth 10"
    (let [how-deep-is-your-love (game-tree @game/board [:r :g :b] 10)
          really-deep (reduce #(%2 %1) how-deep-is-your-love (->> (cycle [:next first])
                                                                  (take 20)))]
      (is (map? really-deep))
      (is (= #{:board :player :next} (set (keys really-deep))))
      (is (nil? (:next really-deep))))))
