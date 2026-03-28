package com.inventory.service;

import com.inventory.domain.BomItem;
import com.inventory.domain.ColumnDefinition;
import com.inventory.domain.InventoryItem;
import com.inventory.domain.ItemValue;
import com.inventory.domain.Project;
import com.inventory.repository.BomItemRepository;
import com.inventory.repository.ColumnDefinitionRepository;
import com.inventory.repository.InventoryItemRepository;
import com.inventory.repository.ItemValueRepository;
import com.inventory.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 프로젝트(현장) 관련 비즈니스 로직을 처리하는 서비스
 * 핵심 기능: 프로젝트 등록 시 BOM 아이템 저장과 재고 자동 차감을 하나의 트랜잭션으로 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    /** 프로젝트 저장소 */
    private final ProjectRepository projectRepository;

    /** BOM 아이템 저장소 */
    private final BomItemRepository bomItemRepository;

    /** 재고 아이템 저장소 — 재고 차감에 사용 */
    private final InventoryItemRepository inventoryItemRepository;

    /** 아이템 값 저장소 — 부품명 조회에 사용 */
    private final ItemValueRepository itemValueRepository;

    /** 컬럼 정의 저장소 — 부품명 컬럼 조회에 사용 */
    private final ColumnDefinitionRepository columnDefinitionRepository;

    /**
     * 프로젝트 등록 — 핵심 트랜잭션
     * 1. project 저장
     * 2. bom_item 여러 개 저장
     * 3. 각 inventory_item의 stock_quantity 차감
     * 4. 재고 부족 시 IllegalStateException 발생 → 전체 롤백
     *
     * @param name       현장명
     * @param contractor 시공사명 (null 허용)
     * @param date       날짜 (null 허용)
     * @param bomRequests BOM 아이템 목록 (inventoryItemId + quantity)
     * @return 생성된 프로젝트 결과 DTO
     */
    @Transactional
    public ProjectResult create(String name, String contractor, LocalDate date,
                                List<BomRequest> bomRequests) {
        // 1. 프로젝트 저장
        Project project = projectRepository.save(new Project(name, contractor, date));

        // 2. BOM 아이템 처리 — 저장 + 재고 차감
        for (BomRequest bom : bomRequests) {
            InventoryItem item = inventoryItemRepository.findById(bom.inventoryItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "재고 아이템을 찾을 수 없습니다. id=" + bom.inventoryItemId()));

            // 3. 재고 부족 시 IllegalStateException 발생 → @Transactional 롤백
            item.deductStock(bom.quantity());

            // 4. BOM 아이템 저장
            bomItemRepository.save(new BomItem(project, item, bom.quantity()));
        }

        return toProjectResult(project, List.of());
    }

    /**
     * 프로젝트 전체 목록 조회 — BOM 아이템 목록 포함, 부품 상세 값 미포함
     * 목록용으로 가볍게 반환한다
     */
    public List<ProjectResult> findAll() {
        return projectRepository.findAllWithBomItems().stream()
                .map(p -> toProjectResult(p, List.of()))
                .toList();
    }

    /**
     * 프로젝트 상세 조회 — BOM 아이템 + 각 부품의 item_value 포함
     * 부품명 등 상세 정보를 함께 반환한다
     *
     * @param id 프로젝트 ID
     */
    public ProjectResult findById(Long id) {
        Project project = projectRepository.findWithBomItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. id=" + id));

        // BOM에 포함된 inventory_item ID 목록 수집
        List<Long> itemIds = project.getBomItems().stream()
                .map(b -> b.getInventoryItem().getId())
                .toList();

        if (itemIds.isEmpty()) {
            return toProjectResult(project, List.of());
        }

        // inventory_item별 item_value 일괄 조회 (N+1 방지)
        List<ItemValue> allValues = itemValueRepository.findByItemIdIn(itemIds);
        Map<Long, List<ItemValue>> valuesByItemId = allValues.stream()
                .collect(Collectors.groupingBy(iv -> iv.getItem().getId()));

        // inventory_item별 column_definition 조회 (컬럼명 확보)
        // 각 아이템이 속한 테이블 ID 수집
        List<Long> tableIds = project.getBomItems().stream()
                .map(b -> b.getInventoryItem().getUserTable().getId())
                .distinct()
                .toList();

        List<ColumnDefinition> allColumns = columnDefinitionRepository.findByUserTableIdIn(tableIds);
        Map<Long, ColumnDefinition> columnById = allColumns.stream()
                .collect(Collectors.toMap(ColumnDefinition::getId, c -> c));

        return toProjectDetailResult(project, valuesByItemId, columnById);
    }

    /**
     * 프로젝트 + BOM 목록을 결과 DTO로 변환 (부품 상세 값 없는 버전 — 목록용)
     */
    private ProjectResult toProjectResult(Project project, List<BomItemResult> bomItems) {
        List<BomItemResult> boms = project.getBomItems().stream()
                .map(b -> new BomItemResult(
                        b.getId(),
                        b.getInventoryItem().getId(),
                        b.getQuantity(),
                        List.of()))
                .toList();

        return new ProjectResult(
                project.getId(),
                project.getName(),
                project.getContractor(),
                project.getDate() != null ? project.getDate().toString() : null,
                project.getCreatedAt().toString(),
                boms);
    }

    /**
     * 프로젝트 + BOM 목록을 결과 DTO로 변환 (부품 item_value 포함 버전 — 상세용)
     */
    private ProjectResult toProjectDetailResult(Project project,
                                                 Map<Long, List<ItemValue>> valuesByItemId,
                                                 Map<Long, ColumnDefinition> columnById) {
        List<BomItemResult> boms = project.getBomItems().stream()
                .map(b -> {
                    Long itemId = b.getInventoryItem().getId();
                    List<ItemValueResult> values = valuesByItemId
                            .getOrDefault(itemId, List.of()).stream()
                            .map(iv -> {
                                ColumnDefinition col = columnById.get(iv.getColumn().getId());
                                String columnName = (col != null) ? col.getName() : "알 수 없음";
                                return new ItemValueResult(iv.getColumn().getId(), columnName, iv.getValue());
                            })
                            .toList();

                    return new BomItemResult(b.getId(), itemId, b.getQuantity(), values);
                })
                .toList();

        return new ProjectResult(
                project.getId(),
                project.getName(),
                project.getContractor(),
                project.getDate() != null ? project.getDate().toString() : null,
                project.getCreatedAt().toString(),
                boms);
    }

    // ─── 요청 DTO ────────────────────────────────────────────────

    /** 프로젝트 생성 요청 내 BOM 아이템 항목 */
    public record BomRequest(Long inventoryItemId, int quantity) {}

    // ─── 응답 DTO ────────────────────────────────────────────────

    /** 프로젝트 응답 DTO */
    public record ProjectResult(
            Long id,
            String name,
            String contractor,
            String date,
            String createdAt,
            List<BomItemResult> bomItems) {}

    /** BOM 아이템 응답 DTO */
    public record BomItemResult(
            Long id,
            Long inventoryItemId,
            int quantity,
            List<ItemValueResult> values) {}   // 목록 조회 시 빈 리스트, 상세 조회 시 부품 값 포함

    /** 부품 셀 값 응답 DTO — 상세 조회 시 부품명 확인에 사용 */
    public record ItemValueResult(Long columnId, String columnName, String value) {}
}
