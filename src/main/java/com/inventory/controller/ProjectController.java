package com.inventory.controller;

import com.inventory.service.ProjectService;
import com.inventory.service.ProjectService.BomRequest;
import com.inventory.service.ProjectService.ProjectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 프로젝트(현장) 관련 HTTP 요청을 처리하는 컨트롤러
 * 기본 경로: /api/projects
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    /** 프로젝트 비즈니스 로직 처리 서비스 */
    private final ProjectService projectService;

    /**
     * 프로젝트 등록 (POST /api/projects)
     * 프로젝트 저장 + BOM 아이템 저장 + 재고 자동 차감을 하나의 트랜잭션으로 처리한다
     * 재고 부족 시 400 응답과 함께 전체 롤백된다
     */
    @PostMapping
    public ResponseEntity<ProjectResult> create(@RequestBody ProjectRequest request) {
        ProjectResult result = projectService.create(
                request.name(),
                request.contractor(),
                request.date(),
                request.bomItems());
        return ResponseEntity.ok(result);
    }

    /**
     * 프로젝트 목록 조회 (GET /api/projects)
     * 각 프로젝트에 BOM 아이템 목록을 포함해 반환한다 (부품 상세 값 미포함)
     */
    @GetMapping
    public ResponseEntity<List<ProjectResult>> findAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    /**
     * 프로젝트 상세 조회 (GET /api/projects/{id})
     * BOM 아이템에 부품 셀 값(부품명 등)까지 포함해 반환한다
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResult> findById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    /**
     * 프로젝트 생성 요청 바디
     * date: ISO 날짜 형식 (yyyy-MM-dd), null 허용
     * bomItems: BOM 아이템 목록 (inventoryItemId + quantity)
     */
    public record ProjectRequest(
            String name,
            String contractor,
            LocalDate date,
            List<BomRequest> bomItems) {}
}
