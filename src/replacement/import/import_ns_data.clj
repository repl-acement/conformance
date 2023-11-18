(ns replacement.import.import-ns-data
  (:require
    [clojure.spec.alpha :as s]
    [replacement.import.db :as db]
    [replacement.import.classpath :as cp]
    [replacement.import.forms :as forms]
    [replacement.import.digest :as digest]
    [replacement.import.ns :as ns-data-handling]
    [replacement.import.persist-state :refer [form-tx-data]]
    [replacement.import.text2edn :as text2edn]
    [replacement.protocol.data :as data]
    [xtdb.api :as db-api]))

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
               (-> (db-api/db db-node)
                   (db-api/q '{:find  [e]
                               :where [[e :ns/name the-ns-name]]})
                   (ffirst))))))

(defn update-ns-id
  "Creates tx-data using the digest of the form-ids as an ID."
  [{::data/keys [var-name]} form-ids]
  (let [id (digest/digest form-ids)]
    [::db-api/put {:xt/id   id
                   :ns/name (name var-name)}]))

(defn import-ns
  [db-node ns-forms]
  (let [tx-data (reduce (fn [tx-datas form]
                          (conj tx-datas [::db-api/put (form-tx-data form)]))
                        [] ns-forms)
        ns-form (first ns-forms)
        form-ids (map #(-> % last :xt/id) tx-data)
        ns-id-tx-data (update-ns-id ns-form form-ids)
        ns-id (-> ns-id-tx-data last :xt/id)
        ns-tx-data (conj tx-data ns-id-tx-data)]
    (db-api/submit-tx db-node ns-tx-data)
    ns-id))

(defn import-ns-tx-data
  [ns-forms]
  (let [tx-data (reduce (fn [tx-datas form]
                          (conj tx-datas (form-tx-data form)))
                        [] ns-forms)
        form-ids (map #(-> % last :xt/id) tx-data)
        ns-id (digest/digest form-ids)]
    (conj tx-data {:xt/id   ns-id
                   :ns/name (-> ns-forms first ::data/var-name name)})))


(defn import-ns-form-tx-data
  [form-text]
  (let [forms (-> form-text
                  (text2edn/text->edn-forms)
                  (text2edn/whole-ns->spec-form-data)
                  (add-reference-data))
        ns-form (first forms)
        required-libs (-> ns-form :conformed last
                          (ns-data-handling/split-ns)
                          (ns-data-handling/ns-required-libs))]
    (concat (import-ns-tx-data forms)
            ;; recurse over the required libs for this ns
            (flatten (some->> required-libs
                              (pmap cp/read-ns-form)
                              (pmap import-ns-form-tx-data))))))

(defn import-new-ns
  [db-node form-text]
  (let [tx-data (import-ns-form-tx-data form-text)
        xt-tx-data (map (fn [tx]
                          [::db-api/put tx]) tx-data)]
    (db-api/submit-tx db-node xt-tx-data)))

(comment

  (time (import-new-ns db/xtdb-node text2edn/hello-sample))
  ;"Elapsed time: 115.144397 msecs"
  ;=> #:xtdb.api{:tx-id 3182, :tx-time #inst"2022-06-19T21:57:22.972-00:00"}

  )
