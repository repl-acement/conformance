(ns replacement.import.ns
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [clojure.core.specs.alpha :as core-specs]))

(defn- unformed-lib
  [conformed-libspec]
  (s/unform ::core-specs/libspec conformed-libspec))

(defn- unformed-require
  [conformed-require]
  (s/unform ::core-specs/ns-require conformed-require))


(defn- ns-require->properties
  [{:keys [lib options]}]
  {:lib    lib
   :alias  (:as options)
   :refers (:refer options)})

(defn- ns-basic-data->properties
  [{:keys [ns-name docstring]}]
  {:name      ns-name
   :docstring docstring})

(defn split-ns
  [conformed-ns-data]
  (let [ns-data (ns-basic-data->properties (:ns-args conformed-ns-data))
        libs (atom [])
        _walked (walk/postwalk
                  (fn [node]
                    (when (and (vector? node) (= :libspec (first node)))
                      (let [lib (last node)
                            unformed (unformed-lib lib)]
                        (swap! libs conj {:conformed (last node)
                                          :unformed  unformed
                                          :text      (pr-str unformed)
                                          :require   (ns-require->properties (last lib))})))
                    node)
                  conformed-ns-data)]
    (merge conformed-ns-data ns-data {:require-libs @libs})))

(defn ns-required-libs
  [{:keys [require-libs]}]
  (map #(get-in % [:require :lib]) require-libs))

