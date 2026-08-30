-- MUST_CHANGE_PASSWORD_YN 추가 및 보정
-- 메뉴 권한 Y/N Check 추가
-- UPPER(LOGIN_ID) 유일 인덱스 추가
-- 활성 사용자 예외 유일 인덱스와 조회 인덱스 추가
-- 이 파일에는 비밀번호가 없는 기준정보만 넣는다. ADMIN은 9절의 개발 전용 Bootstrap이 생성하도록 한다.

-- FORM_PATH='Admin::MenuManage.xfdl'은 Nexacro 프로젝트에 Admin 서비스와 MenuManage.xfdl이 실제 존재할 때의 예시이다.
-- 실제 서비스 prefix와 파일명이 다르면 DB 값도 Nexacro 프로젝트에 맞춰야 한다.

-- * 왜 회사·역할·메뉴까지만 Flyway에 넣는가
--      회사 코드, 시스템 역할 코드, 시스템 메뉴 코드는 애플리케이션이 의존하는 기준정보다.
--      ADMIN 비밀번호는 환경 마다 달라야 하는 secret 이다.
--      BCrypt 해시는 같은 비밀번호도 매번 다른 salt를 사용하므로 소스에 고정하지 않는 것이 좋다.
--      따라서 Flyway는 비밀이 없는 기준정보를,
--      Bootstrap은 환경변수로 받은 최초 관리자 비밀번호를 담당하는 구성이 가장 안전하다.

-- * 연습 환경에서 ADMIN까지 SQL로 완전 시드하는 대안
--      사용자가 요구한 것처럼 S2C_USER, S2C_USER_ROLE, S2C_ROLE_MENU_AUTH까지 마이그레이션만으로 만들 수도 있다.
--      다만 평문 비밀번호를 SQL에 넣으면 안 된다. 사전에 로컬에서 BCrypt 해시를 생성하고 Flyway placeholder로 주입한다.

-- * 이 방식을 선택하면 애플리케이션 Bootstrap은 중복 생성자가 되므로 다음 중 하나로 정리해야 한다.
--      todo: [soft2cost1.bootstrap.enabled=false] 로 끈다.
--      또는 Bootstrap을 생성 코드가 아니라 “ADMIN/역할/메뉴 연결이 올바른지 검증하는 코드”로 바꾼다.
--      * 연습용으로는 가능하지만 실전에서는 7절의 기준정보 Flyway + secret 기반 Bootstrap을 권장한다.


/* ============================================================
   Soft2Cost initial system reference data
   - No plaintext password or BCrypt hash is stored here.
   ============================================================ */

INSERT INTO S2C_COMPANY
(COMPANY_CODE, COMPANY_NAME, USE_YN, CREATED_AT, CREATED_BY)
SELECT
    'SOFT2COST',
    'Soft2Cost',
    'Y',
    SYSTIMESTAMP,
    'FLYWAY'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM S2C_COMPANY
    WHERE COMPANY_CODE = 'SOFT2COST'
);

INSERT INTO S2C_ROLE
(ROLE_CODE, ROLE_NAME, ROLE_TYPE, DESCRIPTION,
 USE_YN, CREATED_AT, CREATED_BY)
SELECT
    'SYSTEM_ADMIN',
    '시스템 관리자',
    'SYSTEM',
    'Soft2Cost 시스템 기본 관리자 역할',
    'Y',
    SYSTIMESTAMP,
    'FLYWAY'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM S2C_ROLE
    WHERE ROLE_CODE = 'SYSTEM_ADMIN'
);

INSERT INTO S2C_MENU
(PARENT_MENU_ID, MENU_CODE, MENU_NAME, MENU_LEVEL, MENU_TYPE,
 SCREEN_ID, FORM_PATH, ICON_NAME, SORT_ORDER,
 SENSITIVE_YN, USE_YN, CREATED_AT, CREATED_BY)
SELECT
    NULL,
    'SYS_ADMIN_GRP',
    '시스템 관리',
    1,
    'GROUP',
    NULL,
    NULL,
    NULL,
    9000,
    'N',
    'Y',
    SYSTIMESTAMP,
    'FLYWAY'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM S2C_MENU
    WHERE MENU_CODE = 'SYS_ADMIN_GRP'
);

INSERT INTO S2C_MENU
(PARENT_MENU_ID, MENU_CODE, MENU_NAME, MENU_LEVEL, MENU_TYPE,
 SCREEN_ID, FORM_PATH, ICON_NAME, SORT_ORDER,
 SENSITIVE_YN, USE_YN, CREATED_AT, CREATED_BY)
SELECT
    P.MENU_ID,
    'SYS_MENU_MGMT',
    '메뉴 관리',
    2,
    'SCREEN',
    'MENU_MGMT',
    'Admin::MenuManage.xfdl',
    NULL,
    10,
    'Y',
    'Y',
    SYSTIMESTAMP,
    'FLYWAY'
FROM S2C_MENU P
WHERE P.MENU_CODE = 'SYS_ADMIN_GRP'
  AND NOT EXISTS (
    SELECT 1
    FROM S2C_MENU
    WHERE MENU_CODE = 'SYS_MENU_MGMT'
);
