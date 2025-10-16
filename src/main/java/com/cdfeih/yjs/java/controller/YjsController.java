package com.cdfeih.yjs.java.controller;

import com.yjs.java.crdt.operation.CRDTOperation;
import com.yjs.java.ydoc.YDoc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket控制器，处理客户端连接和CRDT操作同步
 */
@Controller
@RequestMapping("/api/yjs")
public class YjsController {

    // 存储所有文档的映射
    private final Map<String, YDoc> documents = new ConcurrentHashMap<>();

    // 用于向特定用户或主题发送消息
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public YjsController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 获取或创建文档
     *
     * @param docId 文档ID
     * @return 文档状态
     */
    @GetMapping("/document/{docId}")
    @ResponseBody
    public Map<String, Object> getDocument(@PathVariable String docId) {
        YDoc doc = documents.computeIfAbsent(docId, k -> new YDoc());
        return doc.getState();
    }

    /**
     * 处理客户端发送的CRDT操作
     *
     * @param operation 操作对象
     */
    @MessageMapping("/operation")
    @SendTo("/topic/operations")
    public CRDTOperation handleOperation(@Payload CRDTOperation operation) {
        // 应用操作到相应的文档
        String docId = operation.getTargetId();
        YDoc doc = documents.get(docId);
        if (doc != null) {
            doc.applyOperation(operation);
        }

        // 广播操作给所有其他客户端
        return operation;
    }

    /**
     * 处理客户端连接
     *
     * @param clientInfo 客户端信息
     */
    @MessageMapping("/connect")
    public void handleConnect(@Payload Map<String, String> clientInfo) {
        String clientId = clientInfo.get("clientId");
        String docId = clientInfo.get("docId");

        if (clientId != null && docId != null) {
            YDoc doc = documents.computeIfAbsent(docId, k -> new YDoc());
            doc.addClient(clientId);

            // 发送当前文档状态给新连接的客户端
            messagingTemplate.convertAndSendToUser(
                    clientId,
                    "/queue/document",
                    doc.getState()
            );

            // 通知其他客户端有新客户端连接
            messagingTemplate.convertAndSend(
                    "/topic/connections",
                    Map.of("type", "connect", "clientId", clientId, "docId", docId)
            );
        }
    }

    /**
     * 处理客户端断开连接
     *
     * @param clientInfo 客户端信息
     */
    @MessageMapping("/disconnect")
    public void handleDisconnect(@Payload Map<String, String> clientInfo) {
        String clientId = clientInfo.get("clientId");
        String docId = clientInfo.get("docId");

        if (clientId != null && docId != null) {
            YDoc doc = documents.get(docId);
            if (doc != null) {
                doc.removeClient(clientId);

                // 通知其他客户端有客户端断开连接
                messagingTemplate.convertAndSend(
                        "/topic/connections",
                        Map.of("type", "disconnect", "clientId", clientId, "docId", docId)
                );

                // 如果文档没有客户端了，可以选择保存并移除
                if (doc.getClientCount() == 0) {
                    // 这里可以添加保存文档的逻辑
                    // saveDocument(docId, doc.getState());
                    // documents.remove(docId);
                }
            }
        }
    }

    /**
     * 合并两个文档的状态
     *
     * @param mergeRequest 合并请求，包含源文档和目标文档ID
     */
    @PostMapping("/merge")
    @ResponseBody
    public Map<String, Object> mergeDocuments(@RequestBody Map<String, String> mergeRequest) {
        String sourceDocId = mergeRequest.get("sourceDocId");
        String targetDocId = mergeRequest.get("targetDocId");

        if (sourceDocId != null && targetDocId != null) {
            YDoc sourceDoc = documents.get(sourceDocId);
            YDoc targetDoc = documents.get(targetDocId);

            if (sourceDoc != null && targetDoc != null) {
                targetDoc.merge(sourceDoc);

                // 广播合并后的状态给所有连接的客户端
                messagingTemplate.convertAndSend(
                        "/topic/document/" + targetDocId,
                        targetDoc.getState()
                );

                return targetDoc.getState();
            }
        }

        return Map.of("error", "Documents not found");
    }

}