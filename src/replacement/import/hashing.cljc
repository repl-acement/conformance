(ns replacement.import.hashing
  #?(:cljs (:require [promesa.core :as p])
     :clj  (:import (java.security MessageDigest)
                    (org.apache.commons.codec.binary Hex))))

(defn string->bytes
  [s]
  #?(:clj  (.getBytes s)
     :cljs (let [encoder (js/TextEncoder.)]                 ;; Always UTF-8
             (.encode encoder s))))

(defn bytes->hex
  [array-buffer]
  #?(:clj  (Hex/encodeHexString ^bytes array-buffer)
     :cljs (->> (js/Array.from (js/Uint8Array. array-buffer))
                (map #(.padStart (.toString % 16) 2 "0"))
                (apply str))))

(defn digest
  "Given some edn-data, produce a hex formatted digest using the given algorithm.
  The default algorithm is SHA-256.
  **CLJS note:** we use web-crypto: a built-in, promise based lib on all modern browsers."
  ([data]
   (digest data "SHA-256"))
  ([data algorithm]
   (let [s (pr-str data)]
     #?(:clj  (let [md5 (MessageDigest/getInstance algorithm)]
                (->> (string->bytes s)
                     (.digest md5)
                     (bytes->hex)))
        :cljs (p/let [web-crypto (.-crypto.subtle js/window)]
                (p/->> (string->bytes s)
                       (.digest web-crypto algorithm)
                       (bytes->hex)))))))

