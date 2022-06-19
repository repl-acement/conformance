(ns replacement.import.classpath
  (:require [clojure.tools.namespace.parse :as ns-parse]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.tools.namespace.file :as ns-file]
            [clojure.tools.namespace.repl :as ns-repl]
            [clojure.java.classpath :as classpath]
            [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [promesa.core :as p])
  (:import (java.io StringReader File PushbackReader)
           (java.util.jar JarFile)))

(set! *warn-on-reflection* true)
(set! *default-data-reader-fn* tagged-literal)

(defn ns-string->ns-decl
  "Obtain the ns declaration from a given ns as an unevaluated form."
  [s]
  (with-open [reader (PushbackReader. (StringReader. s))]
    (ns-parse/read-ns-decl reader)))

;; borrowed from c.t.n.file
(defmacro ^:private ignore-reader-exception
  "If body throws an exception caused by a syntax error (from
  tools.reader), returns nil. Rethrows other exceptions."
  [& body]
  `(try ~@body
        (catch Exception e#
          (if (= :reader-exception (:type (ex-data e#)))
            nil
            (throw e#)))))

(defn- read-stream*
  ([rdr]
   (read-stream* rdr nil))
  ([rdr read-opts]
   (let [EOF (Object.)
         opts (assoc (or read-opts ns-parse/clj-read-opts) :eof EOF)]
     (ignore-reader-exception
       (loop [content []]
         (let [form (reader/read opts rdr)]
           (if (identical? EOF form)
             content
             (recur (conj content form)))))))))

(defn read-stream
  [stream read-opts]
  (with-open [rdr (-> stream
                      (io/reader)
                      (PushbackReader.))]
    (read-stream* rdr read-opts)))

(defn read-jarfile-entry
  ([jarfile entry-name]
   (read-jarfile-entry jarfile entry-name nil))
  ([^JarFile jarfile ^String entry-name platform]
   {:text nil}
   #_(let [{:keys [read-opts]} (or platform ns-find/clj)
           text (->> (.getEntry jarfile entry-name)
                     (.getInputStream jarfile)
                     (slurp))]
       {:text text})))

(defn read-dir-entry
  ([file]
   (read-dir-entry file nil))
  ([^File file platform]
   {:text nil}
   #_(let [text (slurp file)]
       {:text text})))

(defn jar+ns-decls
  [jarfile]
  "Produce mapping from ns name to source and from source to ns name for entries in the given JAR"
  (reduce
    (fn [ns-decls source-location]
      (let [decl-ns (not-empty (ns-find/read-ns-decl-from-jarfile-entry jarfile source-location))
            name-ns (and decl-ns (ns-parse/name-from-ns-decl decl-ns))
            text+forms (and name-ns (binding [*ns* name-ns]
                                      (read-jarfile-entry jarfile source-location)))
            coords {:source-location source-location
                    :source-type     :jar-entry
                    :jar-file        jarfile
                    :decl-ns         decl-ns
                    :name-ns         name-ns}]
        (cond-> ns-decls
                (and decl-ns name-ns) (assoc name-ns (merge coords text+forms)
                                             source-location {:decl-ns     decl-ns
                                                              :source-type :jar-entry
                                                              :name-ns     name-ns}))))
    {} (ns-find/sources-in-jar jarfile)))

(defn jar-data
  [jars]
  (reduce
    (fn [ns-decls jar]
      (let [jarfile (JarFile. ^File jar)
            decls (not-empty (jar+ns-decls jarfile))]
        (cond-> (merge ns-decls decls)
                decls (assoc jar (jar+ns-decls jarfile)))))
    {} jars))

(defn dir-files+ns-decls
  [dir]
  "Produce mapping from ns name to source and from source to ns
  name for files below the given directory"
  (reduce
    (fn [ns-decls source-location]
      (let [decl-ns (not-empty (ns-file/read-file-ns-decl source-location))
            name-ns (and decl-ns (ns-parse/name-from-ns-decl decl-ns))
            text+forms (and name-ns (binding [*ns* name-ns]
                                      (read-dir-entry source-location)))
            coords {:source-location source-location
                    :source-type     :file
                    :dir             dir
                    :decl-ns         decl-ns
                    :name-ns         name-ns}]
        (cond-> ns-decls
                decl-ns (assoc name-ns (merge coords text+forms)
                               source-location {:decl-ns     decl-ns
                                                :source-type :file
                                                :name-ns     name-ns}))))
    {} (ns-find/find-sources-in-dir dir)))

(defn dir-data
  [dirs]
  (reduce (fn [ns-decls dir]
            (let [decls (not-empty (dir-files+ns-decls dir))]
              (cond-> (merge ns-decls decls)
                      decls (assoc dir decls))))
          {} dirs))

(defonce classpath-data
  (time (let [cp (classpath/classpath)
              cp-jars (filter classpath/jar-file? cp)
              cp-dirs (filter #(-> (.isDirectory ^File %)) cp)]
          (merge (jar-data cp-jars)
                 (dir-data cp-dirs)))))

(defn read-form
  ([form-name]
   (read-form form-name classpath-data))
  ([form-name cp-data]
   (let [cp-entry (get cp-data form-name)
         {:keys [source-type source-location]} cp-entry]
     (if (= :jar-entry source-type)
       (read-jarfile-entry (:jar-file cp-entry) source-location)
       (read-dir-entry source-location)))))

;;;; Q: is each path safe to be evaluated in parallel?

;; add a pipeline function to make a small change

;;;; eg to add #trace from flow-storm

;; simulate changes from the editor
;;;; eg to add a new outbound call to a function in this ns
;;;; eg to add a new outbound call to a function in a required ns
;;;; eg to add a new outbound call to a function in a new ns
;;;; eg to add a new outbound call to a function in new library ns

;; reload the changes
;;;; just reload all initially and lose state
;;;; need an in memory / DB version of ns-repl/refresh-all

;;;; Add datafy / nav to the code AST once the DB graph is designed

