![FoldPatch — 接起破裂的屏幕，让手机继续可用。](docs/media/foldpatch-cover.png)

# FoldPatch

[English](README.md) · [한국어](README.ko.md) · [日本語](README.ja.md) · **简体中文** · [繁體中文](README.zh-Hant.md)

**[下载 APK](https://github.com/att083/FoldPatch/releases/download/v0.1.0-alpha.4/foldpatch-release.apk)** · [安装指南](docs/INSTALL.zh-Hans.md) · [兼容性（英文）](docs/COMPATIBILITY.en.md)

**让屏幕中间损坏、只有一侧触控正常的折叠屏手机继续可用的 Android 应用。**

这是维护者自己的 Galaxy Z Fold3 损坏后开发的开源应用。希望遇到同样问题的人，即使暂时无法维修，也能用剩下的屏幕继续使用手机。[开发背景与相关工具（英文）](docs/WHY.en.md)。

## 屏幕范围调整前后

| 调整前：左侧 50% · 右侧 50% | 调整后：左侧 44% · 右侧 43.5% |
| --- | --- |
| [<img src="docs/media/range-before-fold3.jpg" width="340" alt="调整前：Fold3 中间的损坏区域遮住了数字 8 和 9。" />](docs/media/range-before-fold3.jpg) | [<img src="docs/media/range-after-fold3.jpg" width="340" alt="调整后：Fold3 两侧可以完整看到数字 1 到 16。" />](docs/media/range-after-fold3.jpg) |

**Galaxy Z Fold3 · Android 15 实机照片。** [查看 Chrome 和分屏的使用前后照片（英文）→](docs/REAL_USE.en.md)

## 能做什么

- **避开损坏区域，接续显示内容。** 应用会按左右可用宽度之和重新排版，FoldPatch 再将这一个画面跨过中间损坏区域显示，不会横向压扁画面。既可用于单个全屏应用，也可保留原有分屏布局。
- **用一侧操作两侧。** 选择触控正常的左侧或右侧，将其作为触控板，对另一侧或整个屏幕进行点击、拖动、滚动和缩放。
- **在触控正常的一侧输入。** 单侧键盘可以向任一侧的输入框输入文字，并提供复制、粘贴等基本编辑功能。

可调整左右显示宽度、工具栏大小和位置，也可设置展开手机时自动开启 FoldPatch。

<p>
  <a href="docs/media/settings-fold3.png"><img src="docs/media/settings-fold3.png" width="280" alt="Fold3 的设置控件位于触控正常的左侧。" /></a>
  <a href="docs/media/calibration-fold3.png"><img src="docs/media/calibration-fold3.png" width="280" alt="使用左侧滑块调整两侧可见宽度。" /></a>
  <a href="docs/media/keyboard-fold3.png"><img src="docs/media/keyboard-fold3.png" width="280" alt="输入测试画面中，左侧显示键盘和竖向工具栏，右侧显示指针。" /></a>
</p>

从左到右：**设置 · 屏幕范围调整 · 键盘与工具栏**。截图来自 Galaxy Z Fold3 / Android 15，界面语言为韩语；键盘背景为输入测试画面。点击图片可查看原图。

## 我的手机能用吗？

目前面向 **竖屏状态下的内屏**。左右两侧都需要有可见区域，至少一侧触控正常。每侧可用宽度从外边缘向内计算，可设为屏幕总宽度的 20–50%。左右均设为 50% 时可保留完整画面，即使屏幕没有损坏，也能用一侧作为触控板。

| 环境 | 验证情况 |
| --- | --- |
| Galaxy Z Fold3 · Android 15 / One UI 7 | 已在真机上验证 |
| Android 16/17 | 仅在模拟器上验证，未经真机测试 |
| 其他折叠屏手机 / Android 14 | 功能尚未验证 |

安装最低要求为 Android 14。设备差异和未验证行为请参阅 [兼容性说明（英文）](docs/COMPATIBILITY.en.md)。不支持与屏幕阅读器同时使用。

**必须安装并运行 [Shizuku](https://shizuku.rikka.app/download/)。** 初次准备需要可信的 Wi-Fi 和无线调试，不需要 root，也不需要一直连接电脑。手机重启后，可能需要重新启动 Shizuku。

## 开始使用

**直接安装现成的 APK，无需自行编译。** 目前为早期 Alpha 版本。

1. 下载并安装 **[foldpatch-release.apk](https://github.com/att083/FoldPatch/releases/download/v0.1.0-alpha.4/foldpatch-release.apk)**。
2. 打开 FoldPatch，按照应用内引导选择触控正常的一侧，准备 Shizuku、权限和键盘。
3. 展开手机，调整并保存两侧可见宽度，然后返回平时使用的应用。

前期准备可以在手机折叠时通过外屏完成。**[安装、手势与恢复指南 →](docs/INSTALL.zh-Hans.md)**

**想让 Codex、Claude Code 等智能体协助安装？** 请从 [智能体安装指南（英文）](docs/AGENT_INSTALL.en.md)开始，仓库入口为 [AGENTS.md（英文）](AGENTS.md)。可以这样提出请求：

> 阅读此仓库的 AGENTS.md 和智能体安装指南，帮我在手机上安装 FoldPatch。请执行你能完成的步骤，需要我在手机上操作时再告诉我，并保留现有应用数据和设置。

界面会跟随系统语言，支持韩语、英语、日语、简体中文和繁体中文。**键盘仅支持韩语和英语输入。** 应用没有互联网权限、广告或统计分析 SDK。[隐私与权限（英文）](PRIVACY.en.md)。

## 参与改进

欢迎反馈难以理解的设置步骤，以及应用在你手机上的表现，也欢迎参与翻译、文档和代码改进。[贡献指南（英文）](CONTRIBUTING.md)。

[构建（英文）](docs/BUILD.en.md) · [架构（英文）](docs/ARCHITECTURE.en.md) · [待完成工作（英文）](docs/BACKLOG.en.md)

[Apache License 2.0](LICENSE) · [第三方声明（英文）](THIRD_PARTY_NOTICES.md)
