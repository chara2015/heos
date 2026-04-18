# Heos 更新日志 - 增强保护功能

## 新增功能

### 1. 玩家保护系统

#### 无敌模式
- ✅ 未认证玩家完全无敌，不会受到任何伤害
- ✅ 不会被怪物、玩家、环境伤害
- ✅ 不会被饥饿、摔落、火焰等伤害

#### 隐身模式
- ✅ 未认证玩家对怪物隐身
- ✅ 怪物不会锁定未认证玩家
- ✅ 不会被僵尸、骷髅、苦力怕等攻击

### 2. 传送门救援系统

#### 下界传送门保护
- ✅ 检测玩家是否在下界传送门中
- ✅ 自动将玩家传送出传送门
- ✅ 在客户端伪装传送门方块为空气
- ✅ 防止玩家被困在传送门中无法登录

**实现原理**：
```java
// 每tick检查玩家位置
if (player in nether portal) {
    // 传送玩家出传送门
    teleport(player, pos + 0.5);
    
    // 发送假的空气方块给客户端
    sendFakeBlock(player, pos, AIR);
    sendFakeBlock(player, pos.up(), AIR);
}
```

### 3. 友好的错误提示

#### 场景1：离线玩家使用纯字母用户名
当离线玩家使用如 `Steve` 这样的用户名时，会看到：

```
§c无效会话

§e离线玩家请在用户名中添加以下符号之一：
§a+ - .

§7例如：Player+123, Test.User, User-Name
§7否则您将走正版验证流程

§cInvalid Session

§eOffline players please add one of these symbols to your username:
§a+ - .

§7Example: Player+123, Test.User, User-Name
§7Otherwise you will go through premium authentication
```

#### 场景2：离线玩家使用正版玩家的用户名
当离线玩家尝试使用已被正版玩家占用的用户名时：

```
§c无效会话

§e此用户名已被正版玩家使用
§e离线玩家请在用户名中添加以下符号之一：
§a+ - .

§7例如：Steve+123, Steve.User

§cInvalid Session

§eThis username is taken by a premium player
§eOffline players please add one of these symbols to your username:
§a+ - .

§7Example: Steve+123, Steve.User
```

### 4. 登录超时系统

- ⏱️ 玩家有60秒时间完成登录
- 📢 每10秒提醒一次登录/注册
- ⏰ 超时后自动踢出服务器
- 🔄 登录成功后重置计时器

### 5. 玩家重生保护

- ✅ 玩家死亡重生后保持认证状态
- ✅ 不需要重新登录
- ✅ 所有认证数据自动复制到新实体

## 技术实现

### EntityMixin
```java
@Mixin(Entity.class)
public class EntityMixin {
    // 使未认证玩家隐身
    @ModifyReturnValue(method = "isInvisible()Z")
    public boolean heos$isInvisible(boolean original) {
        return original || !authenticated;
    }
    
    // 使未认证玩家无敌
    @ModifyReturnValue(method = "isInvulnerable()Z")
    public boolean heos$isInvulnerable(boolean original) {
        return original || !authenticated;
    }
}
```

### ServerPlayerEntityMixin 增强
```java
@Inject(method = "playerTick()V")
private void onPlayerTick(CallbackInfo ci) {
    if (!authenticated) {
        // 1. 踢出计时器
        if (kickTimer <= 0) {
            disconnect("登录超时");
        }
        
        // 2. 传送门救援
        if (inNetherPortal()) {
            rescueFromPortal();
        }
        
        // 3. 定期提醒
        if (kickTimer % 200 == 0) {
            sendAuthMessage();
        }
        
        ci.cancel(); // 停止正常tick
    }
}
```

### ServerLoginNetworkHandlerMixin 增强
- 添加了详细的中英双语错误提示
- 区分不同的失败场景
- 提供具体的解决方案

## 使用示例

### 正版玩家
```
用户名: Steve
结果: ✅ 自动通过验证，无需登录
```

### 离线玩家（正确）
```
用户名: Steve+123
结果: ✅ 进入服务器，需要登录/注册
提示: 请使用 /register 或 /login
```

### 离线玩家（错误）
```
用户名: Steve
结果: ❌ 被踢出
提示: 请在用户名中添加 + - . 符号
```

### 传送门场景
```
1. 离线玩家在下界传送门中下线
2. 重新连接服务器
3. 系统检测到玩家在传送门中
4. 自动传送玩家出传送门
5. 客户端看到传送门变成空气
6. 玩家可以正常登录
```

## 安全特性总结

| 功能 | 状态 | 说明 |
|------|------|------|
| 无敌保护 | ✅ | 未认证玩家不受任何伤害 |
| 隐身保护 | ✅ | 怪物无法锁定未认证玩家 |
| 传送门救援 | ✅ | 自动救援被困玩家 |
| 操作限制 | ✅ | 无法移动、聊天、破坏等 |
| 登录超时 | ✅ | 60秒超时自动踢出 |
| 重生保护 | ✅ | 死亡重生保持认证 |
| 友好提示 | ✅ | 中英双语错误提示 |

## 依赖更新

新增依赖：
```gradle
implementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
```

用于支持 `@ModifyReturnValue` 等高级Mixin功能。

## 测试建议

### 测试1：无敌测试
1. 离线玩家连接但不登录
2. 尝试跳下悬崖
3. 结果：不受伤害

### 测试2：隐身测试
1. 离线玩家连接但不登录
2. 走到僵尸旁边
3. 结果：僵尸不攻击

### 测试3：传送门测试
1. 离线玩家进入下界传送门
2. 在传送门中下线
3. 重新连接
4. 结果：自动传送出传送门

### 测试4：错误提示测试
1. 使用纯字母用户名（如Steve）
2. 尝试连接
3. 结果：看到友好的中英双语提示

## 已知限制

- 密码仍为明文存储（待实现加密）
- 数据存储在内存中（待实现持久化）
- 无会话管理（待实现记住登录）

## 下一步计划

- [ ] 实现密码哈希加密
- [ ] 添加数据库持久化
- [ ] 实现会话管理
- [ ] 添加坐标隐藏功能
- [ ] 实现配置文件系统



