(ns kouyou.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [kouyou.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[kouyou started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[kouyou has shut down successfully]=-"))
   :middleware wrap-dev})
