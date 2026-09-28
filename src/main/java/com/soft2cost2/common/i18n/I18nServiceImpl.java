package com.soft2cost2.common.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;


/**
 * 다국어 기준 정보, 메시지 번역, 메뉴 번약과 아이콘 정보를 조회 한다.
 * todo: 기존 업무 트랜잭션을일시 중단하고 조회하면, 조회 장애 후 5초 동안 반복 조회를 제한 한다.
 *       DB 구조 변경이나 Redis 명령은 실행하지 않는다.
 */
@Service("i18nService")
@Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
public class I18nServiceImpl implements I18nService {
    private static final Logger log = LoggerFactory.getLogger(I18nServiceImpl.class);

    private final I18nMapper i18nMapper;
    /**
     * CPU 캐시 우회 및 메모리 직접 동기화: 멀티스레드 환경에서 각 CPU 코어는 성능 향상을 위해
     *      변수 값을 개별 캐시(L1, L2 등)에 두고 사용.
     *      volatile 키워드를 붙이면 변수를 읽고 쓸 때 CPU 캐시가 아닌 메인 메모리(RAM)에 직접 접근.
     * 가시성(Visibility) 보장: 특정 스레드가 DB 조회 실패로 retryAfter의 차단 만료 시각을 갱신했을 때,
     *      다른 스레드들이 캐시의 옛날 값(0)을 읽지 않고 변경된 차단 시각을 즉각 인지할 수 있도록 동기화.
     * todo: read() 메서드: 서킷 브레이커(Circuit Breaker) 동작 구조
     *      DB 장애 발생 시 시스템 부하를 줄이기 위해 5초간 요청을 차단하는 보호 로직
     */
    private volatile long retryAfter;

    public I18nServiceImpl(I18nMapper i18nMapper) {
        this.i18nMapper = i18nMapper;
    }

    /** 현재 사용할 수 있는 언어 코드를 조회 한다. */
    @Override
    public List<String> languages() {
        return read(i18nMapper::selectLanguages);
    }

    /** 인증된 사용자의 저장된 선호 언어 조회 */
    @Override
    public String preference(Long userId) {
        return read(() -> i18nMapper.selectPreference(userId));
    }

    /** 메시지 코드 또는 개발용 메시지 이름으로 원장 조회 */
    @Override
    public I18nRows.Message message(MessageRef ref) {
        return read(() -> ref.kind() == MessageRef.Kind.CODE
                ? i18nMapper.selectMessageByCode(ref.value())
                : i18nMapper.selectMessageByName(ref.value())
        );
    }

    /** 메시지 코드에 해당하는 언어별 번역문 조회 */
    @Override
    public List<I18nRows.Text> texts(String code, List<String> languages) {
        return read(() -> i18nMapper.selectTexts(code, languages()));
    }

//    /** 기존 메시지 가져오기   */
//    @Override
//    public List<I18nRows.Text> texts(MessageRef ref) {
//        return read(() -> i18nMapper.selectTexts(ref.value(), languages()));
//    }
//    public List<I18nRows.Text> texts(String messageCode, MessageRef ref) {
//        // MessageRef 내부의 코드 값과 필요한 언어 목록을 전달하여 조회
//        return read(() -> i18nMapper.selectTexts(ref.value(), languages()));
//    }


    /** 권한 계산으로 하용된 메뉴 번호의 번역 조회 */
    @Override
    public List<I18nRows.MenuText> menuTexts(List<Long> ids, List<String> languages) {
        return read(() -> i18nMapper.selectMenuTexts(ids, languages));
    }

    /** 메뉴에 연결된 아이콘 정보 조회 */
    @Override
    public List<I18nRows.MenuIcon> menuIcons(List<Long> ids) {
        return read(() -> i18nMapper.selectMenuIcons(ids));
    }

    /**
     * 조회 실패를 공통 장애 식별자로 변환한다.
     * 내부 예외 본문, SQL 및 바인딩 값은 노출하지 않는다.
     * todo: read() 메서드: 서킷 브레이커(Circuit Breaker) 동작 구조
     *      DB 장애 발생 시 시스템 부하를 줄이기 위해 5초간 요청을 차단하는 보호 로직
     */
    private <T> T read(Supplier<T> query) {
        long deadline = retryAfter;

        if (deadline != 0L && System.nanoTime() - deadline < 0L) {
            throw new I18nService.Unavailable();
        }
        try {
            return query.get();
        } catch (RuntimeException failure) {
            retryAfter = System.nanoTime() + 5_000_000L;

            log.warn("I18n read unavailable: {}", failure.getClass().getSimpleName());
            throw new I18nService.Unavailable();
        }
    }

}
