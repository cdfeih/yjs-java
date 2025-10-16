package com.cdfeih.yjs.java.crdt.types;

import com.yjs.java.crdt.BaseCRDT;
import com.yjs.java.crdt.CRDT;
import com.yjs.java.crdt.operation.CRDTOperation;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * YText是一个分布式文本CRDT实现，支持字符级别的协作编辑
 */
@Getter
@Setter
public class YText extends BaseCRDT {

    // 内部字符存储，使用链表结构以支持高效的插入和删除操作
    private Node head;
    private Node tail;
    private int length;
    private final ReadWriteLock lock;
    private final Map<String, Node> nodes;

    // 用于表示文本节点的内部类
    private static class Node {
        char value;
        String id;
        long timestamp;
        Node prev;
        Node next;

        Node(char value, String id, long timestamp) {
            this.value = value;
            this.id = id;
            this.timestamp = timestamp;
        }
    }

    public YText() {
        super();
        // 创建哨兵头节点和尾节点
        this.head = new Node('\0', "HEAD", 0);
        this.tail = new Node('\0', "TAIL", 0);
        this.head.next = this.tail;
        this.tail.prev = this.head;
        this.length = 0;
        this.lock = new ReentrantReadWriteLock();
        this.nodes = new ConcurrentHashMap<>();
    }

    /**
     * 追加文本到末尾
     *
     * @param text 要追加的文本
     * @return 追加后的文本长度
     */
    public int append(String text) {
        if (text == null || text.isEmpty()) {
            return length;
        }

        lock.writeLock().lock();
        try {
            for (int i = 0; i < text.length(); i++) {
                insertAtEnd(text.charAt(i));
            }
            incrementVersion();
            return length;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 在指定位置插入文本
     *
     * @param index 插入位置
     * @param text  要插入的文本
     */
    public void insert(int index, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }

        lock.writeLock().lock();
        try {
            Node current = getNodeAt(index);
            for (int i = 0; i < text.length(); i++) {
                Node newNode = new Node(text.charAt(i), generateId(), System.currentTimeMillis());
                insertBefore(current, newNode);
            }
            incrementVersion();
        } finally {
            lock.writeLock().unlock();
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
        if (!(other instanceof YText)) {
            return false;
        }
        YText otherText = (YText) other;
        return otherText.getVersion() > this.version ||
                (otherText.getVersion() == this.version && otherText.getTimestamp() > this.timestamp);
    }

    /**
     * 删除指定范围的文本
     *
     * @param start 起始位置（包含）
     * @param end   结束位置（不包含）
     * @return 删除的文本
     */
    public String delete(int start, int end) {
        if (start < 0 || end > length || start >= end) {
            throw new IndexOutOfBoundsException("Invalid range: [" + start + ", " + end + ")");
        }

        lock.writeLock().lock();
        try {
            StringBuilder deletedText = new StringBuilder();
            Node startNode = getNodeAt(start);

            for (int i = start; i < end; i++) {
                deletedText.append(startNode.value);
                Node next = startNode.next;
                removeNode(startNode);
                startNode = next;
            }

            incrementVersion();
            return deletedText.toString();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected void incrementVersion() {
        this.version++;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取文本长度
     *
     * @return 文本长度
     */
    public int length() {
        lock.readLock().lock();
        try {
            return length;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查文本是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return length == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 转换为字符串
     *
     * @return 文本内容
     */
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder(length);
            Node current = head.next;
            while (current != tail) {
                sb.append(current.value);
                current = current.next;
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清除所有文本内容
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            head.next = tail;
            tail.prev = head;
            nodes.clear();
            length = 0;
            incrementVersion();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void merge(CRDT other) {
        if (other == null || other == this || !(other instanceof YText)) {
            return;
        }

        YText otherText = (YText) other;
        lock.writeLock().lock();
        try {
            // 收集所有节点，使用ID作为键以避免重复
            Map<String, Node> allNodes = new HashMap<>();
            
            // 添加本地节点
            Node current = this.head.next;
            while (current != this.tail) {
                allNodes.put(current.id, current);
                current = current.next;
            }
            
            // 添加远程节点
            current = otherText.head.next;
            while (current != otherText.tail) {
                // 如果本地已存在该节点ID，比较时间戳
                if (allNodes.containsKey(current.id)) {
                    Node existingNode = allNodes.get(current.id);
                    // 保留时间戳较新的节点
                    if (current.timestamp > existingNode.timestamp) {
                        allNodes.put(current.id, current);
                    }
                } else {
                    // 新节点直接添加
                    allNodes.put(current.id, current);
                }
                current = current.next;
            }
            
            // 重建文本链表
            this.head.next = this.tail;
            this.tail.prev = this.head;
            this.nodes.clear();
            this.length = 0;
            
            // 按时间戳排序节点
            allNodes.values().stream()
                .sorted(Comparator.comparingLong(n -> n.timestamp))
                .forEach(node -> {
                    Node newNode = new Node(node.value, node.id, node.timestamp);
                    insertBefore(this.tail, newNode);
                });
            
            this.version = Math.max(this.version, otherText.getVersion());
            this.timestamp = Math.max(this.timestamp, otherText.getTimestamp());
            incrementVersion();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Object getState() {
        return toString();
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
                    String text = (String) data.get("text");
                    if (index != null && text != null) {
                        insert(index, text);
                    }
                }
                break;
            case DELETE:
                if (op.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) op.getData();
                    Integer start = (Integer) data.get("start");
                    Integer end = (Integer) data.get("end");
                    if (start != null && end != null) {
                        // 确保删除范围有效
                        if (start >= 0 && end <= length && start < end) {
                            delete(start, end);
                        }
                    }
                }
                break;
            case UPDATE:
                // 对于文本，UPDATE操作通常被分解为DELETE和INSERT
                break;
            case CLEAR:
                clear();
                break;
            default:
                break;
        }
    }

    // 私有辅助方法
    private Node getNodeAt(int index) {
        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }

        Node current;
        if (index < length / 2) {
            // 从头部开始查找
            current = head.next;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            // 从尾部开始查找
            current = tail;
            for (int i = length; i > index; i--) {
                current = current.prev;
            }
        }

        return current;
    }

    private void insertAtEnd(char value) {
        Node newNode = new Node(value, generateId(), System.currentTimeMillis());
        insertBefore(tail, newNode);
    }

    private void insertBefore(Node target, Node newNode) {
        newNode.prev = target.prev;
        newNode.next = target;
        target.prev.next = newNode;
        target.prev = newNode;
        nodes.put(newNode.id, newNode);
        length++;
    }

    private void removeNode(Node node) {
        if (node == head || node == tail) {
            return;
        }

        node.prev.next = node.next;
        node.next.prev = node.prev;
        nodes.remove(node.id);
        length--;
    }

}