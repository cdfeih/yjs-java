package com.yjs.java.crdt.operation;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import java.util.HashMap;

/**
 * CRDT操作的基础实现类
 */
@Getter
@Setter
public class BaseCRDTOperation implements CRDTOperation {

    private String operationId;
    private OperationType operationType;
    private String targetId;
    private long timestamp;
    private Object data;
    private Map<String, Object> metadata;

    public BaseCRDTOperation() {
        this.operationId = CRDTOperation.generateOperationId();
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    public BaseCRDTOperation(OperationType operationType, String targetId, Object data) {
        this();
        this.operationType = operationType;
        this.targetId = targetId;
        this.data = data;
    }

    @Override
    public String toString() {
        return "BaseCRDTOperation{" +
                "operationId='" + operationId + '\'' +
                ", operationType=" + operationType +
                ", targetId='" + targetId + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }

}