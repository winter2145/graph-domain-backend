#!/bin/bash
# Description: 初始化Elasticsearch索引和映射
# Usage: 在Elasticsearch容器启动后运行此脚本
# Author: xin

set -e

echo "🚀 Starting Elasticsearch initialization..."

# 等待ES服务完全启动
echo "⏳ Waiting for Elasticsearch to be ready..."
until curl -s -f http://elasticsearch:9200/_cluster/health > /dev/null; do
    echo "Elasticsearch is not ready yet, waiting..."
    sleep 10
done

# 额外等待确保ES完全就绪
sleep 5

echo "✅ Elasticsearch is ready! Creating indices..."

# 创建索引函数
create_index() {
    local index_name=$1
    local mapping_file=$2

    echo "📝 Processing index: $index_name"

    # 检查索引是否已存在
    if curl -s -f -XGET "http://elasticsearch:9200/$index_name" > /dev/null; then
        echo "⚠️ Index $index_name already exists, skipping creation."
        return 0
    fi

    # 创建索引
    if curl -s -f -XPUT "http://elasticsearch:9200/$index_name" -H 'Content-Type: application/json' -d@"$mapping_file"; then
        echo "✅ Index $index_name created successfully."
    else
        echo "❌ Failed to create index $index_name."
        return 1
    fi
}

# 创建所有索引
create_index "user" "/mappings/user-mapping.json"
create_index "picture" "/mappings/picture-mapping.json"
create_index "space" "/mappings/space-mapping.json"
create_index "search_keyword" "/mappings/search_keyword_mapping.json"

echo "🎉 All indices creation completed!"

# 显示索引状态
echo -e "\n📊 Current indices status:"
curl -s "http://elasticsearch:9200/_cat/indices?v"

# 验证IK分词器
echo -e "\n🔍 Testing IK analyzer..."
curl -X POST "http://elasticsearch:9200/_analyze" -H 'Content-Type: application/json' -d'{
  "analyzer": "ik_max_word",
  "text": "这是一个测试文本"
}' || echo "IK analyzer test failed, but continuing..."

echo -e "\n✨ Elasticsearch initialization completed successfully!"