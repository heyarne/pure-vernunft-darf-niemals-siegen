(ns ai-for-games.darwin
  (:require [ai-for-games.core :as game]
            [ai-for-games.player :as player]
            [ai-for-games.minimax :refer [*friend-factor* *enemy-factor* *immobilization-facor*]]
            [ai-for-games.helpers :as h]))

(def ^:dynamic mutation-rate 0.1)
(def domain 50) ;; defines range +/- for each factor

;; 1. initialization

(defrecord Specimen
    [player friend-factor enemy-factor immobilization-factor])

(defn initialize-population
  "Generates 3 initial specimen with randomly selected attributes."
  []
  (for [i (range 3)]
    (let [friend-factor (- domain (rand (* 2 domain)))
          enemy-factor (- domain (rand (* 2 domain)))
          immobilization-factor (- domain (rand (* 2 domain)))]
      (Specimen. (player/player i :minimax) friend-factor enemy-factor immobilization-factor))))

;; 2. selection

(defn calculate-fitness
  "Lets all members of a population fight against each other until no one
  survives, returning the final score."
  [population]
  ;; initialize score with 0 for all specimina
  (loop [board @game/board
         turns (cycle population)
         score (zipmap population (repeat 0))]
    (println "\nturn: " (get-in (first turns) [:player :color]) "current board:")
    (println (h/format-board board))
    (if-let [current-turn (first turns)]
      ;; we still have some players in game
      (if-let [move (binding [*friend-factor* (:friend-factor current-turn)
                              *enemy-factor* (:enemy-factor current-turn)
                              *immobilization-facor* (:immobilization-factor current-turn)]
                      (player/pick-move board (:player current-turn)))]
        ;; we can still make a move
        (recur (game/apply-move board move)
               (rest turns)
               (update score current-turn inc))
        ;; our specimen is disqualified
        (recur (game/disqualify board (get-in current-turn [:player :color]))
               (remove (partial = current-turn) turns)
               score))
      ;; game over!
      score)))

;; 3. reproduction
