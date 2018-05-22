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

(pop (nth @board 36))
(nth @board 37)


(print-board! (assoc @board 36 nil))

;; now we can implement different players that behave accordingly

(defrecord Player [n color strategy])

(defn player
  "Given a player number and an optional strategy, creates a player"
  ([n] (player n :bottom-left))
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

;; TODO: Use the server coordinates

(defn get-move!
  "A small helper to make a `Move` nicer to work with"
  [client]
  (when-let [m (.receiveMove client)]
    {:from [(.fromX m) (.fromY m)]
     :to [(.toX m) (.toY m)]}))

(defn game-ended?
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

(defn connect! [host team icon-path]
  (let [icon (ImageIO/read (File. icon-path))
        client (NetworkClient. host team icon)
        n (.getMyPlayerNumber client)
        p (player n)
        ;; TODO: Something with these two ↓
        time-limit (.getTimeLimitInSeconds client)
        latency (.getExpectedNetworkLatencyInMilliseconds client)]
    (println+ "Player number" n "Time limit" time-limit "Latency" latency)
    (loop [move (get-move! client)]
      (println+ "Got move for " n " - " move)
      (when-not (game-ended? move)
        (if (nil? move)
          (do
            (let [m (pick-move @board p)]
              (println+ "I'm player " p " and it's my turn"
                        "My move will be " m)
              #_(send-move! client (pick-move @board p))))
          (println+ "Got move!" move))
        (recur (get-move! client))))
    (println+ "Game over")))

;; the function that will be invoked when calling the command line script

(defn -main
  [& args]
  (doall
   (map #(future
           (connect! nil (str "Player " (inc %)) (nth icons %)))
        (range 3))))
