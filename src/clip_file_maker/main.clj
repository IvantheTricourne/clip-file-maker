(ns clip-file-maker.main
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell])
  (:use seesaw.core)
  (:gen-class))

(def ^:dynamic *timestamps* (atom []))
(def ^:dynamic *start* (atom nil))
(def timestamp-field (text :multi-line? true :text ""))

(defn pad-number
  [x]
  (let [x (str x)]
    (if (< (count x) 2)
      (str 0 x) x)))

(defn convert-to-timestamp
  [seconds]
  (let [carry-seconds (quot seconds 60)
        seconds (rem seconds 60)
        minutes (rem carry-seconds 60)
        hours (quot carry-seconds 60)]
    (str (pad-number hours) ":"
         (pad-number minutes) ":"
         (pad-number seconds))))

(defn get-timestamp
  "Should probably just get shell here and not convert yet"
  []
  #_(apply shell/sh (str/split "curl -su :420 localhost:3000/requests/status.json | jq '.time'" #" "))
  "520")

(defn start-handler
  [event]
  (let [current-timestamp (get-timestamp)]
    (reset! *start* current-timestamp)))

(defn end-handler
  [event]
  (let [current-timestamp (get-timestamp)
        start @*start*]
    (if start
      (let [start-end-string (str start " " current-timestamp)]
        (do
          (swap! *timestamps* #(conj % start-end-string))
          (text! timestamp-field (str/join \newline @*timestamps*))
          (reset! *start* nil)))
      (alert event (str "<html>Pressed end button, without start.")))))

(defn -main
  [& args]
  (native!)
  (-> (frame
       :title "Clip File Maker"
       :on-close :exit
       :content
       (border-panel
        :border 10 :hgap 5 :vgap 30
        :north (label "Hello, fucker.")
        :south (horizontal-panel
                :items
                [(button :text "start" :listen [:action start-handler])
                 (button :text "end" :listen [:action end-handler])])
        :center timestamp-field))
      pack!
      show!))
