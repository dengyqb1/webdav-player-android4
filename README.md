# WebDAV 播放器 - 安卓4版

一个兼容 Android 4.0 (API 15) 的 WebDAV 文件浏览器和流式音视频播放器。

## 功能
- 📂 浏览 WebDAV 服务器目录
- 🎵 流式播放音频 (MP3/FLAC/OGG/AAC/WAV/APE等)
- 🎬 流式播放视频 (MP4/MKV/WebM/AVI/3GP等)
- 🔐 支持用户名/密码认证
- 📱 纯 Holo 主题，安卓4原生风格

## 自动构建 (GitHub Actions)

推送到 GitHub 后会自动构建 APK：

1. 在 GitHub 创建仓库
2. 推送代码：
   ```bash
   cd webdav-player-android4
   git init && git add . && git commit -m "init"
   git remote add origin https://github.com/YOUR_USERNAME/webdav-player-android4.git
   git push -u origin main
   ```
3. 在仓库的 **Actions** 页面，手动触发 "Build APK" workflow
4. 等构建完成后，在 Artifacts 区域下载 APK

## 本地构建

需要：
- JDK 8
- Android SDK (API 25 + Build Tools 25.0.3)

```bash
./gradlew assembleDebug
```

## 技术栈
- Gradle 6.9 + AGP 4.2.2
- minSdk 15 / targetSdk 15
- Sardine WebDAV 客户端
- Android MediaPlayer
