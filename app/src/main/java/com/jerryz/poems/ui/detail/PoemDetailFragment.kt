package com.jerryz.poems.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.stuxuhai.jpinyin.PinyinFormat
import com.google.android.material.color.MaterialColors
import com.jerryz.poems.R
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository
import com.jerryz.poems.databinding.FragmentPoemDetailBinding
import com.google.android.material.snackbar.Snackbar
import com.github.stuxuhai.jpinyin.PinyinHelper
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat

// PoemDetailViewModelFactory 定义
class PoemDetailViewModelFactory(
    private val repository: PoemRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoemDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PoemDetailViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class PoemDetailFragment : Fragment() {

    private var _binding: FragmentPoemDetailBinding? = null
    private val binding get() = _binding!!

    private val args: PoemDetailFragmentArgs by navArgs()
    private lateinit var viewModel: PoemDetailViewModel
    private var currentPoem: Poem? = null
    private var showTranslations = false

    // 拼音弹窗
    private var pinyinPopupWindow: PopupWindow? = null

    // 是否已显示过拼音提示
    private var hasShownPinyinGuide = false

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // 使用Navigation组件进行返回
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 注册自定义返回处理
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        // 设置 Toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 动态设置标题
        binding.collapsingToolbar.title = ""

        Log.d("PoemDetailFragment", "onViewCreated: 参数ID为 ${args.poemId}")

        // 使用全局单例Repository
        val repository = PoemRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, PoemDetailViewModelFactory(repository, requireContext()))
            .get(PoemDetailViewModel::class.java)

        // 设置菜单
        setupMenu()

        // 显示加载指示器
        binding.containerPoemContent.visibility = View.INVISIBLE

        // 如果Repository还没有数据，启动加载
        if (repository.poems.value.isNullOrEmpty()) {
            Log.d("PoemDetailFragment", "Repository没有数据，启动加载")
            lifecycleScope.launch {
                repository.loadPoems()
            }
        }

        // 检查是否已显示过拼音提示
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        hasShownPinyinGuide = prefs.getBoolean("has_shown_pinyin_guide", false)

        // 加载指定ID的诗词
        viewModel.loadPoem(args.poemId)

        // 观察诗词数据变化
        viewModel.poem.observe(viewLifecycleOwner) { poem ->
            if (poem != null) {
                Log.d("PoemDetailFragment", "显示诗词: ${poem.title}")
                displayPoem(poem)
                currentPoem = poem
                binding.containerPoemContent.visibility = View.VISIBLE

                // 设置 CollapsingToolbarLayout 标题
                binding.collapsingToolbar.title = poem.title

                // 如果是第一次显示，展示拼音引导提示
                if (!hasShownPinyinGuide) {
                    showPinyinGuide()
                }
            } else {
                Log.e("PoemDetailFragment", "无法显示诗词，数据为空")
                Snackbar.make(
                    binding.root,
                    "未找到诗词",
                    Snackbar.LENGTH_LONG
                ).setAnchorView(binding.fabFavorite).show()
            }
        }

        // 设置收藏按钮，使用 ExtendedFAB 的展开/收缩功能
        binding.fabFavorite.setOnClickListener {
            currentPoem?.let {
                viewModel.toggleFavorite(it.id)
            }
        }

        // 监听滚动以收缩/展开 FAB
        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY && binding.fabFavorite.isExtended) {
                    // 向下滚动时收缩
                    binding.fabFavorite.shrink()
                } else if (scrollY < oldScrollY && !binding.fabFavorite.isExtended && scrollY == 0) {
                    // 滚动到顶部时展开
                    binding.fabFavorite.extend()
                }
            }
        )

        // 设置翻译开关
        binding.switchTranslation.setOnCheckedChangeListener { _, isChecked ->
            showTranslations = isChecked
            currentPoem?.let { displayPoemContent(it) }
        }

        // 设置字体大小调整按钮
        binding.btnDecreaseTextSize.setOnClickListener {
            viewModel.decreaseTextSize()
        }

        binding.btnIncreaseTextSize.setOnClickListener {
            viewModel.increaseTextSize()
        }

        binding.btnResetTextSize.setOnClickListener {
            viewModel.resetTextSize()
        }

        // 观察字体大小变化
        viewModel.textSize.observe(viewLifecycleOwner) { newSize ->
            // 刷新诗词显示以应用新字体大小
            currentPoem?.let { displayPoemContent(it) }
        }

        // 应用边缘到边缘的内容布局
        applyEdgeToEdgeContent()
    }

    private fun showPinyinGuide() {
        Snackbar.make(
            binding.root,
            "提示：点击诗句中的单字可查看拼字形字音（仅供参考）",
            Snackbar.LENGTH_LONG
        ).setAction("我知道了") {
            // 标记已显示过提示
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            prefs.edit().putBoolean("has_shown_pinyin_guide", true).apply()
            hasShownPinyinGuide = true
        }.setAnchorView(binding.fabFavorite).show()
    }

    private fun applyEdgeToEdgeContent() {
        // 确保内容不被状态栏遮挡
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // 为AppBar添加顶部内边距，避免被状态栏遮挡
            binding.appBarLayout.updatePadding(top = insets.top)

            // 确保底部内容不被导航栏遮挡
            binding.nestedScrollView.updatePadding(bottom = insets.bottom)

            // 调整FAB的位置，避免被导航栏遮挡
            (binding.fabFavorite.layoutParams as ViewGroup.MarginLayoutParams).apply {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.fab_margin)
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.poem_detail_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        // 处理返回按钮点击
                        onBackPressedCallback.handleOnBackPressed()
                        true
                    }
                    R.id.action_share -> {
                        sharePoem()
                        true
                    }
                    R.id.action_copy -> {
                        copyPoemToClipboard()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun displayPoem(poem: Poem) {
        binding.textTitle.text = poem.title
        binding.textAuthorDynasty.text = poem.author

        // 显示标签
        displayTags(poem.tags)

        // 显示诗词内容和翻译
        displayPoemContent(poem)

        // 更新收藏按钮状态
        updateFavoriteButton(poem.isFavorite)
    }

    private fun displayTags(tags: List<String>) {
        binding.chipGroupTags.removeAllViews()

        tags.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = tag
                isCheckable = false
                isClickable = false
                chipStrokeWidth = 0f

                // 使用 Material 3 颜色，与首页/搜索页一致
                val bg = com.google.android.material.color.MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    0
                )
                val fg = com.google.android.material.color.MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSecondaryContainer,
                    0
                )
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(bg)
                setTextColor(fg)

                textSize = 12f
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun displayPoemContent(poem: Poem) {
        binding.containerPoemContent.removeAllViews()

        // 确定是否显示翻译开关
        binding.switchTranslation.visibility =
            if (poem.translation.isNotEmpty()) View.VISIBLE else View.GONE

        // 判断是否需要特殊格式（词或文言文使用左对齐和首行缩进）
        val isSpecialFormat = poem.tags.any { it.contains("词") || it.contains("文言文") }

        // 获取当前字体大小
        val currentTextSize = viewModel.getTextSize()

        // 添加每行诗词内容和翻译
        for (i in poem.content.indices) {
            val lineContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 24)  // 增加行间距
                }
            }

            // 创建诗句文本视图 - 使用字符级点击处理
            val contentTextView = InteractiveTextView(requireContext()).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setLineSpacing(0f, 1.5f)

                // 设置文本和调整文本大小
                setText(poem.content[i], isSpecialFormat, currentTextSize)

                // 设置点击监听，显示单字拼音
                setOnCharacterTouchListener { char, x, y, charBounds ->
                    showSingleCharPinyin(char.toString(), this, x, y)
                }
            }
            lineContainer.addView(contentTextView)

            // 如果启用了翻译并且该行有翻译，则添加翻译
            if (showTranslations && i < poem.translation.size) {
                val translationTextView = TextView(requireContext()).apply {
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)

                    // 使用 Material You 颜色来突出显示翻译文本
                    // 使用 tertiaryContainer 的颜色作为背景，给翻译一个轻微的背景色
                    val bgColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, Color.TRANSPARENT)
                    // 给背景添加一个浅色背景，同时增加圆角
                    background = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                        context,
                        R.drawable.translation_background
                    )?.mutate()?.apply {
                        setTint(bgColor)
                    }

                    // 使用 onTertiaryContainer 的颜色作为文本颜色，保证与背景对比度
                    setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK))

                    setLineSpacing(0f, 1.2f)
                    val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.translation_padding_horizontal)
                    val paddingVertical = resources.getDimensionPixelSize(R.dimen.translation_padding_vertical)
                    setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

                    // 应用字体大小
                    textSize = currentTextSize

                    if (isSpecialFormat) {
                        // 词或文言文的翻译也使用左对齐和首行缩进
                        textAlignment = View.TEXT_ALIGNMENT_TEXT_START

                        // 应用首行缩进
                        val spannableString = SpannableString(poem.translation[i])
                        val indentSize = (textSize * 2).toInt()
                        spannableString.setSpan(
                            LeadingMarginSpan.Standard(indentSize, 0),
                            0, spannableString.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        text = spannableString
                    } else {
                        // 默认使用居中对齐
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        text = poem.translation[i]
                    }
                }
                lineContainer.addView(translationTextView)
            }

            binding.containerPoemContent.addView(lineContainer)
        }
    }

    private fun showSingleCharPinyin(character: String, anchorView: View, x: Float, y: Float) {
        // 关闭之前的拼音弹窗
        pinyinPopupWindow?.dismiss()

        // 将单个汉字转换为拼音
        val pinyin = if (character.trim().isNotEmpty()) {
            try {
                PinyinHelper.convertToPinyinString(character, "", PinyinFormat.WITH_TONE_MARK).trim()
            } catch (e: Exception) {
                // 如果转换出错，比如标点符号等，就显示原字符
                character
            }
        } else {
            character
        }

        // 创建弹窗视图
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_pinyin, null)
        val cardView = popupView.findViewById<MaterialCardView>(R.id.cardViewPinyin)
        val textViewCharacter = popupView.findViewById<TextView>(R.id.textViewCharacter)
        val textViewPinyin = popupView.findViewById<TextView>(R.id.textViewPinyin)

        // 设置原字符和拼音文本
        textViewCharacter.text = character
        textViewPinyin.text = pinyin

        // 设置文本颜色
        val textColor = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorOnSurface,
            Color.BLACK
        )
        textViewCharacter.setTextColor(textColor)
        textViewPinyin.setTextColor(textColor)

        // 设置卡片背景颜色
        val backgroundColor = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorSurface,
            Color.WHITE
        )
        cardView.setCardBackgroundColor(backgroundColor)

        // 测量视图大小
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // 创建并显示弹窗
        pinyinPopupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // 设置为可获取焦点，使其能够接收触摸事件
        ).apply {
            elevation = resources.getDimension(R.dimen.popup_elevation)

            // 设置点击外部区域关闭弹窗
            setOnDismissListener {
                pinyinPopupWindow = null
            }

            // 获取弹窗大小
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight

            // 显示在点击位置的上方 - 考虑到屏幕边界
            val screenWidth = resources.displayMetrics.widthPixels

            // 计算x坐标，确保弹窗不会超出屏幕边界
            var xPosition = x - popupWidth / 2
            if (xPosition < 0) xPosition = 0f
            if (xPosition + popupWidth > screenWidth) xPosition = screenWidth - popupWidth.toFloat()

            // 计算y坐标，确保弹窗显示在字符上方，增加更大的垂直偏移
            val extraOffset = resources.getDimensionPixelSize(R.dimen.popup_extra_vertical_offset)
            val yPosition = y - popupHeight - resources.getDimensionPixelSize(R.dimen.popup_vertical_offset) - extraOffset

            // 在anchorView上显示弹窗
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)

            // 设置最终位置
            showAtLocation(
                anchorView,
                android.view.Gravity.NO_GRAVITY,
                (location[0] + xPosition).toInt(),
                (location[1] + yPosition).toInt()
            )
        }
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        binding.fabFavorite.apply {
            icon = AppCompatResources.getDrawable(
                requireContext(),
                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
            )

            // 根据状态设置文本
            text = if (isFavorite) "已收藏" else "收藏"

            // 根据收藏状态更改颜色（可选）
            if (isFavorite) {
                backgroundTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiaryContainer, Color.TRANSPARENT)
                )
                iconTint = ColorStateList.valueOf(
                    MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnTertiaryContainer, Color.WHITE)
                )
            } else {
                backgroundTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSecondaryContainer, Color.TRANSPARENT)
                )
                iconTint = ColorStateList.valueOf(
                    MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSecondaryContainer, Color.WHITE)
                )
            }
        }
    }

    private fun sharePoem() {
        currentPoem?.let { poem ->
            val shareText = buildString {
                append("https://poems.jerryz.com.cn/")
                append(poem.title)
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, null))
        }
    }

    private fun copyPoemToClipboard() {
        currentPoem?.let { poem ->
            val shareText = buildString {
                append("《${poem.title}》\n")
                append("${poem.author}\n\n")
                append(poem.content.joinToString("\n"))
            }

            val clipboard = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("诗词", shareText)
            clipboard.setPrimaryClip(clip)

            Snackbar.make(
                binding.root,
                R.string.copied_to_clipboard,
                Snackbar.LENGTH_SHORT
            ).setAnchorView(binding.fabFavorite).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // 移除自定义返回处理
        onBackPressedCallback.remove()

        // 关闭拼音弹窗
        pinyinPopupWindow?.dismiss()
        pinyinPopupWindow = null

        val window = requireActivity().window

        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT

        _binding = null
    }

    /**
     * 交互式文本视图，支持单个字符的点击和视觉反馈
     */
    private inner class InteractiveTextView(context: Context) : androidx.appcompat.widget.AppCompatTextView(context) {

        // 存储每个字符的边界矩形
        private val charRects = mutableListOf<RectF>()
        // 原始文本内容
        private var originalText = ""
        // 当前点击的字符索引
        private var touchedCharIndex = -1
        // 触摸反馈动画计时
        private val handler = Handler(Looper.getMainLooper())
        // 字符触摸监听器
        private var characterTouchListener: ((Char, Float, Float, RectF) -> Unit)? = null

        // 背景颜色
        private val rippleColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorControlHighlight,
            ContextCompat.getColor(context, android.R.color.darker_gray)
        )

        // 绘制用的画笔
        private val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = rippleColor
            alpha = 0 // 初始透明
        }

        init {
            // 确保能接收触摸事件
            isClickable = true
            isFocusable = true
        }

        /**
         * 设置文本内容，同时处理特殊格式
         */
        fun setText(text: String, isSpecialFormat: Boolean, textSizeValue: Float) {
            originalText = text
            textSize = textSizeValue

            if (isSpecialFormat) {
                // 词或文言文使用左对齐
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                // 同时设置 gravity 以便 layout 计算时生效
                gravity = android.view.Gravity.START
                // 应用首行缩进
                val spannableString = SpannableString(text)
                val indentSize = (textSize * 2).toInt()
                spannableString.setSpan(
                    LeadingMarginSpan.Standard(indentSize, 0),
                    0, spannableString.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                this.text = spannableString
            } else {
                // 默认使用居中对齐
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                this.text = text
            }

            // 重新计算字符边界，在布局完成后调用
            post { calculateCharacterBounds() }
        }

        /**
         * 设置字符触摸监听器
         */
        fun setOnCharacterTouchListener(listener: (Char, Float, Float, RectF) -> Unit) {
            characterTouchListener = listener
        }

        /**
         * 计算每个字符的边界，支持多行文本
         */
        private fun calculateCharacterBounds() {
            charRects.clear()
            if (originalText.isEmpty()) return

            val layout = layout
            if (layout != null && layout.lineCount > 0) {
                for (i in originalText.indices) {
                    // 获取当前字符所在的行号
                    val line = layout.getLineForOffset(i)
                    val lineTop = layout.getLineTop(line)
                    val lineBottom = layout.getLineBottom(line)
                    // 当前字符起始位置
                    val startX = layout.getPrimaryHorizontal(i)
                    // 如果下一个字符在同一行，使用其位置；否则使用本行的右边界
                    val endX = if (i < originalText.length - 1 && layout.getLineForOffset(i + 1) == line)
                        layout.getPrimaryHorizontal(i + 1)
                    else
                        layout.getLineRight(line)
                    // 为了更容易点击，加一些 padding
                    val padding = textSize * 0.15f
                    val rect = RectF(
                        startX - padding,
                        lineTop - padding,
                        endX + padding,
                        lineBottom + padding
                    )
                    charRects.add(rect)
                }
            } else {
                // 若 layout 不可用，仍使用简单算法计算（可能仅适用于单行）
                val textWidth = paint.measureText(originalText)
                val contentWidth = width - paddingLeft - paddingRight
                val startX = when (gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK) {
                    android.view.Gravity.CENTER_HORIZONTAL -> paddingLeft + (contentWidth - textWidth) / 2f
                    android.view.Gravity.END, android.view.Gravity.RIGHT -> paddingLeft + contentWidth - textWidth
                    else -> paddingLeft.toFloat()
                }
                var currentX = startX
                for (i in originalText.indices) {
                    val char = originalText[i]
                    val charWidth = paint.measureText(char.toString())
                    val padding = textSize * 0.15f
                    val top = paddingTop.toFloat()
                    val bottom = top + textSize
                    val rect = RectF(
                        currentX - padding,
                        top - padding,
                        currentX + charWidth + padding,
                        bottom + padding
                    )
                    charRects.add(rect)
                    currentX += charWidth
                }
            }
        }


        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            // 大小变化时重新计算字符边界
            post { calculateCharacterBounds() }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (touchedCharIndex >= 0 && touchedCharIndex < charRects.size) {
                // 复制原始区域，避免直接修改
                val rect = RectF(charRects[touchedCharIndex])
                // 调整 inset 值，可以根据需要调整，比如这里用 textSize * 0.1f
                val inset = textSize * 0.1f
                rect.inset(inset, inset)
                // 绘制缩小后的点击反馈区域
                canvas.drawRoundRect(rect, textSize * 0.2f, textSize * 0.2f, backgroundPaint)
            }
        }


        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val touchX = event.x
                    val touchY = event.y

                    touchedCharIndex = -1

                    for (i in charRects.indices) {
                        val rect = charRects[i]
                        if (rect.contains(touchX, touchY)) {
                            touchedCharIndex = i
                            backgroundPaint.alpha = 80
                            invalidate()
                            return true
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (touchedCharIndex >= 0 && touchedCharIndex < originalText.length) {
                        val char = originalText[touchedCharIndex]
                        val rect = charRects[touchedCharIndex]
                        characterTouchListener?.invoke(
                            char,
                            rect.centerX(),
                            rect.top, // 使用顶部位置，使弹窗显示在字符上方
                            rect
                        )
                        handler.postDelayed({
                            backgroundPaint.alpha = 0
                            invalidate()
                        }, 100)
                    }
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    backgroundPaint.alpha = 0
                    invalidate()
                    touchedCharIndex = -1
                }
            }
            return super.onTouchEvent(event)
        }
    }
}
