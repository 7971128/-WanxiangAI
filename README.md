# 万象棋三分倒转 AI（实验版）

安卓 11+ 研究用辅助项目。使用截图 OCR、三分倒转本地策略提示以及可选 DeepSeek API，支持无障碍悬浮窗中的建议及受限单步点击。尚不具备完整棋子视觉识别、拖拽与无人值守全局对局能力。

构建：GitHub Actions 中打开 `Build Android APK`，或使用 JDK 17、Android SDK 35 与 Gradle 8.7 执行 `gradle :app:assembleDebug`。构建产物 `app/build/outputs/apk/debug/app-debug.apk`。

**安全提醒：** 自动操作可能违反游戏规则；请在了解规则后使用。不要在公开仓库上传 DeepSeek API Key。
