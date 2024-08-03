(ns replacement.structure.java-interop
  "Structure for java interop forms passed directly to the compiler.
  Provides the data formats for each known form and will be maintained
  as more are added. Specs are provided for each of the formats.")

(def form-class-name
  "A class name")

(def form-nested-class-name
  "A nested class name")

(def form-member-access
  "Access to instance property")

(def form-member-invoke
  "Invocation of an instance method")

(def form-static-member-access
  "Access to static class property")

(def form-static-member-invoke
  "Invocation of a static class method")

(def form-dot
  "Class and instance access")

(def form-double-dot
  "Class and instance access")

(def form-doto
  "Create a class and invoke access methods")

(def form-new
  "Create a class via new")

(def form-new-dot
  "Create a class using SomeClass.")

(def form-instance
  "Check the type of class")

(def form-set
  "Assignment on a class or instance")

(def form-memfn
  "Archaic mechanism to perform .method")

(def form-bean
  "Create a map from a class as a Java bean")

(def form-proxy
  "Create a proxy for the named class or interface")

(def form-reify
  "Implement an interface")

(def form-gen-class
  "Will create a class from the ns")

(def form-definterface
  "Define a Java interface")
