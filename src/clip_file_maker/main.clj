(ns clip-file-maker.main
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [jsonista.core :as j])
  (:use seesaw.core)
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dynamic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *timestamps* (atom []))
(def ^:dynamic *start* (atom nil))
(def timestamp-field (text :multi-line? true :text ""))
(def info-field (text :editable? false :text "press start, then end to insert clip timestamps"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; util
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- pad-number
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

(def get-vlc-status-command
  ;; @TODO: use ring
  ;; @TODO: use CLI args
  ;; @NOTE: this assumes vlc is start with:
  ;; `vlc --http-port 3000 --http-password 420`
  (str/split "curl -su :420 localhost:3000/requests/status.json" #" "))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run-vlc-status-command
  "Returns a parsed JSON object of VLC's status"
  [event]
  (let [result (apply shell/sh get-vlc-status-command)]
    (if-let [out (not-empty (:out result))]
      (j/read-value out)
      (alert event (str "error getting timetsamp from vlc: " (:err result))))))

(defn get-timestamp
  "Parse current timestamp from VLC's status"
  [event]
  (let [status-map (run-vlc-status-command event)]
    (-> status-map
        (get "time")
        (convert-to-timestamp))))

(defn start-handler
  "start button handler"
  [event]
  (let [current-timestamp (get-timestamp event)]
    (reset! *start* current-timestamp)
    (text! info-field (str "start: " current-timestamp))))

(defn end-handler
  "end button handler"
  [event]
  (let [end (get-timestamp event)
        start @*start*]
    (if start
      (let [start-end-string (str start " " end)]
        (do
          (swap! *timestamps* #(conj % start-end-string))
          (text! timestamp-field (str/join \newline @*timestamps*))
          (reset! *start* nil)
          (text! info-field (str "end: " end))))
      (alert event (str "<html>Pressed end button, without start.")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  [& args]
  (native!)
  (-> (frame
       :title "Clip File Maker"
       :on-close :exit
       :content
       (border-panel
        :border 10 :hgap 5 :vgap 20
        :center timestamp-field
        :south (horizontal-panel
                :items
                [info-field
                 (button :text "start" :listen [:action start-handler])
                 (button :text "end" :listen [:action end-handler])])))
      pack!
      show!))
