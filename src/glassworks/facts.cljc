(ns glassworks.facts
  "Per-jurisdiction / per-product-class glazing-standard evidence
  catalog -- the G2-style spec-basis table the Tempering Governor
  checks every `:glazing-standard-rules/verify` proposal against.

  This actor's flat-glass manufacturer produces TWO distinct product
  classes (see README `Scope note`):

    - **automotive safety glazing** (windshields, tempered side/rear
      glass) -- a GOVERNMENT-mandated vehicle-safety requirement in
      most jurisdictions, mirroring `cloud-itonami-isic-2910`'s own
      type-approval catalog shape.
    - **cover glass** (chemically-strengthened glass for smartphones/
      electronics, e.g. the Gorilla-Glass-style category) -- governed
      by voluntary INDUSTRY test-method standards (ASTM), not a
      vehicle-safety statute, the same 'industry spec vs government
      statute' honesty distinction `cloud-itonami-isic-2920`'s
      `bodyshop.facts` draws for its own AHSS material catalog.

  Real anchors seeded here (also cited in the README Scope note and
  `glassworks.robotics`'s own docstring for the flexural-strength
  bounds):
    - USA (automotive):  ANSI/SAE Z26.1 -- \"Safety Glazing Materials
                          for Glazing Motor Vehicles\", referenced by
                          49 CFR 571.205 (FMVSS 205). ASTM C1036
                          (Standard Specification for Flat Glass) is
                          cited alongside it as the flat-glass
                          baseline spec Z26.1 itself references for
                          flexural-strength test methods.
    - Japan (automotive): JIS R 3211 -- 自動車用安全ガラス (Safety
                          glazing materials for road vehicles),
                          stewarded by JISC (日本産業標準調査会).
    - UNECE (automotive,
      multi-market):      UNECE Regulation No. 43 -- Uniform
                          provisions concerning the approval of
                          safety glazing materials, adopted across
                          the EU, Japan and many other 1958-Agreement
                          contracting-party markets as a
                          type-approval basis alongside (or instead
                          of) each market's own national statute.
    - Cover glass
      (global, test-
      method-based,
      not jurisdiction-
      bound):            ASTM C158 -- Standard Test Methods for
                          Strength of Flat Glass by three-point/
                          four-point bend loading. ASTM International
                          is a voluntary standards body, not a
                          government -- this catalog entry cites it
                          honestly as an industry test-method
                          standard, never inflated into a statute.

  Coverage is reported HONESTLY: a jurisdiction/product-class not in
  this table has NO spec-basis. Seed values cite official/standards-
  body sources; this is a starting catalog, not a survey of every
  market or every OEM's own supplement. Where this ns is not
  confident of a citation for a given jurisdiction/product-class
  (e.g. Germany/EU-specific automotive glazing type-approval beyond
  the UNECE R43 multi-market basis, or a non-US/JP cover-glass
  regulatory anchor), it is deliberately left OUT rather than
  fabricated -- see `coverage`.")

(def catalog
  {"USA" {:name "United States"
          :product-class :automotive-safety-glazing
          :owner-authority "NHTSA (National Highway Traffic Safety Administration) / SAE International"
          :legal-basis "49 CFR 571.205 (FMVSS 205) referencing ANSI/SAE Z26.1 -- Safety Glazing Materials for Glazing Motor Vehicles; ASTM C1036 -- Standard Specification for Flat Glass (flat-glass baseline spec, flexural-strength test-method reference)"
          :national-spec "US self-certification of automotive safety-glazing conformity to ANSI/SAE Z26.1"
          :provenance "https://www.nhtsa.gov/"
          :required-evidence ["flexural-strength-test-report (ASTM C158 three-point/four-point bend)"
                              "flat-glass-baseline-conformance-report (ASTM C1036)"
                              "fragmentation-or-penetration-test-report (ANSI/SAE Z26.1 test class)"
                              "material-certification-record"]}
   "JPN" {:name "Japan"
          :product-class :automotive-safety-glazing
          :owner-authority "国土交通省 (MLIT) 自動車局 / 日本産業標準調査会 (JISC)"
          :legal-basis "JIS R 3211 自動車用安全ガラス (Safety glazing materials for road vehicles) / 道路運送車両の保安基準 (参考)"
          :national-spec "JIS R 3211準拠の自動車用安全ガラス適合証明"
          :provenance "https://www.jisc.go.jp/"
          :required-evidence ["曲げ強度試験報告書 (flexural-strength-test-report, ASTM C158相当の3点/4点曲げ)"
                              "破砕性試験報告書 (fragmentation-test-report)"
                              "完成検査連鎖記録 (end-of-line-quality-chain-of-custody-record)"
                              "材料証明記録 (material-certification-record)"]}
   "UNECE" {:name "UNECE 1958-Agreement contracting parties (EU/Japan/multi-market type-approval)"
            :product-class :automotive-safety-glazing
            :owner-authority "UNECE World Forum for Harmonization of Vehicle Regulations (WP.29)"
            :legal-basis "UNECE Regulation No. 43 -- Uniform provisions concerning the approval of safety glazing materials and their installation on vehicles"
            :national-spec "UNECE R43 type-approval conformity-of-production requirements for safety glazing"
            :provenance "https://unece.org/transport/vehicle-regulations"
            :required-evidence ["flexural-strength-test-report (ASTM C158 or equivalent three-point/four-point bend)"
                                "fragmentation-or-penetration-test-report (UNECE R43 annex test)"
                                "end-of-line-quality-chain-of-custody-record"
                                "material-certification-record"]}
   "COVER-GLASS" {:name "Cover / chemically-strengthened glass (global, test-method-based, not jurisdiction-bound)"
                   :product-class :cover-glass
                   :owner-authority "ASTM International"
                   :legal-basis "ASTM C158 -- Standard Test Methods for Strength of Flat Glass by three-point/four-point loading (voluntary industry test-method standard, not a government statute)"
                   :national-spec "ASTM C158 flexural-strength conformance for chemically-strengthened cover glass"
                   :provenance "https://www.astm.org/c0158-02r18.html"
                   :required-evidence ["flexural-strength-test-report (ASTM C158 three-point/four-point bend)"
                                       "ion-exchange-process-record (chemical-strengthening bath time/temperature log)"
                                       "optical-clarity-and-haze-report"
                                       "material-certification-record"]}})

(defn spec-basis [iso3] (get catalog iso3))

(defn coverage
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-2310 R0: " (count catalog)
                 " jurisdiction/product-class entries seeded. Extend "
                 "`glassworks.facts/catalog`, never fabricate a "
                 "jurisdiction or product-class's requirements.")})))

(defn required-evidence-satisfied?
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
