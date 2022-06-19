(ns replacement.import.persist-state
  (:require [clojure.spec.test.alpha :as stest]
            [replacement.import.db :as xt-db]
            [replacement.import.hashing :as hashing]
            [replacement.protocol.data :as data]
            [xtdb.api :as xt-api]))

(set! *warn-on-reflection* true)

(comment
  "Check all calls"
  (stest/instrument))

(defn store-form
  "Save the form using the db-node using the digest of its conformed data as an ID"
  [{::data/keys [ns-name var-name] :as form-data}]
  (let [qualified-form-name (str (name ns-name) "/" (name var-name))
        id (hashing/digest [:ns-name ns-name (:conformed form-data)])]
    (merge form-data {:xt/id     id
                      :form-name qualified-form-name})))

; TODO
; - add a project which contains
;   { :name "Blah"
;     :digest "sum of ns digests"}
;     :nses {:name->id {the-ns-name {:id uuid :digest "123"}}
;           :id->name {uuid  {:name the-ns-name :digest "123"}}}
;
;------- ^^^ the way forward


;; TODO lookup ns versions via deps.edn classpath data
;; associate each ns with the version of the source JAR file


(comment

  (require '[replacement.import.text2edn :as text2edn])

  (time (let [n (->> text2edn/hello-sample
                     (text2edn/text->edn-forms)
                     (text2edn/whole-ns->spec-form-data)
                     (add-reference-data))]
          (import-ns xt-db/xtdb-node n)))

  ;; Examples
  (def conforming-defn '(defn xy [x y {:keys [a b]}] (+ x y (/ a b))))
  (def non-conforming-defn '(defn xy (+ x y (/ a b))))

  )

