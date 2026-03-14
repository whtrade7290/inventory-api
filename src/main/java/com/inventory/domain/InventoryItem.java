package com.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 정의 테이블에 저장된 하나의 데이터 행(row)을 나타내는 엔티티
 * 실제 값은 ItemValue를 통해 컬럼별로 저장된다
 */
@Entity
@Table(name = "inventory_item")
@Getter
@NoArgsConstructor
public class InventoryItem {

    /** 아이템 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 아이템이 속한 사용자 정의 테이블 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private UserTable userTable;

    /** 생성 시각 (최초 저장 시 자동 설정, 이후 변경 불가) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 마지막 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 이 아이템의 컬럼별 실제 값 목록 */
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemValue> values = new ArrayList<>();

    /**
     * 어느 테이블에 속할지를 지정해 아이템 생성
     * 실제 셀 값은 ItemValue로 별도 저장된다
     */
    public InventoryItem(UserTable userTable) {
        this.userTable = userTable;
    }

    /**
     * 최초 저장 직전 자동 호출
     * createdAt과 updatedAt을 현재 시각으로 초기화한다
     */
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 엔티티 수정 직전 자동 호출
     * updatedAt을 현재 시각으로 갱신한다
     */
    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
