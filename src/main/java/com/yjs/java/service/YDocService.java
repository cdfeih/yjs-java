package com.yjs.java.service;

import com.yjs.java.crdt.CRDT;
import com.yjs.java.crdt.operation.CRDTOperation;
import com.yjs.java.crdt.types.YArray;
import com.yjs.java.crdt.types.YMap;
import com.yjs.java.crdt.types.YText;
import com.yjs.java.ydoc.YDoc;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YDoc服务类，提供文档管理的业务逻辑
 */
@Service
public class YDocService {

    private final Map<String, YDoc> documents = new ConcurrentHashMap<>();

    /**
     * 创建新文档
     *
     * @return 新文档ID
     */
    public String createDocument() {
        YDoc doc = new YDoc();
        documents.put(doc.getId(), doc);
        return doc.getId();
    }

    /**
     * 获取文档
     *
     * @param docId 文档ID
     * @return 文档实例
     */
    public YDoc getDocument(String docId) {
        return documents.get(docId);
    }

    /**
     * 删除文档
     *
     * @param docId 文档ID
     * @return 是否删除成功
     */
    public boolean deleteDocument(String docId) {
        return documents.remove(docId) != null;
    }

    /**
     * 在文档中创建YArray
     *
     * @param docId 文档ID
     * @param name  数组名称
     * @return 创建的YArray实例
     */
    public YArray createYArray(String docId, String name) {
        YDoc doc = documents.get(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        YArray array = new YArray();
        doc.register(name, array);
        return array;
    }

    /**
     * 在文档中创建YMap
     *
     * @param docId 文档ID
     * @param name  映射名称
     * @return 创建的YMap实例
     */
    public YMap createYMap(String docId, String name) {
        YDoc doc = documents.get(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        YMap map = new YMap();
        doc.register(name, map);
        return map;
    }

    /**
     * 在文档中创建YText
     *
     * @param docId 文档ID
     * @param name  文本名称
     * @return 创建的YText实例
     */
    public YText createYText(String docId, String name) {
        YDoc doc = documents.get(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        YText text = new YText();
        doc.register(name, text);
        return text;
    }

    /**
     * 从文档中获取共享类型
     *
     * @param docId 文档ID
     * @param name  共享类型名称
     * @return 共享类型实例
     */
    public CRDT getSharedType(String docId, String name) {
        YDoc doc = documents.get(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        CRDT sharedType = doc.get(name);
        if (sharedType == null) {
            throw new IllegalArgumentException("Shared type not found: " + name);
        }

        return sharedType;
    }

    /**
     * 应用操作到文档
     *
     * @param docId     文档ID
     * @param operation 操作
     */
    public void applyOperation(String docId, CRDTOperation operation) {
        YDoc doc = documents.get(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        doc.applyOperation(operation);
    }

    /**
     * 合并两个文档
     *
     * @param sourceDocId 源文档ID
     * @param targetDocId 目标文档ID
     */
    public void mergeDocuments(String sourceDocId, String targetDocId) {
        YDoc sourceDoc = documents.get(sourceDocId);
        YDoc targetDoc = documents.get(targetDocId);

        if (sourceDoc == null) {
            throw new IllegalArgumentException("Source document not found: " + sourceDocId);
        }

        if (targetDoc == null) {
            throw new IllegalArgumentException("Target document not found: " + targetDocId);
        }

        targetDoc.merge(sourceDoc);
    }

    /**
     * 获取所有文档ID
     *
     * @return 文档ID集合
     */
    public Iterable<String> getAllDocumentIds() {
        return documents.keySet();
    }

    /**
     * 获取文档数量
     *
     * @return 文档数量
     */
    public int getDocumentCount() {
        return documents.size();
    }

    /**
     * 保存文档状态（示例实现）
     * 在实际应用中，应该持久化到数据库
     *
     * @param docId 文档ID
     * @return 是否保存成功
     */
    public boolean saveDocument(String docId) {
        YDoc doc = documents.get(docId);
        if (doc == null) {
            return false;
        }

        // 这里应该实现文档的持久化逻辑
        // 例如：保存到数据库、文件系统等
        System.out.println("Saving document: " + docId);
        System.out.println("Document state: " + doc.getState());

        return true;
    }

    /**
     * 从保存的状态加载文档
     * 在实际应用中，应该从数据库加载
     *
     * @param docId 文档ID
     * @return 加载的文档实例
     */
    public YDoc loadDocument(String docId) {
        // 这里应该实现从持久化存储加载文档的逻辑
        // 例如：从数据库、文件系统等加载
        System.out.println("Loading document: " + docId);

        // 示例实现：如果文档不存在，则创建一个新的
        return documents.computeIfAbsent(docId, k -> new YDoc());
    }

}