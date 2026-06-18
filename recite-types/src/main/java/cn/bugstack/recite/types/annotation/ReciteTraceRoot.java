package cn.bugstack.recite.types.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 链路追踪根节点 — 标注在 Controller 入口方法上.
 * AOP 自动生成 traceId、记录 trace_runs 表、编排子节点.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReciteTraceRoot {

    /** 入口方法名，如 "submitAnswer"、"finishRecite" */
    String value();
}
