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
 * 사용자 정의 테이블에 대한 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTableService {

    /** 사용자 정의 테이블 저장소 */
    private final UserTableRepository userTableRepository;

    /** 컬럼 정의 저장소 (참조 여부 확인에 사용) */
    private final ColumnDefinitionRepository columnDefinitionRepository;

    /**
     * 모든 테이블 목록 조회
     * DB에 저장된 전체 UserTable을 리스트로 반환한다
     */
    public List<UserTable> findAll() {
        return userTableRepository.findAll();
    }

    /**
     * 새 테이블 생성
     * 테이블 저장 후 ID 시스템 컬럼을 자동으로 삽입한다
     * ID 컬럼은 isSystem=true, colOrder=0으로 생성되어 삭제/수정이 불가하다
     */
    @Transactional
    public UserTable create(String name) {
        UserTable table = userTableRepository.save(new UserTable(name));

        // ID 시스템 컬럼 자동 삽입 (colOrder=0, isSystem=true)
        columnDefinitionRepository.save(
                new ColumnDefinition(table, "ID", ColumnDefinition.DataType.NUMBER, 0, true)
        );

        return table;
    }

    /**
     * 테이블 삭제
     * ID로 테이블을 조회한 뒤, 다른 테이블에서 RELATION으로 참조 중이면 예외를 던진다.
     * 참조가 없으면 테이블과 연관된 컬럼·아이템·값을 연쇄 삭제한다 (CascadeType.ALL)
     */
    @Transactional
    public void delete(Long id) {
        UserTable table = userTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("테이블을 찾을 수 없습니다. id=" + id));

        if (columnDefinitionRepository.existsByRefTableId(id)) {
            throw new IllegalStateException("다른 테이블에서 RELATION으로 참조 중인 테이블은 삭제할 수 없습니다.");
        }

        userTableRepository.delete(table);
    }
}
