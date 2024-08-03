(ns replacement.structure.def-forms
  "Structure for Clojure's complex defining forms.
  Provides the data formats for each known form and will be maintained
  as more are added. Specs are provided for each of the formats."
  (:require [clojure.spec.alpha :as s]))

(def form-defmulti
  "A multi-method definition")

(def form-defmethod
  "A defmethod definition")

;; TODO add more support for multimethod things

(def form-defprotocol
  "A protocol definition"
  {::specs/value nil
   ::specs/spec  (s/def ::form-defprotocol protocol?)})

;; TODO add more support for protocol things




