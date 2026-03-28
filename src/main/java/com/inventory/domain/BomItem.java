package com.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BOM(Bill of Materials) 아이템 — 특정 현장에 배정된 재고 아이템과 수량을 저장하는 엔티티
 * project와 inventory_item을 연결하는 중간 테이블 역할을 한다
 */
@Entity
@Table(name = "bom_item")
@Getter
@NoArgsConstructor
public class BomItem {

    /** BOM 아이템 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 BOM 아이템이 속한 현장 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** 배정된 재고 아이템 (EAV 구조의 행) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    /** 배정 수량 (필수) */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 생성 시각 (최초 저장 시 자동 설정, 이후 변경 불가) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * BOM 아이템 생성
     * @param project 배정될 현장
     * @param inventoryItem 배정할 재고 아이템
     * @param quantity 수량
     */
    public BomItem(Project project, InventoryItem inventoryItem, Integer quantity) {
        this.project = project;
        this.inventoryItem = inventoryItem;
        this.quantity = quantity;
    }

    /**
     * 최초 저장 직전 자동 호출
     * createdAt을 현재 시각으로 초기화한다
     */
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 수량 수정
     * @param quantity 변경할 수량
     */
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
