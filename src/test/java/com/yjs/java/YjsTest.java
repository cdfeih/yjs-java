package com.yjs.java;

import com.yjs.java.crdt.operation.BaseCRDTOperation;
import com.yjs.java.crdt.operation.CRDTOperation;
import com.yjs.java.crdt.types.YArray;
import com.yjs.java.crdt.types.YMap;
import com.yjs.java.crdt.types.YText;
import com.yjs.java.ydoc.YDoc;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * YJS功能测试类
 */
class YjsTest {

    @Test
    void testYDocBasicFunctionality() {
        // 创建文档
        YDoc doc = new YDoc();
        assertNotNull(doc.getId());
        assertEquals(0, doc.getVersion());
        
        // 注册共享类型
        YArray array = new YArray();
        YMap map = new YMap();
        YText text = new YText();
        
        doc.register("array", array);
        doc.register("map", map);
        doc.register("text", text);
        
        // 检查共享类型是否注册成功
        assertEquals(array, doc.get("array"));
        assertEquals(map, doc.get("map"));
        assertEquals(text, doc.get("text"));
        
        // 检查版本号是否增加
        assertTrue(doc.getVersion() > 0);
    }

    @Test
    void testYArrayFunctionality() {
        YArray array = new YArray();
        
        // 测试添加元素
        assertEquals(1, array.add("item1"));
        assertEquals(2, array.add("item2"));
        assertEquals(3, array.add("item3"));
        
        // 测试获取元素
        assertEquals("item1", array.get(0));
        assertEquals("item2", array.get(1));
        assertEquals("item3", array.get(2));
        
        // 测试插入元素
        array.insert(1, "inserted");
        assertEquals(4, array.size());
        assertEquals("inserted", array.get(1));
        assertEquals("item2", array.get(2));
        
        // 测试删除元素
        assertEquals("inserted", array.remove(1));
        assertEquals(3, array.size());
        assertEquals("item2", array.get(1));
        
        // 测试更新元素
        assertEquals("item2", array.set(1, "updated"));
        assertEquals("updated", array.get(1));
        
        // 测试清除数组
        array.clear();
        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
    }

    @Test
    void testYMapFunctionality() {
        YMap map = new YMap();
        
        // 测试设置键值对
        assertNull(map.set("key1", "value1"));
        assertNull(map.set("key2", 42));
        assertNull(map.set("key3", true));
        
        // 测试获取值
        assertEquals("value1", map.get("key1"));
        assertEquals(42, map.get("key2"));
        assertEquals(true, map.get("key3"));
        
        // 测试包含键
        assertTrue(map.containsKey("key1"));
        assertFalse(map.containsKey("nonexistent"));
        
        // 测试更新值
        assertEquals("value1", map.set("key1", "updated"));
        assertEquals("updated", map.get("key1"));
        
        // 测试删除键值对
        assertEquals(42, map.remove("key2"));
        assertFalse(map.containsKey("key2"));
        
        // 测试获取大小
        assertEquals(2, map.size());
        
        // 测试键集合和值集合
        assertTrue(map.keySet().contains("key1"));
        assertTrue(map.keySet().contains("key3"));
        assertTrue(map.values().contains("updated"));
        assertTrue(map.values().contains(true));
        
        // 测试清除映射
        map.clear();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    void testYTextFunctionality() {
        YText text = new YText();
        
        // 测试追加文本
        assertEquals(5, text.append("Hello"));
        assertEquals(6, text.append(" "));
        assertEquals(11, text.append("World!"));
        
        // 测试字符串表示
        assertEquals("Hello World!", text.toString());
        assertEquals(11, text.length());
        
        // 测试插入文本
        text.insert(6, "beautiful ");
        assertEquals("Hello beautiful World!", text.toString());
        assertEquals(20, text.length());
        
        // 测试删除文本
        String deleted = text.delete(6, 16);
        assertEquals("beautiful ", deleted);
        assertEquals("Hello World!", text.toString());
        assertEquals(11, text.length());
        
        // 测试清除文本
        text.clear();
        assertEquals("", text.toString());
        assertEquals(0, text.length());
        assertTrue(text.isEmpty());
    }

    @Test
    void testCRDTMerge() {
        // 创建两个YArray实例
        YArray array1 = new YArray();
        YArray array2 = new YArray();
        
        // 向第一个数组添加元素
        array1.add("item1");
        array1.add("item2");
        
        // 向第二个数组添加元素
        array2.add("item3");
        array2.add("item4");
        
        // 合并两个数组
        array1.merge(array2);
        
        // 检查合并结果
        assertEquals(4, array1.size());
        assertTrue(array1.getState().toString().contains("item1"));
        assertTrue(array1.getState().toString().contains("item2"));
        assertTrue(array1.getState().toString().contains("item3"));
        assertTrue(array1.getState().toString().contains("item4"));
    }

    @Test
    void testYDocMerge() {
        // 创建两个文档
        YDoc doc1 = new YDoc();
        YDoc doc2 = new YDoc();
        
        // 在第一个文档中添加共享类型
        YArray array1 = new YArray();
        array1.add("array1-item1");
        doc1.register("array", array1);
        
        YMap map1 = new YMap();
        map1.set("key1", "value1");
        doc1.register("map", map1);
        
        // 在第二个文档中添加共享类型
        YArray array2 = new YArray();
        array2.add("array2-item1");
        doc2.register("array", array2);
        
        YText text2 = new YText();
        text2.append("text2-content");
        doc2.register("text", text2);
        
        // 合并文档
        doc1.merge(doc2);
        
        // 检查合并结果
        assertTrue(doc1.getState().containsKey("array"));
        assertTrue(doc1.getState().containsKey("map"));
        assertTrue(doc1.getState().containsKey("text"));
        
        // 检查文本内容是否正确合并
        YText mergedText = (YText) doc1.get("text");
        assertEquals("text2-content", mergedText.toString());
    }

    @Test
    void testApplyOperation() {
        YText text = new YText();
        
        // 创建插入操作
        Map<String, Object> insertData = new HashMap<>();
        insertData.put("index", 0);
        insertData.put("text", "Test content");
        CRDTOperation insertOp = new BaseCRDTOperation(CRDTOperation.OperationType.INSERT, text.getId(), insertData);
        
        // 应用操作
        text.applyOperation(insertOp);
        
        // 检查结果
        assertEquals("Test content", text.toString());
        
        // 创建删除操作
        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("start", 0);
        deleteData.put("end", 5);
        CRDTOperation deleteOp = new BaseCRDTOperation(CRDTOperation.OperationType.DELETE, text.getId(), deleteData);
        
        // 应用操作
        text.applyOperation(deleteOp);
        
        // 检查结果
        assertEquals(" content", text.toString());
    }

}