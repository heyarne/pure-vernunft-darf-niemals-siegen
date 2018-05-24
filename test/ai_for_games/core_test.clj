(ns ai-for-games.core-test
  (:require [clojure.test :refer :all]
            [ai-for-games.core :refer :all]
            [ai-for-games.helpers :refer [set-cell]]))

(deftest all-allowed-starts
  (testing "An allowed start is every cell where the player is on top"
    (is (= [0 1 2 3 4] (valid-starts @board :r)))
    (is (= [36 46 56 66 76] (valid-starts @board :g)))
    (is (= [44 53 62 71 80] (valid-starts @board :b)))))

(deftest index-coord-conversion
  (testing "Transformations between coordinates and indices"
    (is (= [0 0] (idx->coord 0)))
    (is (= [1 0] (idx->coord 1)))
    (is (= [0 1] (idx->coord 9)))

    (is (= 0 (coord->idx [0 0])))
    (is (= 1 (coord->idx [1 0])))
    (is (= 9 (coord->idx [0 1]))))

  (testing "The starting positions as coords are given correctly"
    (is (= [[0 0] [1 0] [2 0] [3 0] [4 0]] (map idx->coord (valid-starts @board :r))))
    (is (= [[0 4] [1 5] [2 6] [3 7] [4 8]] (map idx->coord (valid-starts @board :g))))
    (is (= [[8 4] [8 5] [8 6] [8 7] [8 8]] (map idx->coord (valid-starts @board :b))))))

(deftest valid-moves
  (testing "A non-existing cell is not a valid move"
    (is (false? (valid-move? @board {:from [4 0] :to [5 0]} :g))))
  (testing "A cell with two stones on it is not a valid move"
    (is (false? (-> (set-cell @board [0 0] [:r :g])
                    (set-cell [0 1] [:b])
                    (valid-move? {:from [0 1] :to [0 0]} :b)))))
  (testing "A cell with our own stone already on it is not a valid move"
    (is (false? (valid-move? @board {:from [0 0] :to [1 0]} :r)))
    (is (false? (valid-move? @board {:from [0 4] :to [1 5]} :g)))
    (is (false? (valid-move? @board {:from [8 8] :to [8 7]} :b))))
  (testing "A cell without a neighboring stone of our own color is an invalid cell"
    (is (false? (valid-move? @board {:from [0 0] :to [0 1]} :r)))
    (is (false? (valid-move? @board {:from [0 4] :to [0 3]} :g)))
    (is (false? (valid-move? @board {:from [8 4] :to [7 3]} :b))))
  (testing "An empty cell is a valid move"
    (is (true? (valid-move? @board {:from [0 0] :to [1 1]} :r)))
    (is (true? (valid-move? @board {:from [0 4] :to [1 4]} :g)))
    (is (true? (valid-move? @board {:from [8 4] :to [7 4]} :b))))
  (testing "A cell with another player's stone on it is a valid move"
    (is (true? (-> (set-cell @board [1 1] [:g])
                   (valid-move? {:from [0 0] :to [1 1]} :r))))
    (is (true? (-> (set-cell @board [1 1] [:b])
                   (valid-move? {:from [0 0] :to [1 1]} :r))))
    (is (true? (-> (set-cell @board [1 4] [:r])
                   (valid-move? {:from [0 4] :to [1 4]} :g))))
    (is (true? (-> (set-cell @board [1 4] [:b])
                   (valid-move? {:from [0 4] :to [1 4]} :g))))
    (is (true? (-> (set-cell @board [7 4] [:r])
                   (valid-move? {:from [8 4] :to [7 4]} :b))))
    (is (true? (-> (set-cell @board [7 4] [:g])
                   (valid-move? {:from [8 4] :to [7 4]} :b))))))

(deftest move-making
  (let [board (apply-move @board {:from [0 4] :to [1 4]})]
    (testing "A cell disappears when empty after a move"
      (is (nil? (nth board (coord->idx [0 4])))))
    (testing "A stone should be added to an empty field"
      (is (= [:g] (nth board (coord->idx [1 4]))))))
  (testing "A stone should be put on top a non-empty field"
    (let [board (apply-move [[:r] [:b]] {:from [1 0] :to [0 0]})]
      (is (on-top? (first board) :b)))))
