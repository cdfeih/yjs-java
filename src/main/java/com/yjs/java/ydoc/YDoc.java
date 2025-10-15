package com.yjs.java.ydoc;

import com.yjs.java.crdt.CRDT;
import com.yjs.java.crdt.operation.CRDTOperation;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YDoc是YJS的核心文档类，负责协调所有CRDT实例
 */
@Getter
@Setter
public class YDoc {

    private String id;
    private Map<String, CRDT> sharedTypes;
    private List<CRDTOperation> pendingOperations;
    private Set<String> clients;
    private long version;
    private long timestamp;

    public YDoc() {
        this.id = UUID.randomUUID().toString();
        this.sharedTypes = new ConcurrentHashMap<>();
        this.pendingOperations = Collections.synchronizedList(new ArrayList<>());
        this.clients = ConcurrentHashMap.newKeySet();
        this.version = 0;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 注册共享类型
     *
     * @param name 类型名称
     * @param crdt CRDT实例
     */
    public void register(String name, CRDT crdt) {
        sharedTypes.put(name, crdt);
        incrementVersion();
    }

    /**
     * 获取共享类型
     *
     * @param name 类型名称
     * @return CRDT实例
     */
    public CRDT get(String name) {
        return sharedTypes.get(name);
    }

    /**
     * 应用操作
     *
     * @param operation 要应用的操作
     */
    public void applyOperation(CRDTOperation operation) {
        CRDT target = sharedTypes.get(operation.getTargetId());
        if (target != null) {
            target.applyOperation(operation);
            incrementVersion();
        } else {
            // 如果目标不存在，将操作加入待处理队列
            pendingOperations.add(operation);
        }
    }

    /**
     * 应用一系列操作
     *
     * @param operations 操作列表
     */
    public void applyOperations(List<CRDTOperation> operations) {
        operations.forEach(this::applyOperation);
    }

    /**
     * 合并另一个文档的状态
     *
     * @param other 要合并的文档
     */
    public void merge(YDoc other) {
        if (other == null || other == this) {
            return;
        }

        // 合并共享类型
        other.getSharedTypes().forEach((name, crdt) -> {
            CRDT localCRDT = this.get(name);
            if (localCRDT != null) {
                localCRDT.merge(crdt);
            } else {
                this.register(name, crdt);
            }
        });

        // 处理待处理操作
        processPendingOperations();
        incrementVersion();
    }

    /**
     * 处理待处理的操作
     */
    private void processPendingOperations() {
        Iterator<CRDTOperation> iterator = pendingOperations.iterator();
        while (iterator.hasNext()) {
            CRDTOperation operation = iterator.next();
            CRDT target = sharedTypes.get(operation.getTargetId());
            if (target != null) {
                target.applyOperation(operation);
                iterator.remove();
            }
        }
    }

    /**
     * 生成文档的状态快照
     *
     * @return 文档状态
     */
    public Map<String, Object> getState() {
        Map<String, Object> state = new HashMap<>();
        sharedTypes.forEach((name, crdt) -> {
            state.put(name, crdt.getState());
        });
        state.put("version", this.version);
        state.put("timestamp", this.timestamp);
        return state;
    }

    /**
     * 添加客户端
     *
     * @param clientId 客户端ID
     */
    public void addClient(String clientId) {
        clients.add(clientId);
    }

    /**
     * 移除客户端
     *
     * @param clientId 客户端ID
     */
    public void removeClient(String clientId) {
        clients.remove(clientId);
    }

    /**
     * 获取客户端数量
     *
     * @return 客户端数量
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * 递增版本号
     */
    private void incrementVersion() {
        this.version++;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 清除文档内容
     */
    public void clear() {
        sharedTypes.clear();
        pendingOperations.clear();
        incrementVersion();
    }

}