# 不背诗词 - 快速参考指南

这是一份精简的快速参考指南，帮助您快速了解应用的核心信息。

---

## 📱 应用信息速览

| 项目 | 信息 |
|------|------|
| 名称 | 不背诗词 |
| 版本 | 1.1.3 (build 113) |
| 包名 | com.jerryz.poems |
| 系统要求 | Android 12+ (API 31+) |
| 应用大小 | ~5-10 MB |
| 开发语言 | Kotlin |
| 架构 | MVVM + Repository |
| UI 框架 | View System + Material 3 |

---

## ⚡ 核心功能速查

### 📖 浏览
- ✅ 卡片式列表
- ✅ 下拉刷新
- ✅ 随机阅读
- ✅ 流畅动画

### 📝 详情
- ✅ 完整正文
- ✅ 注释译文
- ✅ 拼音标注
- ✅ 字号调节
- ✅ 收藏管理
- ✅ 分享复制

### 🤖 AI 功能
- ✅ AI 解读（对话）
- ✅ AI 默写（练习）
- ✅ 流式响应
- ✅ 智能批改

### 🔍 搜索
- ✅ 全文搜索
- ✅ 标签筛选
- ✅ 实时结果

### ⭐ 收藏
- ✅ 一键收藏
- ✅ 集中管理
- ✅ 本地存储

---

## 🎨 设计特色

| 特色 | 说明 |
|------|------|
| 🎨 Material You | 动态取色，融入系统主题 |
| ✨ 流畅动画 | 共享元素过渡 + 预测性返回 |
| 📱 边到边 | 沉浸式全屏体验 |
| 🌙 深色模式 | 自动适配系统 |
| 💾 离线可用 | 三级缓存策略 |
| 🔒 隐私保护 | 无广告，数据本地 |

---

## 🛠️ 技术栈一览

### UI 层
```
View System + Material Components 3
├── CardView
├── RecyclerView
├── FloatingActionButton
├── BottomSheet
├── Toolbar
└── Chip
```

### 架构层
```
MVVM
├── Activity: MainActivity
├── Fragments: Home, Detail, Search, Favorite, About, AI
├── ViewModels: 各页面 ViewModel
└── Repository: PoemRepository
```

### 数据层
```
三级缓存
├── 内存缓存: List<Poem>
├── 文件缓存: poems_data.txt
└── 网络源: poems.jerryz.com.cn
```

### 第三方库
```
核心:
├── OkHttp (网络)
├── Kotlin Coroutines (异步)
├── Navigation Component (导航)
└── LiveData + ViewModel (数据)

工具:
├── JPinyin (拼音)
├── Glide (图片)
└── Material Components (UI)
```

---

## 📊 功能对照表

| 功能 | 首页 | 详情 | 搜索 | 收藏 | 关于 |
|------|:----:|:----:|:----:|:----:|:----:|
| 浏览诗词 | ✅ | - | ✅ | ✅ | - |
| 查看详情 | ✅ | ✅ | ✅ | ✅ | - |
| 搜索筛选 | - | - | ✅ | - | - |
| 收藏管理 | - | ✅ | - | ✅ | - |
| AI 功能 | - | ✅ | - | - | - |
| 分享复制 | - | ✅ | - | - | - |
| 拼音标注 | - | ✅ | - | - | - |
| 字号调节 | - | ✅ | - | - | - |

---

## 🔄 数据流简图

```
用户操作
    ↓
Fragment
    ↓
ViewModel
    ↓
Repository
    ↓
[内存] → [文件] → [网络]
    ↓
解析 (Parser)
    ↓
LiveData
    ↓
UI 更新
```

---

## 🎯 使用场景

### 学生
- 📚 背诵高考必背诗词
- ✍️ AI 默写检验记忆
- 💡 AI 解读理解内涵

### 教师
- 📖 教学演示
- 📝 布置作业
- 📤 分享诗词

### 爱好者
- 🌙 睡前阅读
- 🚇 通勤消遣
- ⭐ 收藏喜欢的诗词

---

## 📁 项目结构速览

```
Poems-Android/
├── app/
│   └── src/main/
│       ├── java/com/jerryz/poems/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── Poem.kt
│       │   │   └── PoemRepository.kt
│       │   ├── ui/
│       │   │   ├── home/
│       │   │   ├── detail/
│       │   │   ├── search/
│       │   │   ├── favorite/
│       │   │   ├── about/
│       │   │   └── ai/
│       │   └── util/
│       └── res/
│           ├── layout/
│           ├── values/
│           └── navigation/
├── docs/
│   ├── README.md (本文档索引)
│   ├── APP_INTRODUCTION.md
│   ├── FEATURE_SUMMARY.md
│   ├── ARCHITECTURE_DIAGRAM.md
│   ├── QUICK_REFERENCE.md (本文件)
│   └── index.html
├── README.md
└── AGENTS.md
```

---

## 🔗 快速链接

### 文档
- [应用介绍](APP_INTRODUCTION.md) - 详细介绍
- [功能总结](FEATURE_SUMMARY.md) - 完整功能清单
- [架构图解](ARCHITECTURE_DIAGRAM.md) - 技术架构
- [网页展示](index.html) - 在线网页

### 项目
- [主 README](../README.md) - 项目总览
- [开发指南](../AGENTS.md) - 开发规范

### 外部
- [官网](https://poems.jerryz.com.cn) - 网页版
- [GitHub](https://github.com/YangguangZhou/Poems-Android) - 源码
- [博客](https://blog.jerryz.com.cn) - 开发者博客

---

## 🚀 快速开始

### 用户
1. 访问官网或下载 APK
2. 安装并打开应用
3. 开始浏览诗词
4. 点击诗词查看详情
5. 尝试 AI 功能

### 开发者
1. Clone 仓库
2. 用 Android Studio 打开
3. 等待 Gradle 同步
4. 运行到设备/模拟器

```bash
git clone https://github.com/YangguangZhou/Poems-Android.git
cd Poems-Android
./gradlew assembleDebug
```

---

## ❓ 常见问题

**Q: 需要联网吗？**  
A: 首次需要联网下载数据，之后可离线使用。

**Q: AI 功能需要配置吗？**  
A: 可选。开发者可配置 `ai.env` 文件自定义 API。

**Q: 支持哪些系统版本？**  
A: Android 12 (API 31) 及以上。

**Q: 应用收费吗？**  
A: 完全免费，无广告。

**Q: 数据安全吗？**  
A: 所有数据本地存储，不上传服务器。

**Q: 如何反馈问题？**  
A: 邮件 i@jerryz.com.cn 或 GitHub Issues。

---

## 📞 联系方式

- 📧 邮箱: i@jerryz.com.cn
- 💬 QQ: 2098412009
- 🐙 GitHub: [@YangguangZhou](https://github.com/YangguangZhou)

---

## 🎓 学习资源

### Android 开发
- [Material Design 3](https://m3.material.io/)
- [Android Developers](https://developer.android.com/)
- [Kotlin 文档](https://kotlinlang.org/)

### 本项目相关
- [完整文档](README.md) - 查看所有文档
- [开发指南](../AGENTS.md) - 参与开发

---

**快速参考指南到此结束。需要更多详细信息，请查看其他文档！** 📚

---

*最后更新: 2025年*  
*版本: 1.0*
