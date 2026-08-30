/** 2026.08.14 MenuController.java by rwkang.
 * 1. Redis 효과로 놀라울 정도로 간단.
 * 2. todo: MenuMapper 도 불필요, DB Oracle 조회도 불필요. ∵) 로그인 -> Oracle DB 권한 계산 -> Redis 로 처리했으므로.
 * 3. todo: ∴) 메뉴 요청은 Nexacro -> X-Auth-Token -> RedisTokenFilter -> Redis -> MenuController -> session.menus()
 *
 */
/** 2026.08.20 Updated by cdx.
 * 변경 이유:
 * 인증·초기 비밀번호 필터를 통과한 세션의 이미 계산된 메뉴만 반환하는 현재 구조는 적절하다.
 * Redis 스냅샷의 즉시 무효화 정책과 PermissionGuard가 구현되면 Controller에서 Oracle을 다시 조회할 필요는 없다.
 */

package com.soft2cost2.menu.controller;

import com.soft2cost2.auth.model.AuthSessionContext;
import com.soft2cost2.auth.model.MenuPermission;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/navigation")
public class MenuController {

    // todo: @AuthenticationPrincipal은 RedisTokenFilter가 SecurityContext에 넣은 AuthSessionContext를 Controller 파라미터로 전달한다.
    //      여기서는 Oracle DB를 다시 조회하지 않는다. 로그인 시점에 계산해 Redis에 저장한 session.menus를 그대로 반환한다.
    //      메뉴 API가 200이었다는 사실은 다음이 모두 성공했음을 뜻한다.
    //          X-Auth-Token 헤더 전송
    //          Redis에서 세션 조회
    //          세션 만료 검사
    //          Spring Security 인증 객체 생성
    //          MenuController 진입
    //          메뉴 JSON 응답 반환

    @GetMapping("/menus")
    public List<MenuPermission> menus(@AuthenticationPrincipal AuthSessionContext session) {
        //return session.menus();
        return List.copyOf(session.menus());
    }
}
