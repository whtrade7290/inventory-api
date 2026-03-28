package com.inventory.controller;

import com.inventory.domain.ColumnDefinition;
import com.inventory.domain.InventoryItem;
import com.inventory.domain.ItemValue;
import com.inventory.domain.UserTable;
import com.inventory.repository.ColumnDefinitionRepository;
import com.inventory.repository.InventoryItemRepository;
import com.inventory.repository.ItemValueRepository;
import com.inventory.repository.UserTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 재고 현황 관련 HTTP 요청을 처리하는 컨트롤러
 * 기본 경로: /api/stock
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    /** 사용자 정의 테이블 저장소 — PARTS 테이블 필터링에 사용 */
    private final UserTableRepository userTableRepository;

    /** 재고 아이템 저장소 */
    private final InventoryItemRepository inventoryItemRepository;

    /** 아이템 값 저장소 — 부품명 조회에 사용 */
    private final ItemValueRepository itemValueRepository;

    /** 컬럼 정의 저장소 — 컬럼명 조회에 사용 */
    private final ColumnDefinitionRepository columnDefinitionRepository;

    /**
     * 재고 현황 조회 (GET /api/stock/summary)
     * PARTS role 테이블의 모든 inventory_item에 대해
     * 현재 재고 수량(stock_quantity)과 부품 셀 값을 반환한다
     * 프론트엔드는 values 배열에서 부품명 컬럼 값을 선택해 표시한다
     */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StockSummaryItem>> summary() {
        // PARTS role 테이블 조회
        List<UserTable> partsTables = userTableRepository.findByRole(UserTable.Role.PARTS);
        if (partsTables.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Long> tableIds = partsTables.stream().map(UserTable::getId).toList();

        // PARTS 테이블의 모든 inventory_item 조회
        List<InventoryItem> items = tableIds.stream()
                .flatMap(tid -> inventoryItemRepository.findByUserTableId(tid).stream())
                .toList();
        if (items.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // inventory_item ID 목록으로 item_value 일괄 조회 (N+1 방지)
        List<Long> itemIds = items.stream().map(InventoryItem::getId).toList();
        List<ItemValue> allValues = itemValueRepository.findByItemIdIn(itemIds);
        Map<Long, List<ItemValue>> valuesByItemId = allValues.stream()
                .collect(Collectors.groupingBy(iv -> iv.getItem().getId()));

        // 컬럼 정의 일괄 조회 (컬럼명 확보)
        List<ColumnDefinition> allColumns = columnDefinitionRepository.findByUserTableIdIn(tableIds);
        Map<Long, ColumnDefinition> columnById = allColumns.stream()
                .collect(Collectors.toMap(ColumnDefinition::getId, c -> c));

        // 테이블 ID → 테이블명 맵
        Map<Long, String> tableNameById = partsTables.stream()
                .collect(Collectors.toMap(UserTable::getId, UserTable::getName));

        // 결과 조립
        List<StockSummaryItem> result = new ArrayList<>();
        for (InventoryItem item : items) {
            List<CellValue> cells = valuesByItemId
                    .getOrDefault(item.getId(), List.of()).stream()
                    .map(iv -> {
                        ColumnDefinition col = columnById.get(iv.getColumn().getId());
                        String colName = (col != null) ? col.getName() : "알 수 없음";
                        return new CellValue(iv.getColumn().getId(), colName, iv.getValue());
                    })
                    .toList();

            result.add(new StockSummaryItem(
                    item.getId(),
                    item.getUserTable().getId(),
                    tableNameById.get(item.getUserTable().getId()),
                    item.getStockQuantity(),
                    cells));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 재고 현황 응답 항목
     * inventoryItemId: 아이템 식별자
     * tableId / tableName: 소속 테이블 정보
     * stockQuantity: 현재 재고 수량 (입고 증가 / BOM 등록 차감)
     * values: 부품 셀 값 목록 — 프론트엔드에서 부품명 등 원하는 컬럼을 선택해 표시
     */
    public record StockSummaryItem(
            Long inventoryItemId,
            Long tableId,
            String tableName,
            int stockQuantity,
            List<CellValue> values) {}

    /**
     * 입고 등록 (POST /api/stock/inbound)
     * 지정한 inventory_item의 stock_quantity를 요청 수량만큼 증가시킨다
     * @param request itemId: 입고할 부품 아이템 ID, quantity: 입고 수량
     */
    @PostMapping("/inbound")
    @Transactional
    public ResponseEntity<Void> inbound(@RequestBody InboundRequest request) {
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다.");
        }
        InventoryItem item = inventoryItemRepository.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다. id=" + request.itemId()));
        item.addStock(request.quantity());
        inventoryItemRepository.save(item);
        return ResponseEntity.ok().build();
    }

    /**
     * 입고 등록 요청 바디
     * itemId: 입고할 inventory_item ID
     * quantity: 입고 수량 (1 이상)
     */
    public record InboundRequest(Long itemId, int quantity) {}

    /** 셀 값 응답 DTO */
    public record CellValue(Long columnId, String columnName, String value) {}
}
