package com.xin.graphdomainbackend.infrastructure.elasticsearch.util;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;

/**
 * Elasticsearch 8.x 新 Java API Client 查询工具类
 * 不再依赖任何已废弃的 QueryBuilders
 */
public class EsQueryUtil {

    /* -------------------- 1. term 相关 -------------------- */
    /** term 精确匹配，无 boost */
    public static Query term(String field, Object value) {
        return new TermQuery.Builder()
                .field(field)
                .value(toFieldValue(value))  // 转换成 FieldValue
                .build()
                ._toQuery();
    }

    /** term 精确匹配，带 boost */
    public static Query term(String field, Object value, float boost) {
        return new TermQuery.Builder()
                .field(field)
                .value(toFieldValue(value))  // ✅ 转换成 FieldValue
                .boost(boost)
                .build()
                ._toQuery();
    }

    // 通用 Object -> FieldValue 转换
    private static FieldValue toFieldValue(Object value) {
        if (value instanceof String s) return FieldValue.of(s);
        if (value instanceof Integer i) return FieldValue.of(i);
        if (value instanceof Long l) return FieldValue.of(l);
        if (value instanceof Double d) return FieldValue.of(d);
        if (value instanceof Boolean b) return FieldValue.of(b);
        return FieldValue.of(JsonData.of(value)); // fallback
    }

    /* -------------------- 2. match 相关 -------------------- */
    /** match 分词匹配，无 boost */
    public static Query match(String field, String text) {
        return new MatchQuery.Builder()
                .field(field)
                .query(text)
                .build()
                ._toQuery();
    }

    /** match 分词匹配，带 boost */
    public static Query match(String field, String text, float boost) {
        return new MatchQuery.Builder()
                .field(field)
                .query(text)
                .boost(boost)
                .build()
                ._toQuery();
    }

    /* -------------------- 3. 其他常用 -------------------- */
    public static Query exists(String field) {
        return new ExistsQuery.Builder().field(field).build()._toQuery();
    }

    public static Query notExists(String field) {
        return BoolQuery.of(b -> b.mustNot(exists(field)))._toQuery();
    }

    /** range gte / lte 简化 */
    public static Query range(String field, Object gte, Object lte) {
        return new RangeQuery.Builder()
                .field(field)
                .gte(JsonData.of(gte))
                .lte(JsonData.of(lte))
                .build()
                ._toQuery();
    }

    /** 快速生成 BoolQuery，避免 new Builder() 样板 */
    public static BoolQueryBuilder bool() {
        return new BoolQueryBuilder();
    }

    /** 链式 BoolQuery 构建器 */
    public static class BoolQueryBuilder {
        private final BoolQuery.Builder builder = new BoolQuery.Builder();

        public BoolQueryBuilder must(Query... queries) {
            for (Query q : queries) builder.must(q);
            return this;
        }

        public BoolQueryBuilder should(Query... queries) {
            for (Query q : queries) builder.should(q);
            return this;
        }

        public BoolQueryBuilder mustNot(Query... queries) {
            for (Query q : queries) builder.mustNot(q);
            return this;
        }

        public BoolQueryBuilder minimumShouldMatch(int i) {
            builder.minimumShouldMatch(String.valueOf(i));
            return this;
        }

        public Query build() {
            return builder.build()._toQuery();
        }
    }

    private EsQueryUtil() {}
}