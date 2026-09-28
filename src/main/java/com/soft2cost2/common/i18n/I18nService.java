/**
 *
 */
package com.soft2cost2.common.i18n;

import java.util.List;

/**
 * 업무 트랜잭션과 분리하여, 다국어 자료를 조회하는 서비스 계약
 */

public interface I18nService {
    List<String> languages();

    String preference(Long userId);
    I18nRows.Message message(MessageRef ref);

    // List<I18nRows.Text> texts(String code, MessageRef ref);
    // List<I18nRows.Text> texts(String code, String  language);
    List<I18nRows.Text> texts(String code, List<String> languages);

    List<I18nRows.MenuText> menuTexts(List<Long> ids, List<String> languages);
    List<I18nRows.MenuIcon> menuIcons(List<Long> ids);

    /** 내부 예외 또는 SQL 구문을 외부 응답에 전달하지 않는, 조회 장애 식별자. */
    final class Unavailable extends RuntimeException {
        public Unavailable() { super("I18n unavailable", null, false, false); }
    }

}

/**
 * 실패한 업무 트랜잭션을 중단하고 별도 읽기로 번역을 조회 한다.
 * DDL(-Data Definition Language vs DML-Data Manipulation Language) or Redis 명령을 실행하지 않는다.
 * 조회 장애 후 5초 동안 재시도를 억제한다.
 * ===> 모든 Service 는 Interface 통해 구현하도록 하는 관계로 아래와 같이 수정 된다.
 */

//@Service
//@Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
//public class I18nService {
//    private static final Logger log = LoggerFactory.getLogger(I18nService.class);
//    private final I18nMapper mapper;
//
//}
