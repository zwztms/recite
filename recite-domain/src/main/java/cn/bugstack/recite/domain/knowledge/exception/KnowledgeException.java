package cn.bugstack.recite.domain.knowledge.exception;

import cn.bugstack.recite.types.exception.AppException;

/**
 * knowledge 子域异常.
 * <p>场景：模块 key 重复、模块不存在、导入文件格式错误、embedding 失败.</p>
 */
public class KnowledgeException extends AppException {

    public KnowledgeException(String message) {
        super("400", message);
    }

    public KnowledgeException(String code, String message) {
        super(code, message);
    }
}
