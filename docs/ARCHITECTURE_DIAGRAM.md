# 不背诗词 - 应用架构与功能图解

## 应用结构总览

```
┌─────────────────────────────────────────────────────────────┐
│                    不背诗词 Android App                      │
│                Material You Design                          │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   数据层       │   │   业务层       │   │   UI 层        │
│  Data Layer   │   │ Business Layer│   │  UI Layer     │
└───────────────┘   └───────────────┘   └───────────────┘
```

## 一、架构分层

### 1.1 数据层（Data Layer）

```
数据源
├── 远程数据
│   └── https://poems.jerryz.com.cn/poems.txt
│
├── 本地文件缓存
│   └── filesDir/poems_data.txt
│
├── 内存缓存
│   └── List<Poem> in PoemRepository
│
└── 持久化存储
    └── SharedPreferences (收藏、设置)
```

**数据流向**:
```
网络 → 本地文件 → 内存缓存 → UI
  ↓
首次加载或强制刷新时
```

### 1.2 业务层（Business Layer）

```
Repository (数据仓库)
└── PoemRepository
    ├── loadPoems()          // 加载诗词
    ├── searchPoems()        // 搜索诗词
    ├── toggleFavorite()     // 切换收藏
    └── getFavoritePoems()   // 获取收藏列表

Parser (解析器)
└── PoemParser
    └── parsePoems()         // 解析文本数据

AI Client (AI 客户端)
└── AiApiClient
    ├── chat()               // AI 对话
    └── chatStream()         // AI 流式对话
```

### 1.3 UI 层（UI Layer）

```
MainActivity (单 Activity)
├── NavHostFragment (导航容器)
│   └── Fragments (多 Fragment)
└── BottomNavigationView (底部导航)
```

## 二、页面导航流程

```
启动应用
    │
    ▼
┌─────────────────────────────────────────────────────┐
│              MainActivity                            │
│  ┌───────────────────────────────────────────────┐ │
│  │         NavHostFragment                       │ │
│  │  ┌─────────────────────────────────────────┐ │ │
│  │  │                                         │ │ │
│  │  │  顶级目的地 (显示底部导航栏)            │ │ │
│  │  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │ │ │
│  │  │  │ 首页 │ │ 收藏 │ │ 搜索 │ │ 关于 │  │ │ │
│  │  │  └──────┘ └──────┘ └──────┘ └──────┘  │ │ │
│  │  │                                         │ │ │
│  │  │  次级页面 (全屏，隐藏底部导航)          │ │ │
│  │  │  ┌──────────────┐                     │ │ │
│  │  │  │  诗词详情页   │                     │ │ │
│  │  │  │  ┌────────┐  │                     │ │ │
│  │  │  │  │AI 功能 │  │ (BottomSheet)      │ │ │
│  │  │  │  └────────┘  │                     │ │ │
│  │  │  └──────────────┘                     │ │ │
│  │  └─────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────┐ │
│  │    BottomNavigationView (底部导航栏)          │ │
│  │  [首页] [收藏] [搜索] [关于]                   │ │
│  └───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

## 三、功能模块详解

### 3.1 首页（Home）功能流程

```
┌──────────────┐
│   HomeFragment│
└───────┬──────┘
        │
        ├── 启动时
        │   └→ ViewModel.loadPoems()
        │       └→ Repository.loadPoems()
        │           ├→ 检查内存缓存
        │           ├→ 检查本地文件
        │           └→ 网络请求（如需要）
        │
        ├── 用户操作
        │   ├→ 下拉刷新
        │   │   └→ loadPoems(forceRefresh=true)
        │   │
        │   ├→ 点击"随机"按钮
        │   │   └→ getRandomPoem() → 导航到详情
        │   │
        │   └→ 点击诗词卡片
        │       └→ 共享元素动画 → 导航到详情
        │
        └── 数据更新
            └→ LiveData.observe()
                └→ Adapter.submitList()
                    └→ UI 更新
```

### 3.2 诗词详情（Detail）功能树

```
PoemDetailFragment
├── 内容展示
│   ├── Toolbar (标题、返回、菜单)
│   ├── 标签 Chips
│   ├── 正文内容 (可点击查看拼音)
│   └── 翻译内容
│
├── 交互功能
│   ├── 拼音标注
│   │   └→ 点击汉字 → PopupWindow 显示拼音
│   │
│   ├── 字号调节
│   │   ├→ 增大字号
│   │   ├→ 减小字号
│   │   └→ 重置字号
│   │
│   ├── 收藏功能 (FAB)
│   │   └→ toggleFavorite()
│   │       └→ SharedPreferences 更新
│   │
│   ├── 分享功能 (菜单)
│   │   └→ 生成链接 → Intent.ACTION_SEND
│   │
│   └── 复制功能 (菜单)
│       └→ 复制到剪贴板 → Snackbar 提示
│
└── AI 功能 (FAB)
    └→ 点击 → BottomSheet 弹出
        ├→ AI 解读模式
        └→ AI 默写模式
```

### 3.3 AI 功能详细流程

#### AI 解读（Chat）

```
用户操作流程:
┌─────────────┐
│  详情页面    │
└──────┬──────┘
       │ 点击 AI FAB
       ▼
┌─────────────────────────┐
│ AiChatBottomSheetFragment│
└──────┬──────────────────┘
       │
       ├→ 显示对话历史
       │   └→ RecyclerView + TurnAdapter
       │
       ├→ 用户输入问题
       │   └→ EditText 输入框
       │
       ├→ 点击发送
       │   └→ ViewModel.sendMessage()
       │       └→ Repository.chat()
       │           └→ AiApiClient.chatStream()
       │               ├→ 建立 SSE 连接
       │               ├→ 流式接收数据
       │               └→ 实时更新 UI
       │
       └→ 显示回答
           ├→ 打字机效果
           ├→ 长按复制
           └→ 可删除消息
```

#### AI 默写（Dictation）

```
默写流程:
┌────────────┐
│ 点击生成题目│
└─────┬──────┘
      │
      ├→ AI 生成默写题
      │   └→ 返回: {遮蔽的句子索引}
      │
      ├→ 显示题目
      │   ├→ 显示未遮蔽的句子
      │   └→ 遮蔽句只显示标点提示
      │
      ├→ 用户填写答案
      │   └→ EditText 输入框 (多个)
      │
      ├→ 点击检查
      │   └→ ViewModel.checkAnswer()
      │       ├→ 去除标点
      │       ├→ 逐字对比
      │       └→ 计算错误
      │
      └→ 显示结果
          ├→ ✓ 正确
          ├→ ✗ 错别字 X 处
          ├→ 整句错误 (显示正确答案)
          └→ 解析说明
```

### 3.4 搜索功能流程

```
SearchFragment
├── 初始状态
│   ├→ 显示搜索框
│   └→ 显示标签云
│       └→ 点击标签 → 自动填入搜索框
│
├── 搜索输入
│   └→ TextWatcher 实时监听
│       └→ ViewModel.search(query)
│           └→ Repository.searchPoems(query)
│               ├→ 标题匹配
│               ├→ 作者匹配
│               ├→ 朝代匹配
│               ├→ 标签匹配
│               └→ 内容匹配
│
└── 搜索结果
    ├→ 卡片列表显示
    ├→ 点击卡片 → 共享元素动画
    └→ 无结果 → 空状态提示
```

### 3.5 收藏管理流程

```
收藏功能
├── 添加收藏
│   └→ 详情页 FAB → toggleFavorite()
│       ├→ SharedPreferences.edit()
│       ├→ 更新内存缓存
│       └→ LiveData.postValue()
│
├── 收藏列表
│   └→ FavoriteFragment
│       ├→ 观察 favoritePoems LiveData
│       ├→ 过滤 isFavorite = true
│       └→ 显示卡片列表
│
└── 取消收藏
    └→ 详情页 FAB → toggleFavorite()
        └→ 自动从收藏列表移除
```

## 四、动画系统

### 4.1 共享元素动画

```
列表卡片 → 详情页
┌──────────┐         ┌──────────────┐
│ CardView │  展开    │  详情页面     │
│  诗词卡片 │ ──────→ │  (全屏显示)   │
│ [id=X]   │         │  [id=X]      │
└──────────┘         └──────────────┘
     │                      │
     │ MaterialContainer   │
     │ Transform动画        │
     │                      │
     └──────────────────────┘

配置:
- fadeMode: THROUGH
- fitMode: AUTO
- scrimColor: TRANSPARENT
- duration: 375ms (进入) / 300ms (返回)
```

### 4.2 底栏动画

```
顶级页面 ↔ 详情页

┌──────────────┐     ┌──────────────┐
│    内容区     │     │    内容区     │
│              │     │   (全屏)      │
├──────────────┤     │              │
│ BottomNav    │     │              │
│  (显示)       │     │              │
└──────────────┘     └──────────────┘
       ↓                     ↓
   translationY=0        translationY=height
   alpha=1.0             alpha=0.0
```

## 五、数据流图

```
应用启动
    │
    ▼
┌─────────────────┐
│ MainActivity    │
│  onCreate()     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ NavController   │
│  navigate to    │
│  HomeFragment   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ HomeViewModel   │
│  loadPoems()    │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ PoemRepository                      │
│  ├→ 检查内存缓存 (poemsCache)       │
│  ├→ 检查本地文件 (poems_data.txt)   │
│  └→ 网络请求 (poems.txt)            │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────┐
│ PoemParser      │
│  parsePoems()   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ LiveData        │
│  _poems.post()  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ HomeFragment    │
│  observe()      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ PoemAdapter     │
│  submitList()   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ RecyclerView    │
│  显示列表        │
└─────────────────┘
```

## 六、技术栈关系图

```
┌──────────────────────────────────────────────────┐
│                 Kotlin                            │
└──────────────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  Android    │ │  Jetpack    │ │  Material   │
│  Framework  │ │  Components │ │  Components │
├─────────────┤ ├─────────────┤ ├─────────────┤
│ • Activity  │ │ • Navigation│ │ • CardView  │
│ • Fragment  │ │ • LiveData  │ │ • FAB       │
│ • View      │ │ • ViewModel │ │ • BottomNav │
│ • Intent    │ │ • Lifecycle │ │ • Toolbar   │
└─────────────┘ └─────────────┘ │ • Chip      │
                                │ • BottomSheet
                                └─────────────┘
        │             │             │
        └─────────────┴─────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  Coroutines │ │   OkHttp    │ │   Glide     │
│  (异步)      │ │   (网络)     │ │   (图片)     │
└─────────────┘ └─────────────┘ └─────────────┘
```

## 七、AI 功能架构

```
┌────────────────────────────────────────┐
│         AI 功能总体架构                 │
└────────────────────────────────────────┘

诗词详情页
    │
    ▼
AI FAB 点击
    │
    ├─→ AI 解读模式
    │   │
    │   ├── UI: AiChatBottomSheetFragment
    │   ├── ViewModel: AiChatViewModel
    │   ├── Repository: ChatRepository
    │   └── API: AiApiClient.chatStream()
    │       │
    │       └→ SSE 流式响应
    │           ├→ data: [DONE]
    │           ├→ delta: 内容片段
    │           └→ 实时更新 UI
    │
    └─→ AI 默写模式
        │
        ├── UI: AiDictationBottomSheetFragment
        ├── ViewModel: AiDictationViewModel
        ├── Repository: DictationRepository
        └── API: AiApiClient.chat()
            │
            └→ 生成题目
                ├→ 遮蔽句子索引
                ├→ 用户填写答案
                ├→ 批改结果
                └→ 解析说明
```

## 八、存储架构

```
┌──────────────────────────────────────┐
│            存储层级                   │
└──────────────────────────────────────┘

运行时 (RAM)
├── poemsCache: List<Poem>
└── LiveData<List<Poem>>
       │
       ▼
持久化 (Internal Storage)
├── filesDir/poems_data.txt
│   └→ 诗词文本数据
│
└── SharedPreferences
    ├→ 收藏的诗词 ID 集合
    ├→ 用户设置（字号等）
    └→ 引导提示状态

远程 (Network)
└── https://poems.jerryz.com.cn/poems.txt
    └→ 诗词数据源
```

---

## 总结

本文档详细展示了"不背诗词"应用的完整架构和功能流程。通过这些图解，可以清晰地了解：

1. **分层架构** - 数据层、业务层、UI 层的职责分离
2. **导航流程** - 单 Activity 多 Fragment 的导航设计
3. **功能模块** - 每个功能的详细实现流程
4. **动画系统** - 共享元素动画和底栏动画的协调
5. **数据流** - 从网络到 UI 的完整数据链路
6. **技术栈** - 各种技术的组合和依赖关系
7. **AI 功能** - 对话和默写的完整实现
8. **存储架构** - 多级缓存的设计思路

希望这些图解能帮助开发者快速理解应用的整体结构和技术实现！

---

*文档版本: 1.0*  
*最后更新: 2025年*
