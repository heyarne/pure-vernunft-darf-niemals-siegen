(ns ai-for-games.core-test
  (:require [clojure.test :refer :all]
            [ai-for-games.core :refer :all]))

(def board' @board) ; < non-changing copy of the initial game state

(deftest all-allowed-starts
  (testing "An allowed start is every cell where the player is on top"
    (is (= [4 12 20 28 36] (valid-starts board' :g)))
    (is (= [8 17 26 35 44] (valid-starts board' :b)))
    (is (= [72 73 74 75 76] (valid-starts board' :r)))))

(deftest moving-in-the-field
  (testing "Top left moves one row up and one further inward"
    ;; at the edges
    (is (nil? (neighbor-idx 0 :top-left)))
    (is (nil? (neighbor-idx 8 :top-left)))
    (is (nil? (neighbor-idx 17 :top-left)))
    ;; in the field
    (is (= 4 (neighbor-idx 12 :top-left)))
    (is (= 5 (neighbor-idx 13 :top-left)))
    (is (= 8 (neighbor-idx 16 :top-left)))
    (is (= 68 (neighbor-idx 76 :top-left))))

  (testing "Top right moves one further inward"
    ;; at the edges
    (is (nil? (neighbor-idx 8 :top-right)))
    (is (nil? (neighbor-idx 17 :top-right)))
    ;; in the field
    (is (= 5 (neighbor-idx 4 :top-right)))
    (is (= 37 (neighbor-idx 36 :top-right)))
    (is (= 73 (neighbor-idx 72 :top-right))))

  (testing "Left takes the one that is \"above\" in our representation"
    ;; at the edges
    (is (nil? (neighbor-idx 0 :left)))
    (is (nil? (neighbor-idx 8 :left)))
    ;; in the field
    (is (= 4 (neighbor-idx 13 :left)))
    (is (= 8 (neighbor-idx 17 :left)))
    (is (= 67 (neighbor-idx 76 :left))))

  (testing "Right is one below, one further inward"
    ;; at the edges
    (is (nil? (neighbor-idx 8 :right)))
    (is (nil? (neighbor-idx 44 :right)))
    (is (nil? (neighbor-idx 72 :right)))
    ;; in the field
    (is (= 14 (neighbor-idx 4 :right)))
    (is (= 46 (neighbor-idx 36 :right)))
    (is (= 52 (neighbor-idx 42 :right))))

  (testing "Bottom left is one further outward"
    ;; at the edges
    (is (nil? (neighbor-idx 0 :bottom-left)))
    (is (nil? (neighbor-idx 36 :bottom-left)))
    (is (nil? (neighbor-idx 72 :bottom-left)))
    ;; in the field
    (is (= 7 (neighbor-idx 8 :bottom-left)))
    (is (= 43 (neighbor-idx 44 :bottom-left)))
    (is (= 75 (neighbor-idx 76 :bottom-left))))

  (testing "Bottom right is one below"
    ;; at the edges
    (is (nil? (neighbor-idx 72 :bottom-right)))
    (is (nil? (neighbor-idx 76 :bottom-right)))
    ;; in the field
    (is (= 13 (neighbor-idx 4 :bottom-right)))
    (is (= 17 (neighbor-idx 8 :bottom-right)))
    (is (= 45 (neighbor-idx 36 :bottom-right)))
    (is (= 72 (neighbor-idx 63 :bottom-right)))
    (is (= 76 (neighbor-idx 67 :bottom-right)))))

(deftest valid-moves
  (testing "A non-existing cell is not a valid move"
    (is (false? (valid-move? nil :g))))
  (testing "A cell with two stones on it is not a valid move"
    (is (false? (valid-move? [:r :r] :b))))
  (testing "A cell with a player's stone already on it is not a valid move"
    (is (false? (valid-move? [:r] :r)))
    (is (false? (valid-move? [:g] :g)))
    (is (false? (valid-move? [:b] :b))))
  (testing "An empty cell is a valid move"
    (is (true? (valid-move? [] :r)))
    (is (true? (valid-move? [] :g)))
    (is (true? (valid-move? [] :b))))
  (testing "A cell with another player's stone on it is a valid move"
    (is (true? (valid-move? [:r] :g)))
    (is (true? (valid-move? [:b] :g)))
    (is (true? (valid-move? [:g] :b)))
    (is (true? (valid-move? [:r] :b)))
    (is (true? (valid-move? [:g] :r)))
    (is (true? (valid-move? [:b] :r)))))

(deftest move-making
  (let [board (apply-move board' {:from 4 :to 5})]
    (testing "A cell disappears when empty after a move"
      (is (nil? (nth board 4))))
    (testing "A stone should be added to an empty field"
      (is (= [:g] (nth board 5)))))
  (testing "A stone should be put on top a non-empty field"
    (let [board (apply-move [[:r] [:b]] {:from 1 :to 0})]
      (is (on-top? (first board) :b)))))
