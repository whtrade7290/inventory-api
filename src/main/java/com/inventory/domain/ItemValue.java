package com.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인벤토리 아이템의 특정 컬럼에 저장된 실제 값을 나타내는 엔티티
 * 아이템(행)과 컬럼의 교차점에 해당하는 셀 값을 저장한다
 */
@Entity
@Table(name = "item_value")
@Getter
@NoArgsConstructor
public class ItemValue {

    /** 값 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 값이 속한 인벤토리 아이템 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    /** 이 값이 해당하는 컬럼 정의 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private ColumnDefinition column;

    /** 실제 저장된 값 (문자열로 통합 저장, 타입 변환은 DataType 기준) */
    @Column
    private String value;

    /**
     * 아이템, 컬럼, 값을 받아 셀 값 생성
     * RELATION 타입의 경우 value에 참조 아이템의 ID를 문자열로 저장한다
     */
    public ItemValue(InventoryItem item, ColumnDefinition column, String value) {
        this.item = item;
        this.column = column;
        this.value = value;
    }
}
