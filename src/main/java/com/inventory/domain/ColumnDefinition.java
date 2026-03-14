package com.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정의 테이블의 컬럼 구조를 나타내는 엔티티
 * 각 컬럼의 이름, 데이터 타입, 순서, 참조 정보 등을 저장한다
 */
@Entity
@Table(name = "column_definition")
@Getter
@NoArgsConstructor
public class ColumnDefinition {

    /** 컬럼 정의 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 컬럼이 속한 사용자 정의 테이블 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private UserTable userTable;

    /** 컬럼 이름 */
    @Column(nullable = false)
    private String name;

    /** 컬럼 데이터 타입 (TEXT, NUMBER, DATE, BOOLEAN, RELATION) */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private DataType dataType;

    /** 컬럼 표시 순서 */
    @Column(name = "col_order", nullable = false)
    private Integer colOrder;

    /** RELATION 타입일 때 참조하는 대상 테이블 ID */
    @Column(name = "ref_table_id")
    private Long refTableId;

    /** RELATION 타입일 때 참조하는 대상 컬럼 ID */
    @Column(name = "ref_column_id")
    private Long refColumnId;

    /** 생성 시각 (최초 저장 시 자동 설정) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 일반 컬럼 생성자 (TEXT, NUMBER, DATE, BOOLEAN)
     * refTableId, refColumnId는 null로 설정된다
     */
    public ColumnDefinition(UserTable userTable, String name, DataType dataType, Integer colOrder) {
        this.userTable = userTable;
        this.name = name;
        this.dataType = dataType;
        this.colOrder = colOrder;
    }

    /**
     * RELATION 타입 컬럼 생성자
     * 참조할 테이블 ID와 조인 키가 되는 컬럼 ID를 함께 설정한다
     */
    public ColumnDefinition(UserTable userTable, String name, DataType dataType, Integer colOrder, Long refTableId, Long refColumnId) {
        this.userTable = userTable;
        this.name = name;
        this.dataType = dataType;
        this.colOrder = colOrder;
        this.refTableId = refTableId;
        this.refColumnId = refColumnId;
    }

    /**
     * 최초 저장 직전 자동 호출
     * createdAt을 현재 시각으로 초기화한다
     */
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /** 컬럼이 지원하는 데이터 타입 */
    public enum DataType {
        TEXT,       // 문자열
        NUMBER,     // 숫자
        DATE,       // 날짜
        BOOLEAN,    // 참/거짓
        RELATION    // 다른 테이블 참조
    }
}
