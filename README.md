# Heos Mod

一个轻量级的 Minecraft Fabric 服务端模组框架。

## 当前状态

项目已完成重构，移除了所有认证相关代码，保留了基础框架。

### 项目结构

```
src/main/java/heos/
├── Heos.java           # 主类
├── HeosFabric.java     # Fabric 初始化器
└── utils/
    └── HeosLogger.java # 简化的日志系统
```

### 特性

- ✅ 简洁的日志系统（基于 SLF4J）
- ✅ 服务器生命周期事件监听
- ✅ 多版本支持（通过 Stonecutter）
- ✅ 最小化依赖

### 构建

确保使用 Java 17 或更高版本：

```bash
./gradlew build
```

### 开发

项目使用 Stonecutter 进行多版本管理。当前活动版本：1.21.11

修改源代码后，需要清理构建缓存：

```bash
./gradlew clean
```

## 依赖

- Fabric API
- Fabric Loader >= 0.15.0
- Minecraft (多版本支持)

## 许可证

MIT License

