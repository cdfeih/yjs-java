package com.yjs.java.crdt.types;

import com.yjs.java.crdt.BaseCRDT;
import com.yjs.java.crdt.CRDT;
import com.yjs.java.crdt.operation.CRDTOperation;
import lombok.Getter;
import lombok.Setter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * YMap是一个分布式键值对CRDT实现，支持并发读写
 */
@Getter
@Setter
public class YMap extends BaseCRDT {

    private Map<String, Object> entries;
    private Map<String, Long> entryTimestamps;

    public YMap() {
        super();
        this.entries = new ConcurrentHashMap<>();
        this.entryTimestamps = new ConcurrentHashMap<>();
    }

    /**
     * 存储键值对
     * @param key 键
     * @param value 值
     * @return 之前的值，如果不存在则返回null
     */
    public Object set(String key, Object value) {
        Object oldValue = entries.put(key, value);
        entryTimestamps.put(key, System.currentTimeMillis());
        incrementVersion();
        return oldValue;
    }

    /**
     * 获取键对应的值
     * @param key 键
     * @return 值，如果不存在则返回null
     */
    public Object get(String key) {
        return entries.get(key);
    }

    /**
     * 检查是否包含指定的键
     * @param key 键
     * @return 是否包含
     */
    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }

    /**
     * 移除指定的键值对
     * @param key 键
     * @return 被移除的值
     */
    public Object remove(String key) {
        Object removedValue = entries.remove(key);
        entryTimestamps.remove(key);
        incrementVersion();
        return removedValue;
    }

    /**
     * 获取映射的大小
     * @return 大小
     */
    public int size() {
        return entries.size();
    }

    /**
     * 检查映射是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * 清除所有键值对
     */
    public void clear() {
        entries.clear();
        entryTimestamps.clear();
        incrementVersion();
    }

    /**
     * 获取所有键的集合
     * @return 键集合
     */
    public Set<String> keySet() {
        return new HashSet<>(entries.keySet());
    }

    /**
     * 获取所有值的集合
     * @return 值集合
     */
    public Collection<Object> values() {
        return new ArrayList<>(entries.values());
    }

    /**
     * 获取所有键值对的集合
     * @return 键值对集合
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        return new HashSet<>(entries.entrySet());
    }

    @Override
    public void merge(CRDT other) {
        if (other == null || other == this || !(other instanceof YMap)) {
            return;
        }
        
        YMap otherMap = (YMap) other;
        
        // 对于每个键，选择时间戳最新的值
        for (String key : otherMap.keySet()) {
            Long otherTimestamp = otherMap.entryTimestamps.get(key);
            Long localTimestamp = this.entryTimestamps.get(key);
            
            // 如果本地没有该键，或者对方的时间戳更新，则更新本地值
            if (!this.containsKey(key) || (otherTimestamp != null && localTimestamp != null && otherTimestamp > localTimestamp)) {
                this.entries.put(key, otherMap.get(key));
                this.entryTimestamps.put(key, otherTimestamp);
            }
        }
        
        // 更新版本和时间戳
        this.version = Math.max(this.version, otherMap.getVersion());
        this.timestamp = Math.max(this.timestamp, otherMap.getTimestamp());
        incrementVersion();
    }

    @Override
    public Object getState() {
        // 返回不可修改的映射副本
        return Collections.unmodifiableMap(new HashMap<>(entries));
    }

    @Override
    public void applyOperation(Object operation) {
        if (!(operation instanceof CRDTOperation)) {
            return;
        }
        
        CRDTOperation op = (CRDTOperation) operation;
        switch (op.getOperationType()) {
            case INSERT:
            case UPDATE:
                if (op.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) op.getData();
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        set(entry.getKey(), entry.getValue());
                    }
                }
                break;
            case DELETE:
                if (op.getData() instanceof String) {
                    remove((String) op.getData());
                }
                break;
            case CLEAR:
                clear();
                break;
            default:
                break;
        }
    }

}