package com.yjs.java.crdt.operation;

import java.io.Serializable;
import java.util.UUID;

/**
 * CRDT操作的基础接口
 */
public interface CRDTOperation extends Serializable {

    /**
     * 获取操作的唯一标识符
     * @return 操作ID
     */
    String getOperationId();

    /**
     * 获取操作的类型
     * @return 操作类型
     */
    OperationType getOperationType();

    /**
     * 获取操作的目标CRDT ID
     * @return 目标CRDT ID
     */
    String getTargetId();

    /**
     * 获取操作的时间戳
     * @return 时间戳
     */
    long getTimestamp();

    /**
     * 获取操作的数据
     * @return 操作数据
     */
    Object getData();

    /**
     * 操作类型枚举
     */
    enum OperationType {
        INSERT, UPDATE, DELETE, CLEAR, MERGE
    }

    /**
     * 生成操作ID的静态方法
     * @return 唯一操作ID
     */
    static String generateOperationId() {
        return UUID.randomUUID().toString();
    }

}