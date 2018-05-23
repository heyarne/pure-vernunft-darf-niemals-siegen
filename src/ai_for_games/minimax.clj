(ns ai-for-games.minimax
  "This namespace contains the implementation of the minimax algorithm, as well
  as the functions we use for scoring."
  (:require [ai-for-games.core :refer [apply-move]]))

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
