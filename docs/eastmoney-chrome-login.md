# 东方财富证券 Chrome 登录工具

`EastMoneyChromeLogin` 使用 Playwright 启动本机 Google Chrome，完成以下流程：

1. 访问 `https://jywg.18.cn/Login`。
2. 等待系统维护/公告弹窗；出现时输出提示并点击“知道了”，未出现则继续。
3. 从配置读取资金账号和密码。
4. 提示用户查看 Chrome 中的图形验证码，并在终端手动输入。
5. 选择在线时间“3 小时”，点击登录。
6. 对“您输入的信息有误，请重新输入!”最多尝试 3 次，每次刷新验证码并重新输入。
7. 其他登录错误立即抛出异常。
8. 登录成功后访问信用买入页，确认持仓接口返回成功。
9. 自动关闭本次工具启动的 Chrome 实例。

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

首次验证码也可以通过命令行参数或 `EM_CAPTCHA` 提供；发生重试时仍会要求在终端重新输入刷新后的验证码。

## 安全边界

- 工具不调用大模型或 OCR 服务识别验证码。
- 工具不输出、返回或持久化 Cookie、validatekey、Token、Session_Id。
- 持仓接口只在当前浏览器会话内验证 HTTP 状态及认证参数是否存在，不输出持仓数据。
- 密码、Cookie、validatekey、Token 等敏感配置不会由 `PropsUtil` 写入日志。
- 请只在本人设备和本人账户上使用，并遵守证券公司的服务条款和风控要求。
