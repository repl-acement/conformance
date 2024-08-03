(ns replacement.import.forms
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [replacement.protocol.data :as data]))

(set! *warn-on-reflection* true)

(comment
  "Check all calls"
  (stest/instrument))

(s/def ::form-event-data
  (s/keys :req [::data/id
                ::data/type
                ::data/var-name
                ::data/ns-name
                ::data/form-data]))

;; Stronger spec:
;; the spec should ensure that the ::data/var-name matches the
;; name in the conformed form or vice-versa
;; (use fmap or bind ... probably fmap)

;; WIP - these methods take conformed data
;; they each have different keys to get the var name
;; - how can we normalize such that multi-method
;; dispatch is possible?
(defmulti conformed-type
  (fn [ns-name data]
    ;; what to do?
    ;; just one map?
    ))

(defmethod conformed-type ::ns
  [ns-name {:keys [ns-args] :as data}]
  {::data/ns-name   ns-name
   ::data/var-name  (:ns-name ns-args)
   ::data/type      ::ns
   ::data/form-data data})

(defmethod conformed-type ::def
  [ns-name {:keys [var-name] :as data}]
  {::data/ns-name   ns-name
   ::data/var-name  var-name
   ::data/type      ::def
   ::data/form-data data})

(defmethod conformed-type ::defn
  [ns-name {:keys [defn-args] :as data}]
  {::data/ns-name   ns-name
   ::data/var-name  (:fn-name defn-args)
   ::data/type      ::defn
   ::data/form-data data})

(defmethod conformed-type :default
  [ns-name data]
  {::data/ns-name   ns-name
   ::data/var-name  'unsupported
   ::data/type      'unsupported
   ::data/form-data data})
