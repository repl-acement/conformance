(ns replacement.structure.simple-forms
  "Structure for Clojure's simplest forms (numbers, strings, etc.)
  Provides the data formats for each known form and will be maintained
  as more are added. Specs are provided for each of the formats."
  (:require [clojure.spec.alpha :as s]
            [replacement.structure.specs :as specs]))

(def form-nil
  "Nil or null"
  {::specs/value nil
   ::specs/spec  (s/def ::form-nil nil?)})

(def form-number
  "Number"
  {::specs/value 0
   ::specs/spec  (s/def ::form-number number?)})

(def form-string
  "A string"
  {::specs/value ""
   ::specs/spec  (s/def ::form-string string?)})

(def form-char
  "A character"
  {::specs/value nil
   ::specs/spec  (s/def ::form-char char?)})

(def form-keyword
  "A Clojure keyword"
  {::specs/value nil
   ::specs/spec  (s/def ::form-keyword keyword?)})

(def form-symbol
  "A Clojure keyword"
  {::specs/value nil
   ::specs/spec  (s/def ::form-symbol symbol?)})


