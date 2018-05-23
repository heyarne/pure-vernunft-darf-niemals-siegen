(ns ai-for-games.cli
  "Functions to actually interact with the server and convert the dry logic
  into commands to send over the network."
  (:require [clojure.tools.cli :refer [parse-opts]]
            [ai-for-games.core :as game]
            [ai-for-games.helpers :refer [println+ format-board]]
            [clojure.string :as str])
  (:import [lenz.htw.gawihs.net NetworkClient]
           [lenz.htw.gawihs Move]
           [javax.imageio ImageIO]
           [java.io File])
  (:gen-class))

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

(defn connect!
  "Starts the connection for a client and plays the game"
  [host team icon-path board]
  (let [board (atom @board) ; < every thread gets its own board. this makes reasoning a bit easier
        icon (ImageIO/read (File. icon-path))
        client (NetworkClient. host team icon)
        n (.getMyPlayerNumber client)
        p (game/player n)
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
            (let [move (game/pick-move @board p)]
              (println+ "Sending move" move)
              (send-move! client move))
            ;; we should update our game state
            (do
              (println+ "Got move!" move)
              (swap! board game/apply-move move)
              (println+ (format-board @board))))
          (recur (get-move! client)))))
    (println+ "Game over")))

;; for an explanation check out https://github.com/clojure/tools.cli#quick-start

(def cli-options
  [["-H" "--host HOSTNAME" "Host name or IP address"
    :default nil
    :default-desc "localhost"]
   ["-n" "--players N" "Amount of players"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 1 % 3) "Must be a number between 1 and 3 (inclusive)."]]
   ["-h" "--help"]])

(defn help [summary]
  (->> ["Spawns one or more clients to play the gawihs game."
        ""
        "Usage: gawihs-client [options]"
        ""
        "Options:"
        summary]
       (str/join \newline)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

;; the actual main function that will be invoked from the command line

(defn -main
  [& args]
  (let [{:keys [options summary]} (parse-opts args cli-options)]
    (if (or (:help options) (:errors options))
      ;; display the help message when asked for it or an error occured
      (exit (if (:errors options) 1 0) (help summary))
      ;; if not: ready, set, go!
      (dotimes [i (:players options)]
        (future (connect! (:host options) "Pure Vernunft" (nth icons i) game/board))))))
