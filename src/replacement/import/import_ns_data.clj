(ns replacement.import.import-ns-data
  (:require
    [clojure.spec.alpha :as s]
    [replacement.import.db :as xt-db]
    [replacement.import.classpath :as cp]
    [replacement.import.forms :as forms]
    [replacement.import.hashing :as hashing]
    [replacement.import.ns :as ns-data-handling]
    [replacement.import.persist-state :refer [store-form]]
    [replacement.import.text2edn :as text2edn]
    [replacement.protocol.data :as data]
    [xtdb.api :as xt-api]))

(defn- add-conforming-reference-data*
  [an-ns-name {:keys [conformed] :as data}]
  (let [a-type (first conformed)
        {:keys [ref-fn]} (or (get forms/reference-type-data a-type)
                             (get forms/reference-type-data :unsupported))]
    (merge data (ref-fn an-ns-name conformed))))

(defn- add-reference-data*
  [an-ns-name conformed-ns-forms]
  (mapv (partial add-conforming-reference-data* an-ns-name)
        conformed-ns-forms))

(defn- conforming-ns-data
  [conformed-ns-forms]
  (->> conformed-ns-forms
       (filter #(and (:conformed %)
                     (= :ns (first (:conformed %)))))
       first))

(defn add-reference-data
  "Add name, types and other extractable reference data to maps of form data."
  [conforming-form-data]
  (when-let [ns-data (conforming-ns-data conforming-form-data)]
    (let [an-ns-name (-> ns-data :conformed last :ns-args :ns-name)
          ref-data (add-reference-data* an-ns-name conforming-form-data)]
      ref-data)))

(defn known-ns-version?
  "Check whether the given form is a conforming ns and exists in the DB with an identifier"
  [db-node {::data/keys [var-name] :as an-ns}]
  (let [the-ns-name (name var-name)]
    (boolean (when (and var-name (s/valid? ::data/ns-form (:form an-ns)))
               (-> (xt-api/db db-node)
                   (xt-api/q '{:find  [e]
                               :where [[e :ns/name the-ns-name]]})
                   (ffirst))))))

(defn update-ns-id
  "Saves the name of the form to the db-node using the digest of the form-ids as an ID."
  [db-node {::data/keys [var-name]} form-ids]
  (let [id (hashing/digest form-ids)]
    (xt-api/submit-tx db-node [[::xt-api/put {:xt/id   id
                                              :ns/name (name var-name)}]])
    id))

(defn import-ns
  [db-node ns-forms]
  (let [form-ids (map #(store-form db-node %) ns-forms)
        ns-form (first ns-forms)
        ns-id (update-ns-id db-node ns-form form-ids)]
    ns-id))

(defn import-ns-forms
  [ns-edn-forms]
  (let [forms (->> ns-edn-forms
                   (text2edn/whole-ns->spec-form-data)
                   (add-reference-data))
        ns-form (first forms)
        required-libs (-> (-> ns-form :conformed last)
                          (ns-data-handling/split-ns)
                          (ns-data-handling/ns-required-libs))]
    (import-ns xt-db/xtdb-node forms)
    required-libs))

(defn import-ns-form-text
  [form-text]
  (-> form-text
      (text2edn/text->edn-forms )
      (import-ns-forms)))