(ns replacement.import2.clojure-core)

(def special-forms
  '#{def
     if
     do
     let
     quote
     var
     loop
     recur
     throw
     try
     catch
     finally
     monitor-enter
     monitor-exit
     new
     set!
     })

;; because clojure.core is built in a very particular way,
;; around line 6000 the following files are loaded which likely contain these missing symbols
;; from the clojure.core ns:
;; (load "core_proxy")
;; (load "core_print")
;; (load "genclass")
;; (load "core_deftype")
;; (load "core/protocols")
;; (load "gvec")

(def syms
  '#{case*
     BlockingQueue
     satisfies?
     Class
     Runtime
     Object
     print-method
     Double
     map
     IExceptionInfo
     String
     print-dup
     load
     global-hierarchy
     tapq
     process-annotation
     cat
     Array
     IllegalArgumentException
     *file*
     tapset
     NumberFormatException
     Throwable
     inst-ms*
     Inst
     letfn*
     *compile-files*
     *loading-verbosely*
     Thread
     reify
     aset-int
     *clojure-version*
     Boolean
     BigInteger
     fn*
     *unchecked-math*
     *out*
     Math
     Integer
     *in*
     Short
     StackTraceElement
     Number
     flatten
     *ns*
     tap-loop
     *assert*
     Exception
     *print-readably*
     *flush-on-newline*
     *agent*
     *loaded-libs*
     BigDecimal
     Float
     *pending-paths*
     StringBuilder
     Long
     *print-dup*
     cancel})
