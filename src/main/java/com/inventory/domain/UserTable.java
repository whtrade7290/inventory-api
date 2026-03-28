package com.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자가 정의한 테이블을 나타내는 엔티티
 * 인벤토리 시스템에서 데이터를 담을 구조(테이블)를 직접 정의할 수 있다
 */
@Entity
@Table(name = "user_table")
@Getter
@NoArgsConstructor
public class UserTable {

    /** 테이블 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 테이블 이름 */
    @Column(nullable = false)
    private String name;

    /**
     * 테이블 역할 — 사이드바 그룹핑 및 재고 집계에 사용
     * PARTS: 부품 테이블 (재고 현황 대상), CONTRACTOR: 시공사, PROJECT: 프로젝트, GENERAL: 일반
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.GENERAL;

    /** 테이블 역할 열거형 */
    public enum Role {
        PARTS,       // 부품 테이블 — 재고 현황 집계 대상
        CONTRACTOR,  // 시공사 테이블
        PROJECT,     // 프로젝트 테이블
        GENERAL      // 일반 (기본값)
    }

    /** 생성 시각 (최초 저장 시 자동 설정, 이후 변경 불가) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 마지막 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 이 테이블에 속한 컬럼 정의 목록 */
    @OneToMany(mappedBy = "userTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColumnDefinition> columns = new ArrayList<>();

    /** 이 테이블에 저장된 인벤토리 아이템 목록 */
    @OneToMany(mappedBy = "userTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryItem> items = new ArrayList<>();

    /**
     * 테이블 이름으로 생성 (role 기본값: GENERAL)
     * createdAt/updatedAt은 @PrePersist에서 자동 설정된다
     */
    public UserTable(String name) {
        this.name = name;
    }

    /**
     * 테이블 이름과 역할을 지정해 생성
     * role을 통해 사이드바 그룹핑과 재고 집계 대상 여부가 결정된다
     */
    public UserTable(String name, Role role) {
        this.name = name;
        this.role = role;
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
