# Yjs-Java 使用指南

## 启动项目

```bash
mvn spring-boot:run
```

或者

```bash
mvn clean package
java -jar target/yjs-java-1.0-SNAPSHOT.jar
```

项目启动后将在 `http://localhost:8080` 上运行。

## WebSocket 连接说明

### 连接端点

WebSocket端点: `ws://localhost:8080/yjs-websocket`
使用STOMP协议进行消息传递。

### 连接流程

1. 客户端通过SockJS连接到 `/yjs-websocket`
2. 建立STOMP连接
3. 发送连接消息到 `/app/connect`
4. 订阅相关主题接收消息

### 消息主题

- `/app/connect` - 发送连接消息
- `/app/disconnect` - 发送断开连接消息
- `/app/operation` - 发送CRDT操作
- `/topic/operations` - 接收广播的操作
- `/topic/connections` - 接收连接状态变化
- `/user/queue/document` - 接收用户特定的文档状态

## 前端集成示例

### 建立WebSocket连接

```javascript
// 引入必要的库
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

// 创建连接
const socket = new SockJS('/yjs-websocket');
const stompClient = Stomp.over(socket);

// 连接并订阅消息
stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
    
    // 订阅操作广播
    stompClient.subscribe('/topic/operations', function (message) {
        const operation = JSON.parse(message.body);
        // 应用接收到的操作到本地文档
        applyOperation(operation);
    });
    
    // 订阅连接状态变化
    stompClient.subscribe('/topic/connections', function (message) {
        const connectionInfo = JSON.parse(message.body);
        // 处理连接状态变化
        handleConnectionChange(connectionInfo);
    });
    
    // 订阅用户特定消息
    stompClient.subscribe('/user/queue/document', function (message) {
        const documentState = JSON.parse(message.body);
        // 更新本地文档状态
        updateDocumentState(documentState);
    });
});
```

### 发送连接消息

```javascript
// 发送连接消息
const connectMessage = {
    clientId: 'unique-client-id',
    docId: 'document-id'
};
stompClient.send("/app/connect", {}, JSON.stringify(connectMessage));
```

### 发送CRDT操作

```javascript
// 发送操作示例
const operation = {
    operationId: 'unique-operation-id',
    operationType: 'INSERT',
    targetId: 'target-crdt-id',
    timestamp: Date.now(),
    data: {
        index: 0,
        text: 'Hello World'
    }
};
stompClient.send("/app/operation", {}, JSON.stringify(operation));
```

### 断开连接

```javascript
// 发送断开连接消息
const disconnectMessage = {
    clientId: 'unique-client-id',
    docId: 'document-id'
};
stompClient.send("/app/disconnect", {}, JSON.stringify(disconnectMessage));

// 关闭连接
stompClient.disconnect();
```

## REST API 接口

### 文档管理

- `POST /api/crdt/documents` - 创建新文档
- `GET /api/crdt/documents/{docId}` - 获取文档状态
- `DELETE /api/crdt/documents/{docId}` - 删除文档

### 共享类型管理

- `POST /api/crdt/documents/{docId}/arrays` - 创建YArray
- `POST /api/crdt/documents/{docId}/maps` - 创建YMap
- `POST /api/crdt/documents/{docId}/texts` - 创建YText
- `GET /api/crdt/documents/{docId}/shared-types/{name}` - 获取共享类型状态

### 操作应用

- `POST /api/crdt/documents/{docId}/shared-types/{name}/operations` - 应用操作到共享类型

### 文档合并

- `POST /api/crdt/merge` - 合并两个文档

## 协同编辑工作流程

1. 客户端连接到WebSocket服务器
2. 客户端发送连接消息，包含客户端ID和文档ID
3. 服务器返回当前文档状态
4. 客户端在本地进行编辑操作
5. 客户端将操作发送到服务器
6. 服务器广播操作给所有连接的客户端
7. 其他客户端应用接收到的操作
8. 所有客户端保持同步

## 注意事项

1. 确保客户端ID在所有连接中是唯一的
2. 操作ID也应该是全局唯一的
3. 在断开连接前发送断开连接消息
4. 处理网络异常和重连逻辑
5. 根据需要实现冲突解决机制