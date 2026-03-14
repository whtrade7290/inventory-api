package com.inventory.repository;

import com.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    /** 특정 테이블에 속한 모든 아이템을 조회 */
    List<InventoryItem> findByUserTableId(Long tableId);
}
