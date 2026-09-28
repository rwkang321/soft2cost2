/** 2026.08.13 RedisAuthSessionService.java by rwkang.
 * 1. Redis 저장
 * 2. 여기서 의도적으로
 * todo: Nexacro 에는 원본 AccessToken,
 * todo: Redis Key 에는 AccessToken SHA-256 해시 를 사용 한다.
 * todo: 즉, Redis 관리 화면을 본다고 해서, 원본 로그인 토큰이 그대로 노출되는 구조를 피한다는 것이다.
 */

/** 2026.08.17 Updated by cdx.
 * 변경 이유:
 * key 구분자와 버전을 명확히 하고 userId별 token hash 인덱스를 둔다.
 * 비밀번호 변경·사용자 비활성화·역할 회수 시 logoutAll이 가능해진다.
 * Redis TTL뿐 아니라 payload의 expiresAt도 fail-closed로 확인한다.
 * null/공백 logout을 멱등 처리한다.
 *
 * 보안 영향과 주의:
 * 현재 세션 스냅샷은 권한 변경을 최대 8시간 늦게 반영한다. 관리자 변경 서비스는 커밋 후 logoutAll(userId)을 반드시 호출해야 한다.
 * Redis pipeline은 원자 트랜잭션이 아니다. 세션 key와 사용자 token set을 완전히 원자화하려면 Lua script를 사용해야 한다.
 * Redis 장애 시 인증을 우회해서는 안 된다. 로그인 발급은 503, 보호 API는 401/503 중 운영 정책을 정하되 절대 permit으로 전환하지 않는다.
 * 원본 accessToken은 로그·오류·MDC에 기록하지 않는다.
 */

package com.soft2cost2.auth.service;

import com.soft2cost2.auth.model.AuthSessionContext;
//import io.swagger.v3.oas.models.security.SecurityScheme;
import com.soft2cost2.auth.model.AuthorizationChangedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class RedisAuthSessionService {

    //private static final String PREFIX = "soft2cost2:auth:session:";
    private static final String SESSION_PREFIX = "soft2cost2:auth:v1:session:";
    private static final String USER_TOKENS_PREFIX = "soft2cost2:auth:v1:user:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final Duration sessionTtl;

    private final SecureRandom secureRandom = new SecureRandom();

    public RedisAuthSessionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${soft2cost2.auth.session-ttl-hours:8}") long sessionTtlHours
    ) {
        if (sessionTtlHours < 1 || sessionTtlHours > 24) {
            throw new IllegalStateException("session-ttl-hours must be between 1 and 24");
        }
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionTtl = Duration.ofHours(sessionTtlHours);
    }

    public IssuedSession issue(AuthSessionContext source) {

        Objects.requireNonNull(source, "source");

        String accessToken = createToken();
        String tokenHash = hashToken(accessToken);
        String sessionKey = sessionKey(tokenHash);
        String userTokenKey = userTokensKey(source.userId());

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(sessionTtl);
        AuthSessionContext context = copyWithTimes(source, issuedAt, expiresAt);

        try {
            String json = objectMapper.writeValueAsString(context);
            redisTemplate.opsForValue().set(sessionKey, json, sessionTtl);
            redisTemplate.opsForSet().add(userTokenKey, tokenHash);
            redisTemplate.expire(userTokenKey, sessionTtl);

            return new IssuedSession(accessToken, context);

        } catch (Exception exception) {
            cleanupPartialIssue(sessionKey, userTokenKey, tokenHash);
            throw new SessionStoreException("세션 발급 실패!!!", exception);
        }

    }

    /**
    public Optional<AuthSessionContext> find(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) { return Optional.empty(); }

        String tokenHash = hashToken(accessToken);
        String sessionKey = sessionKey(tokenHash);
        //String redisKey = SESSION_PREFIX + hashToken(accessToken);

        try {
            String json = redisTemplate.opsForValue().get(sessionKey);
            if (json == null) { return Optional.empty(); }

            AuthSessionContext context = objectMapper.readValue(json, AuthSessionContext.class);
            if (context.expiresAt() == null || !context.expiresAt().isAfter(Instant.now())) {
                deleteSession(tokenHash, context.userId());
                return Optional.empty();
            }

            return Optional.of(context);

        } catch (SessionStoreException exception) {
            throw new SessionStoreException("세션 조회 실패!!!", exception);
        }
    } */

    //? logout, deleteSession도 외부 공개 경계에서 같은 예외로 감싼다.
    //? 최초 실행 후 Redis down, timeout, 손상 JSON, 부분 session key를 각각 503/E503 fail-closed로 검증한다.
    public Optional<AuthSessionContext> find(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = hashToken(accessToken);
        try {
            String json = redisTemplate.opsForValue().get(sessionKey(tokenHash));
            if (json == null) {
                return Optional.empty();
            }
            AuthSessionContext context = objectMapper.readValue(
                    json, AuthSessionContext.class
            );
            if (context.expiresAt() == null
                    || !context.expiresAt().isAfter(Instant.now())) {
                deleteSession(tokenHash, context.userId());
                return Optional.empty();
            }
            return Optional.of(context);
        } catch (Exception exception) {
            throw new SessionStoreException("세션 조회 실패!!!", exception);
        }
    }

    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) { return; }

        try {
            String tokenHash = hashToken(accessToken);
            Optional<AuthSessionContext> context = find(accessToken);
            if (context.isPresent()) {
                deleteSession(tokenHash, context.get().userId());
            } else {
                redisTemplate.delete(sessionKey(tokenHash));
            }
        } catch (SessionStoreException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SessionStoreException("세션 삭제 실패!!!", exception);
        }
    }

    public void logoutAll(Long userId) {
        if (userId == null) { return; }

        String userTokenKey = userTokensKey(userId);
        try {
            Set<String> tokenHashes = redisTemplate.opsForSet().members(userTokenKey);
            if (tokenHashes != null && !tokenHashes.isEmpty()) {
                redisTemplate.delete(tokenHashes.stream()
                        .map(this::sessionKey)
                        .toList()
                );
            }
            redisTemplate.delete(userTokenKey);
        } catch (Exception exception) {
            throw new SessionStoreException("Redis sessionrevocation failed!!!", exception);
        }

    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        logoutAll(event.userId());
    }

    private void deleteSession(String tokenHash, Long userId) {
        redisTemplate.delete(sessionKey(tokenHash));
        redisTemplate.opsForSet().remove(userTokensKey(userId), tokenHash);
    }

    private void cleanupPartialIssue(String sessionKey, String userTokenKey, String tokenHash) {
        try {
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForSet().remove(userTokenKey, tokenHash);
        } catch (RuntimeException ignored) {

        }
    }

    private AuthSessionContext copyWithTimes(AuthSessionContext source, Instant issuedAt, Instant expiresAt) {
        return new AuthSessionContext(
                source.userId(),
                source.loginId(),
                source.userName(),
                source.companyId(),
                source.siteId(),
                source.deptId(),
                source.roleCodes(),
                source.menus(),
                source.passwordChangeRequired(),
                issuedAt,
                expiresAt
        );
    }

    private  String sessionKey(String tokenHash) {
        return SESSION_PREFIX + tokenHash;
    }

    private String userTokensKey(Long userId) {
        return USER_TOKENS_PREFIX + userId + ":tokens";
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    private String hashToken(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public record IssuedSession(
            String accessToken,
            AuthSessionContext context
    ) {}

    public record PasswordChangedEvent(Long userId) {}

    public static final class SessionStoreException extends RuntimeException {
        public SessionStoreException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuthorizationChanged(AuthorizationChangedEvent event) {
        SessionStoreException firstFailure = null;
        for (Long userId : event.userIds()) {
            try {
                logoutAll(userId);
            } catch (SessionStoreException exception) {
                if (firstFailure == null) {
                    firstFailure = exception;
                } else {
                    firstFailure.addSuppressed(exception);
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

}
