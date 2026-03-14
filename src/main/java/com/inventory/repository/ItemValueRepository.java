package com.inventory.repository;

import com.inventory.domain.ItemValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemValueRepository extends JpaRepository<ItemValue, Long> {

    /** 특정 아이템의 모든 셀 값을 조회 */
    List<ItemValue> findByItemId(Long itemId);

    /** 여러 아이템의 셀 값을 한 번에 조회 (N+1 방지) */
    List<ItemValue> findByItemIdIn(List<Long> itemIds);

    /** 특정 아이템의 특정 컬럼 값을 조회 (RELATION 해석 시 사용) */
    Optional<ItemValue> findByItemIdAndColumnId(Long itemId, Long columnId);
}
