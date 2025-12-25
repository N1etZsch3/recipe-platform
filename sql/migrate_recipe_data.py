#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
菜谱数据迁移脚本

功能：将 recipe_info 表的 description 字段中的 JSON 数据迁移到 
recipe_ingredient 和 recipe_step 表中，并将剩余的 intro 存回 description 字段。

使用方法：
    1. 安装依赖：pip install pymysql
    2. 修改下方的数据库连接配置
    3. 运行脚本：python migrate_recipe_data.py

注意事项：
    - 执行前请先备份数据库！
    - 脚本会自动跳过已有步骤/用料数据的菜谱
    - 脚本支持 dry-run 模式，先预览再执行

作者：AI Assistant
日期：2025-12-25
"""

import json
import pymysql
from pymysql.cursors import DictCursor
import argparse
import sys

# ==================== 数据库配置 ====================
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'your_password',  # 请修改为实际密码
    'database': 'recipe-platform',
    'charset': 'utf8mb4'
}
# ===================================================


def get_connection():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG, cursorclass=DictCursor)


def parse_description(description):
    """
    解析 description 字段中的 JSON 数据
    
    期望格式：
    {
        "intro": "创作心得...",
        "ingredients": [{"name": "鸡肉", "amount": "500g"}, ...],
        "steps": [{"content": "第一步..."}, ...]
    }
    
    返回：(intro, ingredients, steps) 或 (原文本, [], []) 如果解析失败
    """
    if not description:
        return '', [], []
    
    try:
        data = json.loads(description)
        intro = data.get('intro', '')
        ingredients = data.get('ingredients', [])
        steps = data.get('steps', [])
        return intro, ingredients, steps
    except (json.JSONDecodeError, TypeError):
        # 非 JSON 格式，当作纯文本处理
        return description, [], []


def check_existing_data(cursor, recipe_id):
    """检查菜谱是否已有步骤或用料数据"""
    cursor.execute("SELECT COUNT(*) as cnt FROM recipe_ingredient WHERE recipe_id = %s", (recipe_id,))
    ingredient_count = cursor.fetchone()['cnt']
    
    cursor.execute("SELECT COUNT(*) as cnt FROM recipe_step WHERE recipe_id = %s", (recipe_id,))
    step_count = cursor.fetchone()['cnt']
    
    return ingredient_count > 0 or step_count > 0


def migrate_recipe(cursor, recipe, dry_run=False):
    """
    迁移单个菜谱的数据
    
    返回：(成功标志, 消息)
    """
    recipe_id = recipe['id']
    title = recipe['title']
    description = recipe['description']
    
    # 解析 JSON
    intro, ingredients, steps = parse_description(description)
    
    # 检查是否有数据需要迁移
    if not ingredients and not steps:
        return False, f"[跳过] ID={recipe_id} '{title}': 无需迁移（无结构化数据）"
    
    # 检查是否已有数据（避免重复插入）
    if check_existing_data(cursor, recipe_id):
        return False, f"[跳过] ID={recipe_id} '{title}': 已存在步骤/用料数据"
    
    if dry_run:
        return True, f"[预览] ID={recipe_id} '{title}': {len(ingredients)} 个用料, {len(steps)} 个步骤"
    
    try:
        # 1. 插入用料数据
        for idx, ing in enumerate(ingredients):
            name = ing.get('name', '').strip()
            amount = ing.get('amount', '').strip()
            if name:  # 只插入有名称的用料
                cursor.execute(
                    """
                    INSERT INTO recipe_ingredient (recipe_id, name, amount, sort_order)
                    VALUES (%s, %s, %s, %s)
                    """,
                    (recipe_id, name, amount or '适量', idx + 1)
                )
        
        # 2. 插入步骤数据
        for idx, step in enumerate(steps):
            # 兼容两种格式：{content: "..."} 或 {description: "..."}
            content = step.get('content', '') or step.get('description', '')
            content = content.strip()
            image_url = step.get('imageUrl', '') or step.get('image_url', '')
            
            if content:  # 只插入有内容的步骤
                cursor.execute(
                    """
                    INSERT INTO recipe_step (recipe_id, step_no, description, image_url)
                    VALUES (%s, %s, %s, %s)
                    """,
                    (recipe_id, idx + 1, content, image_url or None)
                )
        
        # 3. 更新 description 字段为纯文本 intro
        cursor.execute(
            """
            UPDATE recipe_info SET description = %s WHERE id = %s
            """,
            (intro[:500] if intro else None, recipe_id)  # 截断到 500 字符限制
        )
        
        return True, f"[成功] ID={recipe_id} '{title}': {len(ingredients)} 个用料, {len(steps)} 个步骤"
    
    except Exception as e:
        return False, f"[失败] ID={recipe_id} '{title}': {str(e)}"


def main():
    parser = argparse.ArgumentParser(description='菜谱数据迁移脚本')
    parser.add_argument('--dry-run', action='store_true', help='预览模式，不实际执行迁移')
    parser.add_argument('--recipe-id', type=int, help='只迁移指定 ID 的菜谱')
    args = parser.parse_args()
    
    print("=" * 60)
    print("菜谱数据迁移脚本")
    print("=" * 60)
    
    if args.dry_run:
        print("⚠️  预览模式：不会实际修改数据库")
    else:
        print("⚠️  生产模式：将实际修改数据库，请确保已备份！")
    
    print()
    
    try:
        conn = get_connection()
        cursor = conn.cursor()
        
        # 查询需要迁移的菜谱
        if args.recipe_id:
            cursor.execute("SELECT id, title, description FROM recipe_info WHERE id = %s", (args.recipe_id,))
        else:
            cursor.execute("SELECT id, title, description FROM recipe_info ORDER BY id")
        
        recipes = cursor.fetchall()
        print(f"📋 共找到 {len(recipes)} 条菜谱记录")
        print("-" * 60)
        
        success_count = 0
        skip_count = 0
        fail_count = 0
        
        for recipe in recipes:
            success, message = migrate_recipe(cursor, recipe, args.dry_run)
            print(message)
            
            if '[成功]' in message or '[预览]' in message:
                success_count += 1
            elif '[跳过]' in message:
                skip_count += 1
            else:
                fail_count += 1
        
        print("-" * 60)
        print(f"📊 统计: 成功/预览 {success_count}, 跳过 {skip_count}, 失败 {fail_count}")
        
        if not args.dry_run and success_count > 0:
            conn.commit()
            print("✅ 事务已提交")
        elif args.dry_run:
            print("ℹ️  预览模式，未提交任何更改")
        
        cursor.close()
        conn.close()
        
    except pymysql.Error as e:
        print(f"❌ 数据库错误: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ 未知错误: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()
