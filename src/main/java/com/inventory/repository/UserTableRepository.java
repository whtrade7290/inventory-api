package com.inventory.repository;

import com.inventory.domain.UserTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTableRepository extends JpaRepository<UserTable, Long> {
}
