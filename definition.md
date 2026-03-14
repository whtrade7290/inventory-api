# 재고관리 시스템 — 백엔드 개발 컨텍스트

## 프로젝트 스펙
- Java 21 (Corretto)
- Spring Boot 3.5.11
- Gradle Kotlin
- PostgreSQL 16 (Docker, localhost:5432)
- 패키지 루트: com.inventory

## 패키지 구조 (이미 생성됨)
src/main/java/com/inventory/
├── domain/
├── repository/
├── service/
└── controller/

## DB 연결 설정 (application.yml 완료)
url: jdbc:postgresql://localhost:5432/inventory
username: inventory
password: inventory1234

## 아키텍처: EAV (Entity-Attribute-Value)
메타데이터와 실제 데이터를 분리하는 구조.
사용자가 테이블·컬럼을 직접 정의하고, 테이블 간 관계(RELATION)도 설정 가능.

## DB 테이블 (이미 생성 완료)
user_table        (id, name, created_at, updated_at)
column_definition (id, table_id, name, data_type, col_order, ref_table_id, ref_column_id, created_at)
inventory_item    (id, table_id, created_at, updated_at)
item_value        (id, item_id, column_id, value TEXT)

인덱스도 생성 완료:
- item_value(item_id), item_value(column_id)
- column_definition(table_id), inventory_item(table_id)

## 컬럼 타입 (data_type)
TEXT / NUMBER / DATE / BOOLEAN / RELATION

## RELATION 타입 동작
- ref_table_id: 참조할 테이블 id
- ref_column_id: 조인 키가 되는 column_definition id
- 조회 시 백엔드가 메타데이터를 읽고 동적 JOIN 쿼리 생성
- 결과는 인라인으로 표시, 읽기 전용

## 삭제 규칙
- 컬럼 삭제 전 RELATION 참조 여부 체크 → 참조 중이면 거부
- 테이블 삭제 시 column_definition, inventory_item, item_value 연쇄 삭제
- 타 테이블에서 RELATION 참조 중인 테이블은 삭제 거부

## ORM 전략
- JPA: user_table, column_definition, inventory_item 엔티티 매핑
- item_value 조회 + 동적 JOIN: Native Query 직접 작성
- ddl-auto: validate (DDL은 수동 관리)

## 현재 진행 상황
- Spring Boot 프로젝트 생성 완료
- DB 테이블 생성 완료
- 패키지 구조 생성 완료
- 엔티티 작성 시작 전

## 지금 할 일
domain 패키지에 JPA 엔티티 4개 작성:
1. UserTable.java
2. ColumnDefinition.java
3. InventoryItem.java
4. ItemValue.java

작성 후 애플리케이션 기동 확인까지 해줘.