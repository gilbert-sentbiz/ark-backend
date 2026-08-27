--liquibase formatted sql

-- PI-246: 한국 금융기관(FI) 엔티티 세그먼트 + 서류 규칙 프론트 파리티.
-- 프론트(PI-220) enabledCommonDocTypes(세그먼트별 공통 서류 화이트리스트)를
-- 백엔드 doc_overrides(disabled = 전체공통 − enabled) 로 표현.
-- createDocumentsForCase 는 매칭 세그먼트(엔티티+서비스)의 서류를 union(distinctBy type)하므로,
-- 각 세그먼트의 enabled 를 정확히 시드하면 union 결과가 프론트와 100% 일치.
--   전체 공통(9): BIZ_REGISTRATION, ID_COPY, SHAREHOLDER_LIST, CONTRACT, SAMPLE_INVOICE_SHIPPING,
--                 BANK_PROOF, WEBSITE_URL, CORPORATE_REGISTRY, SEAL_CERTIFICATE
--   결과: CORP 9 / FI 8(공통6+own2) / INDIV 6 (서비스 SVC_PAYOUT 기준)

--changeset ark-dev:V005-doc-registry-seal-to-common
-- 법인등기부·법인인감: CORP own → common (법인·FI 공용). 세그먼트별 doc_overrides 로 노출 제어.
UPDATE doc_template
   SET classification = 'common', owner_segment_id = null
 WHERE type IN ('CORPORATE_REGISTRY', 'SEAL_CERTIFICATE')
   AND deactivated_at IS NULL;

--changeset ark-dev:V005-segment-fi
-- 한국 금융기관(FI) 엔티티 세그먼트. 트리거: businessType=financial + foundingCountry=KR.
-- doc_overrides: FI 미해당 공통(CONTRACT·SAMPLE·WEBSITE) disable → 공통 6종.
INSERT INTO segment (id, axis, code, label, classification_trigger, doc_overrides) VALUES
  ('a0000004-0001-0000-0000-000000000000', 'entity', 'ENTITY_FI', '한국 금융기관',
   '[{"priority":12,"logic":"AND","conditions":[{"field":"businessType","op":"eq","value":"financial"},{"field":"foundingCountry","op":"eq","value":"KR"}]}]',
   '[{"type":"CONTRACT","enabled":false},{"type":"SAMPLE_INVOICE_SHIPPING","enabled":false},{"type":"WEBSITE_URL","enabled":false}]');

--changeset ark-dev:V005-doc-fi-own
-- FI 전용 서류 2종 (own, owner=ENTITY_FI).
INSERT INTO doc_template (id, type, display_name, classification, owner_segment_id, is_required, is_conditional, guide) VALUES
  ('e0000010-0001-0000-0000-000000000000', 'REMITTANCE_LICENSE', 'Remittance License (또는 동등 인허가)', 'own', 'a0000004-0001-0000-0000-000000000000', true, false, null),
  ('e0000011-0001-0000-0000-000000000000', 'INTERNAL_POLICIES',  'Internal Policies (Compliance/Risk)',   'own', 'a0000004-0001-0000-0000-000000000000', true, false, null);

--changeset ark-dev:V005-segment-doc-overrides
-- 기존 세그먼트 공통 서류 선택 정합(프론트 enabledCommonDocTypes 대응).
-- ENTITY_CORP: 전체 공통 9종 사용 → override 불필요.
-- ENTITY_INDIV: 주주명부·법인등기부·법인인감 제외 → 공통 6종.
UPDATE segment
   SET doc_overrides = '[{"type":"SHAREHOLDER_LIST","enabled":false},{"type":"CORPORATE_REGISTRY","enabled":false},{"type":"SEAL_CERTIFICATE","enabled":false}]'
 WHERE code = 'ENTITY_INDIV' AND deactivated_at IS NULL;
-- SVC_PAYOUT: 기본 3종(사업자등록증·신분증·통장) → 나머지 공통 disable.
UPDATE segment
   SET doc_overrides = '[{"type":"SHAREHOLDER_LIST","enabled":false},{"type":"CONTRACT","enabled":false},{"type":"SAMPLE_INVOICE_SHIPPING","enabled":false},{"type":"WEBSITE_URL","enabled":false},{"type":"CORPORATE_REGISTRY","enabled":false},{"type":"SEAL_CERTIFICATE","enabled":false}]'
 WHERE code = 'SVC_PAYOUT' AND deactivated_at IS NULL;
