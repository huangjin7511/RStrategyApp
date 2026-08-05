# R策略切换 App 📊

> 基于 R = 创业板指价格 ÷ 中证红利价格 的比值，自动判断应持有哪个指数。  
> 阈值和全收益系数**全部可在 App 内实时修改**。

---

## 🏗️ 工程结构

```
RStrategyApp/
├── .github/
│   ├── workflows/
│   │   ├── build.yml              ← CI：自动构建 + 发版
│   │   └── codeql.yml            ← 安全扫描
│   ├── pull_request_template.md
│   └── ISSUE_TEMPLATE/
├── app/
│   ├── build.gradle.kts           ← 版本号从 version.properties 读取
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/rstrategy/app/
│       │   ├── MainActivity.kt    ← WebView + JS桥接 + 腾讯行情API
│       │   └── RStrategyApp.kt
│       ├── res/
│       │   ├── layout/activity_main.xml
│       │   ├── values/{strings,themes}.xml
│       │   └── xml/network_security_config.xml
│       └── assets/
│           └── dashboard.html     ← 图表 + 阈值编辑 + 信号面板
├── scripts/
│   └── bump_version.sh            ← 一键递增版本号
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── .gitignore
├── version.properties             ← 版本号统一管理
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 GitHub Action 自动构建

### 触发方式

| 触发条件 | 行为 |
|---|---|
| Push to `main` / `master` | 自动跑 lint + 构建 **Debug APK** |
| 打 Tag `v*` (如 `v1.0.1`) | 自动构建 + 签名 Release APK + 创建 GitHub Release |
| `workflow_dispatch` 手动触发 | 可选 debug 或 release 构建 |

### 自动产出

- ✅ **Debug APK**：每次 push 自动构建，保留 30 天
- ✅ **Release APK**：打 tag 后自动签名 + 发版，保留 90 天
- ✅ **Lint 报告**：每次构建附带，可在 Artifacts 下载
- ✅ **CodeQL 安全扫描**：每周一自动跑 + PR 时触发

---

## 📦 发版流程（3步）

```bash
# 1. 递增版本号（patch/minor/major）
./scripts/bump_version.sh patch

# 2. 提交 + 打 tag
git add version.properties
git commit -m "chore: bump version to 1.0.1"
git tag v1.0.1

# 3. 推送（自动触发 GitHub Action 构建 + 发版）
git push && git push --tags
```

推送后 GitHub Action 会自动：
1. 编译 Release APK
2. 用密钥签名
3. 在 GitHub Releases 页面发布，附带 APK 下载

---

## 🔐 Release 签名配置

在 GitHub 仓库 **Settings → Secrets and variables → Actions** 中添加：

| Secret 名称 | 说明 |
|---|---|
| `KEYSTORE_BASE64` | Keystore 文件 Base64 编码（`base64 -i my-key.jks`） |
| `KEYSTORE_PATH` | Keystore 路径（默认 `app/release-key.jks`） |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

> 未配置签名密钥时，Release 构建仍可产出**未签名 APK**，不会报错。

---

## 📱 App 功能

### 实时信号
- 自动从腾讯财经拉取创业板指(399006) & 中证红利(000922)实时行情
- R = 创业板全收益价 ÷ 红利全收益价
- **R < 买入阈值** → 🔴 切创业板
- **R > 卖出阈值** → 🟢 切红利
- **中间区间** → 🔵 观察持有

### 阈值可修改（核心）
App 内四个参数**随时可调**，点"应用"立即生效：

| 参数 | 默认值 | 说明 |
|---|---|---|
| 买入阈值 | 0.45 | R 低于此值触发买入信号 |
| 卖出阈值 | 0.95 | R 高于此值触发卖出信号 |
| 创业板全收益系数 | 1.10 | 价格指数 → 全收益折算 |
| 红利全收益系数 | 1.55 | 价格指数 → 全收益折算 |

修改后自动存入 localStorage，下次打开保留。

### 其他
- 📈 近 60 日 R 值走势图（Chart.js）
- 📋 信号切换历史记录
- 🔄 手动刷新 / 5 分钟自动刷新
- 🌐 网络失败时自动降级为模拟数据

---

## 🛠️ 本地开发

```bash
# 克隆
git clone https://github.com/<your-username>/RStrategyApp.git
cd RStrategyApp

# 用 Android Studio 打开，或直接命令行构建：
./gradlew assembleDebug

# APK 输出位置：
# app/build/outputs/apk/debug/app-debug.apk
```

要求：
- JDK 17
- Android SDK 34
- Gradle 8.7（Wrapper 自动下载）

---

## ⚠️ 风险提示

本 App 仅供策略研究参考，不构成投资建议。指数全收益系数为估算值，与官方全收益指数存在偏差。实盘前请充分回测验证。

---

## License

MIT
