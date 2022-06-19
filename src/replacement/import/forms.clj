(ns replacement.import.forms
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [replacement.protocol.data :as data]))

(set! *warn-on-reflection* true)

(comment
  "Check all calls"
  (stest/instrument))

(s/def ::form-event-data
  (s/keys :req [::data/id ::data/type ::data/var-name ::data/ns-name ::data/form-data]))
;; Stronger spec:
;; the ::data/name spec should ensure that is matches the name in the conformed form
;; or vice-versa

(defn ns-reference-data
  [ns-name [type {:keys [ns-args] :as data}]]
  {::data/ns-name   ns-name
   ::data/var-name  (:ns-name ns-args)
   ::data/type      type
   ::data/form-data data})

(defn def-reference-data
  [ns-name [type {:keys [var-name] :as data}]]
  {::data/ns-name   ns-name
   ::data/var-name  var-name
   ::data/type      type
   ::data/form-data data})

(defn defn-reference-data
  [ns-name [type {:keys [defn-args] :as data}]]
  {::data/ns-name   ns-name
   ::data/var-name  (:fn-name defn-args)
   ::data/type      type
   ::data/form-data data})

(defn unsupported-reference-data
  [ns-name data]
  {::data/ns-name   ns-name
   ::data/var-name  :unsupported
   ::data/type      :unsupported
   ::data/form-data data})

;; TODO re-implement as a multimethod
(def reference-type-data
  "Table of mechanisms to enrich conformed data per type"
  {:ns          {:spec   ::data/ns-form
                 :ref-fn ns-reference-data}
   :def         {:spec   ::data/def-form
                 :ref-fn def-reference-data}
   :defn        {:spec   ::data/defn-form
                 :ref-fn defn-reference-data}
   :unsupported {:spec   nil
                 :ref-fn unsupported-reference-data}})
