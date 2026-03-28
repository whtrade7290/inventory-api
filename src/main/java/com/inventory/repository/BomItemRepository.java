package com.inventory.repository;

import com.inventory.domain.BomItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BomItemRepository extends JpaRepository<BomItem, Long> {
}
