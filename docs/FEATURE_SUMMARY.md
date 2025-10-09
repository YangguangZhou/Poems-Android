# 不背诗词应用功能总结

## 应用概述

**不背诗词**是一款基于 Android 平台的古诗词学习应用，采用 Material Design 3 设计语言，为用户提供优雅、流畅的诗词阅读和学习体验。

### 基本信息
- **应用名称**: 不背诗词
- **包名**: com.jerryz.poems
- **版本**: 1.1.3 (113)
- **最低系统**: Android 12 (API 31)
- **目标系统**: Android 15 (API 36)
- **开发语言**: Kotlin
- **设计风格**: Material You (Material Design 3)

---

## 功能清单

### 一、诗词内容管理

#### 1.1 数据来源与存储
- **远程数据源**: `https://poems.jerryz.com.cn/poems.txt`
- **缓存策略**: 
  - 内存缓存（运行时）
  - 本地文件缓存（`filesDir/poems_data.txt`）
  - 首次联网获取，后续离线可用
- **数据刷新**: 支持下拉刷新强制更新

#### 1.2 诗词数据结构
```kotlin
data class Poem(
    val id: Int,              // 唯一标识
    val title: String,        // 诗词标题
    val author: String,       // 作者
    val tags: List<String>,   // 标签（如"高中"、"唐诗"）
    val content: List<String>,    // 正文内容（分句）
    val translation: List<String>, // 翻译（分句）
    val isFavorite: Boolean   // 收藏状态
)
```

### 二、核心功能模块

#### 2.1 首页（Home）
**路径**: `ui/home/HomeFragment`

**功能特性**:
- 卡片式列表展示所有诗词
- 每张卡片显示：标题、作者、朝代标签、内容预览
- 下拉刷新：强制从服务器更新数据
- 随机阅读：工具栏按钮，随机跳转到任意诗词
- RecyclerView 优化：DiffUtil、视图缓存、预取

**交互设计**:
- 点击卡片 → 共享元素展开动画 → 进入详情页
- 流畅的滚动体验
- 底部导航栏自动适配

#### 2.2 诗词详情（Detail）
**路径**: `ui/detail/PoemDetailFragment`

**内容展示**:
- 诗词标题（CollapsingToolbar）
- 作者与朝代信息
- 标签（Chips 形式）
- 正文内容（支持拼音标注）
- 翻译内容（背景色区分）

**操作功能**:
1. **拼音标注**
   - 点击任意汉字弹出拼音弹窗
   - 基于 JPinyin 库实现
   - 自定义 PopupWindow + MaterialCardView

2. **字号调节**
   - 工具栏菜单：增大、减小、重置
   - 实时调整正文和翻译的字体大小

3. **收藏功能**
   - 左下角悬浮按钮（FAB）
   - 动态图标和颜色变化
   - 数据保存在 SharedPreferences

4. **分享与复制**
   - 右上角菜单
   - 分享：生成诗词链接
   - 复制：完整诗词内容到剪贴板

5. **AI 功能入口**
   - 右下角 AI 图标（Extended FAB）
   - 点击弹出 AI 功能底部弹窗

**动画效果**:
- MaterialContainerTransform：卡片展开/收起
- 预测性返回手势支持
- FAB 滚动时自动收缩/展开

#### 2.3 AI 解读（AI Chat）
**路径**: `ui/ai/AiChatBottomSheetFragment`

**功能描述**:
- 与 AI 对话，询问关于诗词的任何问题
- 了解创作背景、艺术手法、思想情感等

**技术实现**:
- 流式响应（SSE - Server-Sent Events）
- 实时打字机效果
- 支持停止生成
- 长按消息可复制内容
- 消息历史记录（可删除单条）

**UI 组件**:
- BottomSheetDialogFragment
- RecyclerView 展示对话历史
- 输入框 + 发送按钮
- 流式生成时显示停止按钮

#### 2.4 AI 默写（AI Dictation）
**路径**: `ui/ai/AiDictationBottomSheetFragment`

**功能描述**:
- AI 自动生成默写题目
- 随机遮蔽诗词中的部分句子
- 用户填写答案后智能批改

**默写流程**:
1. 点击"生成题目"按钮
2. AI 返回题目（包含遮蔽的句子索引）
3. 用户根据标点提示填写答案（无需输入标点）
4. 点击"检查"按钮
5. 显示批改结果：
   - ✓ 正确
   - 错别字 X 处（标注位置）
   - 整句错误（显示正确答案）
6. 查看解析说明
7. 可"再来一组"继续练习

**智能批改**:
- 自动去除标点符号对比
- 逐字比对，标注错误位置
- 统计错别字数量

#### 2.5 搜索（Search）
**路径**: `ui/search/SearchFragment`

**搜索范围**:
- 诗词标题
- 作者姓名
- 朝代信息
- 标签
- 诗词内容（正文和翻译）

**UI 设计**:
- 顶部 AppCompatSearchView
- 标签云展示所有可用标签
- 点击标签快速筛选
- 搜索结果以卡片形式展示
- 同样支持共享元素动画

**搜索特性**:
- 实时搜索（输入即搜）
- 大小写不敏感
- 多字段匹配

#### 2.6 收藏（Favorite）
**路径**: `ui/favorite/FavoriteFragment`

**功能描述**:
- 集中展示用户收藏的所有诗词
- 卡片式列表，样式与首页一致
- 支持点击查看详情
- 空状态提示

**数据同步**:
- 收藏状态实时同步
- 取消收藏后自动从列表移除
- 数据持久化保存（SharedPreferences）

#### 2.7 关于（About）
**路径**: `ui/about/AboutFragment`

**展示内容**:
1. **应用信息**
   - 应用描述
   - 功能简介

2. **版本与构建**
   - 版本号（versionName）
   - 构建号（versionCode）
   - 构建类型（Debug/Release）
   - 最近更新时间

3. **开发者信息**
   - 开发者头像（Glide 加载）
   - 姓名：Jerry Zhou
   - 座右铭：永远相信美好的事情即将发生

4. **链接**
   - 个人网站
   - 博客
   - GitHub 仓库
   - 网页版入口

5. **反馈渠道**
   - 邮箱：i@jerryz.com.cn（点击复制或打开邮件应用）
   - QQ：2098412009（点击复制或打开 QQ）

### 三、导航架构

#### 3.1 导航模式
- **架构**: 单 Activity + 多 Fragment
- **库**: AndroidX Navigation Component + Safe Args
- **底部导航栏**: BottomNavigationView

#### 3.2 顶级目的地（Top-Level Destinations）
显示底部导航栏的页面：
1. 首页（Home） - `R.id.homeFragment`
2. 收藏（Favorite） - `R.id.favoriteFragment`
3. 搜索（Search） - `R.id.searchFragment`
4. 关于（About） - `R.id.aboutFragment`

#### 3.3 次级页面
不显示底部导航栏的页面：
- 诗词详情（PoemDetail） - `R.id.poemDetailFragment`

#### 3.4 导航特性
- 底部导航栏自动显示/隐藏
- 预测性返回手势（Android 13/14）
- 平滑的页面切换动画
- Safe Args 类型安全的参数传递

### 四、设计与动画

#### 4.1 Material You 特性
- **动态取色**: 从系统壁纸提取主题色
- **自适应主题**: 自动适配浅色/深色模式
- **Material 3 组件**: 
  - MaterialCardView
  - ExtendedFloatingActionButton
  - MaterialButton
  - MaterialToolbar
  - Chip / ChipGroup
  - BottomSheet

#### 4.2 动画系统

**共享元素动画（Shared Element Transition）**:
- 类型：MaterialContainerTransform
- 场景：列表卡片 → 详情页
- 配置：
  - fadeMode = THROUGH
  - fitMode = AUTO
  - scrimColor = TRANSPARENT
  - duration = 375ms（进入）/ 300ms（返回）

**底栏动画**:
- 使用 translationY + alpha 实现平滑显示/隐藏
- 避免直接切换 VISIBLE/GONE 造成的闪烁
- 与共享元素过渡同步

**预测性返回**:
- 支持 Android 13/14 的预测性返回手势
- 返回时显示动画预览
- OnBackInvokedDispatcher 集成

**其他动画**:
- FAB 滚动时收缩/展开
- 触觉反馈（Haptic Feedback）
- 加载动画
- 错误提示动画

#### 4.3 边到边布局（Edge-to-Edge）
- `WindowCompat.setDecorFitsSystemWindows(window, false)`
- 透明状态栏和导航栏
- ViewCompat.setOnApplyWindowInsetsListener 处理 insets
- 内容区域自动适配系统栏高度

### 五、性能优化

#### 5.1 数据加载优化
- 三级缓存：内存 → 本地文件 → 网络
- 惰性加载：按需加载数据
- 协程：异步操作，不阻塞主线程

#### 5.2 列表优化
- RecyclerView.DiffUtil：高效更新
- ViewHolder 模式：视图复用
- 预取（Prefetch）：提前加载视图
- 视图缓存：setItemViewCacheSize(10)

#### 5.3 内存管理
- 及时释放资源（onDestroyView）
- WeakReference 避免内存泄漏
- Glide 图片加载优化

### 六、技术栈详解

#### 6.1 核心技术
```
UI 层:
├── View System (非 Compose)
├── Material Components for Android (M3)
├── CoordinatorLayout + AppBarLayout
└── ConstraintLayout

架构层:
├── MVVM (Model-View-ViewModel)
├── LiveData (响应式数据)
├── ViewModel (数据持有者)
└── Repository 模式

导航:
├── Navigation Component
├── Safe Args Plugin
└── Fragment Transactions

异步:
├── Kotlin Coroutines
├── Dispatchers.IO / Main
└── Flow (未来可能使用)

网络:
├── OkHttp
└── Server-Sent Events (SSE)

数据存储:
├── SharedPreferences (收藏、设置)
└── Internal Storage (诗词数据文件)

工具库:
├── JPinyin (拼音标注)
├── Glide (图片加载)
└── 自定义 PoemParser (文本解析)
```

#### 6.2 关键类说明

**数据层**:
- `Poem.kt`: 诗词数据模型
- `PoemRepository.kt`: 数据仓库（单例）
- `PoemParser.kt`: 文本解析器

**UI 层**:
- `MainActivity.kt`: 主 Activity，管理导航
- `BaseFragment.kt`: Fragment 基类
- 各 Fragment: Home, Detail, Search, Favorite, About, AI Chat, AI Dictation

**网络层**:
- `AiApiClient.kt`: AI API 客户端
- `AiErrorFormatter.kt`: 错误格式化

**工具类**:
- `AnimationUtils.kt`: 动画工具
- `InteractiveTextView.kt`: 支持点击单字的 TextView

### 七、配置与构建

#### 7.1 构建配置
```gradle
android {
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.jerryz.poems"
        minSdk = 31
        targetSdk = 36
        versionCode = 113
        versionName = "1.1.3"
    }
}
```

#### 7.2 AI 功能配置
可选配置文件 `ai.env`:
```properties
AI_BASE_URL=https://one.jerryz.com.cn/v1
AI_API_KEY=your_api_key
AI_MODEL=qwen3-max-preview
AI_BANNED_KEYWORDS=敏感词列表
AI_ERROR_FILTER_DOMAIN=错误过滤域名
```

#### 7.3 依赖版本
- Kotlin: 1.9.24
- Android Gradle Plugin: 8.5.2
- Material Components: 1.12.0
- Navigation: 2.8.9
- OkHttp: 4.9.1
- Lifecycle: 2.8.7

### 八、数据安全与隐私

#### 8.1 隐私保护
- ✅ 不收集用户个人信息
- ✅ 不跟踪用户行为
- ✅ 不展示广告
- ✅ 数据本地存储
- ✅ AI 功能可选使用

#### 8.2 数据存储
- 诗词内容：本地文件（只读）
- 收藏记录：SharedPreferences
- 用户设置：SharedPreferences
- 无用户账号系统
- 无云端同步

### 九、适用场景

#### 9.1 学习场景
- 高中/初中语文考试复习
- 日常诗词背诵
- 语文教学辅助
- 古典文学爱好者阅读

#### 9.2 使用场景
- 通勤路上随机阅读
- 睡前诗词欣赏
- 课堂教学演示
- 考前默写练习

### 十、未来规划

#### 10.1 功能扩展
- [ ] 语音朗读
- [ ] 更详细的诗词赏析
- [ ] 学习进度统计
- [ ] 每日推荐
- [ ] 自定义主题颜色
- [ ] 社区功能（可选）

#### 10.2 技术优化
- [ ] Compose UI 迁移（可选）
- [ ] Room 数据库（替代文件存储）
- [ ] Flow 替代 LiveData
- [ ] 多语言支持
- [ ] 平板优化

---

## 总结

**不背诗词**是一款功能完整、设计精美的古诗词学习应用。它不仅提供了传统的浏览、搜索、收藏功能，还创新性地集成了 AI 解读和 AI 默写功能，让古诗词学习变得更加智能和高效。

应用采用现代化的 Android 开发技术，完整支持 Material You 设计规范，提供了流畅的动画效果和良好的用户体验。无论是学生、教师还是诗词爱好者，都能在这款应用中找到适合自己的使用方式。

---

*文档版本: 1.0*  
*最后更新: 2025年*  
*文档作者: AI Assistant*
