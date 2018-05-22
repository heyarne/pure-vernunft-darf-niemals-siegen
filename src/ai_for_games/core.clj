(ns ai-for-games.core
  (:require [clojure.string :as str])
  (:import [lenz.htw.gawihs.net NetworkClient]
           [lenz.htw.gawihs Move]
           [javax.imageio ImageIO]
           [java.io File])
  (:gen-class))

;; NOTE: :r, :g, :b in the order of player 1, 2 and 3

;; TODO: It's confusing to have colors and numbers representing players :/
;; TODO: Players get disqualified if they don't make a move in time
;; TODO: Genetic algorithm
;; TODO: Race so that we always finish in time (one thread counts down, one thread gives as many results as possible)
;; TODO: Make sure -main can take two args, server and port

(def per-row 9)

;; the board is a hexagonal grid with edges of length 5; [] is a field that's
;; empty, [:g] has one green stone on it, [:g :r] has a green stone at the top
;; and a red stone on top and nil is a non-existant cell (either it disappeared
;; or it wasn't there to begin with)

(def board (atom [[:r] [:r] [:r] [:r] [:r] nil  nil  nil  nil
                  []   []   []   []   []   []   nil  nil  nil
                  []   []   []   []   []   []   []   nil  nil
                  []   []   []   []   []   []   []   []   nil
                  [:g] []   []   []   nil  []   []   []   [:b]
                  nil  [:g] []   []   []   []   []   []   [:b]
                  nil  nil  [:g] []   []   []   []   []   [:b]
                  nil  nil  nil  [:g] []   []   []   []   [:b]
                  nil  nil  nil  nil  [:g] []   []   []   [:b]]))

(defn format-board
  "Prints a nice human-readable output of the current board to System.out (useful
  for debugging)"
  [board]
  (->> (map str board)
       (map (partial format "%-8s"))
       (partition per-row)
       (map str/join)
       (str/join "\n")))

(comment
  ;; this is how you use it:
  (println (format-board @board)))

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

(defn coord->idx
  "The game uses two-dimensional indices, we have the array unrolled. Translates
  from coordinate to index."
  [[x y]]
  (+ (* y per-row) x))

(defn idx->coord
  "The game uses two-dimensional indices, we have the array unrolled. Translates
  from index to coordinate."
  [idx]
  (let [x (mod idx per-row)]
    [x (/ (- idx x) per-row)]))

(def directions [:left :top-left :top-right :right :bottom-left :bottom-right])

(defn neighbor
  "Returns a neighbor as a cell and the corresponding coordinates"
  [board [x y] direction]
  (let [cell-coord (case direction
                     :left [(dec x) y]
                     :top-left [x (inc y)]
                     :top-right [(inc x) (inc y)]
                     :right [(inc x) y]
                     :bottom-right [x (dec y)]
                     :bottom-left [(dec x) (dec y)])]
    (when (and (not-any? neg? cell-coord)
               (not-any? #(>= % per-row) cell-coord))
      [cell-coord (nth board (coord->idx cell-coord))])))

(defn valid-move?
  "Checks a cell to see whether we can move there"
  [cell player]
  (and (not (nil? cell))
       (not (= 2 (count cell)))
       (not (some #{player} cell))))

(defn moves-from-cell
  "Gives us all possible moves for a cell"
  [board coord player]
  (->>
   ;; get all neighbors
   (map #(neighbor board coord %) directions)
   ;; keep only those that we can go to
   (filter (fn [[cell-coord cell]]
             (valid-move? cell player)))
   ;; ... and give them a nice representation
   (map (fn [[cell-coord cell]]
          {:from coord :to cell-coord}))))

(defn all-moves
  "Gives us all possible moves for a player"
  [board player]
  (->>
   (valid-starts board player)
   (map idx->coord)
   (mapcat #(moves-from-cell board % player))))

(defn apply-move
  "Given a valid move, returns the board with this move applied"
  [board move]
  (let [move' {:from (coord->idx (:from move))
               :to (coord->idx (:to move))}
        from (let [from (pop (nth board (:from move')))]
               (when-not (empty? from) from))
        to (conj (nth board (:to move'))
                 (peek (nth board (:from move'))))]
    (-> board
        (assoc (:from move') from)
        (assoc (:to move') to))))

;; now we can implement different players that behave accordingly

(defrecord Player [n color strategy])

(defn player
  "Given a player number and an optional strategy, creates a player"
  ([n] (player n :random))
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

(comment
  ;; now we can pick a move for player red like this:
  (let [player (player 0)]
    (pick-move @board player))
  )

;; logic for actually connecting to the server and interacting with it

(def icons (map #(str "resources/icons/" % ".png") ["aperture", "bolt", "bug"]))

(defn send-move!
  "Sends a move to the client"
  [client move]
  (let [[from-x from-y] (:from move)
        [to-x to-y] (:to move)]
    (.sendMove client (Move. from-x from-y to-x to-y))))

(defn get-move!
  "A small helper to make a `Move` nicer to work with"
  [client]
  (when-let [m (.receiveMove client)]
    {:from [(.fromX m) (.fromY m)]
     :to [(.toX m) (.toY m)]}))

(defn invalid-move?
  "The client receives an invalid move when the game has ended"
  [move]
  (= move {:from [0 -1] :to [0 -1]}))

(def is-printing? (atom false))

(defn println+
  "A synchronized println"
  [& args]
  (loop []
    (dosync
     (if-not @is-printing?
       (do (swap! is-printing? not)
           (apply println args)
           (swap! is-printing? not))
       (recur)))))

(def making-move? (atom false)) ; < lock used so only one move is applied at once

(defn connect!
  "Starts the connection for a client and plays the game"
  [host team icon-path]
  (let [icon (ImageIO/read (File. icon-path))
        client (NetworkClient. host team icon)
        n (.getMyPlayerNumber client)
        p (player n)
        ;; FIXME: For now we assume everybody picks a move in time.
        time-limit (.getTimeLimitInSeconds client)
        latency (.getExpectedNetworkLatencyInMilliseconds client)]
    (println+ "Player number" n "Time limit" time-limit "Latency" latency)
    (loop [move (get-move! client)]
      (if (invalid-move? move)
        (println+ "Invalid move!" move)
        (do
          (if (nil? move)
            ;; it's time to pick a move
            (let [move (pick-move @board p)]
              (println+ "Sending move" move)
              (send-move! client (pick-move @board p)))
            ;; we should update our game state
            (do
              (dosync (when-not @making-move?
                        (reset! making-move? true)))
              (println+ "Got move!" move)
              (swap! board apply-move move)
              (reset! making-move? false)
              (println+ (format-board @board))))
          (recur (get-move! client)))))
    (println+ "Game over")))

;; below are some more helper functions for debugging

(defn log->moves
  "Parses log output into moves"
  [log-output]
  (->>
   (re-seq #"\d+,\d+ -> \d+,\d+" log-output)
   (map #(str/split % #" -> "))
   (map (fn [m] (->> (mapcat #(str/split % #",") m)
                     (map #(Long/parseLong %)))))
   (map (fn [[a b c d]] {:from [a b] :to [c d]}))))

(defn replay-game
  "Given a board and some moves, returns the board after
  all those moves are applied"
  [board moves]
  (println+ "Initial board config\n" (format-board board) "\n")
  (loop [board board
         moves moves]
    (when (seq moves)
      (println+ (str "Applying move" (first moves) "\n"
                     (format-board (apply-move board (first moves)))))
      (recur (apply-move board (first moves)) (rest moves)))))

;; the function that will be invoked when calling the command line script

(defn -main
  [& args]
  (dotimes [i 3]
    (future
      (Thread/sleep (* 100 i))
      (connect! nil (nth ["Red" "Green" "Blue"] i) (nth icons i)))))
