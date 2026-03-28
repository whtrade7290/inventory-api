package com.inventory.repository;

import com.inventory.domain.ColumnDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColumnDefinitionRepository extends JpaRepository<ColumnDefinition, Long> {

    /** 특정 테이블의 컬럼 목록을 표시 순서(colOrder) 기준으로 조회 */
    List<ColumnDefinition> findByUserTableIdOrderByColOrder(Long tableId);

    /** 특정 테이블을 RELATION으로 참조하는 컬럼이 존재하는지 확인 (테이블 삭제 전 체크) */
    boolean existsByRefTableId(Long refTableId);

    /** 특정 컬럼을 RELATION의 ref_column_id로 참조하는 컬럼이 존재하는지 확인 (컬럼 삭제 전 체크) */
    boolean existsByRefColumnId(Long columnId);

    /** 특정 테이블에서 가장 큰 colOrder 값 조회 (새 컬럼 추가 시 순서 자동 계산에 사용) */
    int countByUserTableId(Long tableId);

    /** 여러 테이블의 컬럼 목록을 한 번에 조회 (프로젝트 상세 조회 시 N+1 방지) */
    List<ColumnDefinition> findByUserTableIdIn(List<Long> tableIds);
}
