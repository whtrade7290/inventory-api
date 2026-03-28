package com.inventory.controller;

import com.inventory.domain.ColumnDefinition;
import com.inventory.service.ColumnDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 컬럼 정의 관련 HTTP 요청을 처리하는 컨트롤러
 * 기본 경로: /api/tables/{tableId}/columns
 */
@RestController
@RequestMapping("/api/tables/{tableId}/columns")
@RequiredArgsConstructor
public class ColumnDefinitionController {

    /** 컬럼 정의 비즈니스 로직 처리 서비스 */
    private final ColumnDefinitionService columnDefinitionService;

    /**
     * 특정 테이블의 컬럼 목록 조회 (GET /api/tables/{tableId}/columns)
     * colOrder 순서로 정렬된 컬럼 목록을 반환한다
     */
    @GetMapping
    public ResponseEntity<List<ColumnResponse>> findAll(@PathVariable Long tableId) {
        List<ColumnResponse> response = columnDefinitionService.findByTableId(tableId).stream()
                .map(ColumnResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 컬럼 추가 (POST /api/tables/{tableId}/columns)
     * 요청 바디의 정보로 컬럼을 생성하고 결과를 반환한다
     */
    @PostMapping
    public ResponseEntity<ColumnResponse> create(@PathVariable Long tableId,
                                                  @RequestBody ColumnRequest request) {
        ColumnDefinition column = columnDefinitionService.create(
                tableId, request.name(), request.dataType(), request.refTableId(), request.refColumnId()
        );
        return ResponseEntity.ok(ColumnResponse.from(column));
    }

    /**
     * 컬럼 이름 변경 (PATCH /api/tables/{tableId}/columns/{columnId})
     * 시스템 컬럼(isSystem=true)은 변경이 거부된다
     */
    @PatchMapping("/{columnId}")
    public ResponseEntity<ColumnResponse> rename(@PathVariable Long tableId,
                                                  @PathVariable Long columnId,
                                                  @RequestBody RenameRequest request) {
        ColumnDefinition column = columnDefinitionService.rename(tableId, columnId, request.name());
        return ResponseEntity.ok(ColumnResponse.from(column));
    }

    /**
     * 컬럼 순서 일괄 변경 (PATCH /api/tables/{tableId}/columns/reorder)
     * 요청 바디: { columnId: newOrder } 맵
     * 시스템 컬럼(colOrder=0)은 순서 변경에서 제외된다
     */
    @PatchMapping("/reorder")
    public ResponseEntity<List<ColumnResponse>> reorder(@PathVariable Long tableId,
                                                         @RequestBody Map<Long, Integer> orderMap) {
        List<ColumnResponse> response = columnDefinitionService.reorder(tableId, orderMap).stream()
                .map(ColumnResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 컬럼 삭제 (DELETE /api/tables/{tableId}/columns/{columnId})
     * 시스템 컬럼 및 RELATION 참조 중인 컬럼은 삭제가 거부된다
     */
    @DeleteMapping("/{columnId}")
    public ResponseEntity<Void> delete(@PathVariable Long tableId, @PathVariable Long columnId) {
        columnDefinitionService.delete(tableId, columnId);
        return ResponseEntity.noContent().build();
    }

    /** 컬럼 생성 요청 바디 */
    public record ColumnRequest(
            String name,
            ColumnDefinition.DataType dataType,
            Long refTableId,    // RELATION 타입일 때만 사용
            Long refColumnId    // RELATION 타입일 때만 사용
    ) {}

    /** 컬럼 이름 변경 요청 바디 */
    public record RenameRequest(String name) {}

    /** 컬럼 응답 DTO */
    public record ColumnResponse(
            Long id,
            String name,
            String dataType,
            Integer colOrder,
            Long refTableId,
            Long refColumnId,
            boolean isSystem,   // true이면 삭제/수정 불가
            String createdAt
    ) {
        /**
         * ColumnDefinition 엔티티를 응답 DTO로 변환
         * 엔티티의 각 필드를 꺼내 ColumnResponse 레코드를 생성해 반환한다
         */
        public static ColumnResponse from(ColumnDefinition column) {
            return new ColumnResponse(
                    column.getId(),
                    column.getName(),
                    column.getDataType().name(),
                    column.getColOrder(),
                    column.getRefTableId(),
                    column.getRefColumnId(),
                    column.isSystem(),
                    column.getCreatedAt().toString()
            );
        }
    }
}
