-- soft2cost2 다국어 적용용 V008 / 20260916_1809
-- 최종 적용 위치: src/main/resources/db/migration/V008__message_i18n_reference_data.sql
-- docs/V004__create_cost_master_tables.sql과 번호를 구분하기 위해 실행 전에 V008로 변경했다.
-- Flyway 실제 이력과 배포 파일에서 V008 중복을 확인하고, 같은 내용의 이전 V004 파일은 함께 배치하지 않는다.
-- Oracle 19c 문법, AL32UTF8, 기존 V001/V002/V003 구조를 전제로 한다.
-- 실행한 파일이 아니다. Java 서비스는 이 DDL을 실행하지 않는다.
--
-- 하나의 익명 블록으로 구성하여 사전 검사 오류 뒤 후속 DDL이 계속 실행되지 않게 한다.
-- DDL 암묵 커밋 때문에 블록 전체 롤백은 불가능하다.
-- 부분 실패 시 전체 재실행 금지: 실제 적용 객체/데이터/Flyway 이력을 대조해
-- 승인된 복구 계획을 세운다. 이 파일은 이미 존재하는 신규 객체를 건너뛰지 않는다.
-- SQL*Plus의 WHENEVER 명령은 포함하지 않아 Flyway Community에서도 사용 가능하다.
-- 기존 메뉴/권한/아이콘 이름/ko·en 번역을 덮어쓰지 않는다. 검증되지 않은 아이콘 seed 없음.

DECLARE
    v_count NUMBER;
    v_charset VARCHAR2(100);
    PROCEDURE assert_zero(p_sql VARCHAR2, p_reason VARCHAR2) IS
        n NUMBER;
    BEGIN
        EXECUTE IMMEDIATE p_sql INTO n;
        IF n <> 0 THEN
            RAISE_APPLICATION_ERROR(-20040, p_reason);
        END IF;
    END;
    PROCEDURE require_column(p_table VARCHAR2, p_column VARCHAR2, p_type VARCHAR2) IS
        n NUMBER;
    BEGIN
        SELECT COUNT(*) INTO n FROM USER_TAB_COLUMNS
        WHERE TABLE_NAME = p_table AND COLUMN_NAME = p_column AND DATA_TYPE = p_type;
        IF n <> 1 THEN
            RAISE_APPLICATION_ERROR(-20041, 'Required column/type mismatch: ' || p_table || '.' || p_column);
        END IF;
    END;
BEGIN
    -- 1. 모든 변경보다 먼저 선행 구조·문자 집합·중복·부분 적용 여부를 검사한다.
    SELECT VALUE INTO v_charset FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET';
    IF v_charset <> 'AL32UTF8' THEN
        RAISE_APPLICATION_ERROR(-20042, 'This migration requires AL32UTF8');
    END IF;
    SELECT COUNT(*) INTO v_count FROM USER_TABLES WHERE TABLE_NAME IN
        ('S2C_COMPANY','S2C_USER','S2C_ROLE','S2C_USER_ROLE','S2C_MENU',
         'S2C_MENU_I18N','S2C_ROLE_MENU_AUTH','S2C_USER_MENU_AUTH','S2C_PROJECT');
    IF v_count <> 9 THEN
        RAISE_APPLICATION_ERROR(-20043, 'V001/V002 prerequisite tables are missing');
    END IF;
    assert_zero(
        'SELECT COUNT(*) FROM USER_OBJECTS WHERE OBJECT_NAME IN
         (''S2C_LANGUAGE'',''S2C_MESSAGE'',''S2C_MESSAGE_I18N'',''S2C_ICON'')',
        'I18n objects already exist: inspect partial migration/history before proceeding');
    assert_zero(
        'SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE
         (TABLE_NAME = ''S2C_MENU'' AND COLUMN_NAME = ''ICON_ID'') OR
         (TABLE_NAME = ''S2C_USER'' AND COLUMN_NAME = ''PREFERRED_LANG_CODE'') OR
         (TABLE_NAME = ''S2C_MENU_I18N'' AND COLUMN_NAME IN
          (''ICON_ALT_TEXT'',''TOOLTIP_TEXT'',''USE_YN'',''CREATED_AT'',''CREATED_BY'',''UPDATED_AT'',''UPDATED_BY''))',
        'I18n columns already exist: do not rerun the whole migration');
    assert_zero(q'~SELECT COUNT(*) FROM USER_OBJECTS WHERE OBJECT_NAME IN
        ('PK_S2C_LANGUAGE','CK_S2C_LANG_CODE','CK_S2C_LANG_NAME','CK_S2C_LANG_USE','PK_S2C_MESSAGE','UK_S2C_MSG_NAME','CK_S2C_MSG_CODE','CK_S2C_MSG_NAME','CK_S2C_MSG_API','CK_S2C_MSG_ARGS','CK_S2C_MSG_USE','PK_S2C_MESSAGE_I18N','FK_S2C_MSGI_MSG','FK_S2C_MSGI_LANG','CK_S2C_MSGI_TEXT','CK_S2C_MSGI_USE','IX_S2C_MSGI_LANG','PK_S2C_ICON','UK_S2C_ICON_CODE','CK_S2C_ICON_CODE','CK_S2C_ICON_TYPE','CK_S2C_ICON_PATH','CK_S2C_ICON_USE','FK_S2C_MI_LANG','CK_S2C_MI_NAME','CK_S2C_MI_USE','IX_S2C_MI_LANG','FK_S2C_MENU_ICON','IX_S2C_MENU_ICON','FK_S2C_USER_LANG','IX_S2C_USER_LANG')~', 'I18n index names already exist');
    assert_zero(q'~SELECT COUNT(*) FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME IN
        ('PK_S2C_LANGUAGE','CK_S2C_LANG_CODE','CK_S2C_LANG_NAME','CK_S2C_LANG_USE','PK_S2C_MESSAGE','UK_S2C_MSG_NAME','CK_S2C_MSG_CODE','CK_S2C_MSG_NAME','CK_S2C_MSG_API','CK_S2C_MSG_ARGS','CK_S2C_MSG_USE','PK_S2C_MESSAGE_I18N','FK_S2C_MSGI_MSG','FK_S2C_MSGI_LANG','CK_S2C_MSGI_TEXT','CK_S2C_MSGI_USE','IX_S2C_MSGI_LANG','PK_S2C_ICON','UK_S2C_ICON_CODE','CK_S2C_ICON_CODE','CK_S2C_ICON_TYPE','CK_S2C_ICON_PATH','CK_S2C_ICON_USE','FK_S2C_MI_LANG','CK_S2C_MI_NAME','CK_S2C_MI_USE','IX_S2C_MI_LANG','FK_S2C_MENU_ICON','IX_S2C_MENU_ICON','FK_S2C_USER_LANG','IX_S2C_USER_LANG')~', 'I18n constraint names already exist');
    require_column('S2C_MENU', 'MENU_ID', 'NUMBER');
    require_column('S2C_MENU', 'MENU_CODE', 'VARCHAR2');
    require_column('S2C_MENU', 'MENU_NAME', 'VARCHAR2');
    require_column('S2C_MENU', 'ICON_NAME', 'VARCHAR2');
    require_column('S2C_MENU', 'USE_YN', 'CHAR');
    require_column('S2C_MENU_I18N', 'MENU_ID', 'NUMBER');
    require_column('S2C_MENU_I18N', 'LANG_CODE', 'VARCHAR2');
    require_column('S2C_MENU_I18N', 'MENU_NAME', 'VARCHAR2');
    require_column('S2C_MENU_I18N', 'DESCRIPTION', 'VARCHAR2');
    require_column('S2C_USER', 'USER_ID', 'NUMBER');
    require_column('S2C_USER', 'USE_YN', 'CHAR');
    SELECT COUNT(*) INTO v_count FROM USER_CONSTRAINTS
    WHERE CONSTRAINT_NAME IN ('PK_S2C_MENU','PK_S2C_USER','PK_S2C_MENU_I18N','FK_S2C_MI_MENU')
      AND STATUS = 'ENABLED' AND VALIDATED = 'VALIDATED';
    IF v_count <> 4 THEN
        RAISE_APPLICATION_ERROR(-20044, 'Required existing primary/foreign keys are not validated');
    END IF;
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM S2C_MENU WHERE MENU_CODE IN
        (''SYS_ADMIN_GRP'',''SYS_MENU_MGMT'')' INTO v_count;
    IF v_count <> 2 THEN
        RAISE_APPLICATION_ERROR(-20045, 'V003 system menu reference data is missing');
    END IF;
    assert_zero(q'~SELECT COUNT(*) FROM (
        SELECT MENU_ID, LOWER(REPLACE(TRIM(LANG_CODE),'_','-'))
        FROM S2C_MENU_I18N
        GROUP BY MENU_ID, LOWER(REPLACE(TRIM(LANG_CODE),'_','-'))
        HAVING COUNT(*) > 1)~', 'Language normalization would collide: preserve and reconcile both rows');
    assert_zero(q'~SELECT COUNT(*) FROM S2C_MENU_I18N
        WHERE LENGTH(LOWER(REPLACE(TRIM(LANG_CODE),'_','-'))) > 35
        OR NOT REGEXP_LIKE(LOWER(REPLACE(TRIM(LANG_CODE),'_','-')),
                          '^[a-z]{2,3}(-[a-z0-9]{2,8})*$', 'c')
        OR TRIM(LANG_CODE) IS NULL OR TRIM(MENU_NAME) IS NULL
        OR LENGTH(MENU_NAME) > 200 OR LENGTH(DESCRIPTION) > 500~',
        'Existing menu translations contain invalid tags or text');
    assert_zero(q'~SELECT COUNT(*) FROM S2C_MENU WHERE TRIM(MENU_NAME) IS NULL~',
        'Existing menu name is blank');
    assert_zero(q'~SELECT COUNT(*) FROM S2C_MENU M JOIN S2C_MENU_I18N T ON T.MENU_ID = M.MENU_ID
        WHERE LOWER(REPLACE(TRIM(T.LANG_CODE),'_','-')) = 'ko' AND T.MENU_NAME <> M.MENU_NAME~',
        'Existing ko translation differs from master: reconcile explicitly; no automatic overwrite');

    -- 2. 신규 원장을 생성한다. 기존 테이블은 재생성하지 않는다.
    EXECUTE IMMEDIATE q'~
CREATE TABLE S2C_LANGUAGE (
    LANG_CODE VARCHAR2(35 CHAR) NOT NULL,
    LANG_NAME VARCHAR2(100 CHAR) NOT NULL,
    USE_YN CHAR(1) DEFAULT 'Y' NOT NULL,
    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY VARCHAR2(50),
    UPDATED_AT TIMESTAMP,
    UPDATED_BY VARCHAR2(50),
    CONSTRAINT PK_S2C_LANGUAGE PRIMARY KEY (LANG_CODE),
    CONSTRAINT CK_S2C_LANG_CODE CHECK (
        REGEXP_LIKE(LANG_CODE, '^[a-z]{2,3}(-[a-z0-9]{2,8})*$', 'c')
    ),
    CONSTRAINT CK_S2C_LANG_NAME CHECK (TRIM(LANG_NAME) IS NOT NULL),
    CONSTRAINT CK_S2C_LANG_USE CHECK (USE_YN IN ('Y','N'))
)
~';

    EXECUTE IMMEDIATE q'~
CREATE TABLE S2C_MESSAGE (
    MESSAGE_CODE VARCHAR2(64 CHAR) NOT NULL,
    MESSAGE_NAME VARCHAR2(120 CHAR) NOT NULL,
    API_CODE VARCHAR2(20 CHAR),
    ARG_COUNT NUMBER(2) DEFAULT 0 NOT NULL,
    DESCRIPTION VARCHAR2(500 CHAR),
    USE_YN CHAR(1) DEFAULT 'Y' NOT NULL,
    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY VARCHAR2(50),
    UPDATED_AT TIMESTAMP,
    UPDATED_BY VARCHAR2(50),
    CONSTRAINT PK_S2C_MESSAGE PRIMARY KEY (MESSAGE_CODE),
    CONSTRAINT UK_S2C_MSG_NAME UNIQUE (MESSAGE_NAME),
    CONSTRAINT CK_S2C_MSG_CODE CHECK (
        REGEXP_LIKE(MESSAGE_CODE, '^[A-Z][A-Z0-9_]*$', 'c')
    ),
    CONSTRAINT CK_S2C_MSG_NAME CHECK (
        REGEXP_LIKE(MESSAGE_NAME, '^[a-z][a-z0-9_.]*$', 'c')
    ),
    CONSTRAINT CK_S2C_MSG_API CHECK (
        API_CODE IS NULL OR REGEXP_LIKE(API_CODE, '^E[0-9]{3}$', 'c')
    ),
    CONSTRAINT CK_S2C_MSG_ARGS CHECK (ARG_COUNT BETWEEN 0 AND 10),
    CONSTRAINT CK_S2C_MSG_USE CHECK (USE_YN IN ('Y','N'))
)
~';

    EXECUTE IMMEDIATE q'~
CREATE TABLE S2C_MESSAGE_I18N (
    MESSAGE_CODE VARCHAR2(64 CHAR) NOT NULL,
    LANG_CODE VARCHAR2(35 CHAR) NOT NULL,
    MESSAGE_TEXT VARCHAR2(1000 CHAR) NOT NULL,
    USE_YN CHAR(1) DEFAULT 'Y' NOT NULL,
    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY VARCHAR2(50),
    UPDATED_AT TIMESTAMP,
    UPDATED_BY VARCHAR2(50),
    CONSTRAINT PK_S2C_MESSAGE_I18N PRIMARY KEY (MESSAGE_CODE, LANG_CODE),
    CONSTRAINT FK_S2C_MSGI_MSG FOREIGN KEY (MESSAGE_CODE)
        REFERENCES S2C_MESSAGE (MESSAGE_CODE),
    CONSTRAINT FK_S2C_MSGI_LANG FOREIGN KEY (LANG_CODE)
        REFERENCES S2C_LANGUAGE (LANG_CODE),
    CONSTRAINT CK_S2C_MSGI_TEXT CHECK (TRIM(MESSAGE_TEXT) IS NOT NULL),
    CONSTRAINT CK_S2C_MSGI_USE CHECK (USE_YN IN ('Y','N'))
)
~';

    EXECUTE IMMEDIATE q'~
CREATE INDEX IX_S2C_MSGI_LANG
    ON S2C_MESSAGE_I18N (LANG_CODE, MESSAGE_CODE)
~';

    EXECUTE IMMEDIATE q'~
CREATE TABLE S2C_ICON (
    ICON_ID NUMBER GENERATED BY DEFAULT AS IDENTITY,
    ICON_CODE VARCHAR2(100 CHAR) NOT NULL,
    RESOURCE_PATH VARCHAR2(300 CHAR) NOT NULL,
    RESOURCE_TYPE VARCHAR2(10 CHAR) DEFAULT 'PNG' NOT NULL,
    USE_YN CHAR(1) DEFAULT 'Y' NOT NULL,
    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY VARCHAR2(50),
    UPDATED_AT TIMESTAMP,
    UPDATED_BY VARCHAR2(50),
    CONSTRAINT PK_S2C_ICON PRIMARY KEY (ICON_ID),
    CONSTRAINT UK_S2C_ICON_CODE UNIQUE (ICON_CODE),
    CONSTRAINT CK_S2C_ICON_CODE CHECK (
        REGEXP_LIKE(ICON_CODE, '^[A-Z][A-Z0-9_]*$', 'c')
    ),
    CONSTRAINT CK_S2C_ICON_TYPE CHECK (RESOURCE_TYPE IN ('PNG','JPG')),
    CONSTRAINT CK_S2C_ICON_PATH CHECK (
        (RESOURCE_TYPE = 'PNG' AND
         REGEXP_LIKE(RESOURCE_PATH, '^/assets/icons/[a-z0-9_-]+[.]png$', 'c'))
        OR
        (RESOURCE_TYPE = 'JPG' AND
         REGEXP_LIKE(RESOURCE_PATH, '^/assets/icons/[a-z0-9_-]+[.]jpg$', 'c'))
    ),
    CONSTRAINT CK_S2C_ICON_USE CHECK (USE_YN IN ('Y','N'))
)
~';

    -- 3. 신규 언어 기준정보 및 기존 태그 정규화. 번역 본문은 보존한다.
    EXECUTE IMMEDIATE q'~
INSERT INTO S2C_LANGUAGE (LANG_CODE, LANG_NAME, CREATED_BY)
VALUES ('ko', '한국어', 'I18N_V008')
~';
    EXECUTE IMMEDIATE q'~
INSERT INTO S2C_LANGUAGE (LANG_CODE, LANG_NAME, CREATED_BY)
VALUES ('en', 'English', 'I18N_V008')
~';
    EXECUTE IMMEDIATE q'~
INSERT INTO S2C_LANGUAGE (LANG_CODE, LANG_NAME, USE_YN, CREATED_BY)
SELECT DISTINCT LOWER(REPLACE(TRIM(T.LANG_CODE),'_','-')),
    LOWER(REPLACE(TRIM(T.LANG_CODE),'_','-')), 'N', 'I18N_V008'
FROM S2C_MENU_I18N T
WHERE LOWER(REPLACE(TRIM(T.LANG_CODE),'_','-')) NOT IN ('ko','en')
~';
    EXECUTE IMMEDIATE q'~
UPDATE S2C_MENU_I18N SET LANG_CODE = LOWER(REPLACE(TRIM(LANG_CODE),'_','-'))
WHERE LANG_CODE <> LOWER(REPLACE(TRIM(LANG_CODE),'_','-'))
~';

    -- 4. 기존 테이블 확장. 뒤따르는 DDL이 앞 DML을 커밋할 수 있다.
    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_MENU_I18N MODIFY (LANG_CODE VARCHAR2(35 CHAR), MENU_NAME VARCHAR2(200 CHAR), DESCRIPTION VARCHAR2(500 CHAR))
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_MENU_I18N ADD (ICON_ALT_TEXT VARCHAR2(200 CHAR), TOOLTIP_TEXT VARCHAR2(500 CHAR), USE_YN CHAR(1) DEFAULT 'Y' NOT NULL, CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL, CREATED_BY VARCHAR2(50), UPDATED_AT TIMESTAMP, UPDATED_BY VARCHAR2(50))
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_MENU_I18N ADD CONSTRAINT FK_S2C_MI_LANG FOREIGN KEY (LANG_CODE) REFERENCES S2C_LANGUAGE (LANG_CODE)
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_MENU_I18N ADD CONSTRAINT CK_S2C_MI_NAME CHECK (TRIM(MENU_NAME) IS NOT NULL)
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_MENU_I18N ADD CONSTRAINT CK_S2C_MI_USE CHECK (USE_YN IN ('Y','N'))
~';

    EXECUTE IMMEDIATE q'~
CREATE INDEX IX_S2C_MI_LANG ON S2C_MENU_I18N (LANG_CODE, MENU_ID)
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_MENU ADD (ICON_ID NUMBER)
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_MENU ADD CONSTRAINT FK_S2C_MENU_ICON FOREIGN KEY (ICON_ID) REFERENCES S2C_ICON (ICON_ID)
~';

    EXECUTE IMMEDIATE q'~
CREATE INDEX IX_S2C_MENU_ICON ON S2C_MENU (ICON_ID)
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_USER ADD (PREFERRED_LANG_CODE VARCHAR2(35 CHAR))
~';

    EXECUTE IMMEDIATE q'~
ALTER TABLE S2C_USER ADD CONSTRAINT FK_S2C_USER_LANG FOREIGN KEY (PREFERRED_LANG_CODE) REFERENCES S2C_LANGUAGE (LANG_CODE)
~';

    EXECUTE IMMEDIATE q'~
CREATE INDEX IX_S2C_USER_LANG ON S2C_USER (PREFERRED_LANG_CODE)
~';

    -- 5. 모든 신규 호출 코드의 한국어·영어 기본자료. 임의 아이콘 파일은 등록하지 않는다.
    EXECUTE IMMEDIATE q'~
MERGE INTO S2C_MESSAGE T
USING (
    SELECT 'MSG_LOGIN_FAILED' MESSAGE_CODE, 'error.auth.login_failed' MESSAGE_NAME, 'E301' API_CODE, 0 ARG_COUNT FROM DUAL
    UNION ALL
    SELECT 'MSG_REQUEST_INVALID', 'error.request.invalid', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_SESSION_INVALID', 'error.auth.session_invalid', 'E303', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_ACCESS_DENIED', 'error.auth.access_denied', 'E304', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_CHANGE_REQUIRED', 'error.auth.password_change_required', 'E305', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_ACCOUNT_LOCKED', 'error.auth.account_locked', 'E306', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_POLICY', 'error.auth.password_policy', 'E307', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_NOT_FOUND', 'error.resource.not_found', 'E404', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_CONFLICT', 'error.request.conflict', 'E409', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_SERVER_ERROR', 'error.server.unexpected', 'E500', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_SERVICE_UNAVAILABLE', 'error.service.unavailable', 'E503', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_SESSION_STORE_UNAVAILABLE', 'error.auth.store_unavailable', 'E503', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_CURRENT_PASSWORD_INVALID', 'error.auth.current_password', 'E301', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_UNCHANGED', 'error.auth.password_unchanged', 'E307', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_NOT_FOUND', 'error.menu.not_found', 'E404', 1 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_CODE_DUPLICATE', 'error.menu.code_duplicate', 'E409', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_CREATE_FAILED', 'error.menu.create_failed', 'E500', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_READ_FAILED', 'error.menu.read_failed', 'E500', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_ADMIN_ROLE_UNAVAILABLE', 'error.menu.admin_role_unavailable', 'E500', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_AUTH_CREATE_FAILED', 'error.menu.auth_create_failed', 'E500', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_AUTH_UPDATE_FAILED', 'error.menu.auth_update_failed', 'E500', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_SELF_PARENT', 'error.menu.self_parent', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_DESCENDANT_PARENT', 'error.menu.descendant_parent', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_GROUP_HAS_CHILDREN', 'error.menu.group_has_children', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_WRITE_CONFLICT', 'error.menu.write_conflict', 'E409', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PROTECTED_DISABLE', 'error.menu.protected_disable', 'E409', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_DISABLE_EMPTY', 'error.menu.disable_empty', 'E409', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PARENT_INACTIVE', 'error.menu.parent_inactive', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PARENT_NOT_GROUP', 'error.menu.parent_not_group', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_GROUP_SCREEN', 'error.menu.group_screen', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_TYPE_INVALID', 'error.menu.type_invalid', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_SCREEN_REQUIRED', 'error.menu.screen_required', 'E302', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_REVISION_CONFLICT', 'error.menu.revision_conflict', 'E409', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PROTECTED_STRUCTURE', 'error.menu.protected_structure', 'E409', 0 FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_ID_INVALID', 'error.menu.id_invalid', 'E302', 0 FROM DUAL
) S
ON (T.MESSAGE_CODE = S.MESSAGE_CODE)
WHEN NOT MATCHED THEN INSERT (MESSAGE_CODE, MESSAGE_NAME, API_CODE, ARG_COUNT, CREATED_BY)
VALUES (S.MESSAGE_CODE, S.MESSAGE_NAME, S.API_CODE, S.ARG_COUNT, 'I18N_V008')
~';
    EXECUTE IMMEDIATE q'~
MERGE INTO S2C_MESSAGE_I18N T
USING (
    SELECT 'MSG_LOGIN_FAILED' MESSAGE_CODE, 'ko' LANG_CODE, '아이디 또는 비밀번호가 올바르지 않습니다!!!' MESSAGE_TEXT FROM DUAL
    UNION ALL
    SELECT 'MSG_LOGIN_FAILED', 'en', 'Invalid user ID or password!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_REQUEST_INVALID', 'ko', '요청 값이 올바르지 않습니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_REQUEST_INVALID', 'en', 'The request values are invalid!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_SESSION_INVALID', 'ko', '인증 세션이 유효하지 않습니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_SESSION_INVALID', 'en', 'The authentication session is invalid!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_ACCESS_DENIED', 'ko', '요청한 기능에 대한 권한이 없습니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_ACCESS_DENIED', 'en', 'You do not have permission to use this function!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_CHANGE_REQUIRED', 'ko', '초기 비밀번호 변경이 필요합니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_CHANGE_REQUIRED', 'en', 'You must change your initial password!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_ACCOUNT_LOCKED', 'ko', '계정이 일시 잠겼습니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_ACCOUNT_LOCKED', 'en', 'The account is temporarily locked!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_POLICY', 'ko', '비밀번호 정책을 확인해 주세요.' FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_POLICY', 'en', 'Please check the password policy.' FROM DUAL
    UNION ALL
    SELECT 'MSG_NOT_FOUND', 'ko', '요청한 대상을 찾을 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_NOT_FOUND', 'en', 'The requested item was not found.' FROM DUAL
    UNION ALL
    SELECT 'MSG_CONFLICT', 'ko', '요청한 변경을 적용할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_CONFLICT', 'en', 'The requested change could not be applied.' FROM DUAL
    UNION ALL
    SELECT 'MSG_SERVER_ERROR', 'ko', '서버 오류가 발생했습니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_SERVER_ERROR', 'en', 'A server error has occurred!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_SERVICE_UNAVAILABLE', 'ko', '서비스를 일시적으로 사용할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_SERVICE_UNAVAILABLE', 'en', 'The service is temporarily unavailable.' FROM DUAL
    UNION ALL
    SELECT 'MSG_SESSION_STORE_UNAVAILABLE', 'ko', '인증 세션 저장소를 사용할 수 없습니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_SESSION_STORE_UNAVAILABLE', 'en', 'The authentication session store is unavailable!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_CURRENT_PASSWORD_INVALID', 'ko', '현재 비밀번호가 올바르지 않습니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_CURRENT_PASSWORD_INVALID', 'en', 'The current password is incorrect!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_UNCHANGED', 'ko', '새 비밀번호는 현재 비밀번호와 달라야 합니다!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_PASSWORD_UNCHANGED', 'en', 'The new password must differ from the current password!!!' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_NOT_FOUND', 'ko', '메뉴를 찾을 수 없습니다. 메뉴 번호: {0}' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_NOT_FOUND', 'en', 'Menu not found. Menu number: {0}' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_CODE_DUPLICATE', 'ko', '이미 존재하는 메뉴 코드입니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_CODE_DUPLICATE', 'en', 'The menu code already exists.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_CREATE_FAILED', 'ko', '메뉴 등록에 실패했습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_CREATE_FAILED', 'en', 'The menu could not be created.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_READ_FAILED', 'ko', '등록된 메뉴를 조회하지 못했습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_READ_FAILED', 'en', 'The created menu could not be retrieved.' FROM DUAL
    UNION ALL
    SELECT 'MSG_ADMIN_ROLE_UNAVAILABLE', 'ko', '메뉴 관리에 필요한 역할을 사용할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_ADMIN_ROLE_UNAVAILABLE', 'en', 'The role required for menu management is unavailable.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_AUTH_CREATE_FAILED', 'ko', '메뉴 권한 등록에 실패했습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_AUTH_CREATE_FAILED', 'en', 'Menu permissions could not be created.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_AUTH_UPDATE_FAILED', 'ko', '메뉴 권한 수정에 실패했습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_AUTH_UPDATE_FAILED', 'en', 'Menu permissions could not be updated.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_SELF_PARENT', 'ko', '메뉴 자신을 부모로 지정할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_SELF_PARENT', 'en', 'A menu cannot be its own parent.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_DESCENDANT_PARENT', 'ko', '메뉴의 자손을 부모로 지정할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_DESCENDANT_PARENT', 'en', 'A descendant cannot be the parent of this menu.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_GROUP_HAS_CHILDREN', 'ko', '자식 메뉴가 있는 GROUP은 SCREEN 또는 POPUP으로 변경할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_GROUP_HAS_CHILDREN', 'en', 'A group with children cannot become a screen or popup.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_WRITE_CONFLICT', 'ko', '메뉴가 동시에 수정되었거나 존재하지 않습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_WRITE_CONFLICT', 'en', 'The menu was changed concurrently or does not exist.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PROTECTED_DISABLE', 'ko', '시스템 관리 진입에 필요한 보호 메뉴는 비활성화할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PROTECTED_DISABLE', 'en', 'Protected administration menus cannot be disabled.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_DISABLE_EMPTY', 'ko', '메뉴 비활성화 대상이 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_DISABLE_EMPTY', 'en', 'There are no menus to disable.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PARENT_INACTIVE', 'ko', '비활성 메뉴는 부모로 지정할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PARENT_INACTIVE', 'en', 'An inactive menu cannot be a parent.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PARENT_NOT_GROUP', 'ko', 'GROUP 메뉴만 부모로 지정할 수 있습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PARENT_NOT_GROUP', 'en', 'Only a group menu can be a parent.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_GROUP_SCREEN', 'ko', 'GROUP 메뉴에는 screenId와 formPath를 지정할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_GROUP_SCREEN', 'en', 'A group cannot have a screen ID or form path.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_TYPE_INVALID', 'ko', 'menuType은 GROUP, SCREEN, POPUP 중 하나여야 합니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_TYPE_INVALID', 'en', 'The menu type must be GROUP, SCREEN or POPUP.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_SCREEN_REQUIRED', 'ko', 'SCREEN과 POPUP 메뉴에는 screenId와 formPath가 필요합니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_SCREEN_REQUIRED', 'en', 'Screens and popups require a screen ID and form path.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_REVISION_CONFLICT', 'ko', '다른 사용자가 먼저 메뉴를 수정했습니다. 다시 조회한 후 재시도하십시오.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_REVISION_CONFLICT', 'en', 'Another user changed this menu. Reload it and try again.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PROTECTED_STRUCTURE', 'ko', '시스템 보호 메뉴의 부모 또는 메뉴 유형은 변경할 수 없습니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_PROTECTED_STRUCTURE', 'en', 'The parent or type of a protected menu cannot be changed.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_ID_INVALID', 'ko', 'menuId는 양수여야 합니다.' FROM DUAL
    UNION ALL
    SELECT 'MSG_MENU_ID_INVALID', 'en', 'The menu ID must be positive.' FROM DUAL
) S
ON (T.MESSAGE_CODE = S.MESSAGE_CODE AND T.LANG_CODE = S.LANG_CODE)
WHEN NOT MATCHED THEN INSERT (MESSAGE_CODE, LANG_CODE, MESSAGE_TEXT, CREATED_BY)
VALUES (S.MESSAGE_CODE, S.LANG_CODE, S.MESSAGE_TEXT, 'I18N_V008')
~';
    EXECUTE IMMEDIATE q'~
MERGE INTO S2C_MENU_I18N T
USING (SELECT MENU_ID, MENU_NAME, USE_YN FROM S2C_MENU) S
ON (T.MENU_ID = S.MENU_ID AND T.LANG_CODE = 'ko')
WHEN NOT MATCHED THEN INSERT (MENU_ID, LANG_CODE, MENU_NAME, USE_YN, CREATED_BY)
VALUES (S.MENU_ID, 'ko', S.MENU_NAME, S.USE_YN, 'I18N_V008')
~';
    EXECUTE IMMEDIATE q'~
MERGE INTO S2C_MENU_I18N T
USING (
    SELECT MENU_ID, 'System administration' MENU_NAME,
        'System administration menu group' DESCRIPTION
    FROM S2C_MENU WHERE MENU_CODE = 'SYS_ADMIN_GRP'
    UNION ALL
    SELECT MENU_ID, 'Menu management', 'Manage navigation menus'
    FROM S2C_MENU WHERE MENU_CODE = 'SYS_MENU_MGMT'
) S
ON (T.MENU_ID = S.MENU_ID AND T.LANG_CODE = 'en')
WHEN NOT MATCHED THEN INSERT
    (MENU_ID, LANG_CODE, MENU_NAME, DESCRIPTION, ICON_ALT_TEXT, CREATED_BY)
VALUES (S.MENU_ID, 'en', S.MENU_NAME, S.DESCRIPTION, S.MENU_NAME, 'I18N_V008')
~';

    -- 6. 필수 기본자료 검증. DDL을 되돌리는 검증이 아니라 불완전 배포를 중단하는 검사이다.
    assert_zero(q'~SELECT COUNT(*) FROM S2C_MESSAGE M WHERE M.USE_YN = 'Y'
        AND (NOT EXISTS (SELECT 1 FROM S2C_MESSAGE_I18N T
             WHERE T.MESSAGE_CODE = M.MESSAGE_CODE AND T.LANG_CODE = 'ko' AND T.USE_YN = 'Y')
          OR NOT EXISTS (SELECT 1 FROM S2C_MESSAGE_I18N T
             WHERE T.MESSAGE_CODE = M.MESSAGE_CODE AND T.LANG_CODE = 'en' AND T.USE_YN = 'Y'))~',
        'Required message translations are missing');
    assert_zero(q'~SELECT COUNT(*) FROM S2C_MENU M WHERE M.USE_YN = 'Y'
        AND NOT EXISTS (SELECT 1 FROM S2C_MENU_I18N T WHERE T.MENU_ID = M.MENU_ID
            AND T.LANG_CODE = 'ko' AND T.USE_YN = 'Y')~', 'Required ko menu translations are missing');
END;
/
