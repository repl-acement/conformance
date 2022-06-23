(ns replacement.import2.import-ns-data
  (:require
   [clojure.spec.alpha :as s]
   [replacement.import.db :as xt-db]
   [replacement.import.classpath :as cp]
   [replacement.import2.forms :as forms]
   [replacement.import.hashing :as hashing]
   [replacement.import.ns :as ns-data-handling]
   [replacement.import.persist-state :refer [form-tx-data]]
   [replacement.import.text2edn :as text2edn]
   [replacement.protocol.data :as data]
   [xtdb.api :as xt-api]))

(defn add-reference-data
  "Add name, types and other extractable reference data to maps of form data."
  [conforming-form-data]

  (let [registry (reduce (fn [registry {:keys [conformed]}]
                           (forms/register-form registry conformed))
                         {:missing-dependencies #{}}
                         conforming-form-data)]
    registry))

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
  "Creates tx-data using the digest of the form-ids as an ID."
  [{::data/keys [var-name]} form-ids]
  (let [id (hashing/digest form-ids)]
    [::xt-api/put {:xt/id   id
                   :ns/name (name var-name)}]))

(defn import-ns-tx-data
  [ns-forms]
  (let [tx-data (reduce (fn [tx-datas form]
                          (conj tx-datas (form-tx-data form)))
                        [] ns-forms)
        form-ids (map #(-> % last :xt/id) tx-data)
        ns-id (hashing/digest form-ids)]
    (conj tx-data {:xt/id   ns-id
                   :ns/name (-> ns-forms first ::data/var-name name)})))


(defn import-ns-form-tx-data
  [form-text]
  (let [registry (-> form-text
                     (text2edn/text->edn-forms)
                     (text2edn/whole-ns->spec-form-data)
                     (add-reference-data))
        _ (println :u (:current-ns-name registry)
                   (count (:vars registry))
                   (keys (:vars registry)))
        ns-form-data (::data/form-data registry)



        required-libs (-> ns-form-data
                          (ns-data-handling/split-ns)
                          (ns-data-handling/ns-required-libs))]
    (concat []
            #_(import-ns-tx-data forms)
            ;; recurse over the required libs for this ns
            )
    #_    (some->> required-libs
                   (pmap cp/read-ns-form)
                   (pmap import-ns-form-tx-data))
    registry))

(defn import-new-ns
  [db-node form-text]
  (let [tx-data (import-ns-form-tx-data form-text)
        #_#_xt-tx-data (map (fn [tx]
                              [::xt-api/put tx]) tx-data)]
    #_(xt-api/submit-tx db-node xt-tx-data)
    tx-data))



(comment

  (time (import-new-ns xt-db/xtdb-node text2edn/hello-sample))
                                        ;"Elapsed time: 115.144397 msecs"
                                        ;=> #:xtdb.api{:tx-id 3182, :tx-time #inst"2022-06-19T21:57:22.972-00:00"}

  )
