(ns replacement.import2.forms
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.walk :as walk]
            [replacement.protocol.data :as data]
            [clojure.zip :as z]
            [replacement.import2.clojure-core :as cc]))

(defn register-dependencies [{:keys [current-ns-name] :as registry} var-name form]
  (let [dependencies (atom #{})
        missing-dependencies (atom #{})
        register (fn [x]
                   (let [var-ns (or (some-> (namespace x) symbol)
                                    (some-> (resolve x) meta :ns str symbol)
                                    current-ns-name)]
                     (if (get-in registry [var-ns :vars (-> x name symbol)])
                       (swap! dependencies conj x)
                       (swap! missing-dependencies conj [var-ns var-name x]))))
        data (or (get-in form [:defn-args :fn-tail])
                 (get-in form [:form-data :init-expr]))]
    (let [local-symbols (atom #{})]
      (walk/prewalk
       (fn [node]
         (when (and (vector? node)
                    (= :local-symbol (first node)))
           (swap! local-symbols conj (second node)))
         ;; a named anonymous fn
         (when (and (map? node)
                    (:fn-name node)
                    (symbol? (:fn-name node)))
           (swap! local-symbols conj (:fn-name node)))

         ;; dot forms
         (when (and (map? node)
                    (:method node)
                    (symbol? (:method node)))
           (swap! local-symbols conj (:method node)))

         ;; map destructuring
         (when (and (vector? node)
                    (= :map-destructure (first node)))
           (let [destructured-keys (:keys (second node))]
             (mapv #(swap! local-symbols conj %)
                   destructured-keys)))
         node)
       data)
      (walk/prewalk
       (fn [node]
         (when (and (symbol? node)
                    (not= '& node)
                    (not (clojure.string/ends-with? (str node) "#"))
                    (not (clojure.string/ends-with? (str node) "."))
                    (not (clojure.string/starts-with? (str node) "."))
                    (not (clojure.string/starts-with? (str node) "clojure"))
                    (not (clojure.string/ends-with? (str node) "__auto__"))
                    (not (cc/syms node))
                    (not (cc/special-forms node))
                    (not (@local-symbols node)))
           (when (= 'ret node)
             (println var-name @local-symbols))
           (register node))
         node)
       data))
    form
    (-> registry
        (update :missing-dependencies into @missing-dependencies)
        (assoc-in [current-ns-name :vars var-name ::data/dependencies]
                  ;; ignore self-referencing functions
                  (disj @dependencies var-name)))))

(set! *warn-on-reflection* true)

(comment
  "Check all calls"
  (stest/instrument))

(s/def ::form-event-data
  (s/keys :req [::data/id ::data/type ::data/var-name ::data/ns-name ::data/form-data]))
;; Stronger spec:
;; the ::data/name spec should ensure that is matches the name in the conformed form
;; or vice-versa

(defmulti register-form (fn [_registry [form-type _]] form-type))

(defmethod register-form :ns
  [{:keys [] :as registry}
   [form-type {:keys [ns-args] :as form}]]
  (assoc registry
         :current-ns-name (:ns-name ns-args)
         ::data/ns-name (:ns-name ns-args)
         ::data/var-name (:ns-name ns-args)
         ::data/type      form-type
         ::data/form-data form ))

(defmethod register-form :def
  [{:keys [current-ns-name] :as registry}
   [form-type {:keys [var-name] :as form}]]
  (assoc-in registry
            [current-ns-name :vars  var-name]
            {::data/ns-name   current-ns-name
             ::data/var-name var-name
             ::data/type      form-type
             ::data/form-data form}))

(defmethod register-form :defn
  [{:keys [current-ns-name] :as registry}
   [form-type {:keys [defn-args] :as form}]]
  (let [fn-name (:fn-name defn-args)]
    (-> registry
        (assoc-in [current-ns-name :vars fn-name]
                  {::data/ns-name   current-ns-name
                   ::data/var-name fn-name
                   ::data/type      form-type
                   ::data/form-data form})
        (register-dependencies fn-name form))))

(defmethod register-form :defmacro
  [{:keys [current-ns-name] :as registry}
   [form-type {:keys [defn-args] :as form}]]
  (let [fn-name (:fn-name defn-args)]
    (-> registry
        (assoc-in [current-ns-name :vars fn-name]
                  {::data/ns-name   current-ns-name
                   ::data/var-name fn-name
                   ::data/type      form-type
                   ::data/form-data form})
        (register-dependencies fn-name form))))

(defmethod register-form :default
  [{:keys [current-ns-name] :as registry}
   data]
  (update registry :unsupported conj
          {::data/ns-name   current-ns-name
           ::data/var-name  'unsupported
           ::data/type      'unsupported
           ::data/form-data data}))


(comment (-> (z/seq-zip
              '(defn rand
                 "Returns a random floating point number between 0 (inclusive) and
  n (default 1) (exclusive)."
        {:added "1.0"
         :static true}
        ([] (. Math (random)))
        ([n] (* n (rand)))))
    z/down z/right z/right
    z/right z/right z/down z/right))
