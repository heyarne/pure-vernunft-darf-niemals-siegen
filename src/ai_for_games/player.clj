(ns ai-for-games.player
  "This module implements different strategies for players."
  (:require [ai-for-games.core :refer [all-moves]]
            [ai-for-games.minimax :as minimax]))

(defrecord Player [n color strategy])

(defn player
  "Given a player number and an optional strategy, creates a player"
  ([n] (player n :minimax))
  ([n strategy] (let [colors [:r :g :b]]
                  (->Player n (nth colors n) strategy))))

(defmulti pick-move
  "Decides which move gets picked, based on the strategy of a player"
  (fn [_ player] (:strategy player)))

(defmethod pick-move :bottom-left [board player]
  ;; this just moves the first stone up, useful for debugging
  {:from [0 0] :to [1 1]})

(defmethod pick-move :random [board player]
  (rand-nth (all-moves board (:color player))))

(defmethod pick-move :minimax [board player]
  (minimax/pick-move board player))

(comment
  ;; now we can pick a move for player red like this:
  (let [player (player 0 :random)]
    (pick-move @board player))
  )
