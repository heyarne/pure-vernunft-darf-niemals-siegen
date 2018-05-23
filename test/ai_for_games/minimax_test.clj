(ns ai-for-games.minimax-test
  (:require [clojure.test :refer :all]
            [ai-for-games.minimax :refer :all]
            [ai-for-games.core :as game]))

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
           (score (fixtures :without-red) {:from [0 4] :to [1 4]} :r))))
  (testing "Should be +Infinity when player has won"
    (is (= Double/POSITIVE_INFINITY
           (score (fixtures :only-green) {:from [4 8] :to [4 7]} :g)))))
