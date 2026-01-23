# 不背诗词 - Poems Android

![GitHub repo size](https://img.shields.io/github/repo-size/YangguangZhou/Poems-Android?style=flat-square)
![License](https://img.shields.io/github/license/YangguangZhou/Poems-Android?style=flat-square)
![Version](https://img.shields.io/badge/version-1.1.3-blue?style=flat-square)

一款采用 Material You 设计的优雅古诗词学习 Android 应用

## 📖 简介

**不背诗词**是一款专为诗词学习者打造的 Android 应用，收录全部高考必背古诗文。应用采用最新的 Material Design 3 设计语言，提供流畅优雅的用户体验，并创新性地集成了 AI 解读和 AI 默写功能，让古诗词学习从枯燥的背诵变成愉快的探索。

### ✨ 核心特色

- 🎨 **Material You 设计** - 完整支持动态取色，与系统主题完美融合
- 📚 **完整诗词库** - 收录全部高考必背古诗文
- 🤖 **AI 智能辅助** - AI 解读与 AI 默写功能
- ✨ **流畅动画** - 共享元素过渡与预测性返回手势
- 📱 **边到边体验** - 沉浸式全屏阅读
- 🌙 **深色模式** - 自动适配系统主题
- 💾 **离线访问** - 三级缓存策略

## 🚀 主要功能

### 诗词浏览
- 优雅的卡片式列表界面
- 流畅的共享元素展开动画
- 支持下拉刷新和随机阅读

### 诗词详情
- 完整正文与注释译文
- 拼音标注（点击任意汉字）
- 字号调节功能
- 收藏、分享、复制功能

### AI 功能
- **AI 解读** - 与 AI 对话，深入理解诗词内涵
- **AI 默写** - 智能生成默写题，自动批改

### 搜索与收藏
- 全方位搜索（标题、作者、朝代、标签、内容）
- 标签云快速筛选
- 收藏管理

## 📱 截图

> 详见 [应用介绍文档](docs/APP_INTRODUCTION.md) 或访问 [在线网页](docs/index.html)

## 🛠️ 技术栈

- **UI**: View + Material Components (M3)
- **架构**: 单 Activity + 多 Fragment + Navigation Component
- **数据**: LiveData + ViewModel + Coroutines
- **网络**: OkHttp
- **动画**: MaterialContainerTransform + 预测性返回
- **缓存**: 内存缓存 + 本地文件 + SharedPreferences

## 📋 系统要求

- 最低系统：Android 12 (API 31)
- 目标系统：Android 15 (API 36)
- 推荐设备：支持 Material You 的 Android 设备

## 🔧 构建说明

```bash
# 克隆仓库
git clone https://github.com/YangguangZhou/Poems-Android.git

# 进入项目目录
cd Poems-Android

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease
```

### AI 功能配置（可选）

如需使用 AI 功能，请创建 `ai.env` 文件：

```properties
AI_BASE_URL=https://your-api-endpoint.com/v1
AI_API_KEY=your_api_key_here
AI_MODEL=your_model_name
```

## 📚 文档

- [完整应用介绍](docs/APP_INTRODUCTION.md) - 详细的功能介绍和使用说明
- [在线网页展示](docs/index.html) - 图文并茂的网页介绍
- [开发者指南](AGENTS.md) - 技术架构和开发规范

## 🌐 相关链接

- 官方网站：https://poems.jerryz.com.cn
- 开发者博客：https://blog.jerryz.com.cn
- GitHub：https://github.com/YangguangZhou/Poems-Android

## 👨‍💻 作者

**Jerry Zhou**
- 邮箱：i@jerryz.com.cn
- QQ：2098412009
- 座右铭：永远相信美好的事情即将发生

## 📄 开源协议

本项目采用开源协议，详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

感谢以下开源项目：
- Android Jetpack
- Material Components
- OkHttp
- JPinyin
- Glide

---

**欢迎 Star ⭐ 和贡献代码！**
