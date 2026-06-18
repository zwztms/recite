package cn.bugstack.recite.types.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 链路追踪节点 — 标注在业务方法上.
 * AOP 自动记录 trace_nodes 表，含节点名、类型、耗时.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReciteTraceNode {

    /** 节点类型：AUTH / CACHE / VALIDATE / LLM / DB / BUSINESS / MQ / SSE */
    String type();

    /** 节点中文名称，如 "DeepSeek评分"、"保存背诵记录" */
    String name();
}
