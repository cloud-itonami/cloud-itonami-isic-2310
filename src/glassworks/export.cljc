(ns glassworks.export
  "Audit-package export for social / regulatory hand-off.

  Produces plain EDN maps and CSV strings over a `glassworks.store/Store`
  snapshot -- the same append-only ledger, glass-panel-batch-shipment
  drafts and Glazing Certificate drafts the governor writes. Pure data
  transforms only: no I/O, no network, no signature. The
  manufacturer's own act is to sign and file the package; this
  namespace only materializes the package body.

  This is the honest delivery of the industry-stack `:export?` contract
  (robotics / audit-ledger capabilities) for ISIC 2310."
  (:require [clojure.string :as str]
            [glassworks.store :as store]))

(defn- csv-escape [v]
  (let [s (str (if (nil? v) "" v))]
    (if (re-find #"[,\"\n\r]" s)
      (str "\"" (str/replace s "\"" "\"\"") "\"")
      s)))

(defn- csv-row [cols]
  (str/join "," (map csv-escape cols)))

(defn ledger-rows
  "Normalize ledger facts into flat row maps suitable for CSV."
  [st]
  (mapv (fn [i f]
          {:seq i
           :t (:t f)
           :op (str (:op f))
           :actor (:actor f)
           :subject (:subject f)
           :disposition (str (:disposition f))
           :basis (pr-str (:basis f))
           :summary (:summary f)})
        (range)
        (store/ledger st)))

(defn shipment-rows [st]
  (mapv (fn [i r]
          {:seq i
           :record_id (get r "record_id")
           :kind (get r "kind")
           :batch_id (get r "batch_id")
           :jurisdiction (get r "jurisdiction")
           :downstream_consumers (pr-str (get r "downstream_consumers"))})
        (range)
        (store/shipment-history st)))

(defn evidence-rows [st]
  (mapv (fn [i r]
          {:seq i
           :record_id (get r "record_id")
           :kind (get r "kind")
           :batch_id (get r "batch_id")
           :jurisdiction (get r "jurisdiction")})
        (range)
        (store/evidence-history st)))

(defn batches-snapshot [st]
  (mapv (fn [b]
          (select-keys b [:id :batch-name :product-class :jurisdiction :status
                          :panel-thickness-actual-mm
                          :sim-peak-flexural-force-n :sim-peak-flexural-stress-mpa
                          :flexural-strength-min-mpa :flexural-strength-max-mpa
                          :thickness-deviation-actual-mm
                          :eol-defect-unresolved?
                          :glass-batch-shipped?
                          :glazing-certified?
                          :shipment-number
                          :evidence-number]))
        (store/all-batches st)))

(defn audit-package
  "Full audit package for a store snapshot -- the body a flat-glass
  manufacturer would hand to market-regulator inspectors, downstream-
  consumer quality auditors or internal compliance. `:format` is
  always `:edn-maps` for the nested package; use `package->csv-bundle`
  for CSV strings."
  [st]
  {:isic "2310"
   :business-id "cloud-itonami-isic-2310"
   :format :edn-maps
   :batches (batches-snapshot st)
   :ledger (vec (store/ledger st))
   :shipments (vec (store/shipment-history st))
   :glazing-certificates (vec (store/evidence-history st))
   :counts {:batches (count (store/all-batches st))
            :ledger (count (store/ledger st))
            :shipments (count (store/shipment-history st))
            :glazing-certificates (count (store/evidence-history st))}})

(defn rows->csv
  "Render a seq of flat maps as CSV using `header` column order."
  [header rows]
  (let [lines (into [(csv-row (map name header))]
                    (map (fn [r] (csv-row (map #(get r %) header))) rows))]
    (str (str/join "\n" lines) (when (seq lines) "\n"))))

(defn package->csv-bundle
  "CSV bundle for spreadsheet hand-off. Keys are filenames; values are
  CSV body strings."
  [st]
  {"batches.csv" (rows->csv [:id :batch-name :product-class :jurisdiction :status
                            :panel-thickness-actual-mm
                            :sim-peak-flexural-stress-mpa
                            :glass-batch-shipped? :glazing-certified?
                            :shipment-number :evidence-number]
                           (batches-snapshot st))
   "ledger.csv" (rows->csv [:seq :t :op :actor :subject :disposition :basis :summary]
                           (ledger-rows st))
   "shipments.csv" (rows->csv [:seq :record_id :kind :batch_id :jurisdiction :downstream_consumers]
                               (shipment-rows st))
   "glazing-certificates.csv" (rows->csv [:seq :record_id :kind :batch_id :jurisdiction]
                                   (evidence-rows st))})

#?(:clj
(defn write-csv-bundle!
  "Write `package->csv-bundle` files under `dir` (created if missing).
  Returns the absolute path of `dir`. JVM-only I/O seam for social
  hand-off scripts; pure package construction stays in `package->csv-bundle`."
  [st dir]
  (let [d (java.io.File. (str dir))
        _ (.mkdirs d)
        bundle (package->csv-bundle st)]
    (doseq [[name body] bundle]
      (spit (java.io.File. d (str name)) body))
    (.getAbsolutePath d))))
