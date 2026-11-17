# RanaSeek 开发环境记录

> 本文件记录本机 Android 开发工具链的实际安装位置与配置，供后续开发/排障参考。
> 更新日期：2026-11-17

## 一、已安装工具链

| 组件 | 版本 | 位置 |
| --- | --- | --- |
| JDK | OpenJDK 21.0.7（Microsoft Build） | `D:\CNI\jdk21\java-runtime-delta` |
| JDK（备用） | Temurin 17.0.18 | `D:\CNI\jdk17\jdk-17.0.18+8` |
| Gradle | 9.9.1 | `D:\Android\gradle\gradle-9.9.1` |
| Gradle 用户缓存 | — | `D:\Android\gradle-home`（避免占用 C 盘） |
| Android SDK | cmdline-tools latest | `D:\Android\Sdk\cmdline-tools\latest` |
| Android SDK Platform | android-36、android-37.0 | `D:\Android\Sdk\platforms` |
| Build-Tools | 36.0.0、37.0.0 | `D:\Android\Sdk\build-tools` |
| Platform-Tools (adb) | 37.0.1 | `D:\Android\Sdk\platform-tools` |

## 二、用户级环境变量（已持久化）

| 变量 | 值 |
| --- | --- |
| `JAVA_HOME` | `D:\CNI\jdk21\java-runtime-delta` |
| `ANDROID_HOME` | `D:\Android\Sdk` |
| `ANDROID_SDK_ROOT` | `D:\Android\Sdk` |
| `GRADLE_USER_HOME` | `D:\Android\gradle-home` |
| `Path` 追加 | `D:\Android\Sdk\platform-tools`、`D:\Android\Sdk\cmdline-tools\latest\bin`、`D:\Android\gradle\gradle-9.9.1\bin` |

> 注意：环境变量对**新打开**的终端 / IDE 才生效，已开着的终端需要重开。
> 系统 PATH 里还残留 Oracle Java 8（`C:\Program Files (x86)\Common Files\Oracle\Java\java8path`），
> 直接敲 `java` 可能仍是 1.8；Gradle 通过 `JAVA_HOME` 找 JVM，不受影响。

## 三、已验证可用的构建配置

冒烟工程已验证下列组合能打出 APK（Java 与 Jetpack Compose 两种都通过）：

- Gradle 9.9.1 + AGP **9.5.1**
- AGP 9 起 **内置 Kotlin 支持**：不要再 apply `org.jetbrains.kotlin.android`，
  否则报错 `The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0`
- Compose 需要 `org.jetbrains.kotlin.plugin.compose`（版本 2.4.21 已验证 OK），并开启
  `buildFeatures { compose = true }`
- Compose BOM `2026.09.00` 的库要求 **compileSdk ≥ 37**，因此项目用
  `compileSdk = 37`（对应 SDK Platform `android-37.0`）
- `gradle.properties` 必须给足内存，默认 512M 会 GC thrashing 构建失败：

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
```

## 四、网络与镜像注意事项

- `dl.google.com`（Google Maven / SDK 仓库）、`repo.maven.apache.org`、`plugins.gradle.org` 均可直连。
- `services.gradle.org` 的发行版下载会 302 到 **GitHub，本机无法访问**。
  因此 Gradle Wrapper 的 `distributionUrl` 必须改用镜像，例如：

```properties
distributionUrl=https://mirrors.cloud.tencent.com/gradle/gradle-9.9.1-bin.zip
```

（华为云 `https://mirrors.huaweicloud.com/gradle/` 同样可用。）

## 五、真机调试（USB）

### 已连接的真机

| 项目 | 值 |
| --- | --- |
| 品牌 / 型号 | HONOR `DNN-AN00`（设备代号 `HNDNN`） |
| 序列号 | `AAHBVB5605002575`（adb 标识） |
| Android | 15（SDK 35） |
| ABI | arm64-v8a（64 位） |
| 屏幕 | 1264 × 2736 px，密度 560 dpi（≈ 2.26x，逻辑宽约 561dp） |
| 安全补丁 | 2025-06-01 |

**minSdk 建议**：真机是 SDK 35，但为兼容旧机建议 `minSdk = 24`（Android 7.0）起。
`targetSdk = 37` 时系统强制 edge-to-edge，界面需用 `enableEdgeToEdge()` + WindowInsets 处理，
否则内容会顶到状态栏后面（冒烟测试已实测复现）。

### 连接步骤（供换机/重连参考）

1. 手机打开：设置 → 关于手机 → 连续点「版本号」7 次 → 开发者选项 → 打开「USB 调试」。
2. USB 连接电脑，手机弹窗选「允许 USB 调试」，并勾选「始终允许」。
3. 验证：

```powershell
adb devices -l          # 应出现 device 而不是 unauthorized
adb install -r app-debug.apk
adb logcat              # 看运行日志
```

### 真机截图流程（已验证）

```powershell
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png D:\Android\_shots\shot.png
adb shell rm /sdcard/shot.png
```

> 不要用 `adb exec-out screencap -p > file.png`，PowerShell 的文本重定向会把 PNG 写坏。

## 六、常用命令

```powershell
# 构建 debug APK（在项目根目录）
gradle assembleDebug

# 或使用项目 wrapper（需镜像配置）
.\gradlew.bat assembleDebug

# 安装到已连接设备
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 清理构建
gradle clean
```

## 七、临时冒烟工程

- `D:\Android\_smoke`：纯 Java + AGP 项目，验证构建链。
- `D:\Android\_smoke2`：Kotlin + Jetpack Compose 项目，验证 Compose 构建链。
- 正式项目建在 `D:\RanaSeek`，上述目录仅作参考，可随时删除。
