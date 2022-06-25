(ns replacement.import.text2edn
  (:require [clojure.spec.alpha :as s]
            [replacement.protocol.data :as spec-data])
  (:import (clojure.lang LineNumberingPushbackReader)
           (java.io StringReader)))

(defmacro with-read-known
  "Evaluates body with *read-eval* set to a \"known\" value,
   i.e. substituting true for :unknown if necessary."
  [& body]
  `(binding [*read-eval* false #_(if (= :unknown *read-eval*) true *read-eval*)]
     ~@body))

(defn text->edn-forms
  "Produce a sequentially ordered collection of edn forms, read from the given text.
  Throws on reader errors."
  [s]
  (tap> s)
  (let [EOF    (Object.)
        reader (LineNumberingPushbackReader. (StringReader. s))]
    (reduce (fn [forms [form _]]
              (if (identical? form EOF)
                (reduced forms)
                (conj forms form)))
            [] (repeatedly #(with-read-known (read+string reader false EOF))))))

(defn form->spec-formed
  "Obtain the conformed and unformed versions of the given form or explain-data for its non-conformance."
  [form]
  (try
    (let [pre-check (s/valid? ::spec-data/form form)
          conformed (and pre-check (s/conform ::spec-data/form form))]
      (cond-> {}
        pre-check (assoc :form form
                         :conformed conformed
                         :unformed (s/unform ::spec-data/form conformed))
        (not pre-check) (assoc :explain (s/explain-data ::spec-data/form form))))
    (catch Exception e
      (println form)
      (throw e))))

(defn whole-ns->spec-form-data
  "Produce a list of maps with conformed and unformed versions or explain-data for the given forms."
  [forms]
  (map form->spec-formed forms))

(defn ns-reference-data
  [ns-name [type {:keys [ns-args] :as data}]]
  (let [var-name (:ns-name ns-args)]
    [ns-name var-name type data]))

(defn def-reference-data
  [ns-name [type {:keys [var-name] :as data}]]
  [ns-name var-name type data])

(defn defn-reference-data
  [ns-name [type {:keys [defn-args] :as data}]]
  (let [var-name (:fn-name defn-args)]
    [ns-name var-name type data]))

(defn- add-reference-data*
  [conformed-forms]
  (let [ns-name (-> conformed-forms first second (get-in [:ns-args :ns-name]))]
    [ns-name (mapv
               (fn [form]
                 (cond
                   (= :ns (first form)) (ns-reference-data ns-name form)
                   (= :def (first form)) (def-reference-data ns-name form)
                   (= :defn (first form)) (defn-reference-data ns-name form)
                   :else form))
               conformed-forms)]))

(defn add-reference-data
  [conformed-list]
  (let [ref-data (->> conformed-list
                      (map #(:conformed %))
                      (add-reference-data*))]
    ref-data))

(def project-sample
  {:name     "Politeness"
   :deps-etc :map-acceptable-to-tools.deps})

(def hello-sample "(ns replacement.greet
\"Hello World.\"
  (:require \n
    [clojure.spec.alpha :as s]))

(def app \"repl-acement\")

(defn hello-world
  \"Welcome to repl-acement\"
  []
  (let [a \"hello\"
        b \"world\"])
  (str a \" \" b))

(defn greeting
  \"Have more interesting ways to greet\"
  {:api-version \"0.1.0\"}
  ([]
  (greeting \"you\"))
  ([name]
  {:pre [(string? name)]}
  (str \"Hello\" name (hello-world)))
  {:multi-arity-meta :valid-but-rarely-used})")

(def goodbye-sample "(ns replacement.goodbye
\"Goodbye Cruel World.\"
  (:require \n
    [clojure.spec.alpha :as s]))

(def app \"repl-acement\")

(defn goodbye-world
  \"Welcome to repl-acement\"
  []
  \"Goodbye cruel world\")

(defn goodbye
  \"Have more interesting ways to bid adieu\"
  {:api-version \"0.1.0\"}
  ([]
  (goodbye \"you\"))
  ([name]
  {:pre [(string? name)]}
  (str \"Goodbye\" name))
  {:multi-arity-meta :valid-but-rarely-used})")
