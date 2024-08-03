(ns replacement.structure.special-forms
  "Structure for special forms passed directly to the compiler.
  Provides the data formats for each known form and will be maintained
  as more are added. Specs are provided for each of the formats."
  (:require [clojure.spec.alpha :as s]
            [replacement.structure.specs :as specs]))

(def form-def
  "A var definition"
  {::specs/value nil
   ::specs/spec  (s/def ::form-def var?)})

(def form-if
  "An if expression - TODO ")

(def form-do
  "A list of expressions - TODO")

(def form-let
  "A set of let bindings - TODO")

(def form-quoted
  "An unevaluated form - TODO")

(def form-var
  "A resolved value of a var - TODO")

(def form-fn
  "A function with one or more arity definitions"
  {::specs/value nil
   ::specs/spec  (s/def ::form-def fn?)})

(def form-loop
  "A loop entry point - TODO")

(def form-recur
  "A recursion to its entry point - TODO")

(def form-throw
  "A throw expression - TODO")

(def form-try
  "A wrapping try - TODO")

(def form-dot-interop
  "A wrapping try - TODO")


