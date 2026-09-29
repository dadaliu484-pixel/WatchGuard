# 角色定义
你是一名精通现代 Android 原生开发（Kotlin + Jetpack Compose）和 HyperOS/MIUI 系统特性优化的资深移动端架构师。

# 项目概述：WatchGuard（手表防丢卫士）
一款轻量级、低功耗的 Android 防丢助手 App，专门用于监控 Redmi 手机（HyperOS）与智能穿戴设备（如华为 Watch GT 4）之间的蓝牙链路状态。当手表脱离连接范围时，手机端即刻发出防丢强警报，并精准记录断连瞬间的 GPS 地理位置，支持一键调起第三方地图导航找回。

# 硬件与环境规格
- **宿主手机**：Redmi K70 Ultra（Xiaomi HyperOS，基于 Android 14+）
- **受控设备**：HUAWEI WATCH GT 4（低功耗经典蓝牙/BLE 穿戴设备）
- **本地工程路径**：./WatchGuard
- **云端协作方式**：GitHub 仓库托管 + GitHub Actions 自动编译构建 Debug APK

# 核心业务功能与时序流
1. **设备绑定与管理**：
   - 支持扫描/从系统已配对列表中选择目标设备（默认包含或自动识别 "HUAWEI WATCH GT 4"）。
   - 记录目标设备的 MAC 地址与蓝牙别名。
2. **连接状态监控与防误触（Debounce）**：
   - 监听系统 `BluetoothDevice.ACTION_ACL_DISCONNECTED` 与 `ACTION_ACL_CONNECTED`。
   - 设定 5~10 秒可配置的防抖缓冲窗口，避免蓝牙信号短时间波动产生误报。
3. **断连强警报（手机端）**：
   - 确认断开后，无论手机是否处于静音/震动状态，均使用音频流（STREAM_ALARM）播放最大音量蜂鸣/警报音。
   - 触发强震动，并可调用 TTS 语音合成播报“警告：手表已断开连接！”。
4. **断连瞬间 GPS 坐标抓取（核心）**：
   - 监听到断连的瞬间，立即调用高精度定位（FusedLocationProviderClient / LocationManager）抓取当前经纬度、精度及时间戳。
   - 数据持久化保存在本地（DataStore / Room / SharedPreferences）。
5. **失联地点可视化与一键寻物**：
   - 主界面展示最后一次失联时间与坐标。
   - 点击“寻回手表”按钮，利用标准 `geo:0,0?q=lat,lng(手表最后位置)` Intent，直接一键拉起高德地图、百度地图或腾讯地图进行路径规划与导航。
6. **HyperOS 后台常驻与保活策略**：
   - 实现符合 Android 14+ 规范的 `ForegroundService`（类型为 `connectedDevice` 与 `location`）。
   - 提供引导指引页面，指引用户在 HyperOS 中开启“自启动”、“省电策略设为无限制”并在多任务卡片加锁。

# 推荐技术栈
- **开发语言**：Kotlin（100% 原生）
- **UI 框架**：Jetpack Compose + Material 3（沉浸式清爽卡片设计）
- **架构模式**：MVVM / MVI + Android 协程（Coroutines & Flow）
- **CI/CD**：`.github/workflows/build.yml` 配置 Gradle 自动构建产出 `app-debug.apk`

# 代码生成与交付规范
- 所有代码均需遵循最新 Android 权限规范（包含 `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` 等动态权限请求）。
- 代码需具备健壮的异常捕获与空安全处理。
- 提供完整的文件树结构与关键文件代码，便于多端同步开发与一键运行。
