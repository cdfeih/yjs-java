package com.yjs.java.crdt.types;

import com.yjs.java.crdt.BaseCRDT;
import com.yjs.java.crdt.CRDT;
import com.yjs.java.crdt.operation.CRDTOperation;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * YArray是一个分布式数组CRDT实现，支持并发读写
 */
@Getter
@Setter
public class YArray extends BaseCRDT {

    private List<Object> elements;
    private Map<String, Integer> elementIds;
    private final ReadWriteLock lock;

    public YArray() {
        super();
        this.elements = new CopyOnWriteArrayList<>();
        this.elementIds = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * 添加元素到数组末尾
     *
     * @param element 要添加的元素
     * @return 添加后的数组大小
     */
    public int add(Object element) {
        String elementId = generateElementId();
        elements.add(element);
        elementIds.put(elementId, elements.size() - 1);
        incrementVersion();
        return elements.size();
    }

    /**
     * 在指定索引位置插入元素
     *
     * @param index   插入位置索引
     * @param element 要插入的元素
     */
    public void insert(int index, Object element) {
        if (index < 0 || index > elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }

        String elementId = generateElementId();
        elements.add(index, element);

        // 更新索引映射
        updateElementIdsAfterInsert(index);
        elementIds.put(elementId, index);
        incrementVersion();
    }

    /**
     * 获取指定索引位置的元素
     *
     * @param index 索引位置
     * @return 元素值
     */
    public Object get(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }
        return elements.get(index);
    }

    /**
     * 移除指定索引位置的元素
     *
     * @param index 索引位置
     * @return 被移除的元素
     */
    public Object remove(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }

        Object removed = elements.remove(index);

        // 移除对应的元素ID
        String elementIdToRemove = null;
        for (Map.Entry<String, Integer> entry : elementIds.entrySet()) {
            if (entry.getValue() == index) {
                elementIdToRemove = entry.getKey();
                break;
            }
        }

        if (elementIdToRemove != null) {
            elementIds.remove(elementIdToRemove);
        }

        // 更新索引映射
        updateElementIdsAfterRemove(index);
        incrementVersion();

        return removed;
    }

    /**
     * 更新指定索引位置的元素
     *
     * @param index   索引位置
     * @param element 新元素
     * @return 旧元素
     */
    public Object set(int index, Object element) {
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }

        Object oldElement = elements.set(index, element);
        incrementVersion();
        return oldElement;
    }

    /**
     * 获取数组大小
     *
     * @return 数组大小
     */
    public int size() {
        return elements.size();
    }

    /**
     * 检查数组是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * 清除数组所有元素
     */
    public void clear() {
        elements.clear();
        elementIds.clear();
        incrementVersion();
    }

    @Override
    public void merge(CRDT other) {
        if (other == null || other == this || !(other instanceof YArray)) {
            return;
        }

        YArray otherArray = (YArray) other;
        if (shouldMerge(other)) {
            lock.writeLock().lock();
            try {
                // 合并两个数组的所有元素
                List<Object> mergedElements = new ArrayList<>(elements);

                // 添加对方数组的所有元素
                for (int i = 0; i < otherArray.size(); i++) {
                    mergedElements.add(otherArray.get(i));
                }

                // 重新生成所有元素ID映射
                Map<String, Integer> mergedIds = new HashMap<>();
                for (int i = 0; i < mergedElements.size(); i++) {
                    mergedIds.put(generateElementId(), i);
                }

                this.elements = new CopyOnWriteArrayList<>(mergedElements);
                this.elementIds = mergedIds;
                this.version = Math.max(this.version, otherArray.getVersion());
                this.timestamp = Math.max(this.timestamp, otherArray.getTimestamp());

                // 增加版本号表示合并操作
                incrementVersion();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * 内部方法：检查是否应该合并另一个CRDT实例
     *
     * @param other 要合并的CRDT实例
     * @return 是否应该合并
     */
    @Override
    protected boolean shouldMerge(CRDT other) {
        if (!(other instanceof YArray)) {
            return false;
        }
        YArray otherArray = (YArray) other;
        return otherArray.getVersion() > this.version ||
                (otherArray.getVersion() == this.version && otherArray.getTimestamp() > this.timestamp);
    }

    @Override
    protected void incrementVersion() {
        this.version++;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public Object getState() {
        return new ArrayList<>(elements);
    }

    @Override
    public void applyOperation(Object operation) {
        if (!(operation instanceof CRDTOperation)) {
            return;
        }

        CRDTOperation op = (CRDTOperation) operation;
        switch (op.getOperationType()) {
            case INSERT:
                if (op.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) op.getData();
                    Integer index = (Integer) data.get("index");
                    Object element = data.get("element");
                    if (index != null && element != null) {
                        insert(index, element);
                    }
                }
                break;
            case UPDATE:
                if (op.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) op.getData();
                    Integer index = (Integer) data.get("index");
                    Object element = data.get("element");
                    if (index != null && element != null) {
                        set(index, element);
                    }
                }
                break;
            case DELETE:
                if (op.getData() instanceof Integer) {
                    remove((Integer) op.getData());
                }
                break;
            case CLEAR:
                clear();
                break;
            default:
                break;
        }
    }

    /**
     * 生成元素唯一ID
     *
     * @return 元素ID
     */
    private String generateElementId() {
        return getClass().getSimpleName() + ":" + generateId();
    }

    /**
     * 在插入元素后更新元素ID索引映射
     *
     * @param startIndex 起始索引
     */
    private void updateElementIdsAfterInsert(int startIndex) {
        for (Map.Entry<String, Integer> entry : elementIds.entrySet()) {
            if (entry.getValue() >= startIndex) {
                entry.setValue(entry.getValue() + 1);
            }
        }
    }

    /**
     * 在删除元素后更新元素ID索引映射
     *
     * @param startIndex 起始索引
     */
    private void updateElementIdsAfterRemove(int startIndex) {
        for (Map.Entry<String, Integer> entry : elementIds.entrySet()) {
            if (entry.getValue() > startIndex) {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

}