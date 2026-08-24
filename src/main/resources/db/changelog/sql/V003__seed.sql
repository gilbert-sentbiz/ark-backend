--liquibase formatted sql

--changeset ark-dev:V003-segment
INSERT INTO segment (id, axis, code, label, classification_trigger) VALUES
  ('a0000001-0001-0000-0000-000000000000', 'entity', 'ENTITY_CORP', '한국 법인',
   '[{"priority":10,"logic":"AND","conditions":[{"field":"businessType","op":"eq","value":"corporation"},{"field":"foundingCountry","op":"eq","value":"KR"}]}]'),
  ('a0000002-0001-0000-0000-000000000000', 'entity', 'ENTITY_INDIV', '한국 개인사업자',
   '[{"priority":11,"logic":"AND","conditions":[{"field":"businessType","op":"eq","value":"individual"},{"field":"foundingCountry","op":"eq","value":"KR"}]}]'),
  ('a0000003-0001-0000-0000-000000000000', 'service', 'SVC_PAYOUT', '해외 송금',
   '[{"priority":1,"logic":"AND","conditions":[{"field":"services","op":"contains","value":"remittance"}]}]');

--changeset ark-dev:V003-staff
INSERT INTO staff (id, email, name, role) VALUES
  ('00000001-0001-0000-0000-000000000000', 'sales@sentbe.com',      '영업 테스트',          'SALES'),
  ('00000002-0001-0000-0000-000000000000', 'ops@sentbe.com',        '운영 테스트',          'OPS'),
  ('00000003-0001-0000-0000-000000000000', 'compliance@sentbe.com', '컴플라이언스 테스트',  'COMPLIANCE'),
  ('00000004-0001-0000-0000-000000000000', 'admin@sentbe.com',      '관리자 테스트',        'ADMIN');

--changeset ark-dev:V003-question-first-common
INSERT INTO question (id, code, phase, classification, label, input_type, is_required, options, show_when, display_order) VALUES
  ('c0000001-0001-0000-0000-000000000000', 'fi_company_name',   'first', 'common', '회사명',         'text',  true,  null, null, 1),
  ('c0000002-0001-0000-0000-000000000000', 'fi_contact_name',   'first', 'common', '담당자 이름',    'text',  true,  null, null, 2),
  ('c0000003-0001-0000-0000-000000000000', 'fi_contact_title',  'first', 'common', '직함',           'text',  false, null, null, 3),
  ('c0000004-0001-0000-0000-000000000000', 'fi_phone',          'first', 'common', '연락처',         'text',  true,  null, null, 4),
  ('c0000005-0001-0000-0000-000000000000', 'fi_services', 'first', 'common', '신청 서비스',
   'multi', true,
   '[{"value":"remittance","label":"해외 송금 (Payout)"},{"value":"collection","label":"해외 수금 (Collection)"}]',
   null, 5),
  ('c0000006-0001-0000-0000-000000000000', 'fi_collection_countries', 'first', 'common', '수금 국가',
   'multi', false,
   '[{"value":"US","label":"미국"},{"value":"EU","label":"유럽"},{"value":"JP","label":"일본"},{"value":"CN","label":"중국"},{"value":"OTHER","label":"기타"}]',
   '{"question_id":"c0000005-0001-0000-0000-000000000000","value":"collection"}', 6),
  ('c0000007-0001-0000-0000-000000000000', 'fi_collection_other', 'first', 'common', '기타 수금 국가 (직접 입력)',
   'text', false, null,
   '{"question_id":"c0000006-0001-0000-0000-000000000000","value":"OTHER"}', 7),
  ('c0000008-0001-0000-0000-000000000000', 'fi_remittance_from', 'first', 'common', '주요 송금 출발 국가',
   'text', false, null,
   '{"question_id":"c0000005-0001-0000-0000-000000000000","value":"remittance"}', 8),
  ('c0000009-0001-0000-0000-000000000000', 'fi_remittance_to', 'first', 'common', '주요 송금 도착 국가',
   'text', false, null,
   '{"question_id":"c0000005-0001-0000-0000-000000000000","value":"remittance"}', 9),
  ('c0000010-0001-0000-0000-000000000000', 'fi_business_type', 'first', 'common', '사업자 유형',
   'radio', true,
   '[{"value":"corporation","label":"법인"},{"value":"individual","label":"개인사업자"}]',
   null, 10),
  ('c0000011-0001-0000-0000-000000000000', 'fi_founding_country', 'first', 'common', '설립 국가',
   'select', true,
   '[{"value":"KR","label":"대한민국"},{"value":"US","label":"미국"},{"value":"JP","label":"일본"},{"value":"CN","label":"중국"},{"value":"other","label":"기타"}]',
   null, 11),
  ('c0000012-0001-0000-0000-000000000000', 'fi_monthly_volume',   'first', 'common', '예상 월간 거래 규모', 'number', false, null, null, 12),
  ('c0000013-0001-0000-0000-000000000000', 'fi_monthly_currency', 'first', 'common', '거래 규모 통화',
   'select', false,
   '[{"value":"KRW","label":"KRW"},{"value":"USD","label":"USD"},{"value":"EUR","label":"EUR"},{"value":"JPY","label":"JPY"},{"value":"CNY","label":"CNY"}]',
   null, 13),
  ('c0000014-0001-0000-0000-000000000000', 'fi_additional_note', 'first', 'common', '추가 문의사항', 'textarea', false, null, null, 14);

--changeset ark-dev:V003-question-second-common
INSERT INTO question (id, code, phase, classification, label, input_type, is_required, options, show_when, display_order) VALUES
  ('d0000001-0001-0000-0000-000000000000', 'qc_biz_reg_no',   'second', 'common', '사업자등록번호', 'text', true,  null, null, 1),
  ('d0000002-0001-0000-0000-000000000000', 'qc_biz_type',     'second', 'common', '업종',           'text', true,  null, null, 2),
  ('d0000003-0001-0000-0000-000000000000', 'qc_biz_category', 'second', 'common', '업태',           'text', true,  null, null, 3),
  ('d0000004-0001-0000-0000-000000000000', 'qc_virtual_asset', 'second', 'common', '가상자산사업자(VASP) 해당 여부',
   'radio', true,
   '[{"value":"yes","label":"예"},{"value":"no","label":"아니요"}]',
   null, 4),
  ('d0000010-0001-0000-0000-000000000000', 'qc_fund_source', 'second', 'common', '주요 자금 원천',
   'multi', true,
   '[{"value":"business_revenue","label":"사업 수익"},{"value":"investment","label":"투자금"},{"value":"loan","label":"대출"},{"value":"other","label":"기타"}]',
   null, 5);

--changeset ark-dev:V003-question-second-common-vasp
INSERT INTO question (id, code, phase, classification, label, input_type, is_required, options, show_when, parent_question_id, display_order) VALUES
  ('d0000005-0001-0000-0000-000000000000', 'qc_vasp_custody', 'second', 'common',
   '가상자산 수탁(커스터디) 서비스 제공 여부', 'radio', true,
   '[{"value":"yes","label":"예"},{"value":"no","label":"아니요"}]',
   '{"question_id":"d0000004-0001-0000-0000-000000000000","value":"yes"}',
   'd0000004-0001-0000-0000-000000000000', 1),
  ('d0000006-0001-0000-0000-000000000000', 'qc_vasp_outside_lic', 'second', 'common',
   '해외 VASP 라이선스 보유 여부', 'radio', true,
   '[{"value":"yes","label":"예"},{"value":"no","label":"아니요"}]',
   '{"question_id":"d0000004-0001-0000-0000-000000000000","value":"yes"}',
   'd0000004-0001-0000-0000-000000000000', 2),
  ('d0000009-0001-0000-0000-000000000000', 'qc_vasp_purpose', 'second', 'common',
   'SentBe 이용 목적 (가상자산 관련)', 'textarea', true, null,
   '{"question_id":"d0000004-0001-0000-0000-000000000000","value":"yes"}',
   'd0000004-0001-0000-0000-000000000000', 5),
  ('d0000007-0001-0000-0000-000000000000', 'qc_vasp_cust_country', 'second', 'common',
   '고객 소재 국가', 'text', true, null,
   '{"question_id":"d0000006-0001-0000-0000-000000000000","value":"yes"}',
   'd0000006-0001-0000-0000-000000000000', 1),
  ('d0000008-0001-0000-0000-000000000000', 'qc_vasp_lic_used', 'second', 'common',
   '라이선스 적용 국가 및 규제기관', 'text', true, null,
   '{"question_id":"d0000006-0001-0000-0000-000000000000","value":"yes"}',
   'd0000006-0001-0000-0000-000000000000', 2);

--changeset ark-dev:V003-question-second-corp
INSERT INTO question (id, code, phase, classification, owner_segment_id, label, input_type, is_required, options, display_order) VALUES
  ('e0000001-0001-0000-0000-000000000000', 'qe_corp_name_kr', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '법인명 (한글)', 'text', true, null, 1),
  ('e0000002-0001-0000-0000-000000000000', 'qe_corp_name_en', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '법인명 (영문)', 'text', true, null, 2),
  ('e0000003-0001-0000-0000-000000000000', 'qe_corp_phone',   'second', 'own', 'a0000001-0001-0000-0000-000000000000', '법인 대표 전화번호', 'text', true, null, 3),
  ('e0000004-0001-0000-0000-000000000000', 'qe_corp_address', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '법인 국내 주소', 'text', true, null, 4),
  ('e0000005-0001-0000-0000-000000000000', 'qe_corp_type', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '법인 구분',
   'select', true,
   '[{"value":"unlisted","label":"비상장"},{"value":"listed","label":"상장"},{"value":"foreign","label":"외국법인 국내지점"}]', 5),
  ('e0000006-0001-0000-0000-000000000000', 'qe_corp_reg_no',  'second', 'own', 'a0000001-0001-0000-0000-000000000000', '법인등록번호', 'text', true, null, 6),
  ('e0000007-0001-0000-0000-000000000000', 'qe_corp_nation',  'second', 'own', 'a0000001-0001-0000-0000-000000000000', '설립 국가', 'text', true, null, 7),
  ('e0000008-0001-0000-0000-000000000000', 'qe_corp_hq_addr', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '본사 주소 (해외 법인인 경우)', 'text', false, null, 8),
  ('e0000009-0001-0000-0000-000000000000', 'qe_corp_rep_type', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '대표 유형', 'radio', true,
   '[{"value":"single","label":"단독 대표"},{"value":"joint","label":"공동 대표"}]', 9),
  ('e0000017-0001-0000-0000-000000000000', 'qe_corp_bo_exempt', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '25% 이상 지분 보유 실소유자 면제 사유', 'radio', true,
   '[{"value":"none","label":"해당 없음 (실소유자 입력 필요)"},{"value":"listed","label":"국내 상장법인"},{"value":"government","label":"국가·지방자치단체"},{"value":"financial","label":"금융기관"}]', 11),
  ('e0000026-0001-0000-0000-000000000000', 'qe_corp_purpose', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   'SentBe 이용 목적', 'multi', true,
   '[{"value":"trade","label":"무역 결제"},{"value":"payroll","label":"급여 송금"},{"value":"investment","label":"투자"},{"value":"other","label":"기타"}]', 15),
  ('e0000028-0001-0000-0000-000000000000', 'qe_corp_size', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '기업 규모', 'select', false,
   '[{"value":"small","label":"중소기업"},{"value":"medium","label":"중견기업"},{"value":"large","label":"대기업"}]', 16),
  ('e0000029-0001-0000-0000-000000000000', 'qe_corp_listed', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '상장 여부', 'radio', true,
   '[{"value":"yes","label":"예"},{"value":"no","label":"아니요"}]', 17),
  ('e0000030-0001-0000-0000-000000000000', 'qe_corp_founded_date', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '법인 설립일', 'date', true, null, 18);

--changeset ark-dev:V003-question-second-corp-conditional
INSERT INTO question (id, code, phase, classification, owner_segment_id, label, input_type, is_required, options, show_when, parent_question_id, display_order) VALUES
  ('e0000010-0001-0000-0000-000000000000', 'qe_corp_rep_count', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '공동 대표 총 인원수', 'number', true, null,
   '{"question_id":"e0000009-0001-0000-0000-000000000000","value":"joint"}',
   'e0000009-0001-0000-0000-000000000000', 1),
  ('e0000018-0001-0000-0000-000000000000', 'qe_corp_bo_has_25', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '25% 이상 지분 보유자 존재 여부', 'radio', true,
   '[{"value":"yes","label":"예"},{"value":"no","label":"아니요"}]',
   '{"question_id":"e0000017-0001-0000-0000-000000000000","value":"none"}', 'e0000017-0001-0000-0000-000000000000', 12),
  ('e0000019-0001-0000-0000-000000000000', 'qe_corp_bo_count', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '25% 이상 지분 보유자 수', 'number', true, null,
   '{"question_id":"e0000017-0001-0000-0000-000000000000","value":"none"}', 'e0000017-0001-0000-0000-000000000000', 13),
  ('e0000027-0001-0000-0000-000000000000', 'qe_corp_purpose_other', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '기타 이용 목적 (직접 입력)', 'text', true, null,
   '{"question_id":"e0000026-0001-0000-0000-000000000000","value":"other"}',
   'e0000026-0001-0000-0000-000000000000', 1);

--changeset ark-dev:V003-question-second-corp-repeat
INSERT INTO question (id, code, phase, classification, owner_segment_id, label, input_type, is_required, repeat, options, display_order) VALUES
  ('e0000011-0001-0000-0000-000000000000', 'qe_corp_rep_group', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '대표자 정보', 'text', true, true, null, 10),
  ('e0000020-0001-0000-0000-000000000000', 'qe_corp_bo_group', 'second', 'own', 'a0000001-0001-0000-0000-000000000000',
   '실소유자 정보', 'text', true, true, null, 14);

--changeset ark-dev:V003-question-second-corp-subfields
INSERT INTO question (id, code, phase, classification, owner_segment_id, label, input_type, is_required, options, parent_question_id, display_order) VALUES
  ('e0000012-0001-0000-0000-000000000000', 'qe_corp_rep_name_kr', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '이름 (한글)', 'text', true, null, 'e0000011-0001-0000-0000-000000000000', 1),
  ('e0000013-0001-0000-0000-000000000000', 'qe_corp_rep_name_en', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '이름 (영문)', 'text', true, null, 'e0000011-0001-0000-0000-000000000000', 2),
  ('e0000014-0001-0000-0000-000000000000', 'qe_corp_rep_dob',     'second', 'own', 'a0000001-0001-0000-0000-000000000000', '생년월일', 'date', true, null, 'e0000011-0001-0000-0000-000000000000', 3),
  ('e0000015-0001-0000-0000-000000000000', 'qe_corp_rep_gender',  'second', 'own', 'a0000001-0001-0000-0000-000000000000', '성별', 'radio', true,
   '[{"value":"male","label":"남"},{"value":"female","label":"여"}]', 'e0000011-0001-0000-0000-000000000000', 4),
  ('e0000016-0001-0000-0000-000000000000', 'qe_corp_rep_nation',  'second', 'own', 'a0000001-0001-0000-0000-000000000000', '국적', 'text', true, null, 'e0000011-0001-0000-0000-000000000000', 5),
  ('e0000021-0001-0000-0000-000000000000', 'qe_corp_bo_name_kr', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '이름 (한글)', 'text', true, null, 'e0000020-0001-0000-0000-000000000000', 1),
  ('e0000022-0001-0000-0000-000000000000', 'qe_corp_bo_name_en', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '이름 (영문)', 'text', true, null, 'e0000020-0001-0000-0000-000000000000', 2),
  ('e0000023-0001-0000-0000-000000000000', 'qe_corp_bo_dob',     'second', 'own', 'a0000001-0001-0000-0000-000000000000', '생년월일', 'date', true, null, 'e0000020-0001-0000-0000-000000000000', 3),
  ('e0000024-0001-0000-0000-000000000000', 'qe_corp_bo_nation',  'second', 'own', 'a0000001-0001-0000-0000-000000000000', '국적', 'text', true, null, 'e0000020-0001-0000-0000-000000000000', 4),
  ('e0000025-0001-0000-0000-000000000000', 'qe_corp_bo_country', 'second', 'own', 'a0000001-0001-0000-0000-000000000000', '거주 국가', 'text', true, null, 'e0000020-0001-0000-0000-000000000000', 5);

--changeset ark-dev:V003-question-second-indiv
INSERT INTO question (id, code, phase, classification, owner_segment_id, label, input_type, is_required, options, display_order) VALUES
  ('f0000001-0001-0000-0000-000000000000', 'qe_indiv_biz_name', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '상호명', 'text', true, null, 1),
  ('f0000002-0001-0000-0000-000000000000', 'qe_indiv_phone',    'second', 'own', 'a0000002-0001-0000-0000-000000000000', '사업장 전화번호', 'text', true, null, 2),
  ('f0000003-0001-0000-0000-000000000000', 'qe_indiv_address',  'second', 'own', 'a0000002-0001-0000-0000-000000000000', '사업장 주소', 'text', true, null, 3),
  ('f0000004-0001-0000-0000-000000000000', 'qe_indiv_residence', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '대표자 거주 국가',
   'select', true,
   '[{"value":"KR","label":"대한민국"},{"value":"other","label":"기타"}]', 4),
  ('f0000005-0001-0000-0000-000000000000', 'qe_indiv_rep_name_kr', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '대표자 이름 (한글)', 'text', true, null, 5),
  ('f0000006-0001-0000-0000-000000000000', 'qe_indiv_rep_name_en', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '대표자 이름 (영문)', 'text', true, null, 6),
  ('f0000007-0001-0000-0000-000000000000', 'qe_indiv_rep_dob',     'second', 'own', 'a0000002-0001-0000-0000-000000000000', '대표자 생년월일', 'date', true, null, 7),
  ('f0000008-0001-0000-0000-000000000000', 'qe_indiv_rep_gender',  'second', 'own', 'a0000002-0001-0000-0000-000000000000', '대표자 성별',
   'radio', true,
   '[{"value":"male","label":"남"},{"value":"female","label":"여"}]', 8),
  ('f0000009-0001-0000-0000-000000000000', 'qe_indiv_rep_nation',  'second', 'own', 'a0000002-0001-0000-0000-000000000000', '대표자 국적', 'text', true, null, 9),
  ('f0000010-0001-0000-0000-000000000000', 'qe_indiv_bo_same', 'second', 'own', 'a0000002-0001-0000-0000-000000000000',
   '실소유자가 대표자와 동일한가요?', 'radio', true,
   '[{"value":"yes","label":"예 (대표자와 동일)"},{"value":"no","label":"아니요 (별도 입력 필요)"}]', 10),
  ('f0000016-0001-0000-0000-000000000000', 'qe_indiv_purpose', 'second', 'own', 'a0000002-0001-0000-0000-000000000000',
   'SentBe 이용 목적', 'multi', true,
   '[{"value":"trade","label":"무역 결제"},{"value":"payroll","label":"급여 송금"},{"value":"investment","label":"투자"},{"value":"other","label":"기타"}]', 11);

--changeset ark-dev:V003-question-second-indiv-conditional
INSERT INTO question (id, code, phase, classification, owner_segment_id, label, input_type, is_required, options, show_when, parent_question_id, display_order) VALUES
  ('f0000011-0001-0000-0000-000000000000', 'qe_indiv_bo_name_kr', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '실소유자 이름 (한글)', 'text', true, null,
   '{"question_id":"f0000010-0001-0000-0000-000000000000","value":"no"}', 'f0000010-0001-0000-0000-000000000000', 1),
  ('f0000012-0001-0000-0000-000000000000', 'qe_indiv_bo_name_en', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '실소유자 이름 (영문)', 'text', true, null,
   '{"question_id":"f0000010-0001-0000-0000-000000000000","value":"no"}', 'f0000010-0001-0000-0000-000000000000', 2),
  ('f0000013-0001-0000-0000-000000000000', 'qe_indiv_bo_dob', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '실소유자 생년월일', 'date', true, null,
   '{"question_id":"f0000010-0001-0000-0000-000000000000","value":"no"}', 'f0000010-0001-0000-0000-000000000000', 3),
  ('f0000014-0001-0000-0000-000000000000', 'qe_indiv_bo_nation', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '실소유자 국적', 'text', true, null,
   '{"question_id":"f0000010-0001-0000-0000-000000000000","value":"no"}', 'f0000010-0001-0000-0000-000000000000', 4),
  ('f0000015-0001-0000-0000-000000000000', 'qe_indiv_bo_country', 'second', 'own', 'a0000002-0001-0000-0000-000000000000', '실소유자 거주 국가', 'text', true, null,
   '{"question_id":"f0000010-0001-0000-0000-000000000000","value":"no"}', 'f0000010-0001-0000-0000-000000000000', 5),
  ('f0000017-0001-0000-0000-000000000000', 'qe_indiv_purpose_other', 'second', 'own', 'a0000002-0001-0000-0000-000000000000',
   '기타 이용 목적 (직접 입력)', 'text', true, null,
   '{"question_id":"f0000016-0001-0000-0000-000000000000","value":"other"}',
   'f0000016-0001-0000-0000-000000000000', 1);

--changeset ark-dev:V003-doc-template
INSERT INTO doc_template (id, type, display_name, classification, owner_segment_id, is_required, is_conditional, guide) VALUES
  ('e0000001-0001-0000-0000-000000000000', 'BIZ_REGISTRATION',        '사업자등록증',          'common', null, true,  false, '발급 3개월 이내 원본'),
  ('e0000002-0001-0000-0000-000000000000', 'ID_COPY',                 '대표자 신분증 사본',    'common', null, true,  false, '주민등록증 · 운전면허증 · 여권 중 1종'),
  ('e0000003-0001-0000-0000-000000000000', 'SHAREHOLDER_LIST',        '주주명부',              'common', null, true,  false, '법인인감 날인 필수'),
  ('e0000004-0001-0000-0000-000000000000', 'CONTRACT',                '거래 계약서 (견본)',    'common', null, false, false, '최근 실거래 계약서 또는 주문서 샘플'),
  ('e0000005-0001-0000-0000-000000000000', 'SAMPLE_INVOICE_SHIPPING', '인보이스 / 선적서류',  'common', null, false, false, '수출입 실거래 증빙: 인보이스 또는 선적서류'),
  ('e0000006-0001-0000-0000-000000000000', 'BANK_PROOF',              '통장 사본',             'common', null, true,  false, '법인 명의 계좌 (최근 3개월 거래내역 포함)'),
  ('e0000007-0001-0000-0000-000000000000', 'WEBSITE_URL',             '홈페이지 URL',          'common', null, false, false, null),
  ('e0000008-0001-0000-0000-000000000000', 'CORPORATE_REGISTRY',      '법인등기부등본',        'own', 'a0000001-0001-0000-0000-000000000000', true,  false, '발급 3개월 이내'),
  ('e0000009-0001-0000-0000-000000000000', 'SEAL_CERTIFICATE',        '법인인감증명서',        'own', 'a0000001-0001-0000-0000-000000000000', true,  false, '발급 3개월 이내');
