# WatchGuard（手表防丢卫士）

> 专门针对 **Redmi 手机（Xiaomi HyperOS，Android 14+）** 与 **智能穿戴设备（如 HUAWEI WATCH GT 4）** 打造的轻量级、低功耗蓝牙链路防丢助手。

---

## 📱 核心业务功能与特性

1. **智能设备绑定与识别**：
   - 支持系统已配对蓝牙设备列表扫描，优先自动识别并推荐绑定 `HUAWEI WATCH GT 4`。
   - 记录目标设备的 MAC 地址与蓝牙别名。

2. **连接状态监控与防误触（Debounce）**：
   - 实时监听系统底层 `BluetoothDevice.ACTION_ACL_DISCONNECTED` 与 `ACTION_ACL_CONNECTED`。
   - 内置 **5~10 秒可配置的防抖缓冲窗口**，智能过滤蓝牙信号瞬间闪断，彻底告别频繁误报。

3. **断连强警报（手机端）**：
   - 确认失联后，绕过手机静音/震动状态，强制使用音频流（`STREAM_ALARM`）播放最大音量蜂鸣警报音。
   - 触发高频节奏强震动。
   - 集成 TTS 语音合成，循环播报：*“警告：手表已断开连接！请立即检查！”*。
   - 支持全屏/通知栏一键解除警报。

4. **断连瞬间 GPS 坐标抓取（核心寻物）**：
   - 监听到断连的瞬间，立即调用高精度定位（优先 `FusedLocationProviderClient`，降级 `LocationManager` GPS/网络提供商）抓取当前经纬度、精度及毫秒级时间戳。
   - 基于 AndroidX DataStore 进行本地持久化存储。

5. **失联地点可视化与一键寻物**：
   - 首页卡片展示最后一次失联时间与坐标（精度 ±X 米）。
   - 提供“寻回手表”按钮，利用标准 `geo:0,0?q=lat,lng` 协议，一键直接唤起手机中的**高德地图**、**百度地图**、**腾讯地图**进行路线规划与步行导航。

6. **Xiaomi HyperOS 深度保活优化**：
   - 遵循 Android 14+（API 34）规范，实现 `ForegroundService`（类型为 `connectedDevice` 与 `location`）。
   - 页面内置专属 HyperOS 保活指引，提供一键跳转系统“应用自启动管理”、“神隐模式/省电策略设为无限制”以及多任务卡片加锁图解。

---

## 🛠 技术栈与工程规范

- **开发语言**：Kotlin（100% 原生）
- **UI 框架**：Jetpack Compose + Material 3（赛博朋克深色沉浸式设计）
- **架构模式**：MVVM + Android 协程（Coroutines & Flow）
- **存储方案**：AndroidX DataStore Preferences
- **定位体系**：Google Play Services Location (`FusedLocationProviderClient`) + 原生 `LocationManager`
- **后台保活**：Android 14+ Foreground Service (`connectedDevice|location`)
- **CI/CD**：`.github/workflows/build.yml`（GitHub Actions 自动化构建产出 Debug APK）

---

## 📂 项目结构树

```text
WatchGuard/
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions 自动化编译构建工作流
├── app/
│   ├── build.gradle.kts            # App 模块依赖与 Compose 配置
│   ├── proguard-rules.pro          # 混淆保护规则
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml # Android 14 完整权限与前台服务声明
│           ├── java/com/watchguard/app/
│           │   ├── WatchGuardApp.kt             # Application 基础配置与通知渠道
│           │   ├── data/
│           │   │   ├── model/
│           │   │   │   ├── DeviceInfo.kt        # 蓝牙设备模型
│           │   │   │   ├── GuardConfig.kt       # 守护配置模型
│           │   │   │   └── DisconnectRecord.kt  # GPS 坐标与断连记录模型
│           │   │   └── repository/
│           │   │       └── GuardPreferences.kt  # DataStore 持久化仓库
│           │   ├── service/
│           │   │   ├── WatchGuardService.kt     # 前台保活与断连防抖核心服务
│           │   │   ├── AlertPlayer.kt           # 强警报音频、震动与 TTS 播放器
│           │   │   ├── LocationTracker.kt       # 高精度 GPS 定位抓取器
│           │   │   └── BootReceiver.kt          # 开机自启动广播接收器
│           │   ├── util/
│           │   │   ├── HyperOsHelper.kt         # 小米/HyperOS 专属保活跳转辅助
│           │   │   ├── MapIntentHelper.kt       # 高德/百度/腾讯地图一键导航唤起
│           │   │   └── PermissionHelper.kt      # Android 运行时权限管理
│           │   └── ui/
│           │       ├── MainActivity.kt          # Compose 入口 Activity 与服务绑定
│           │       ├── theme/                   # Material 3 主题配色与字体
│           │       ├── components/              # 状态雷达、设备卡片、警报弹窗
│           │       ├── screens/                 # 首页、设置页、HyperOS 保活指引页
│           │       └── viewmodel/               # MVVM ViewModel 与 UI State
│           └── res/                             # 应用图标、主题与多语言资源
├── build.gradle.kts                # 根 Gradle 配置
├── settings.gradle.kts             # 模块与仓库源配置
├── gradle.properties               # JVM 与 AndroidX 配置
└── gradlew / gradlew.bat           # Gradle Wrapper 脚本
```

---

## 🚀 云端自动编译（GitHub Actions）

本项目已完整配置 `.github/workflows/build.yml`：
1. 推送代码至 GitHub 仓库（`main` / `master` 分支）。
2. GitHub Actions 将自动拉起 Ubuntu 虚拟环境，配置 Temurin JDK 17 并执行 `./gradlew assembleDebug`。
3. 构建完成后，可在 Actions 页面的 **Artifacts** 中直接下载编译好的 `WatchGuard-debug-apk` 安装至 Redmi K70 Ultra。

---

## ⚡ HyperOS / Redmi K70 Ultra 最佳实践设置

为了确保锁屏与待机时断连守护 100% 灵敏生效，请在手机端完成以下配置：
1. **应用自启动**：在设置 -> 应用设置 -> 授权管理 -> 自启动管理中，开启 WatchGuard。
2. **省电策略设为无限制**：长按 WatchGuard 图标 -> 应用信息 -> 省电策略 -> 选择“无限制”。
3. **多任务卡片加锁**：从屏幕底部上滑呼出最近任务列表，长按 WatchGuard 卡片，点击“小锁”图标加锁。
4. **后台弹出界面权限**：在应用权限中，开启“后台弹出界面”权限，以便在手表脱离范围瞬间弹出强警报界面。
