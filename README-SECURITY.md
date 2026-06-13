# 安全提示 🔒

## ⚠️ 上传前必读

本项目在上传到 GitHub 前已进行隐私数据清理，但请注意：

### 已排除的敏感文件

以下文件已在 `.gitignore` 中排除，**切勿提交**：

```
back_end/src/main/resources/application.properties
```

该文件包含：
- 数据库密码
- JWT 密钥
- 邮箱 SMTP 授权码
- 其他私密配置

### 如何配置本地环境

1. 复制配置模板：
```bash
cd back_end/src/main/resources
cp application.properties.example application.properties
```

2. 编辑 `application.properties`，填入你的实际配置

3. 详细配置说明请参考 [SETUP.md](./SETUP.md)

### 贡献者注意事项

- 提交代码前，务必检查 `git status`，确保没有敏感文件
- 不要使用 `git add .`，请明确指定要添加的文件
- 如果不小心提交了敏感信息，请立即使用 `git filter-branch` 或 `BFG Repo-Cleaner` 清理历史记录

### 发现安全问题？

如果发现代码中有遗漏的敏感信息，请立即联系项目维护者。
