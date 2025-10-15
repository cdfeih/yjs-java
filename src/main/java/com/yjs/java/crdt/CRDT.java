package com.yjs.java.crdt;

import java.io.Serializable;

/**
 * 所有CRDT类型的基础接口
 */
public interface CRDT extends Serializable {

    /**
     * 合并另一个CRDT实例的状态
     * @param other 要合并的CRDT实例
     */
    void merge(CRDT other);

    /**
     * 获取CRDT的当前状态
     * @return 状态对象
     */
    Object getState();

    /**
     * 应用操作到CRDT
     * @param operation 要应用的操作
     */
    void applyOperation(Object operation);

    /**
     * 生成唯一标识符
     * @return 唯一标识符
     */
    String generateId();
    
    /**
     * 获取CRDT的唯一标识符
     * @return 唯一标识符
     */
    String getId();

}