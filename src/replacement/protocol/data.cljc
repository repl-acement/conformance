(ns replacement.protocol.data
  (:require [clojure.spec.alpha :as s]
            #?(:clj  [clojure.core.specs.alpha :as core-specs]
               :cljs [replacement.protocol.cljs-fn-specs :as core-specs])
            [clojure.spec.test.alpha :as stest]
            [clojure.set :as set]
            [clojure.string :as string]
            [replacement.protocol.patched-core-specs]))

(comment
  "Check all calls"
  (stest/instrument))

(s/def ::id uuid?)

(s/def ::def-sym
  (s/and symbol?
         #(= 'def %)))

(s/def ::reify-sym
  (s/and symbol?
         #(= 'reify %)))

(s/def ::defn-sym
  (s/and symbol?
         #(or (= 'defn %)
              (= 'defn- %))))

(s/def ::fn-sym
  (s/and symbol?
         #(= 'fn %)))

(s/def ::defmacro-sym
  (s/and symbol?
         #(= 'defmacro %)))

(s/def ::ns-sym
  (s/and symbol?
         #(= 'ns %)))

(s/def ::dot-sym
  (s/and symbol?
         #(= '. %)))

(s/def ::loop-sym
  (s/and symbol?
         #(= 'loop %)))

(s/def ::dotimes-sym
  (s/and symbol?
         #(= 'dotimes %)))

(s/def ::doseq-sym
  (s/and symbol?
         #(= 'doseq %)))

(s/def ::for-sym
  (s/and symbol?
         #(= 'for %)))

(s/def ::with-open-sym
  (s/and symbol?
         #(= 'with-open %)))

(s/def ::let-sym
  (s/and symbol?
         #(= 'let %)))

(s/def ::if-let-sym
  (s/and symbol?
         #(= 'if-let %)))

(s/def ::when-let-sym
  (s/and symbol?
         #(= 'when-let %)))

(s/def ::when-some-sym
  (s/and symbol?
         #(= 'when-some %)))

(s/def ::if-some-sym
  (s/and symbol?
         #(= 'if-some %)))

(s/def ::require-sym
  (s/and symbol?
         #(= 'require %)))


(s/def ::minimal-string
  (s/and string? #(not (string/blank? %))))

(s/def ::var-name symbol?)

(s/def ::ns-name ::var-name)

(s/def ::type
  (s/or :ns ::ns-sym
        :dot ::dot-sym
        :def ::def-sym
        :defn ::defn-sym
        :defmacro :defmacro-sym
        :reify :reify-sym
        :let ::let-sym
        :with-open ::with-open-sym
        :loop ::loop-sym
        :dotimes ::dotimes-sym
        :for ::for-sym
        :doseq ::doseq-sym
        :if-let ::if-let-sym
        :when-let ::when-let-sym
        :if-some ::if-some-sym
        :when-some ::when-some-sym
        :require ::require-sym
        :other symbol?))

(s/def ::binding-form
  (s/or :local-symbol ::core-specs/local-name
        :seq-destructure ::seq-binding-form
        :map-destructure ::core-specs/map-binding-form))

(s/def ::seq-binding-form
  (s/and vector?
         (s/conformer identity vec)
         (s/cat :elems (s/* ::binding-form)
                :rest (s/? (s/cat :amp #{'&} :form ::binding-form))
                :as (s/? (s/cat :as #{:as} :sym ::core-specs/local-name)))))

(defn even-number-of-forms?
  "Returns true if there are an even number of forms in a binding vector"
  [forms]
  (even? (count forms)))

(s/def ::binding (s/cat :form ::core-specs/binding-form :init-expr ::form))
(s/def ::bindings (s/and vector?
                         even-number-of-forms?
                         (s/conformer identity vec)
                         (s/* ::binding)))

(s/def ::loop-form
  (s/cat
    :loop ::loop-sym
    :loop-args (s/cat :bindings ::bindings
                      :body (s/* ::form))))

(s/def ::dotimes-form
  (s/cat
    :dotimes ::dotimes-sym
    :dotimes-args (s/cat :bindings ::bindings
                         :body (s/* ::form))))

(s/def ::for-form
  (s/cat
    :for ::for-sym
    :for-args (s/cat :bindings ::bindings
                     :body (s/* ::form))))

(s/def ::doseq-form
  (s/cat
    :doseq ::doseq-sym
    :doseq-args (s/cat :bindings ::bindings
                       :body (s/* ::form))))

(s/def ::with-open-form
  (s/cat
    :with-open ::with-open-sym
    :with-open-args (s/cat :bindings ::bindings
                           :body (s/* ::form))))

(s/def ::let-form
  (s/cat
    :let ::let-sym
    :let-args (s/cat :bindings ::bindings
                     :body (s/* ::form))))

(s/def ::if-let-form
  (s/cat
    :if-let ::if-let-sym
    :if-let-args (s/cat
                   :bindings (s/and vector? ::binding)
                   :then ::form
                   :else (s/? ::form))))

(s/def ::when-let-form
  (s/cat :when-let ::when-let-sym
         :when-let-args (s/cat :bindings (s/and vector? ::binding)
                               :body (s/* ::form))))

(s/def ::if-some-form
  (s/cat
    :if-some ::if-some-sym
    :if-some-args (s/cat
                    :bindings (s/and vector? ::binding)
                    :then ::form
                    :else (s/? ::form))))

(s/def ::when-some-form
  (s/cat :when-some ::when-some-sym
         :when-some-args (s/cat :bindings (s/and vector? ::binding)
                                :body (s/* ::form))))

(s/def ::ns-form
  (s/cat
    :ns ::ns-sym
    :ns-args ::core-specs/ns-form))

(s/def ::params+body
  (s/cat :params ::core-specs/param-list
         :body (s/alt :prepost+body (s/cat :prepost map?
                                           :body (s/+ ::form))
                      :body (s/* ::form))))

(s/def ::defn-args
  (s/cat :fn-name simple-symbol?
         :docstring (s/? string?)
         :meta (s/? map?)
         :fn-tail (s/alt :arity-1 ::params+body
                         :arity-n (s/cat :bodies (s/+ (s/spec ::params+body))
                                         :attr-map (s/? map?)))))

(s/def ::fn-args
  (s/cat :fn-name (s/? simple-symbol?)
         :docstring (s/? string?)
         :meta (s/? map?)
         :fn-tail (s/alt :arity-1 ::params+body
                         :arity-n (s/cat :bodies (s/+ (s/spec ::params+body))
                                         :attr-map (s/? map?)))))
(s/def ::defn-form
  (s/cat
    :defn-type ::defn-sym
    :defn-args ::defn-args))

(s/def ::fn-form
  (s/cat
    :defn-type ::fn-sym
    :defn-args ::fn-args))

(s/def ::defmacro-form
  (s/cat
    :defn-type ::defmacro-sym
    :defn-args ::defn-args))

(s/def ::def-form
  (s/cat
    :def ::def-sym
    :var-name symbol?
    :docstring (s/? string?)
    :init-expr (s/+ any?)))

(s/def ::protocol-impl
  (s/cat :method symbol?
         :args ::core-specs/param-list
         :bodies (s/* ::form)))

(s/def ::reify-form
  (s/cat :reify ::reify-sym
         :reify-args (s/* (s/or :protocol symbol?
                                :impl (s/* ::protocol-impl)))))

(s/def ::require-form
  (s/cat :require ::require-sym
         :args (s/+ (s/alt :libspec ::core-specs/libspec
                           :prefix-list ::core-specs/prefix-list
                           :flag #{:reload :reload-all :verbose}))))

(s/def ::java-call
  (s/and list?
         (s/cat :method (s/and symbol?
                               ;; need a better discriminator cos this gets called too much
                               #(class? (some-> (namespace %)
                                                symbol
                                                resolve)))
                :args (s/* any?))))

(s/def ::java-class (s/and symbol?
                           #(class? %)))

(def primitives-string-names
  (set/map-invert primitives-classnames))

(s/def ::java-type (s/and symbol?
                          #(primitives-string-names (str %))))

;(s/def ::java-value #(type %))

;; [ ] (. instance-expr member-symbol)
;; [x] (. Classname-symbol member-symbol)
;; [ ] (. instance-expr -field-symbol)
;; [ ] (. instance-expr (method-symbol args*))
;; [ ] (. instance-expr method-symbol args*)
;; [x] (. Classname-symbol (method-symbol args*))
;; [x] (. Classname-symbol method-symbol args*)

(s/def ::dot-form
  (s/cat
    :dot ::dot-sym
    :classname symbol?
    :method+args (s/alt :method symbol?
                        :method+args (s/cat :method symbol?
                                            :args* (s/* ::form))
                        :method+args-sexp (s/or :method symbol?
                                                :method+args (s/cat :method symbol?
                                                                    :args* (s/* ::form))))
    ))

(s/def ::form
  (s/or :ns ::ns-form
        :def ::def-form
        :defn ::defn-form
        :dot-form ::dot-form
        :fn ::fn-form
        :reify ::reify-form
        :require ::require-form
        :java-call ::java-call
        :java-class ::java-class
        :java-type ::java-type
        :defmacro ::defmacro-form
        :loop ::loop-form
        :dotimes ::dotimes-form
        :for ::for-form
        :doseq ::doseq-form
        :with-open ::with-open-form
        :let ::let-form
        :if-let ::if-let-form
        :when-let-form ::when-let-form
        :if-some ::if-some-form
        :when-some-form ::when-some-form
        :expr (s/+ ::form)
        :any any?))

(s/def ::text ::minimal-string)

(s/def ::conformed (s/nilable map?))
(s/def ::explain map?)
(s/def ::unformed (s/nilable ::form))

(s/def ::form-data
  (s/keys :req-un [::text ::conformed ::unformed]
          :opt-un [::explain]))
