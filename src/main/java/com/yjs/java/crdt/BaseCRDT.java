package com.yjs.java.crdt;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

/**
 * CRDT接口的基础实现类
 */
@Getter
@Setter
public abstract class BaseCRDT implements CRDT {

    private String id;
    protected long version;
    protected long timestamp;

    public BaseCRDT() {
        this.id = generateId();
        this.version = 0;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public abstract void merge(CRDT other);

    @Override
    public abstract Object getState();

    @Override
    public abstract void applyOperation(Object operation);

    /**
     * 递增版本号
     */
    protected void incrementVersion() {
        this.version++;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 检查是否需要合并
     * @param other 要比较的CRDT实例
     * @return 是否需要合并
     */
    protected boolean shouldMerge(CRDT other) {
        if (other instanceof BaseCRDT) {
            BaseCRDT otherBase = (BaseCRDT) other;
            return otherBase.version > this.version || 
                    (otherBase.version == this.version && otherBase.timestamp > this.timestamp);
        }
        return true;
    }

}