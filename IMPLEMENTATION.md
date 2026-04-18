# Heos 混合认证系统实现文档

## 系统概述

Heos实现了一个混合认证系统，允许正版和离线玩家同时在开启正版验证的服务器上游戏。

## 核心特性

### 1. 玩家类型识别

- **正版玩家**：通过Mojang官方验证，无需额外登录
- **离线玩家**：用户名包含特殊字符（如`+`、`-`、`.`）或无法通过Mojang验证的玩家

### 2. 认证流程

#### 正版玩家
1. 客户端连接服务器
2. ServerLoginNetworkHandlerMixin检查用户名格式
3. 查询Mojang API验证UUID
4. UUID匹配 → 继续正常验证流程
5. 加入服务器后自动认证，无需登录

#### 离线玩家
1. 客户端连接服务器
2. 检测到用户名包含特殊字符或UUID不匹配
3. 跳过Mojang验证，使用离线UUID
4. 加入服务器后需要登录/注册
5. 未认证期间无法进行任何操作

### 3. 操作限制

未认证的离线玩家被限制：
- ❌ 移动（会被传送回原位）
- ❌ 聊天
- ❌ 破坏方块
- ❌ 使用物品
- ❌ 与实体交互
- ❌ 攻击实体
- ❌ 执行命令（除了/login和/register）
- ✅ 保持连接（心跳包等）

## 技术实现

### 核心类

#### 1. ServerLoginNetworkHandlerMixin
```java
// 拦截登录握手，判断玩家类型
@Inject(method = "onHello", ...)
private void checkPremiumAccount(LoginHelloC2SPacket packet, CallbackInfo ci)
```

**关键逻辑**：
- 检查用户名格式（`^[a-zA-Z0-9_]{3,16}$`）
- 查询Mojang API获取正版UUID
- UUID匹配 → 正版玩家，继续vanilla验证
- UUID不匹配或无账号 → 离线玩家，使用离线UUID

#### 2. PlayerAuth接口
扩展ServerPlayerEntity，添加认证状态：
- `isAuthenticated()` - 是否已认证
- `canSkipAuth()` - 是否可跳过认证
- `isUsingMojangAccount()` - 是否正版账号

#### 3. AuthEventHandler
拦截所有玩家操作，未认证玩家返回FAIL

#### 4. ServerPlayNetworkHandlerMixin
拦截网络数据包，只允许必要的包通过

### 命令系统

#### /register <密码> <确认密码>
- 离线玩家首次注册
- 密码长度：4-32字符
- 注册后自动登录

#### /login <密码>
- 离线玩家登录
- 验证密码后设置认证状态

## 项目结构

```
src/main/java/heos/
├── Heos.java                    # 主类，玩家数据管理
├── HeosFabric.java              # Fabric初始化器
├── commands/
│   ├── LoginCommand.java        # 登录命令
│   └── RegisterCommand.java     # 注册命令
├── event/
│   └── AuthEventHandler.java   # 事件处理器
├── integrations/
│   └── MojangApi.java          # Mojang API集成
├── interfaces/
│   └── PlayerAuth.java         # 玩家认证接口
├── mixin/
│   ├── ServerLoginNetworkHandlerMixin.java  # 登录拦截
│   ├── ServerPlayerEntityMixin.java         # 玩家扩展
│   ├── PlayerManagerMixin.java              # 加入/离开处理
│   └── ServerPlayNetworkHandlerMixin.java   # 数据包拦截
├── storage/
│   └── PlayerData.java         # 玩家数据
└── utils/
    └── HeosLogger.java         # 日志工具
```

## 使用方法

### 服务器配置
1. 在`server.properties`中设置：
   ```properties
   online-mode=true
   ```

2. 启动服务器

### 玩家连接

#### 正版玩家
- 直接使用正版客户端连接
- 自动通过验证，无需额外操作

#### 离线玩家
- 使用包含特殊字符的用户名（如`Player+123`、`Test.User`）
- 首次连接：`/register <密码> <确认密码>`
- 后续连接：`/login <密码>`

## 安全特性

1. **UUID验证**：防止离线玩家冒充正版玩家
2. **操作限制**：未认证玩家完全无法操作
3. **密码保护**：离线玩家账号受密码保护
4. **IP记录**：记录玩家IP用于会话管理

## 待实现功能

- [ ] 密码哈希加密（当前明文存储）
- [ ] 数据库持久化（当前内存存储）
- [ ] 会话超时自动登出
- [ ] 登录尝试次数限制
- [ ] 坐标隐藏功能
- [ ] 配置文件系统

## 参考

本实现参考了EasyAuth的核心思路：
- 登录握手阶段的玩家类型判断
- Mojang API查询机制
- 操作拦截系统

## 许可证

MIT License



