(ns replacement.import.digest
  (:import (java.security MessageDigest)
           (org.apache.commons.codec.binary Hex)))

(defn string->bytes
  [s]
  (.getBytes s))

(defn bytes->hex
  [^bytes array-buffer]
  (Hex/encodeHexString array-buffer))

(defn digest
  "Given some edn-data, produce a hex formatted digest using the given algorithm.
  The default algorithm is SHA-256."
  ([data]
   (digest data "SHA-256"))
  ([data algorithm]
   (let [s (pr-str data)]
     (let [md5 (MessageDigest/getInstance algorithm)]
       (->> (string->bytes s)
            (.digest md5)
            (bytes->hex))))))

