(ns replacement.import.state
  (:require [replacement.import.hashing :as hashing]))

(defn record-state-change
  "Add updated-item to the current state under the change-id key.
  If the change-id does not exist, add it to a LIFO list of changes."
  [{:keys [changelog] :as state} updated-item change-id]
  (tap> [:updated-item updated-item :change-id change-id])
  (cond-> state
          (not (get state change-id)) (assoc :changelog (vec (cons change-id changelog)))
          :always (assoc change-id updated-item)))

(defn update-state
  [state updated-item distinction-key form-id]
  (let [digest-input (get updated-item distinction-key)
        digest (hashing/digest digest-input)]
    (record-state-change state updated-item (keyword form-id digest))))

