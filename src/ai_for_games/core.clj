(ns ai-for-games.core
  "This module describes the core primitives: What does the board look like,
  what are the rules, how do we make a move etc.")

;; NOTE: :r, :g, :b in the order of player 1, 2 and 3

;; TODO: Players get disqualified if they don't make a move in time
;; TODO: Players also get disqualified when they make invalid moves
;; TODO: Genetic algorithm
;; TODO: Race so that we always finish in time (one thread counts down, one thread gives as many results as possible)

(def per-row 9)

;; the board is a hexagonal grid with edges of length 5; [] is a field that's
;; empty, [:g] has one green stone on it, [:g :r] has a green stone a and a red
;; stone on top and nil is a non-existant cell (either it disappeared or it
;; wasn't there to begin with)

(def board (atom [[:r] [:r] [:r] [:r] [:r] nil  nil  nil  nil
                  []   []   []   []   []   []   nil  nil  nil
                  []   []   []   []   []   []   []   nil  nil
                  []   []   []   []   []   []   []   []   nil
                  [:g] []   []   []   nil  []   []   []   [:b]
                  nil  [:g] []   []   []   []   []   []   [:b]
                  nil  nil  [:g] []   []   []   []   []   [:b]
                  nil  nil  nil  [:g] []   []   []   []   [:b]
                  nil  nil  nil  nil  [:g] []   []   []   [:b]]))

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

(defn all-neighbors
  [board cell]
  (map (partial neighbor board cell) directions))

(defn valid-move?
  "Checks a cell to see whether we can move there"
  [board {:keys [from to]} player]
  (let [cell (nth board (coord->idx to))]
    (and
     (not (nil? cell)) ;; does the cell exist?
     (not (= 2 (count cell))) ;; is there still space?
     (not (some #{player} cell)) ;; are we not occupying it yet?
     ;; ... and is there one of our own stones next to the cell we're moving to?
     (let [from-idx (coord->idx from)
           board' (assoc board from-idx nil)
           surrounding-stones (->> (keep (partial neighbor board' to) directions)
                                   (mapcat second)
                                   (set))]
       (keyword? (surrounding-stones player))))))

(defn moves-from-cell
  "Gives us all possible moves for a cell"
  [board from-coord player]
  (->>
   ;; get all neighbors which are not nil
   (keep #(neighbor board from-coord %) directions)
   ;; keep only those that we can go to
   (filter (fn [[cell-coord cell]]
             (valid-move? board {:from from-coord :to cell-coord} player)))
   ;; ... and give them a nice representation
   (map (fn [[cell-coord cell]]
          {:from from-coord :to cell-coord}))))

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

(defn disqualify
  "Removes a player's stones from the field"
  [board player]
  (mapv (fn [cell]
          (if (some #{player} cell)
            (if (= 1 (count cell))
              nil
              (into [] (remove #{player} cell)))
            cell))
        board))
