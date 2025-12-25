# 菜谱数据迁移脚本使用说明

## 📋 概述

本目录包含两个数据迁移脚本，用于将 `recipe_info.description` 字段中的 JSON 数据迁移到独立的 `recipe_ingredient` 和 `recipe_step` 表中。

## 📁 文件说明

| 文件 | 类型 | 推荐场景 |
|------|------|----------|
| `migrate_recipe_data.py` | Python 脚本 | 推荐使用，支持预览模式 |
| `migrate_recipe_data.sql` | SQL 存储过程 | 直接在数据库执行 |

---

## 🐍 Python 脚本使用方法

### 安装依赖

```bash
pip install pymysql
```

### 配置数据库连接

编辑 `migrate_recipe_data.py`，修改第 28-34 行的数据库配置：

```python
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'your_password',  # 修改此处
    'database': 'recipe_platform',
    'charset': 'utf8mb4'
}
```

### 执行迁移

```bash
# 1. 预览模式（推荐先执行）
python migrate_recipe_data.py --dry-run

# 2. 正式迁移
python migrate_recipe_data.py

# 3. 只迁移指定菜谱
python migrate_recipe_data.py --recipe-id 123
```

---

## 🗄️ SQL 脚本使用方法

```bash
# 使用 MySQL 客户端执行
mysql -u root -p recipe_platform < migrate_recipe_data.sql
```

或在 Navicat/DataGrip 等工具中打开并执行。

---

## ⚠️ 重要提示

1. **执行前务必备份数据库！**
2. 脚本会自动跳过已有步骤/用料数据的菜谱
3. Python 脚本支持预览模式，建议先使用 `--dry-run` 确认影响范围
