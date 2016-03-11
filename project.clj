(defproject rf-cons "0.1.0-SNAPSHOT"
  :dependencies [;; Backend ------------------------------
                 [org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [com.novemberain/monger "3.0.2"]
                 
                 ;; Frontend -----------------------------
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.5.1"]
                 [re-frame "0.6.0"]
                 [secretary "1.2.3"]
                 [garden "1.3.0"]
                 
                 ;; HTTP libs
                 [cljs-ajax "0.5.3"] ;; make requests
                 #_[com.cognitect/transit-cljs "0.8.237"] ;; process responses
                 
                 
                 ]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-6"]
            [lein-garden "0.2.6"]
            [lein-doo "0.1.6"]

            [lein-ring "0.9.7"]
            ]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"
                                    "resources/public/css/compiled"]

  :ring {:handler rf-cons.core/reloady-handler}
  
  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler rf-cons.core/reloady-handler ;; TODO "reloady" is for dev only
             }

  :garden {:builds [{:id "screen"
                     :source-paths ["src/clj"]
                     :stylesheet rf-cons.css/screen
                     :compiler {:output-to "resources/public/css/compiled/screen.css"
                                :pretty-print? true}}]}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "rf-cons.core/mount-root"}
                        :compiler {:main rf-cons.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/test.js"
                                   :main rf-cons.runner
                                   :optimizations :none}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main rf-cons.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}]})
