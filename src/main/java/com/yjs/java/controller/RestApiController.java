package com.yjs.java.controller;

import com.yjs.java.crdt.CRDT;
import com.yjs.java.crdt.operation.CRDTOperation;
import com.yjs.java.crdt.operation.BaseCRDTOperation;
import com.yjs.java.crdt.types.YArray;
import com.yjs.java.crdt.types.YMap;
import com.yjs.java.crdt.types.YText;
import com.yjs.java.service.YDocService;
import com.yjs.java.ydoc.YDoc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

/**
 * REST API控制器，提供HTTP接口访问YDoc服务功能
 */
@RestController
@RequestMapping("/api/crdt")
public class RestApiController {

    private final YDocService yDocService;

    @Autowired
    public RestApiController(YDocService yDocService) {
        this.yDocService = yDocService;
    }

    /**
     * 创建新文档
     * @return 新文档ID
     */
    @PostMapping("/documents")
    public ResponseEntity<Map<String, String>> createDocument() {
        String docId = yDocService.createDocument();
        return ResponseEntity.ok(Map.of("docId", docId));
    }

    /**
     * 获取文档状态
     * @param docId 文档ID
     * @return 文档状态
     */
    @GetMapping("/documents/{docId}")
    public ResponseEntity<?> getDocumentState(@PathVariable String docId) {
        YDoc doc = yDocService.getDocument(docId);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc.getState());
    }

    /**
     * 删除文档
     * @param docId 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<Map<String, Boolean>> deleteDocument(@PathVariable String docId) {
        boolean deleted = yDocService.deleteDocument(docId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    /**
     * 在文档中创建YArray
     * @param docId 文档ID
     * @param name 数组名称
     * @return 创建的YArray信息
     */
    @PostMapping("/documents/{docId}/arrays")
    public ResponseEntity<Map<String, String>> createYArray(@PathVariable String docId, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.isEmpty()) {
            name = "array_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        try {
            YArray array = yDocService.createYArray(docId, name);
            return ResponseEntity.ok(Map.of(
                    "name", name,
                    "id", array.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 在文档中创建YMap
     * @param docId 文档ID
     * @param name 映射名称
     * @return 创建的YMap信息
     */
    @PostMapping("/documents/{docId}/maps")
    public ResponseEntity<Map<String, String>> createYMap(@PathVariable String docId, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.isEmpty()) {
            name = "map_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        try {
            YMap map = yDocService.createYMap(docId, name);
            return ResponseEntity.ok(Map.of(
                    "name", name,
                    "id", map.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 在文档中创建YText
     * @param docId 文档ID
     * @param name 文本名称
     * @return 创建的YText信息
     */
    @PostMapping("/documents/{docId}/texts")
    public ResponseEntity<Map<String, String>> createYText(@PathVariable String docId, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.isEmpty()) {
            name = "text_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        try {
            YText text = yDocService.createYText(docId, name);
            return ResponseEntity.ok(Map.of(
                    "name", name,
                    "id", text.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取共享类型的状态
     * @param docId 文档ID
     * @param name 共享类型名称
     * @return 共享类型状态
     */
    @GetMapping("/documents/{docId}/shared-types/{name}")
    public ResponseEntity<?> getSharedTypeState(@PathVariable String docId, @PathVariable String name) {
        try {
            CRDT sharedType = yDocService.getSharedType(docId, name);
            return ResponseEntity.ok(sharedType.getState());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 应用操作到共享类型
     * @param docId 文档ID
     * @param name 共享类型名称
     * @param operation 操作
     * @return 操作结果
     */
    @PostMapping("/documents/{docId}/shared-types/{name}/operations")
    public ResponseEntity<Map<String, String>> applyOperation(
            @PathVariable String docId,
            @PathVariable String name,
            @RequestBody CRDTOperation operation) {
        
        try {
            // 确保操作的目标ID是共享类型的ID
            CRDT sharedType = yDocService.getSharedType(docId, name);
            if (operation instanceof BaseCRDTOperation) {
                ((BaseCRDTOperation) operation).setTargetId(sharedType.getId());
            }
            
            // 应用操作
            yDocService.applyOperation(docId, operation);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "operationId", operation.getOperationId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 合并两个文档
     * @param mergeRequest 合并请求
     * @return 合并结果
     */
    @PostMapping("/merge")
    public ResponseEntity<Map<String, String>> mergeDocuments(@RequestBody Map<String, String> mergeRequest) {
        String sourceDocId = mergeRequest.get("sourceDocId");
        String targetDocId = mergeRequest.get("targetDocId");
        
        if (sourceDocId == null || targetDocId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "sourceDocId and targetDocId are required"));
        }
        
        try {
            yDocService.mergeDocuments(sourceDocId, targetDocId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "targetDocId", targetDocId
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有文档ID
     * @return 文档ID列表
     */
    @GetMapping("/documents")
    public ResponseEntity<Iterable<String>> getAllDocuments() {
        return ResponseEntity.ok(yDocService.getAllDocumentIds());
    }

    /**
     * 保存文档
     * @param docId 文档ID
     * @return 保存结果
     */
    @PostMapping("/documents/{docId}/save")
    public ResponseEntity<Map<String, Boolean>> saveDocument(@PathVariable String docId) {
        boolean saved = yDocService.saveDocument(docId);
        if (!saved) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("saved", true));
    }

    /**
     * 加载文档
     * @param docId 文档ID
     * @return 加载的文档状态
     */
    @PostMapping("/documents/{docId}/load")
    public ResponseEntity<?> loadDocument(@PathVariable String docId) {
        YDoc doc = yDocService.loadDocument(docId);
        return ResponseEntity.ok(doc.getState());
    }

}