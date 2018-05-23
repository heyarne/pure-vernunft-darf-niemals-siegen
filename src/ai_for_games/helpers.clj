(ns ai-for-games.helpers
  "Some helper functions that ease debugging / working with the data structures."
  (:require [clojure.string :as str]
            [ai-for-games.core :as game]))

(defn format-board
  "Prints a nice human-readable output of the current board to System.out"
  [board]
  (->> (map str board)
       (map (partial format "%-8s"))
       (partition game/per-row)
       (map str/join)
       (str/join "\n")))

(comment
  ;; this is how you use it:
  (println (format-board @board)))

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
                     (format-board (game/apply-move board (first moves)))))
      (recur (game/apply-move board (first moves)) (rest moves)))))
