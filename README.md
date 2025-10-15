# YJS-Java: Java实现的YJS (CRDT) 库

YJS-Java是一个使用Java实现的YJS (CRDT) 库，基于Spring Boot 3和JDK 21开发。它提供了分布式数据结构的实现，支持实时协作编辑和数据同步。

## 功能特性

- **分布式数据结构**：实现了YArray（数组）、YMap（键值对）、YText（文本）等CRDT数据类型
- **实时同步**：基于WebSocket的实时数据同步机制
- **冲突解决**：自动解决分布式环境中的数据冲突
- **REST API**：提供HTTP接口访问CRDT功能
- **线程安全**：所有数据结构都支持并发读写操作

## 技术栈

- Java 21
- Spring Boot 3.3.11
- Maven
- WebSocket (STOMP + SockJS)
- Lombok
- JUnit 5

## 项目结构

```
src/main/java/com/yjs/java/
├── config/             # 配置类
│   └── WebSocketConfig.java
├── controller/         # 控制器
│   ├── RestApiController.java
│   └── YjsController.java
├── crdt/               # CRDT核心实现
│   ├── BaseCRDT.java
│   ├── CRDT.java
│   ├── operation/      # CRDT操作
│   │   ├── BaseCRDTOperation.java
│   │   └── CRDTOperation.java
│   └── types/          # 具体CRDT类型
│       ├── YArray.java
│       ├── YMap.java
│       └── YText.java
├── service/            # 服务层
│   └── YDocService.java
├── ydoc/               # YDoc文档实现
│   └── YDoc.java
└── YjsJavaApplication.java  # 应用程序入口
```

## 快速开始

### 前提条件

- JDK 21或更高版本
- Maven 3.8或更高版本

### 构建项目

```bash
mvn clean package
```

### 运行应用程序

```bash
java -jar target/yjs-java-1.0-SNAPSHOT.jar
```

或者使用Maven插件运行：

```bash
mvn spring-boot:run
```

### 使用示例

#### 1. 创建文档和共享类型

```java
// 创建YDoc文档
YDoc doc = new YDoc();

// 创建并注册共享类型
YArray array = new YArray();
YMap map = new YMap();
YText text = new YText();

doc.register("myArray", array);
doc.register("myMap", map);
doc.register("myText", text);
```

#### 2. 操作共享类型

```java
// 操作YArray
array.add("item1");
array.insert(1, "inserted item");
Object item = array.get(0);

// 操作YMap
map.set("key1", "value1");
Object value = map.get("key1");

// 操作YText
text.append("Hello, ");
text.append("World!");
text.insert(7, "beautiful ");
String content = text.toString(); // "Hello, beautiful World!"
```

#### 3. 合并文档

```java
// 创建两个文档
YDoc doc1 = new YDoc();
YDoc doc2 = new YDoc();

// 在两个文档中分别进行操作
// ...

// 合并文档
doc1.merge(doc2);
```

#### 4. 使用REST API

应用程序启动后，可以通过以下API访问：

- 创建文档: `POST /api/crdt/documents`
- 获取文档: `GET /api/crdt/documents/{docId}`
- 创建YArray: `POST /api/crdt/documents/{docId}/arrays`
- 创建YMap: `POST /api/crdt/documents/{docId}/maps`
- 创建YText: `POST /api/crdt/documents/{docId}/texts`
- 获取共享类型: `GET /api/crdt/documents/{docId}/shared-types/{name}`
- 应用操作: `POST /api/crdt/documents/{docId}/shared-types/{name}/operations`
- 合并文档: `POST /api/crdt/merge`

#### 5. 使用WebSocket

客户端可以通过WebSocket连接到服务器，实现实时数据同步：

- WebSocket端点: `/yjs-websocket`
- STOMP前缀: `/app`
- 主题前缀: `/topic`, `/queue`

## WebSocket消息格式

### 发送操作

```json
{
  "operationId": "唯一操作ID",
  "operationType": "INSERT/UPDATE/DELETE/CLEAR/MERGE",
  "targetId": "目标共享类型ID",
  "timestamp": 时间戳,
  "data": 操作数据
}
```

### 连接消息

```json
{
  "clientId": "客户端ID",
  "docId": "文档ID"
}
```

## 测试

运行单元测试：

```bash
mvn test
```

## 注意事项

- 本实现是YJS的Java版本，提供了基本的CRDT功能
- 实际生产环境中，应该考虑数据持久化、安全性等因素
- 对于大规模协作场景，可能需要进一步优化性能和冲突解决算法

## 扩展建议

- 添加数据持久化功能（如存储到数据库）
- 实现更复杂的CRDT算法（如RGA、OR-Set等）
- 添加身份验证和授权机制
- 优化大文档的性能
- 实现离线操作支持

## License

[MIT](LICENSE)