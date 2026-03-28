package com.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 현장(프로젝트) 정보를 저장하는 엔티티
 * 현장명, 시공사명, 날짜를 관리하며 BOM 아이템과 1:N 관계를 가진다
 */
@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor
public class Project {

    /** 현장 고유 식별자 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 현장명 (필수) */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 시공사명 (선택) */
    @Column(name = "contractor", length = 100)
    private String contractor;

    /** 현장 날짜 */
    @Column(name = "date")
    private LocalDate date;

    /** 생성 시각 (최초 저장 시 자동 설정, 이후 변경 불가) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 이 현장에 배정된 BOM 아이템 목록 */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomItem> bomItems = new ArrayList<>();

    /**
     * 현장 생성
     * @param name 현장명
     * @param contractor 시공사명 (null 허용)
     * @param date 현장 날짜 (null 허용)
     */
    public Project(String name, String contractor, LocalDate date) {
        this.name = name;
        this.contractor = contractor;
        this.date = date;
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
     * 현장 정보 수정
     * @param name 변경할 현장명
     * @param contractor 변경할 시공사명
     * @param date 변경할 날짜
     */
    public void update(String name, String contractor, LocalDate date) {
        this.name = name;
        this.contractor = contractor;
        this.date = date;
    }
}
