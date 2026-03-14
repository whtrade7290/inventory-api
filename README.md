# inventory-api

ユーザーが自由にテーブル構造を定義し、データを管理できる在庫管理システムのバックエンド API です。
EAV（Entity-Attribute-Value）パターンを採用し、柔軟なデータ構造を実現しています。

---

## 技術スタック

| 項目 | 内容 |
|------|------|
| 言語 | Java 21 (Corretto) |
| フレームワーク | Spring Boot 3.5.11 |
| データベース | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| ビルドツール | Gradle (Kotlin DSL) |

---

## 機能概要

- ユーザー定義テーブルの作成・削除
- テーブルへのカラム追加・削除（TEXT / NUMBER / DATE / BOOLEAN / RELATION）
- アイテム（行）の追加・削除
- RELATION タイプのカラムによるテーブル間参照
- 参照中のテーブル・カラムの削除防止

---

## 事前準備

### PostgreSQL（Docker）

```bash
docker run --name inventory-db \
  -e POSTGRES_DB=inventory \
  -e POSTGRES_USER=inventory \
  -e POSTGRES_PASSWORD=inventory1234 \
  -p 5432:5432 \
  -d postgres:16
```

---

## 起動方法

```bash
./gradlew bootRun
```

起動後、`http://localhost:8080` でアクセスできます。

---

## API 一覧

### テーブル

| メソッド | パス | 説明 |
|----------|------|------|
| GET | `/api/tables` | テーブル一覧取得 |
| POST | `/api/tables` | テーブル作成 |
| DELETE | `/api/tables/{id}` | テーブル削除 |

### カラム

| メソッド | パス | 説明 |
|----------|------|------|
| GET | `/api/tables/{id}/columns` | カラム一覧取得 |
| POST | `/api/tables/{id}/columns` | カラム追加 |
| DELETE | `/api/tables/{id}/columns/{columnId}` | カラム削除 |

### アイテム

| メソッド | パス | 説明 |
|----------|------|------|
| GET | `/api/tables/{id}/items` | アイテム一覧取得（RELATION 解決済み） |
| POST | `/api/tables/{id}/items` | アイテム追加 |
| DELETE | `/api/tables/{id}/items/{itemId}` | アイテム削除 |

---

## リクエスト例

### テーブル作成
```json
POST /api/tables
{
  "name": "商品リスト"
}
```

### カラム追加（通常）
```json
POST /api/tables/1/columns
{
  "name": "商品名",
  "dataType": "TEXT",
  "refTableId": null,
  "refColumnId": null
}
```

### カラム追加（RELATION）
```json
POST /api/tables/1/columns
{
  "name": "カテゴリ",
  "dataType": "RELATION",
  "refTableId": 2,
  "refColumnId": 5
}
```

### アイテム追加
```json
POST /api/tables/1/items
{
  "values": {
    "1": "ノートパソコン",
    "2": "1500000"
  }
}
```

---

## アーキテクチャ

```
controller/   — HTTP リクエスト受付
service/      — ビジネスロジック
repository/   — DB アクセス
domain/       — JPA エンティティ
```

### EAV パターン

```
user_table        — ユーザー定義テーブル
column_definition — カラム構造定義
inventory_item    — データ行（row）
item_value        — セル値（column × item）
```
