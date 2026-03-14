package com.inventory.service;

import com.inventory.domain.ColumnDefinition;
import com.inventory.domain.UserTable;
import com.inventory.repository.ColumnDefinitionRepository;
import com.inventory.repository.UserTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 컬럼 정의에 대한 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColumnDefinitionService {

    /** 컬럼 정의 저장소 */
    private final ColumnDefinitionRepository columnDefinitionRepository;

    /** 사용자 정의 테이블 저장소 (테이블 존재 여부 확인에 사용) */
    private final UserTableRepository userTableRepository;

    /**
     * 특정 테이블의 컬럼 목록 조회
     * colOrder 순서로 정렬해서 반환한다
     */
    public List<ColumnDefinition> findByTableId(Long tableId) {
        validateTableExists(tableId);
        return columnDefinitionRepository.findByUserTableIdOrderByColOrder(tableId);
    }

    /**
     * 새 컬럼 추가
     * colOrder는 현재 컬럼 수 + 1로 자동 부여된다
     * RELATION 타입인 경우 refTableId, refColumnId를 함께 설정한다
     */
    @Transactional
    public ColumnDefinition create(Long tableId, String name, ColumnDefinition.DataType dataType,
                                   Long refTableId, Long refColumnId) {
        UserTable userTable = userTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("테이블을 찾을 수 없습니다. id=" + tableId));

        // RELATION 타입이면 refTableId, refColumnId 필수
        if (dataType == ColumnDefinition.DataType.RELATION) {
            if (refTableId == null || refColumnId == null) {
                throw new IllegalArgumentException("RELATION 타입은 refTableId와 refColumnId가 필요합니다.");
            }
        }

        // 새 컬럼의 순서는 기존 컬럼 수 + 1
        int colOrder = columnDefinitionRepository.countByUserTableId(tableId) + 1;

        ColumnDefinition column = (dataType == ColumnDefinition.DataType.RELATION)
                ? new ColumnDefinition(userTable, name, dataType, colOrder, refTableId, refColumnId)
                : new ColumnDefinition(userTable, name, dataType, colOrder);

        return columnDefinitionRepository.save(column);
    }

    /**
     * 컬럼 삭제
     * 다른 컬럼에서 ref_column_id로 참조 중인 경우 삭제가 거부된다
     */
    @Transactional
    public void delete(Long tableId, Long columnId) {
        ColumnDefinition column = columnDefinitionRepository.findById(columnId)
                .orElseThrow(() -> new IllegalArgumentException("컬럼을 찾을 수 없습니다. id=" + columnId));

        // 요청한 테이블에 속한 컬럼인지 확인
        if (!column.getUserTable().getId().equals(tableId)) {
            throw new IllegalArgumentException("해당 테이블에 속한 컬럼이 아닙니다.");
        }

        // 다른 RELATION 컬럼에서 이 컬럼을 참조 중이면 삭제 거부
        if (columnDefinitionRepository.existsByRefColumnId(columnId)) {
            throw new IllegalStateException("다른 컬럼에서 RELATION으로 참조 중인 컬럼은 삭제할 수 없습니다.");
        }

        columnDefinitionRepository.delete(column);
    }

    /**
     * 테이블 존재 여부를 확인하고 없으면 예외를 던진다
     */
    private void validateTableExists(Long tableId) {
        if (!userTableRepository.existsById(tableId)) {
            throw new IllegalArgumentException("테이블을 찾을 수 없습니다. id=" + tableId);
        }
    }
}
