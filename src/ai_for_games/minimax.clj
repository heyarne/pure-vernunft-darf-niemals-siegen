(ns ai-for-games.minimax
  "This namespace contains the implementation of the minimax algorithm, as well
  as the functions we use for scoring."
  (:require [clojure.walk :refer [postwalk]]
            [ai-for-games.core :refer [valid-starts idx->coord all-neighbors
                                       all-moves apply-move]]))

;; these dynamic vars can be used to influence the scoring

(def ^:dynamic *friend-factor* 1.0)
(def ^:dynamic *enemy-factor* 1.0)
(def ^:dynamic *immobilization-facor* 1.0)

(comment
  ;; you can call the scoring function like this to influence the weights
  (binding [*friend-factor* 2.0
            *enemy-factor* 1.5
            *immobilization-facor* -3.0]
    (score board player)))

;; for orientation, our game tree will look like this:
;; {:board b
;;  :player p
;;  :next [{:board b'
;;          :player p+1
;;          :next [...]},
;;          ...]}

(defn players-on-field
  "Returns a set of all players on the field"
  [board]
  (->> (flatten board)
       (remove nil?)
       (set)))

(defn neighbor-cells
  "Returns all neighboring cells for all cells that a player has stones on"
  [board player]
  (->> (valid-starts board player)
       (map idx->coord)
       (mapcat (partial all-neighbors board))
       (map second)))

(defn score-by-neighboring-friends
  "Returns a higher score if a player has many neighbors of its own color"
  [neighbor-cells player]
  ;; for each cell that we could make a move in our next round, we count
  ;; how many neighbors have our own color; the reasoning being that we're
  ;; more flexible if we have more of our own stones next to us
  (->> neighbor-cells
       (filter (partial some #{player}))
       count))

(defn score-by-neighboring-enemies
  "Returns a lower score when enemies are next to a player"
  [neighbor-cells enemies]
  (->> neighbor-cells
       (filter (partial some enemies))
       count
       (* -1)))

(defn score-by-immobilized-enemies
  "Returns a higher score when more enemies are below a player's stone"
  [board player]
  (->> (valid-starts board player)
       (map (partial nth board))
       (filter #(= 2 (count %)))
       count))

(defn score
  "Given a board configuration and player whose perspective we take, returns a
  score between -Infinity and +Infinity."
  [board player]
  (let [on-field (players-on-field board)]
    (cond
      ;; we lost, let's just try to avoid that
      (not (on-field player)) Double/NEGATIVE_INFINITY
      ;; we won, that would be pretty sweet
      (and (on-field player) (= (count on-field) 1)) Double/POSITIVE_INFINITY
      ;; ... and if neither is the case we need a more sophisticated scoring algorithm
      :else (let [neighbor-cells (neighbor-cells board player)
                  enemies (disj on-field player)
                  [friend-score enemy-score immobilized-score] (pvalues (score-by-neighboring-friends neighbor-cells player)
                                                                        (score-by-neighboring-enemies neighbor-cells enemies)
                                                                        (score-by-immobilized-enemies board player))]
              (+ (* friend-score *friend-factor*)
                 (* enemy-score *enemy-factor*)
                 (* immobilized-score *immobilization-facor*))))))

(defn game-tree
  "Given the current status of the game, will return a tree of possible outcomes
  up to a given depth."
  [board players depth]
  (let [turns (cycle players)]
    (when (> depth -1)
      (let [player (first turns)
            next-boards (->> (all-moves board player)
                             (map (fn [move]
                                    {:board (apply-move board move)
                                     :player player
                                     :move move})))]
        (map (fn [board-with-move]
               (assoc board-with-move
                      :next (game-tree board (rest turns) (dec depth))))
             next-boards)))))

(defn minimax
  "Picks the most promising path in a game tree"
  [node player]
  ;; we recurse until we've reached a leaf; when we've reached one, we score it.
  ;; after scoring we can discard all the leaves with sub-optimal score
  (if-let [next (:next node)]
    (let [next (->> (map #(minimax % player) (:next node))
                    (sort-by :score))]
      (assoc node
             :next next
             :score (:score (last next))))
    ;; we score a node such that each player picks the best move for themselves
    (assoc node :score (score (:board node) (:player node)))))

(defn pick-move
  "Complete application of the minimax algorithm"
  [board player]
  (->> (game-tree board [:r :g :b] 10)
       (sort-by :score)
       last
       :move))
