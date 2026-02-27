# Heos - 混合认证系统

一个支持正版和离线玩家共存的 Minecraft Fabric 服务端认证模组。

## 特性

### 🔐 混合认证
- ✅ 正版玩家自动通过Mojang验证，无需额外登录
- ✅ 离线玩家使用密码登录/注册系统
- ✅ 智能识别玩家类型（基于用户名格式和UUID验证）

### 🛡️ 完善的保护系统
- ✅ 未认证玩家完全无敌（不受任何伤害）
- ✅ 未认证玩家对怪物隐身（不会被攻击）
- ✅ 传送门救援（防止玩家被困在下界传送门中）
- ✅ 完全的操作限制（移动、聊天、破坏方块等）
- ✅ 登录超时保护（60秒）
- ✅ 死亡重生保持认证状态

### 💬 友好的用户体验
- ✅ 中英双语错误提示
- ✅ 清晰的用户名规则说明
- ✅ 定期登录提醒
- ✅ 详细的操作反馈

## 快速开始

### 服务器配置

1. 在 `server.properties` 中启用正版验证：
```properties
online-mode=true
```

2. 将mod放入 `mods` 文件夹

3. 启动服务器

### 玩家使用

#### 正版玩家
直接使用正版客户端连接，自动通过验证。

#### 离线玩家
用户名必须包含特殊符号（`+` `-` `.`），例如：
- `Player+123`
- `Test.User`
- `User-Name`

首次连接：
```
/register <密码> <确认密码>
```

后续连接：
```
/login <密码>
```

## 技术细节

### 核心实现

- **ServerLoginNetworkHandlerMixin** - 拦截登录握手，判断玩家类型
- **ServerPlayerEntityMixin** - 实现玩家保护和认证状态管理
- **EntityMixin** - 提供无敌和隐身保护
- **AuthEventHandler** - 拦截未认证玩家的所有操作

### 依赖

- Minecraft 1.20+
- Fabric Loader >= 0.15.0
- Fabric API
- Mixin Extras 0.4.1

## 构建

```bash
# 清理缓存
./gradlew clean

# 构建项目
./gradlew build
```

构建产物位于：`versions/1.21.2/build/libs/`

## 文档

- [快速开始指南](QUICKSTART.md)
- [实现文档](IMPLEMENTATION.md)
- [功能特性表](FEATURES.md)
- [更新日志](CHANGELOG.md)

## 安全说明

⚠️ **当前版本仅用于测试**

- 密码以明文存储在内存中
- 数据不持久化（服务器重启后丢失）
- 无登录尝试次数限制

生产环境使用前需要实现：
- 密码哈希加密（BCrypt/Argon2）
- 数据库持久化（SQLite/MySQL）
- 会话管理
- 登录尝试限制

## 许可证

MIT License

## 致谢

本项目参考了 EasyAuth 的核心思路，特此感谢。
