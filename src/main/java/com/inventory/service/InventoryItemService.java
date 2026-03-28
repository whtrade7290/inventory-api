package com.inventory.service;

import com.inventory.domain.ColumnDefinition;
import com.inventory.domain.InventoryItem;
import com.inventory.domain.ItemValue;
import com.inventory.domain.UserTable;
import com.inventory.repository.ColumnDefinitionRepository;
import com.inventory.repository.InventoryItemRepository;
import com.inventory.repository.ItemValueRepository;
import com.inventory.repository.UserTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 인벤토리 아이템에 대한 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryItemService {

    /** 사용자 정의 테이블 저장소 */
    private final UserTableRepository userTableRepository;

    /** 컬럼 정의 저장소 */
    private final ColumnDefinitionRepository columnDefinitionRepository;

    /** 인벤토리 아이템 저장소 */
    private final InventoryItemRepository inventoryItemRepository;

    /** 셀 값 저장소 */
    private final ItemValueRepository itemValueRepository;

    /**
     * 특정 테이블의 아이템 목록 조회
     * 각 아이템의 모든 셀 값을 함께 반환하며, RELATION 타입 컬럼은 참조된 아이템의 값으로 해석해서 반환한다
     */
    public List<ItemResult> findByTableId(Long tableId) {
        validateTableExists(tableId);

        // 테이블의 컬럼 목록 조회 (순서 보장)
        List<ColumnDefinition> columns = columnDefinitionRepository.findByUserTableIdOrderByColOrder(tableId);

        // 테이블의 아이템 목록 조회
        List<InventoryItem> items = inventoryItemRepository.findByUserTableId(tableId);
        if (items.isEmpty()) {
            return List.of();
        }

        // 모든 아이템의 셀 값을 한 번에 조회 (N+1 방지)
        List<Long> itemIds = items.stream().map(InventoryItem::getId).toList();
        List<ItemValue> allValues = itemValueRepository.findByItemIdIn(itemIds);

        // 아이템 ID → (컬럼 ID → ItemValue) 맵으로 변환
        Map<Long, Map<Long, ItemValue>> valueMap = allValues.stream()
                .collect(Collectors.groupingBy(
                        iv -> iv.getItem().getId(),
                        Collectors.toMap(iv -> iv.getColumn().getId(), iv -> iv)
                ));

        // 각 아이템을 결과 DTO로 변환
        return items.stream()
                .map(item -> toItemResult(item, columns, valueMap.getOrDefault(item.getId(), Map.of())))
                .toList();
    }

    /**
     * 새 아이템 생성
     * 요청에서 받은 columnId → value 맵을 기반으로 ItemValue를 생성해 함께 저장한다
     * stockQuantity는 inventory_item.stock_quantity에 저장되며 실제 재고 기준값이다
     */
    @Transactional
    public ItemResult create(Long tableId, Map<Long, String> columnValues, int stockQuantity) {
        UserTable userTable = userTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("테이블을 찾을 수 없습니다. id=" + tableId));

        // 테이블의 컬럼 목록을 맵으로 변환 (columnId → ColumnDefinition)
        List<ColumnDefinition> columns = columnDefinitionRepository.findByUserTableIdOrderByColOrder(tableId);
        Map<Long, ColumnDefinition> columnMap = columns.stream()
                .collect(Collectors.toMap(ColumnDefinition::getId, c -> c));

        // 아이템 생성 — 초기 재고 수량 포함
        InventoryItem item = inventoryItemRepository.save(new InventoryItem(userTable, stockQuantity));

        // 요청된 컬럼별 값을 ItemValue로 생성
        List<ItemValue> values = new ArrayList<>();
        for (Map.Entry<Long, String> entry : columnValues.entrySet()) {
            ColumnDefinition column = columnMap.get(entry.getKey());
            if (column == null) {
                throw new IllegalArgumentException("존재하지 않는 컬럼입니다. id=" + entry.getKey());
            }
            values.add(new ItemValue(item, column, entry.getValue()));
        }
        itemValueRepository.saveAll(values);

        // 저장 완료 후 결과 반환
        Map<Long, ItemValue> savedValueMap = values.stream()
                .collect(Collectors.toMap(iv -> iv.getColumn().getId(), iv -> iv));
        return toItemResult(item, columns, savedValueMap);
    }

    /**
     * 아이템 삭제
     * 연관된 ItemValue는 CascadeType.ALL에 의해 함께 삭제된다
     */
    @Transactional
    public void delete(Long tableId, Long itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다. id=" + itemId));

        // 요청한 테이블에 속한 아이템인지 확인
        if (!item.getUserTable().getId().equals(tableId)) {
            throw new IllegalArgumentException("해당 테이블에 속한 아이템이 아닙니다.");
        }

        inventoryItemRepository.delete(item);
    }

    /**
     * 아이템과 셀 값 맵을 받아 ItemResult DTO로 변환
     * RELATION 타입 컬럼의 경우, 저장된 참조 아이템 ID로 실제 값을 조회해서 반환한다
     */
    private ItemResult toItemResult(InventoryItem item, List<ColumnDefinition> columns,
                                    Map<Long, ItemValue> valueMap) {
        List<CellResult> cells = columns.stream()
                .map(column -> {
                    ItemValue iv = valueMap.get(column.getId());
                    String rawValue = (iv != null) ? iv.getValue() : null;
                    String displayValue = rawValue;

                    // RELATION 타입: 저장된 값은 참조 아이템 ID → 실제 값으로 해석
                    if (column.getDataType() == ColumnDefinition.DataType.RELATION
                            && rawValue != null && column.getRefColumnId() != null) {
                        try {
                            Long refItemId = Long.parseLong(rawValue);
                            displayValue = itemValueRepository
                                    .findByItemIdAndColumnId(refItemId, column.getRefColumnId())
                                    .map(ItemValue::getValue)
                                    .orElse(null);
                        } catch (NumberFormatException e) {
                            displayValue = null;
                        }
                    }

                    return new CellResult(column.getId(), column.getName(), column.getDataType().name(),
                            rawValue, displayValue);
                })
                .toList();

        return new ItemResult(item.getId(), item.getStockQuantity(),
                item.getCreatedAt().toString(), item.getUpdatedAt().toString(), cells);
    }

    /**
     * 테이블 존재 여부를 확인하고 없으면 예외를 던진다
     */
    private void validateTableExists(Long tableId) {
        if (!userTableRepository.existsById(tableId)) {
            throw new IllegalArgumentException("테이블을 찾을 수 없습니다. id=" + tableId);
        }
    }

    /** 아이템 조회 결과 DTO — stockQuantity는 실제 재고 기준값 */
    public record ItemResult(Long id, int stockQuantity, String createdAt, String updatedAt, List<CellResult> values) {}

    /** 셀 값 결과 DTO */
    public record CellResult(
            Long columnId,
            String columnName,
            String dataType,
            String value,           // 저장된 원본 값 (RELATION이면 참조 아이템 ID)
            String displayValue     // 표시용 값 (RELATION이면 참조된 실제 값)
    ) {}
}
