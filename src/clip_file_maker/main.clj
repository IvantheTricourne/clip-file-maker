(ns clip-file-maker.main
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [jsonista.core :as j]
   [clojure.java.io :as io])
  (:use seesaw.core)
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dynamic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *timestamps* (atom []))
(def ^:dynamic *start* (atom nil))
(def timestamp-field (text :multi-line? true))
(def info-field (text :editable? false
                      :text "press start, then end to insert clip timestamps"))

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
  (let [{:keys [exit out err]} (apply shell/sh get-vlc-status-command)]
    (if (zero? exit)
      (j/read-value out)
      (alert event (str "error getting timetsamp from vlc: " err)))))

(defn get-timestamp
  "Parse current timestamp from VLC's status"
  [event]
  (let [timestamp (-> event
                      (run-vlc-status-command)
                      (get "time")
                      (convert-to-timestamp))]
    (text! info-field (str "timestamp: " timestamp))
    timestamp))

(defn start-handler
  "start button handler"
  [event]
  (let [current-timestamp (get-timestamp event)]
    (reset! *start* current-timestamp)))

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
          (reset! *start* nil)))
      (alert event (str "<html>Pressed end button, without start.")))))

(defn save-to-file
  [event]
  (let [timestamps @*timestamps*
        output (io/file "clip-file")]
    (binding [*out* (io/writer output)]
      (println (str/join \newline timestamps)))
    (text! info-field (str "clip file saved: " output))))

(defn clear-clips
  [event]
  (reset! *timestamps* [])
  (text! timestamp-field (str/join \newline @*timestamps*))
  (text! info-field "cleared."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  [& args]
  (native!)
  (-> (frame
       :title "Clip File Maker"
       :on-close :exit
       :menubar
       (menubar :items
                [(menu :text "File" :items [(action :handler save-to-file
                                                    :name "Save")])
                 (menu :text "Edit" :items [(action :handler clear-clips
                                                    :name "Clear clips")])])
       :content
       (border-panel
        :border 10 :hgap 5 :vgap 20
        :center timestamp-field
        :south (vertical-panel
                :items
                [info-field
                 (horizontal-panel
                  :items
                  [(button :text "start" :listen [:action start-handler])
                   (button :text "end" :listen [:action end-handler])])])))
      pack!
      show!))
