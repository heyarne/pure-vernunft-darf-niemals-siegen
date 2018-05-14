(ns ai-for-games.core
  (:require [clojure.string :as str])
  (:import [lenz.htw.gawihs.net NetworkClient]
           [javax.imageio ImageIO]
           [java.io File])
  (:gen-class))

;; TODO: Genetic algorithm
;; TODO: Race so that we always finish in time (one thread counts down, one thread gives as many results as possible)

;; the board is a 5x5 hexagonal grid; [] is a field that's empty, [:green] has
;; one green stone on it, [:green :red] has a green stone at the bottom and a
;; red stone on top and nil is a non-existant cell (either it disappeared or
;; it wasn't there to begin with)
(def board (atom [nil      nil      [:green] []  [:blue],
                  nil      [:green] []       []  [:blue],
                  [:green] []       nil      []  [:blue],
                  []       []       []       []  nil,
                  [:red]   [:red]   [:red]   nil nil]))

(defn print-board!
  "Prints a nice human-readable output of the current board to System.out (useful
  for debugging)"
  [board]
  (->> (map str board)
       ; [:green :green] is the longest string we might have and it's 15 chars long
       (map (partial format "%-15s"))
       (partition 5)
       (map str/join)
       (str/join "\n")
       (println)))

(defn on-top?
  "Predicate to tell whether a player is on top in a given cell"
  [cell player]
  (or (= [player] cell)
      (= player (last cell))))

(defn valid-move-starts
  "Gives us indices of cells that can be used as a starting point for moves"
  [board player]
  (keep-indexed (fn [idx cell]
                  (when (on-top? cell player) idx))
                board))

(defn possible-moves
  "Gives us all possible moves"
  [board idx player]
  (let [cell (nth board idx)
        neighbors {:top (when (> idx 4) (nth board (- idx 5)))
                   :left (when (> idx 0) (nth board (dec idx)))}]))

(defn accessible?
  "Can we enter a cell?"
  [cell]
  (and (not (nil? cell))
       (< (count cell) 2)))

(def icons (map #(str "resources/icons/" % ".png") ["aperture", "bolt", "bug"]))

(defn connect! []
  (let [icon (ImageIO/read (File. (rand-nth icons)))
        client (NetworkClient. nil "" icon)]
    (println "Player number" (.getMyPlayerNumber client)
             "Time limit" (.getTimeLimitInSeconds client)
             "Latency" (.getExpectedNetworkLatencyInMilliseconds client))
    (if-let [move (.receiveMove client)]
      (println "I should update, received move" (bean move))
      (println "I should make a move"))))

(defn -main
  [& args]
  (doall
   (map (fn [_] (future (connect!))) (range 3))))
