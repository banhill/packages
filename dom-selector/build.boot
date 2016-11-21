(set-env!
 :resource-paths #{"resources"}
 :dependencies '[[cljsjs/boot-cljsjs "0.5.2"  :scope "test"]])

(require '[cljsjs.boot-cljsjs.packaging :refer :all]
         '[boot.core :as boot]
         '[boot.tmpdir :as tmpd]
         '[clojure.java.io :as io]
         '[boot.util :refer [sh]])

(def +lib-version+ "0.1.0")
(def +version+ (str +lib-version+ "-0"))

(task-options!
 pom  {:project     'cljsjs/dom-selector
       :version     +version+
       :description "Decorate elements with delegated events"
       :url         "https://github.com/Raynos/dom-selector"
       :scm         {:url "https://github.com/cljsjs/packages"}
       :license     {"MIT" "https://opensource.org/licenses/MIT"}})

(deftask build-dd []
  (let [tmp (boot/tmp-dir!)]
    (with-pre-wrap
      fileset
      ;; Copy all files in fileset to temp directory
      (doseq [f (->> fileset boot/input-files)
              :let [target (io/file tmp (tmpd/path f))]]
        (io/make-parents target)
        (io/copy (tmpd/file f) target))
      (binding [boot.util/*sh-dir* (str (io/file tmp "dom-selector-master"))]
        ((sh "npm" "install"))
        ((sh "npm" "run" "build"))
        ((sh "npm" "install" "browserify"))
        ((sh "node" "node_modules/browserify/bin/cmd.js" "lib/dom_selector.js" "-s" "DomSelector" "-o" "dom-selector.inc.js")))
      (-> fileset (boot/add-resource tmp) boot/commit!))))

(deftask package []
  (comp
   (download :url "https://github.dev.pages/pro/dom-selector/archive/master.zip"
             :checksum "125FEDF2648BBFA3AD1312597B597315"
             :unzip true)

   (build-dd)
   (sift :move {#"^dom-selector(.*)/dom-selector.inc.js$" "cljsjs/dom-selector/development/dom-selector.inc.js"
                #"^dom-selector(.*)/dom_selector.css$" "cljsjs/dom-selector/common/dom_selector.css"})
   (minify :in "cljsjs/dom-selector/development/dom-selector.inc.js"
           :out "cljsjs/dom-selector/production/dom-selector.min.inc.js")
   (sift :include #{#"^cljsjs"})
   (deps-cljs :name "cljsjs.dom-selector")
   (pom)
   (jar)))
