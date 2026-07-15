(ns glassworks.glassworksadvisor
  "Glass Advisor client -- the *contained intelligence node* for the
  glass-and-glass-products manufacturing actor.

  It normalizes glass-panel-batch intake, drafts a per-jurisdiction/
  per-product-class glazing-standard evidence checklist, screens
  batches for an unresolved end-of-line optical/edge/inclusion defect,
  runs (drafts) the robot flexural-bend-test verification mission,
  drafts the batch-shipment action, and drafts the Glazing-Certificate-
  issuance action. CRITICAL: it is a smart-but-untrusted advisor. It
  returns a *proposal* (with a rationale + the fields it cited), never
  a committed record or a real robot dispatch/Glazing-Certificate
  issuance. Every output is censored downstream by
  `glassworks.governor` before anything touches the SSoT, and
  `:actuation/ship-glass-panel-batch`/`:actuation/issue-glazing-
  certificate` proposals NEVER auto-commit at any phase -- see README
  `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/ship-glass-panel-batch | :actuation/issue-glazing-certificate | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [glassworks.facts :as facts]
            [glassworks.registry :as registry]
            [glassworks.robotics :as robotics]
            [glassworks.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the batch, thickness-deviation figures or
  jurisdiction. High confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "ガラスパネルバッチ記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :glass-panel-batch/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-requirements
  "Per-jurisdiction/per-product-class glazing-standard evidence
  checklist draft. `:no-spec?` injects the failure mode we must defend
  against: proposing a checklist for a jurisdiction with NO official/
  industry spec-basis in `glassworks.facts` -- the Tempering Governor
  must reject this (never invent a jurisdiction/product-class's
  requirements)."
  [db {:keys [subject no-spec?]}]
  (let [a (store/batch db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction a))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式/業界spec-basisが見つかりません")
       :rationale  "glassworks.facts に未登録の法域/製品クラス。要件を推測で作らない。"
       :cites      []
       :effect     :verification/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式/業界ソース: " (:provenance sb) " / 根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :verification/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-eol-defect
  "End-of-line optical/edge/inclusion defect screening draft.
  `:eol-defect-unresolved?` on the batch record injects the failure
  mode: the Tempering Governor must HOLD, un-overridably, on any
  unresolved defect."
  [db {:keys [subject]}]
  (let [a (store/batch db subject)]
    (cond
      (nil? a)
      {:summary "対象バッチ記録が見つかりません" :rationale "no batch record"
       :cites [] :effect :eol-screen/set :value {:batch-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:eol-defect-unresolved? a))
      {:summary    (str (:batch-name a) ": 未解決の完成検査欠陥(光学歪み/エッジ欠陥/介在物)を検出")
       :rationale  "完成検査スクリーニングが未解決の欠陥を検出。人手確認とホールドが必須。"
       :cites      [:eol-check]
       :effect     :eol-screen/set
       :value      {:batch-id subject :verdict :unresolved}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:batch-name a) ": 未解決の完成検査欠陥なし")
       :rationale  "完成検査欠陥スクリーニング完了。"
       :cites      [:eol-check]
       :effect     :eol-screen/set
       :value      {:batch-id subject :verdict :resolved}
       :stake      nil
       :confidence 0.9})))

(defn- simulate-flexural-strength-test
  "Runs the robot flexural-bend-test verification mission
  (`glassworks.robotics`) -- a REAL time-stepped `physics-2d`
  simulation, not a hand-set field -- and drafts its result as a
  proposal. High confidence -- the mission itself is deterministic
  simulated telemetry derived from the batch's own recorded
  `:flexural-test-pin-mass-kg`/`:panel-thickness-actual-mm`, not an
  LLM guess; the Tempering Governor still independently re-derives
  :passed? from the batch's own persisted telemetry before any
  `:actuation/ship-glass-panel-batch` proposal may commit -- see
  `glassworks.governor`'s `robotics-simulation-violations`."
  [db {:keys [subject]}]
  (let [a (store/batch db subject)]
    (if (nil? a)
      {:summary "対象バッチ記録が見つかりません" :rationale "no batch record"
       :cites [] :effect :glass-panel-batch/upsert :value {:id subject :robotics-sim-verified? false}
       :stake nil :confidence 0.0}
      (let [{:keys [mission actions passed? sim-peak-flexural-force-n sim-peak-flexural-stress-mpa]}
            (robotics/simulate-flexural-test-cell subject a)]
        {:summary    (str subject ": 曲げ強度試験(ASTM C158)検証ミッション " (if passed? "合格" "不合格")
                          " (実測曲げ応力=" sim-peak-flexural-stress-mpa "MPa)")
         :rationale  (str "mission=" (:mission/id mission) " actions=" (count actions)
                          " sim-peak-flexural-force-n=" sim-peak-flexural-force-n
                          " sim-peak-flexural-stress-mpa=" sim-peak-flexural-stress-mpa)
         :cites      [(:mission/id mission)]
         :effect     :glass-panel-batch/upsert
         :value      {:id subject
                      :robotics-sim-verified? passed?
                      :sim-peak-flexural-force-n sim-peak-flexural-force-n
                      :sim-peak-flexural-stress-mpa sim-peak-flexural-stress-mpa
                      :robotics-sim-record {:mission-id (:mission/id mission)
                                            :actions (mapv #(dissoc % :action) actions)
                                            :passed? passed?}}
         :stake      nil
         :confidence 0.95}))))

(defn- propose-glass-panel-batch-shipment
  "Draft the actual GLASS-PANEL-BATCH-SHIPMENT action -- dispatching a
  real finished glass-panel batch onward to a downstream consumer
  (automotive glazing -> `cloud-itonami-isic-2910`/`-2920`, cover
  glass -> `cloud-itonami-isic-2630`). ALWAYS `:stake :actuation/ship-
  glass-panel-batch` -- this is a REAL-WORLD safety-critical act,
  never a draft the actor may auto-run. See README `Actuation`: no
  phase ever adds this op to a phase's `:auto` set
  (`glassworks.phase`); the governor also always escalates on
  `:actuation/ship-glass-panel-batch`. Two independent layers agree,
  deliberately."
  [db {:keys [subject]}]
  (let [a (store/batch db subject)]
    {:summary    (str subject " 向け出荷提案"
                      (when a (str " (batch=" (:batch-name a) ")")))
     :rationale  (if a
                   (str "thickness-deviation-actual-mm=" (:thickness-deviation-actual-mm a)
                        " spec=[" (:thickness-deviation-min-mm a) "," (:thickness-deviation-max-mm a) "]")
                   "バッチ記録が見つかりません")
     :cites      (if a [subject] [])
     :effect     :glass-panel-batch/mark-shipped
     :value      {:batch-id subject}
     :stake      :actuation/ship-glass-panel-batch
     :confidence (if (and a (not (registry/panel-thickness-deviation-out-of-range? a))) 0.9 0.3)}))

(defn- propose-glazing-certificate
  "Draft the actual GLAZING-CERTIFICATE action -- issuing a real
  Glazing/Glass Test Certificate certifying a batch as conforming to
  the applicable standard. ALWAYS `:stake :actuation/issue-glazing-
  certificate` -- this is a REAL-WORLD safety-critical act, never a
  draft the actor may auto-run. See README `Actuation`: no phase ever
  adds this op to a phase's `:auto` set (`glassworks.phase`); the
  governor also always escalates on `:actuation/issue-glazing-
  certificate`. Two independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [a (store/batch db subject)]
    {:summary    (str subject " 向けガラス試験証明書発行提案"
                      (when a (str " (batch=" (:batch-name a) ")")))
     :rationale  (if a
                   "jurisdiction-evidence-checklist referenced"
                   "バッチ記録が見つかりません")
     :cites      (if a [subject] [])
     :effect     :glass-panel-batch/mark-certified
     :value      {:batch-id subject}
     :stake      :actuation/issue-glazing-certificate
     :confidence (if a 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :glass-panel-batch/intake                    (normalize-intake db request)
    :glazing-standard-rules/verify                (verify-requirements db request)
    :end-of-line-quality/screen                  (screen-eol-defect db request)
    :robotics/simulate-flexural-strength-test    (simulate-flexural-strength-test db request)
    :actuation/ship-glass-panel-batch            (propose-glass-panel-batch-shipment db request)
    :actuation/issue-glazing-certificate         (propose-glazing-certificate db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたはガラス製品製造工場の出荷・試験証明書発行エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:glass-panel-batch/upsert|:verification/set|:eol-screen/set|"
       ":glass-panel-batch/mark-shipped|:glass-panel-batch/mark-certified) "
       "(:robotics/simulate-flexural-strength-test も :glass-panel-batch/upsert で "
       ":robotics-sim-verified? を提案する) "
       ":stake(:actuation/ship-glass-panel-batch か :actuation/issue-glazing-certificate か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域/製品クラスの要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :glazing-standard-rules/verify                 {:batch (store/batch st subject)}
    :end-of-line-quality/screen                    {:batch (store/batch st subject)}
    :robotics/simulate-flexural-strength-test      {:batch (store/batch st subject)}
    :actuation/ship-glass-panel-batch              {:batch (store/batch st subject)}
    :actuation/issue-glazing-certificate           {:batch (store/batch st subject)}
    {:batch (store/batch st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Tempering Governor
  escalates/holds -- an LLM hiccup can never auto-ship a glass-panel
  batch or auto-issue a Glazing Certificate."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :glassworksadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
