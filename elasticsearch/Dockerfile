FROM elasticsearch:8.10.2

USER root

# 复制IK分词器插件到容器
COPY elasticsearch-analysis-ik-8.10.2.zip /tmp/elasticsearch-analysis-ik-8.10.2.zip

# 安装IK分词器（root用户下执行）
RUN bin/elasticsearch-plugin install -b file:///tmp/elasticsearch-analysis-ik-8.10.2.zip && \
    rm -f /tmp/elasticsearch-analysis-ik-8.10.2.zip

# 创建IK配置目录
RUN mkdir -p /usr/share/elasticsearch/config/analysis-ik/

# 设置权限
RUN chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/

USER elasticsearch

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=300s --retries=3 \
    CMD curl -f http://localhost:9200/_cluster/health || exit 1