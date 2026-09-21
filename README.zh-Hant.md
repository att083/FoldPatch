![FoldPatch — 接起破裂的螢幕，讓手機繼續可用。](docs/media/foldpatch-cover.png)

# FoldPatch

[English](README.md) · [한국어](README.ko.md) · [日本語](README.ja.md) · [简体中文](README.zh-Hans.md) · **繁體中文**

**[下載 APK](https://github.com/att083/FoldPatch/releases/download/v0.1.0-alpha.4/foldpatch-release.apk)** · [安裝指南](docs/INSTALL.zh-Hant.md) · [相容性（英文）](docs/COMPATIBILITY.en.md)

**讓螢幕中間損壞、只有一側觸控正常的摺疊手機繼續可用的 Android 應用程式。**

這是維護者自己的 Galaxy Z Fold3 損壞後開發的開源應用程式。希望遇到相同問題的人，即使暫時無法維修，也能用剩下的螢幕繼續使用手機。[開發背景與相關工具（英文）](docs/WHY.en.md)。

## 螢幕範圍調整前後

| 調整前：左側 50% · 右側 50% | 調整後：左側 44% · 右側 43.5% |
| --- | --- |
| [<img src="docs/media/range-before-fold3.jpg" width="340" alt="調整前：Fold3 中間的損壞區域遮住了數字 8 和 9。" />](docs/media/range-before-fold3.jpg) | [<img src="docs/media/range-after-fold3.jpg" width="340" alt="調整後：Fold3 兩側可以完整看到數字 1 到 16。" />](docs/media/range-after-fold3.jpg) |

**Galaxy Z Fold3 · Android 15 實機照片。** [查看 Chrome 和分割畫面的使用前後照片（英文）→](docs/REAL_USE.en.md)

## 能做什麼

- **避開損壞區域，接續顯示內容。** 應用程式會依左右可用寬度的總和重新排版，FoldPatch 再將這一個畫面跨過中間損壞區域顯示，不會橫向壓扁畫面。既可用於單一全螢幕應用程式，也可保留原有的分割畫面配置。
- **用一側操作兩側。** 選擇觸控正常的左側或右側，將其作為觸控板，對另一側或整個螢幕進行點擊、拖曳、捲動和縮放。
- **在觸控正常的一側輸入。** 單側鍵盤可以向任一側的輸入欄位輸入文字，並提供複製、貼上等基本編輯功能。

可調整左右顯示寬度、工具列大小和位置，也可設定展開手機時自動開啟 FoldPatch。

<p>
  <a href="docs/media/settings-fold3.png"><img src="docs/media/settings-fold3.png" width="280" alt="Fold3 的設定控制項位於觸控正常的左側。" /></a>
  <a href="docs/media/calibration-fold3.png"><img src="docs/media/calibration-fold3.png" width="280" alt="使用左側滑桿調整兩側可見寬度。" /></a>
  <a href="docs/media/keyboard-fold3.png"><img src="docs/media/keyboard-fold3.png" width="280" alt="輸入測試畫面中，左側顯示鍵盤和直向工具列，右側顯示指標。" /></a>
</p>

由左至右：**設定 · 螢幕範圍調整 · 鍵盤與工具列**。擷取自 Galaxy Z Fold3 / Android 15，介面語言為韓語；鍵盤背景為輸入測試畫面。點選圖片可查看原圖。

## 我的手機能用嗎？

目前適用於 **直向的內側螢幕**。左右兩側都需要有可見區域，至少一側觸控正常。每側可用寬度從外側邊緣向內計算，可設為螢幕總寬度的 20–50%。左右均設為 50% 時可保留完整畫面，即使螢幕沒有損壞，也能用一側作為觸控板。

| 環境 | 驗證情況 |
| --- | --- |
| Galaxy Z Fold3 · Android 15 / One UI 7 | 已在實機上驗證 |
| Android 16/17 | 僅在模擬器上驗證，未經實機測試 |
| 其他摺疊手機 / Android 15+ | 功能尚未驗證 |

安裝最低需求為 Android 15。裝置差異和未驗證行為請參閱 [相容性說明（英文）](docs/COMPATIBILITY.en.md)。不支援與螢幕閱讀器同時使用。

**必須安裝並執行 [Shizuku](https://shizuku.rikka.app/download/)。** 初次準備需要可信任的 Wi-Fi 和無線偵錯，不需要 root，也不需要一直連接電腦。手機重新啟動後，可能需要重新啟動 Shizuku。

## 開始使用

**直接安裝現成的 APK，無需自行編譯。** 目前為早期 Alpha 版本。

1. 下載並安裝 **[foldpatch-release.apk](https://github.com/att083/FoldPatch/releases/download/v0.1.0-alpha.4/foldpatch-release.apk)**。
2. 開啟 FoldPatch，按照應用程式內的引導選擇觸控正常的一側，準備 Shizuku、權限和鍵盤。
3. 展開手機，調整並儲存兩側可見寬度，然後返回平常使用的應用程式。

前期準備可以在手機摺疊時透過外側螢幕完成。**[安裝、手勢與還原指南 →](docs/INSTALL.zh-Hant.md)**

**想讓 AI 幫你在手機上安裝？** 請讓 Codex、Claude Code 等 AI 代理閱讀 [AI 安裝指南（英文）](docs/AGENT_INSTALL.en.md)。可以複製以下請求：

> 閱讀此儲存庫的 `docs/AGENT_INSTALL.en.md`，幫我在手機上安裝 FoldPatch。請執行你能完成的步驟，需要我在手機上操作時再告訴我，並保留現有應用程式資料和設定。

介面會跟隨系統語言，支援韓語、英語、日語、簡體中文和繁體中文。**鍵盤僅支援韓語和英語輸入。** 應用程式沒有網際網路權限、廣告或分析 SDK。[隱私與權限（英文）](PRIVACY.en.md)。

## 參與改進

歡迎回報難以理解的設定步驟，以及應用程式在你手機上的表現，也歡迎參與翻譯、文件和程式碼改進。[貢獻指南（英文）](CONTRIBUTING.md)。

[建置（英文）](docs/BUILD.en.md) · [架構（英文）](docs/ARCHITECTURE.en.md) · [待完成工作（英文）](docs/BACKLOG.en.md)

[Apache License 2.0](LICENSE) · [第三方聲明（英文）](THIRD_PARTY_NOTICES.md)
