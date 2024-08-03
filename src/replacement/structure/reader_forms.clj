(ns replacement.structure.reader-forms
  "Structure for reader forms.
  Provides the data formats for each known form and a means to extend
  beyond the existing subset of all possible forms. Specs are provided
  for each of the formats."
  (:require
    [clojure.spec.alpha :as s]
    [replacement.structure.specs :as specs]))

(def form-regex
  "A regex - TODO, probably via edamame"
  {::specs/value #""
   ::specs/spec  (s/def ::form-regex any?)})

(def form-set
  "A set of things"
  {::specs/value #{}
   ::specs/spec  (s/def ::form-set set?)})

(def form-list
  "A list of things"
  {::specs/value ()
   ::specs/spec  (s/def ::form-set list?)})

(def form-vector
  "A vector of things"
  {::specs/value []
   ::specs/spec  (s/def ::form-vector vector?)})

(def form-map
  "A map of things"
  {::specs/value {}
   ::specs/spec  (s/def ::form-map map?)})

(def form-array-map
  "A small ordered map of things"
  {::specs/value []
   ::specs/spec  (s/def ::form-array-map map?)})

(def form-deftype
  "A Java type"
  {::specs/value nil
   ::specs/spec  (s/def ::form-deftype class?)})

(def form-defrecord
  "A Java type over a map"
  {::specs/value nil
   ::specs/spec  (s/def ::form-defrecord class?)})

(def form-constructor
  "A constructor for the Java type, record or class
  - TODO, probably via edamame")

(def form-tag-literal
  "An EDN tagged value of something"
  {::specs/value nil
   ::specs/spec  (s/def ::form-tag-literal tagged-literal?)})

(def form-reader-conditional
  "A reader conditional value of something"
  {::specs/value nil
   ::specs/spec  (s/def ::form-reader-conditional reader-conditional?)})

