(ns leiningen.i18n
  "Plugin for i18n tasks. Start by using i18n init"
  (:require [leiningen.core.main :as l]
            [leiningen.core.eval :as e]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.java.shell :as sh :refer [sh]]))

(defn help
  []

" The i18n tooling expects that you have GNU make and the gettext tools
  installed.

  The following subtasks are supported:
    init  - add i18n tool support to the project, then run 'make help'
    make  - invoke 'make i18n'
")

(defn path-join
  [root & args]
  (apply str root (map #(str java.io.File/separator %) args)))

(defn bundle-package-name
  "Return the Java package name in which our resource bundles should
  live. This is the project's group and name in dotted notation. If the
  project has no group, we use 'nogroup'"
  [project]
  (namespace-munge
   (str (:group project "nogroup") "."
        (clojure.string/replace (:name project) "/" "."))))

(defn makefile-i18n-path
  [{:keys [target-path] :as project}]
  (path-join target-path "Makefile.i18n"))

(defn copy-makefile-to-target
  [{:keys [target-path compile-path] :as  project}]
  (.mkdirs (io/file target-path))
  (let [package (str "PACKAGE=" (bundle-package-name project))
        prefix (str "COMPILE_PATH=" (path-join compile-path "META-INF" "maven"))
        contents (-> (io/resource "leiningen/i18n/Makefile")
                     (clojure.string/replace #"PACKAGE=.*" package)
                     (clojure.string/replace #"COMPILE_PATH=.*" prefix))]
    (spit (io/as-file (makefile-i18n-path project))
          contents)))

(defn project-file
  "Construct a path in the project's root by appending rest to it and
  return a file"
  [{:keys [root] :as project} & rest]
  (io/as-file (apply path-join root rest)))

(defn ensure-contains-line
  "Make sure that file contains the given line, if not append it. If file
  does not exist yet, create it and put line into it"
  [file line]
  (if (.isFile file)
    (let [contents (slurp file)]
      (if-not (.contains contents line)
        (do
          (if-not (.endsWith contents "\n")
            (spit file "\n" :append true))
          (spit file (str line "\n") :append true))))
      (spit file (str line "\n"))))

(defn edit-toplevel-makefile
  "Add a line to include Makefile.i18n to an existing Makefile or create a
  new one with just the include statement"
  [project]
  (let [include-line (str "include " (makefile-i18n-path project))
        makefile (project-file project "Makefile")]
    (ensure-contains-line makefile include-line)))

(defn edit-gitignore
  "Add generated i18n files that should not be checked in to .gitignore"
  [project]
  (let [gitignore (project-file project ".gitignore")]
    (ensure-contains-line gitignore "/resources/locales.clj")
    (ensure-contains-line gitignore "/mp-*")))

(defn i18n-init
  [project]
  (l/info "Setting up Makefile; don't forget to check it in")
  (copy-makefile-to-target project)
  (edit-toplevel-makefile project)
  (edit-gitignore project))

(defn i18n-make
  [project]
  (l/debug "Running 'make i18n'")
  (copy-makefile-to-target project)
  (sh "make" "i18n" "-f" (makefile-i18n-path project)))

(defn abort
  [& rest]
  (apply l/abort (concat '("Error:") rest (list "\n\n" (help)))))

(defn i18n
  [project command]

  (when-not (:root project)
    (abort "The i18n plugin can only be run inside a project"))

  (condp = command
    nil       (abort "You need to provide a subcommand")
    "init"    (i18n-init project)
    "make"    (i18n-make project)
    (abort "Unexpected command:" command)))
