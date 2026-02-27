# Heos 服务器启动 JVM 参数配置

为了消除警告信息，请在服务器启动脚本中添加以下 JVM 参数：

## 需要添加的 JVM 参数

```
--add-opens java.base/sun.misc=ALL-UNNAMED
--add-opens java.base/java.lang=ALL-UNNAMED
```

## 如何添加

### 方法 1: 修改启动脚本
在你的 `Front-loaded.bat` 或启动脚本中，找到 Java 启动命令，添加这些参数。

例如，如果你的启动命令是：
```batch
java -Xmx4G -Xms4G -jar server.jar nogui
```

改为：
```batch
java -Xmx4G -Xms4G --add-opens java.base/sun.misc=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar server.jar nogui
```

### 方法 2: 使用 JVM 参数文件
创建一个文件 `jvm_args.txt`，内容为：
```
--add-opens
java.base/sun.misc=ALL-UNNAMED
--add-opens
java.base/java.lang=ALL-UNNAMED
```

然后在启动命令中使用：
```batch
java -Xmx4G -Xms4G @jvm_args.txt -jar server.jar nogui
```

## 警告说明

1. **Reference map 警告** - 这是开发环境的正常警告，生产环境可以忽略
2. **Package scanning 警告** - 已通过移除 `packages` 属性修复
3. **Log4j 插件警告** - 已通过添加注解处理器修复
4. **Unsafe 警告** - 需要添加上述 JVM 参数来消除

重新构建项目后，这些警告应该会减少或消失。




