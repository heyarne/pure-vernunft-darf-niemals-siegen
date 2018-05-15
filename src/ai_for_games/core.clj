(ns ai-for-games.core
  (:require [clojure.string :as str])
  (:import [lenz.htw.gawihs.net NetworkClient]
           [javax.imageio ImageIO]
           [java.io File])
  (:gen-class))

;; NOTE: :r, :g, :b in the order of player 1, 2 and 3

;; TODO: Genetic algorithm
;; TODO: Race so that we always finish in time (one thread counts down, one thread gives as many results as possible)
;; TODO: Make sure -main can take two args, server and port

(def per-row 9)

;; the board is a hexagonal grid with edges of length 5; [] is a field that's
;; empty, [:g] has one green stone on it, [:g :r] has a green stone at the top
;; and a red stone on top and nil is a non-existant cell (either it disappeared
;; or it wasn't there to begin with)

(def board (atom [nil  nil  nil  nil  [:g] []   []   []   [:b]
                  nil  nil  nil  [:g] []   []   []   []   [:b]
                  nil  nil  [:g] []   []   []   []   []   [:b]
                  nil  [:g] []   []   []   []   []   []   [:b]
                  [:g] []   []   []   nil  []   []   []   [:b]
                  []   []   []   []   []   []   []   []   nil
                  []   []   []   []   []   []   []   nil  nil
                  []   []   []   []   []   []   nil  nil  nil
                  [:r] [:r] [:r] [:r] [:r] nil  nil  nil  nil]))

(defn print-board!
  "Prints a nice human-readable output of the current board to System.out (useful
  for debugging)"
  [board]
  (->> (map str board)
       (map (partial format "%-8s"))
       (partition per-row)
       (map str/join)
       (str/join "\n")
       (println)))

(comment
  ;; this is how you use it:
  (print-board! @board))

(defn on-top?
  "Predicate to tell whether a player is on top in a given cell"
  [cell player]
  (= player (last cell)))

(defn valid-starts
  "Gives us indices of cells that can be used as a starting point for moves"
  [board player]
  (keep-indexed (fn [idx cell]
                  (when (on-top? cell player) idx))
                board))

(def directions [:top-left :top-right :left :right :bottom-left :bottom-right])

(defn neighbor-idx
  "Returns the neighboring index in a given direction or nil if the neighbor is
  not part of the board"
  [idx direction]
  (let [right-most (dec per-row)
        left-most 0]
    (case direction
      :top-left (when (and (> idx per-row)
                           (< left-most (mod idx per-row) right-most))
                  (inc (- idx per-row)))
      :top-right (when (< (mod idx per-row) right-most)
                   (inc idx))
      :left (when (> idx per-row)
              (- idx per-row))
      :right (when (and (< idx (* per-row (dec per-row)))
                        (< (mod idx per-row) right-most))
               (inc (+ idx per-row)))
      :bottom-left (when (> (mod idx per-row) left-most)
                     (dec idx))
      :bottom-right (when (< idx (* per-row (dec per-row)))
                      (+ idx per-row)))))

(defn neighbor
  "Returns a neighbor as a cell and the corresponding index"
  [board idx direction]
  (when-let [n-idx (neighbor-idx idx direction)]
    [n-idx (nth board n-idx)]))

(defn valid-move?
  "Checks a cell to see whether we can move there"
  [cell player]
  (and (not (nil? cell))
       (not (= 2 (count cell)))
       (not (some #{player} cell))))

(defn moves-from-cell
  "Gives us all possible moves for a cell"
  [board idx player]
  (->>
   ;; get all neighbors
   (map #(neighbor board idx %) directions)
   ;; keep only those that we can go to
   (filter (fn [[n-idx cell]]
             (valid-move? cell :player)))
   ;; ... and give them a nice representation
   (map (fn [[n-idx cell]]
          {:from idx :to n-idx}))))

(defn all-moves
  "Gives us all possible moves for a player"
  [board player]
  (->>
   (valid-starts board player)
   (mapcat #(moves-from-cell board % player))))

(defn apply-move
  "Given a valid move, returns the board with this move applied"
  [board move]
  (let [from (let [from' (pop (nth board (move :from)))]
               (if (empty? from') nil from'))
        to (conj (nth board (move :to)) (peek (nth board (move :from))))]
    (-> board
        (assoc (move :from) from)
        (assoc (move :to) to))))

;; now we can implement different players that behave accordingly

(defrecord Player [color strategy])

(defmulti pick-move
  "Decides which move gets picked, based on the strategy of a player"
  (fn [_ player] (:strategy player)))

(defmethod pick-move :random [board player]
  (rand-nth (all-moves board (:color player))))

(comment
  ;; now we can pick a move like this:
  (let [player (map->Player {:color :g
                             :strategy :random})]
    (pick-move @board player))
  )

;; logic for actually connecting to the server and interacting with it

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

;; the function that will be invoked when calling the command line script

(defn -main
  [& args]
  (doall
   (map (fn [_] (future (connect!))) (range 3))))
