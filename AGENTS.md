# AGENTS.md

本指南面向 AI 代码代理（和新加入的开发者），用于快速理解并安全地在本项目中实施变更。

## 项目概览

- 名称: 古诗词 Android 应用（View 系统 + Material 3）
- 最低/目标/编译 SDK: minSdk 31, targetSdk 35, compileSdk 35
- 导航: AndroidX Navigation + 单 Activity + 多 Fragment
- 数据源: 远程 txt 数据集，应用内解析与本地缓存
- 主要功能: 列表浏览、搜索、收藏、详情阅读、AI 聊天与听写（底部弹窗）

## 技术栈

- UI: View + Material Components (M3 样式)，CoordinatorLayout + AppBarLayout
- 导航: `androidx.navigation`（Safe Args 已启用）
- 动画: `MaterialContainerTransform`（共享元素卡片展开/收起），底栏自定义淡入/下滑动画
- 手势: Android 13/14 预测性返回（OnBackInvokedDispatcher + Navigation 回退）
- 网络: OkHttp
- 解析: 自定义 `PoemParser`（纯文本格式 → `Poem` 列表）
- 首选项: SharedPreferences（收藏状态、引导提示）

## 目录结构（关键项）

- `app/src/main/java/com/jerryz/poems/MainActivity.kt` — 单 Activity，底部导航，预测性返回与底栏动画协调
- `app/src/main/res/layout/activity_main.xml` — 全屏 NavHost + 覆盖式 BottomNavigationView
- `app/src/main/java/com/jerryz/poems/ui/home` — 列表页（适配共享元素），`PoemAdapter` 设置 `transitionName`
- `app/src/main/java/com/jerryz/poems/ui/detail/PoemDetailFragment.kt` — 详情页（共享元素过渡、边到边布局、拼音弹窗）
- `app/src/main/java/com/jerryz/poems/ui/search` — 搜索页（AppCompat SearchView + 结果列表）
- `app/src/main/java/com/jerryz/poems/ui/favorite` — 收藏页
- `app/src/main/java/com/jerryz/poems/ui/ai` — AI 聊天/听写（底部弹窗，流式/非流式）
- `app/src/main/java/com/jerryz/poems/data/PoemResponsitory.kt` — 数据仓库（网络获取 + 本地文件缓存 + 内存缓存）
- `app/src/main/java/com/jerryz/poems/util/PoemParser.kt` — 文本解析器

## 导航与页面层级

- 顶层目的地（显示底栏）：Home、Favorite、Search、About
  - 维护位置：`MainActivity.topLevelDestinations`
- 详情页（全屏）：`PoemDetailFragment`
  - 进入：从顶部页面点击列表卡片，携带共享元素 `poem_card_{id}`
  - 返回：系统返回/手势返回；支持预测性返回动画

建议：新增顶层 Tab 时，需要同时
- 更新 `bottom_nav_menu`、`MainActivity.topLevelDestinations`
- 确保内容留出底栏空间（`MainActivity.setNavHostBottomPadding(...)` 会在顶层自动处理）

## 动画与手势（关键）

- 共享元素展开（卡片 → 全屏）：
  - 列表项 root 需设置 `ViewCompat.setTransitionName(view, "poem_card_" + poem.id)`
  - 导航时使用 `FragmentNavigatorExtras(sharedView to "poem_card_{id}")`
  - 详情页中：`sharedElementEnterTransition/ReturnTransition` 使用 `MaterialContainerTransform`
    - `fadeMode = THROUGH`，`fitMode = AUTO`，`scrimColor = TRANSPARENT`
  - 底栏与过渡同步：`MainActivity.onDetailEnterTransitionStart()`/`onDetailReturnTransitionStart()`

- 底栏动画策略：
  - 不直接切 `VISIBLE/GONE`，避免闪烁；使用 `translationY + alpha` 平滑隐藏/显示
  - 顶层页面为 `NavHost` 添加底部 padding（底栏高度 + 系统 inset）
  - 详情页清空底部 padding，呈现真正的全屏内容

- 预测性返回（Android 13/14）：
  - `MainActivity` 已接入 `OnBackInvokedDispatcher` 并委托给 `NavController.popBackStack()`
  - 若新增 Fragment，避免拦截返回；如需自定义返回，优先 `onBackPressedDispatcher.addCallback` 并调用 `navigateUp()`

## 数据流与存储

- 远程数据：`https://poems.jerryz.com.cn/poems.txt`
  - 首次从网络获取，保存到内部文件 `filesDir/poems_data.txt`
  - 后续优先本地文件与内存缓存，支持下拉强制刷新
- 解析：`PoemParser.parsePoems(InputStream)` 将文本拆为 `Poem` 列表
- 收藏：SharedPreferences 存储收藏 ID 集，`PoemRepository.toggleFavorite` 即时更新内存与 LiveData
- 搜索/标签：基于内存缓存快速过滤，大小写不敏感

## AI 功能（聊天/听写）

- 入口：详情页右下角 Extended FAB，弹出 `AiChatBottomSheetFragment`
- 客户端：`AiApiClient`
  - 支持流式（SSE）与非流式两种
  - 配置从 BuildConfig 读取，请在本地 `ai.env` 中填入 API Key 与模型等信息
  - 本地开发时请复制 `ai.env.sample` → `ai.env`，并填入 `AI_API_KEY`（已加入 `.gitignore`，不会提交）
- UI：`item_chat_turn.xml` 等适配器，支持追加消息、滚动到底部、加载状态

## 主题与 UI 规范

- 使用 Material 3 色彩与组件；动态取色 `DynamicColors.applyToActivityIfAvailable`
- 边到边：`WindowCompat.setDecorFitsSystemWindows(window, false)`，各 Fragment 通过 inset/padding 适配
- Toolbar/CollapsingToolbar：详情页标题在加载后设置；列表/搜索/收藏/关于页顶部 AppBar 标准化
- 文本与排版：详情页支持字号增减与重置，翻译展示风格使用 `colorSurfaceVariant` 背景与 `onSurfaceVariant`
- 拼音弹窗：`PopupWindow + MaterialCardView`，基于自定义 `InteractiveTextView` 单字命中区域

## 构建与运行

- 依赖与版本见：`gradle/libs.versions.toml`
- 构建命令：`./gradlew :app:assembleDebug`（需要本地 JDK 11）
- 运行前提：Android Studio Giraffe+ / JDK 11 / Android 14 或 13 模拟器（体验预测性返回）

## 常见任务指引

- 新增页面并加入底栏
  - 添加 Fragment、布局与菜单项
  - 更新 `bottom_nav_menu` 与 `MainActivity.topLevelDestinations`
  - 顶层页面无需手动处理底部 inset，Activity 会为 `NavHost` 添加 padding

- 新增从列表到详情的转场
  - 在列表项 root 设置 `transitionName`
  - 导航时传入 `FragmentNavigatorExtras`
  - 详情页如需从指定卡片展开，无需额外代码（已全局配置）

- 优化/扩展搜索
  - 在 `PoemRepository.searchPoems` 中扩展匹配字段或添加拼音/简繁转换
  - 适配 `SearchFragment` 文本监听

- AI 接口替换
  - 在 `AiApiClient` 更换 `baseUrl`、`model` 与鉴权方式
  - 将密钥从硬编码迁移到 `BuildConfig` 或远端代理

## 代码风格与约定

- Kotlin，命名清晰，避免一字母变量
- 保持现有架构：ViewModel 驱动 UI，Fragment 仅持有 UI 逻辑
- 动画优先无闪烁策略：避免在过渡中切换 `GONE`，优先使用属性动画
- 不在 PR 中引入与需求无关的重构或依赖升级

## 性能与可访问性

- 列表项布局扁平化、使用 `RecyclerView` DiffUtil
- 弹窗和 FAB 注意避开系统导航栏（已通过 insets/padding 处理）
- 颜色对比与动态色适配已启用

## 风险与注意事项

- API Key：仓库中存在硬编码密钥，仅供本地调试。请在发布前替换为安全方案。
- 预测性返回：请避免在 Fragment 中屏蔽返回；如需自定义，务必调用 `navigateUp()` 保持导航一致性。
- 共享元素：务必保证 `transitionName` 一致性，否则会回退到无共享元素的普通过渡。
- 文件名：`PoemResponsitory.kt` 为已存在的拼写，实际类名为 `PoemRepository`（请勿随意更名以免影响资源/生成代码）。

---

如需帮助：
- 导航问题：查看 `MainActivity.onDestinationChanged` 与相关布局 inset/padding 处理
- 详情页动画：查看 `PoemDetailFragment` 中的 `MaterialContainerTransform` 配置
- 数据加载问题：查看 `PoemRepository.loadPoems` 与日志
- AI 问题：抓取网络日志，检查 `AiApiClient` 请求体与响应解析
