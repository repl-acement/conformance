(ns replacement.import2.forms
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [replacement.import2.clojure-core :as cc]
            [replacement.protocol.data :as data]))

(defn register-dependencies
  "Analyze the form for dependencies"
  [{:keys [current-ns-name] :as registry} var-name form]
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
    ;;NOTE: this very naive an wrongly assigns the scope of a local symbol
    ;; to the whole var.
    ;;WARNING: it may ignore a dependency if a local binding is shadowing
    ;; a var that is also used within the form outside of the local binding
    ;;TODO: maybe use zipers to better control the navigation within the
    ;; form and keep an accurate local scope
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
         (when (and (vector? node)
                    (= :method (first node))
                    (symbol? (second node)))
           (swap! local-symbols conj (second node)))

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
         ;;NOTE: a bunch of hacks to weed out the undesirable vars
         (when (and (symbol? node)
                    (not= '& node)
                    (not (string/ends-with? (str node) "#"))
                    (not (string/ends-with? (str node) "."))
                    (not (string/starts-with? (str node) "."))
                    (not (string/starts-with? (str node) "clojure"))
                    (not (string/ends-with? (str node) "__auto__"))
                    (not (cc/syms node))
                    (not (cc/special-forms node))
                    (not (@local-symbols node))
                    ;; probably the ugliest hack of them all, to get
                    ;; rid of java classes
                    (not (= java.lang.Class (type (resolve node)))))
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
