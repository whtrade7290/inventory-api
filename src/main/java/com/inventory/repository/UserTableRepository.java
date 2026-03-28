package com.inventory.repository;

import com.inventory.domain.UserTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTableRepository extends JpaRepository<UserTable, Long> {

    /** 특정 role의 테이블 목록 조회 — 재고 현황 집계 시 PARTS 테이블 필터링에 사용 */
    List<UserTable> findByRole(UserTable.Role role);
}
