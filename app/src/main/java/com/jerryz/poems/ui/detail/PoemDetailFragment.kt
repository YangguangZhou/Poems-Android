package com.jerryz.poems.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spannable
import android.text.style.LeadingMarginSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.google.android.material.color.MaterialColors
import com.jerryz.poems.R
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository
import com.jerryz.poems.databinding.FragmentPoemDetailBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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
                isClickable = false
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

            // 创建诗句文本视图
            val contentTextView = TextView(requireContext()).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setLineSpacing(0f, 1.5f)

                // 应用字体大小
                textSize = currentTextSize

                if (isSpecialFormat) {
                    // 词或文言文使用左对齐
                    textAlignment = View.TEXT_ALIGNMENT_TEXT_START

                    // 应用首行缩进（两个中文字符）
                    val spannableString = SpannableString(poem.content[i])
                    // 计算缩进大小（大约两个中文字符宽度）
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
                    text = poem.content[i]
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

        val window = requireActivity().window

        window.navigationBarColor = Color.TRANSPARENT;
        window.statusBarColor = Color.TRANSPARENT

        _binding = null
    }
}