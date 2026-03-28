package com.inventory.controller;

import com.inventory.service.InventoryItemService;
import com.inventory.service.InventoryItemService.ItemResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 인벤토리 아이템 관련 HTTP 요청을 처리하는 컨트롤러
 * 기본 경로: /api/tables/{tableId}/items
 */
@RestController
@RequestMapping("/api/tables/{tableId}/items")
@RequiredArgsConstructor
public class InventoryItemController {

    /** 아이템 비즈니스 로직 처리 서비스 */
    private final InventoryItemService inventoryItemService;

    /**
     * 특정 테이블의 아이템 목록 조회 (GET /api/tables/{tableId}/items)
     * 각 아이템의 셀 값과 RELATION 해석 결과를 포함해 반환한다
     */
    @GetMapping
    public ResponseEntity<List<ItemResult>> findAll(@PathVariable Long tableId) {
        return ResponseEntity.ok(inventoryItemService.findByTableId(tableId));
    }

    /**
     * 아이템 추가 (POST /api/tables/{tableId}/items)
     * 요청 바디의 columnId → value 맵을 기반으로 아이템과 셀 값을 함께 생성한다
     * stockQuantity는 실제 재고 기준값으로 inventory_item.stock_quantity에 저장된다
     */
    @PostMapping
    public ResponseEntity<ItemResult> create(@PathVariable Long tableId,
                                              @RequestBody ItemRequest request) {
        return ResponseEntity.ok(inventoryItemService.create(tableId, request.values(), request.stockQuantity()));
    }

    /**
     * 아이템 삭제 (DELETE /api/tables/{tableId}/items/{itemId})
     * 아이템과 연관된 모든 셀 값이 함께 삭제된다
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Long tableId, @PathVariable Long itemId) {
        inventoryItemService.delete(tableId, itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 아이템 생성 요청 바디
     * values: 컬럼 ID → 저장할 값 (RELATION 타입이면 참조할 아이템 ID를 문자열로 전달)
     * stockQuantity: 초기 재고 수량 (미전송 시 0으로 처리)
     */
    public record ItemRequest(Map<Long, String> values, int stockQuantity) {}
}
