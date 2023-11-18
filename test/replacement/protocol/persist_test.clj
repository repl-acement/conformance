(ns replacement.protocol.persist-test
  (:require [clojure.test :refer [deftest is testing]]
            [replacement.import.db :as xt-db]
            [replacement.import.persist-state :as persist-state]
            [replacement.import.text2edn :as text-parsing]

            [xtdb.api :as xt-api]))

(def hello-sample "(ns replacement.greet \"Hello World.\"
  (:require [cljs.spec.alpha :as s]
            [zprint.core :as zp :refer [zprint-file-str]]))
(def app \"repl-acement\")
(defn hello-world \"Welcome to repl-acement\"
  []  \"Hello world\")
(defn greeting \"Have more interesting ways to greet\"
  {:api-version \"0.1.0\"}
  ([] (greeting \"you\"))
  ([name] {:pre [(string? name)]}
  (str \"Hello\" name))
  {:multi-arity-meta :valid-but-rarely-used})")

(def goodbye-sample "(ns replacement.goodbye \"Goodbye Cruel World.\"
  (:require
    [cljs.spec.alpha :as s]
    [zprint.core :as zp :refer [zprint-file-str]]))
(def app \"repl-acement\")
(defn goodbye-world \"Welcome to repl-acement\"
  []  \"Goodbye cruel world\")
(defn goodbye \"Have more interesting ways to bid adieu\"
  {:api-version \"0.1.0\"}
  ([] (goodbye \"you\"))
  ([name] {:pre [(string? name)]}
  (str \"Goodbye\" name))
  {:multi-arity-meta :valid-but-rarely-used})")

(defn test-form-name<->form-id-in-sync
  [{:keys [form-name->id form-id->name]} an-ns-name form-name]
  (let [name-form-name (symbol (name form-name))
        app-form-id (get form-name->id form-name)
        id-digest (name app-form-id)
        app-form-name (get form-id->name app-form-id)
        [the-ns-name the-form-name form-digest] (persist-state/destruct-form-name app-form-name)]
    (and (= name-form-name the-form-name)
         (= id-digest form-digest)
         (= an-ns-name the-ns-name))))


(deftest adding-ns-to-xtdb
  (let [hello-edn (->> hello-sample
                       (text-parsing/text->edn-forms)
                       (text-parsing/whole-ns->spec-form-data)
                       (persist-state/add-reference-data))
        goodbye-edn (->> goodbye-sample
                         (text-parsing/text->edn-forms)
                         (text-parsing/whole-ns->spec-form-data)
                         (persist-state/add-reference-data))]
    (testing "That we can add an ns to the database"
      (let [hello-db (persist-state/add+index-ns {} hello-edn)]
        (is hello-db)))))

(deftest adding-ns-tests
  (let [hello-edn (->> hello-sample
                       (text-parsing/text->edn-forms)
                       (text-parsing/whole-ns->spec-form-data)
                       (persist-state/add-reference-data))
        goodbye-edn (->> goodbye-sample
                         (text-parsing/text->edn-forms)
                         (text-parsing/whole-ns->spec-form-data)
                         (persist-state/add-reference-data))]
    (testing "That we can add an ns to the database"
      (let [hello-db (persist-state/add+index-ns {} hello-edn)]
        (is hello-db)))
    (testing "That we can add > 1 ns to the database"
      (let [hello-db (persist-state/add+index-ns {} hello-edn)
            goodbye-db (persist-state/add+index-ns hello-db goodbye-edn)]
        (is (and hello-db goodbye-db))
        (is (not= hello-db goodbye-db))))
    (testing "That adding nses is idempotent"
      (let [hello-db (persist-state/add+index-ns {} hello-edn)
            goodbye-db (persist-state/add+index-ns hello-db goodbye-edn)
            same-db (persist-state/add+index-ns goodbye-db hello-edn)
            same2-db (persist-state/add+index-ns goodbye-db goodbye-edn)]
        (is (and hello-db goodbye-db))
        (is (not= hello-db goodbye-db))
        (is (= same-db goodbye-db))
        (is (= same2-db goodbye-db))))
    (testing "That the names, ids and digests are indexed and in sync"
      (let [hello-db (persist-state/add+index-ns {} hello-edn)
            the-ns-name (-> (:ns-name->ns-id hello-db) keys first)]
        (is (every? true? (map #(test-form-name<->form-id-in-sync hello-db the-ns-name %) (keys (:form-name->id hello-db)))))))
    (testing "All the forms are in the ns-name->forms list"
      (let [hello-db (persist-state/add+index-ns {} hello-edn)
            ns-name->forms-set (-> (:ns-name->forms hello-db) vals first set)
            form-id-set (-> (:form-id->name hello-db) keys set)]
        (is (= ns-name->forms-set form-id-set))))))
