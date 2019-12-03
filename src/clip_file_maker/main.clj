(ns clip-file-maker.main
  (:use seesaw.core)
  (:gen-class))

(defn start-handler
  [event]
  (alert event (str "<html>Pressed start button.")))

(defn end-handler
  [event]
  (alert event (str "<html>Pressed end button.")))

(defn -main
  [& args]
  (native!)
  (-> (frame
       :title "Clip File Maker"
       :on-close :exit
       :content
       (border-panel
        :border 15 :hgap 15 :vgap 15
        :north (label "Hello, fucker.")
        :center (border-panel :west (button :text "start" :listen [:action start-handler])
                              :east (button :text "end" :listen [:action end-handler]))
        :south (scrollable (listbox :model ["hello" "motherfuck"]))))
      pack!
      show!))
