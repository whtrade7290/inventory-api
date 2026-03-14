package com.inventory.controller;

import com.inventory.domain.UserTable;
import com.inventory.service.UserTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 정의 테이블 관련 HTTP 요청을 처리하는 컨트롤러
 * 기본 경로: /api/tables
 */
@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class UserTableController {

    /** 테이블 비즈니스 로직 처리 서비스 */
    private final UserTableService userTableService;

    /**
     * 모든 테이블 목록 조회 (GET /api/tables)
     * 서비스에서 전체 목록을 가져와 UserTableResponse 리스트로 변환해 반환한다
     */
    @GetMapping
    public ResponseEntity<List<UserTableResponse>> findAll() {
        List<UserTableResponse> response = userTableService.findAll().stream()
                .map(UserTableResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 새 테이블 생성 (POST /api/tables)
     * 요청 바디의 name을 서비스에 전달해 테이블을 생성하고 생성된 결과를 반환한다
     */
    @PostMapping
    public ResponseEntity<UserTableResponse> create(@RequestBody UserTableRequest request) {
        UserTable table = userTableService.create(request.name());
        return ResponseEntity.ok(UserTableResponse.from(table));
    }

    /**
     * 테이블 삭제 (DELETE /api/tables/{id})
     * 경로 변수로 받은 id를 서비스에 전달해 삭제하고 204 No Content를 반환한다
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userTableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 테이블 생성 요청 바디 */
    public record UserTableRequest(String name) {}

    /** 테이블 응답 DTO */
    public record UserTableResponse(Long id, String name, String createdAt, String updatedAt) {

        /**
         * UserTable 엔티티를 응답 DTO로 변환
         * 엔티티의 각 필드를 꺼내 UserTableResponse 레코드를 생성해 반환한다
         */
        public static UserTableResponse from(UserTable table) {
            return new UserTableResponse(
                    table.getId(),
                    table.getName(),
                    table.getCreatedAt().toString(),
                    table.getUpdatedAt().toString()
            );
        }
    }
}
