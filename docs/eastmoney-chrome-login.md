# 东方财富证券 Chrome 登录工具

`EastMoneyChromeLogin` 使用 Playwright 启动本机 Google Chrome，完成以下流程：

1. 访问 `https://jywg.18.cn/Login`。
2. 等待系统维护/公告弹窗；出现时输出提示并点击“知道了”，未出现则继续。
3. 从配置读取资金账号和密码。
4. 刷新当前登录会话的图形验证码，将响应图片转换为 PNG 并覆盖保存到项目根目录 `tdx_zip/验证码.png`。
5. 提示用户查看本地验证码图片，并在终端手动输入。
6. 选择在线时间“3 小时”，点击登录。
7. 对“您输入的信息有误，请重新输入!”最多尝试 3 次，每次重新下载并覆盖验证码图片。
8. 其他登录错误立即抛出异常。
9. 登录成功后访问信用买入页，确认持仓接口返回成功。
10. 自动关闭本次工具启动的 Chrome 实例。

## 配置

项目沿用现有配置项：

```yaml
eastmoney:
  username: "资金账号"
  password: "交易密码"
```

本机运行时也可以使用环境变量覆盖配置：

```bash
export EM_ACCOUNT="资金账号"
export EM_PASSWORD="交易密码"
```

启动：

```bash
mvn exec:java \
  -Dexec.mainClass=com.bebopze.tdx.quant.automation.EastMoneyChromeLogin
```

每次登录尝试都会刷新验证码，将与当前浏览器会话匹配的图片保存到：

```text
项目根目录/tdx_zip/验证码.png
```

发生重试时会覆盖该文件，并要求在终端重新输入新的四位数字验证码。

## 安全边界

- 工具不调用大模型或 OCR 服务识别验证码。
- 工具不输出、返回或持久化 Cookie、validatekey、Token、Session_Id。
- 持仓接口只在当前浏览器会话内验证 HTTP 状态、validatekey 和 Cookie 是否存在，不输出持仓数据或认证值。
- 密码、Cookie、validatekey、Token 等敏感配置不会由 `PropsUtil` 写入日志。
- 请只在本人设备和本人账户上使用，并遵守证券公司的服务条款和风控要求。
