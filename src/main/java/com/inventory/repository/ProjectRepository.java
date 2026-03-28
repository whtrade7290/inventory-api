package com.inventory.repository;

import com.inventory.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 모든 프로젝트를 bomItems + inventoryItem과 함께 조회
     * DISTINCT로 중복 제거, inventoryItem JOIN으로 LAZY 로딩 예외 방지
     */
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.bomItems b LEFT JOIN FETCH b.inventoryItem ORDER BY p.createdAt DESC")
    List<Project> findAllWithBomItems();

    /**
     * 특정 프로젝트를 bomItems + inventoryItem + userTable까지 함께 조회
     * 상세 조회 시 부품 값 및 테이블 ID 접근을 위해 연관 엔티티를 미리 로드한다
     * @param id 프로젝트 ID
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.bomItems b LEFT JOIN FETCH b.inventoryItem i LEFT JOIN FETCH i.userTable WHERE p.id = :id")
    Optional<Project> findWithBomItemsById(Long id);
}
