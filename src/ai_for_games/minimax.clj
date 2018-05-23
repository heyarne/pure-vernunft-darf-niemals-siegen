(ns ai-for-games.minimax
  "This namespace contains the implementation of the minimax algorithm, as well
  as the functions we use for scoring."
  (:require [ai-for-games.core :refer [all-moves apply-move]]))

;; for orientation, our game tree will look like this:
;; {:board b
;;  :player p
;;  :next [{:board b'
;;          :player p+1
;;          :next [...]},
;;          ...]}

(defn calc-next
  "Given a start configuration, an infinite sequence of a player's turns and
  a maximum depth, calculates all possible next configurations"
  [board turns depth]
  (when (> depth -1)
    (let [player (first turns)
          next-boards (->> (all-moves board player)
                           (map (partial apply-move board)))]
      (map (fn [board] {:board board
                        :player player
                        :next (calc-next board (rest turns) (dec depth))})
           next-boards))))

(defn game-tree
  "Given the current status of the game, will return a tree of possible outcomes
  up to a given depth."
  [board players depth]
  (let [turns (cycle players)]
    {:board board
     :player (first turns)
     :next (calc-next board (rest turns) (dec depth))}))

(defn players-on-field
  "Returns a set of all players on the field"
  [board]
  (->> (flatten board)
       (remove nil?)
       (set)))

(defn score
  "Given a board configuration, a move made and player whose perspective we
  take, returns a score between -Infinity and +Infinity."
  [board move player]
  (let [on-field (players-on-field board)]
    (cond
      ;; we lost, let's just try to avoid that
      (not (on-field player)) Double/NEGATIVE_INFINITY

      ;; we won, that would be pretty sweet
      (and (on-field player) (= (count on-field) 1)) Double/POSITIVE_INFINITY

      ;; TODO: More complicated scoring algorithms
      :else 0.0)))
