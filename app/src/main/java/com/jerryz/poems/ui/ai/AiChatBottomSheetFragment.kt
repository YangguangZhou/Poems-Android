package com.jerryz.poems.ui.ai

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.jerryz.poems.R
import com.jerryz.poems.databinding.FragmentAiChatBottomSheetBinding

class AiChatBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAiChatBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val poemId: Int by lazy { requireArguments().getInt(ARG_ID) }
    private val title: String by lazy { requireArguments().getString(ARG_TITLE, "") }
    private val author: String by lazy { requireArguments().getString(ARG_AUTHOR, "") }
    private val content: String by lazy { requireArguments().getString(ARG_CONTENT, "") }
    private val translation: String by lazy { requireArguments().getString(ARG_TRANSLATION, "") }

    private val vm: AiChatViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AiChatViewModel(requireContext(), poemId, title, author, content, translation) as T
            }
        }
    }

    private lateinit var adapter: TurnAdapter
    private var autoScrollBottom = true

    override fun getTheme(): Int = R.style.ThemeOverlay_M3_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiChatBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 添加预测性返回手势支持
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        dismiss()
                    }
                }
            )
        }

        binding.topAppBar.title = getString(R.string.ai_features)
        binding.subtitle.text = title

        adapter = TurnAdapter(
            onDeleteTurn = { turn ->
                vm.deleteTurn(turn)
            },
            onLongPressCopy = { text ->
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("message", text))
                com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.copied_to_clipboard, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        )
        val lm = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = lm
        binding.recyclerView.adapter = adapter
        (binding.recyclerView.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                val last = lm.findLastCompletelyVisibleItemPosition()
                autoScrollBottom = last >= (adapter.itemCount - 1)
            }
        })

        vm.turns.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            if (autoScrollBottom) {
                binding.recyclerView.post { binding.recyclerView.scrollToPosition((list.size - 1).coerceAtLeast(0)) }
            }
        }

        binding.buttonSend.setOnClickListener { view ->
            if (vm.isStreaming.value == true) {
                // 停止流式输出时使用轻微震动
                com.jerryz.poems.util.AnimationUtils.performHapticFeedback(view, com.jerryz.poems.util.AnimationUtils.HapticType.LIGHT)
                vm.stopStreaming()
            } else {
                val text = binding.inputEditText.text?.toString() ?: ""
                if (text.isNotBlank()) {
                    // 发送消息时使用中等强度震动和动画
                    com.jerryz.poems.util.AnimationUtils.animateButtonWithHaptic(view) {
                        vm.sendMessage(text)
                        binding.inputEditText.setText("")
                        hideKeyboard()
                        autoScrollBottom = true
                    }
                } else {
                    // 空内容时的轻微提示
                    com.jerryz.poems.util.AnimationUtils.performHapticFeedback(view, com.jerryz.poems.util.AnimationUtils.HapticType.LIGHT)
                }
            }
        }

        vm.isStreaming.observe(viewLifecycleOwner) { streaming ->
            if (streaming == true) {
                binding.buttonSend.setIconResource(R.drawable.ic_stop)
                binding.buttonSend.contentDescription = getString(R.string.stop)
                binding.inputEditText.isEnabled = false
            } else {
                binding.buttonSend.setIconResource(R.drawable.ic_send)
                binding.buttonSend.contentDescription = getString(R.string.send)
                binding.inputEditText.isEnabled = true
            }
        }

        vm.outputCompletionTick.observe(viewLifecycleOwner) {
            com.jerryz.poems.util.AnimationUtils.performHapticFeedback(
                binding.recyclerView,
                com.jerryz.poems.util.AnimationUtils.HapticType.LIGHT
            )
        }

        binding.topAppBar.setNavigationOnClickListener { view ->
            com.jerryz.poems.util.AnimationUtils.performHapticFeedback(view, com.jerryz.poems.util.AnimationUtils.HapticType.LIGHT)
            dismiss()
        }

        // Toggle buttons: 解读 / 默写
        showChat(true)

        binding.toggleButtonGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                // 为切换按钮添加震动反馈
                val targetButton = group.findViewById<View>(checkedId)
                targetButton?.let { view ->
                    com.jerryz.poems.util.AnimationUtils.performHapticFeedback(view, com.jerryz.poems.util.AnimationUtils.HapticType.LIGHT)
                }
                
                when (checkedId) {
                    R.id.button_insights -> showChat(true)
                    R.id.button_dictation -> showChat(false)
                }
            }
        }

        // Dictation setup
        setupDictation()
    }

    override fun onStart() {
        super.onStart()
        // Ensure rounded top corners on the BottomSheet
        val dialog = dialog as? BottomSheetDialog ?: return
        val sheetView = dialog.findViewById<android.widget.FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        sheetView?.background = MaterialShapeDrawable(
            ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(resources.getDimension(com.jerryz.poems.R.dimen.bottom_sheet_corner))
                .setTopRightCornerSize(resources.getDimension(com.jerryz.poems.R.dimen.bottom_sheet_corner))
                .setBottomLeftCornerSize(0f)
                .setBottomRightCornerSize(0f)
                .build()
        ).apply {
            this.fillColor = android.content.res.ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, 0)
            )
        }

        // Unify drag handle color with sheet background
        val dragId = resources.getIdentifier("drag_handle", "id", "com.google.android.material")
        if (dragId != 0) {
            val handle = dialog.findViewById<View>(dragId)
            handle?.backgroundTintList = android.content.res.ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, 0)
            )
        }
        // Fallback possible id name
        val altId = resources.getIdentifier("bottom_sheet_drag_handle", "id", "com.google.android.material")
        if (altId != 0) {
            val handle = dialog.findViewById<View>(altId)
            handle?.backgroundTintList = android.content.res.ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, 0)
            )
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.inputEditText.windowToken, 0)
    }

    private fun showChat(show: Boolean) {
        binding.containerChat.visibility = if (show) View.VISIBLE else View.GONE
        binding.containerDictation.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupDictation() {
        val dictAdapter = DictationAdapter(
            onTextChanged = { index, text -> dictVm.updateUserInput(index, text) },
            onCheck = { index ->
                hideKeyboard()
                val result = dictVm.checkAnswer(index)
                // 根据检查结果立即触发震动反馈
                when (result) {
                    is CheckResult.Correct -> {
                        com.jerryz.poems.util.AnimationUtils.performHapticFeedback(binding.recyclerViewDictation, com.jerryz.poems.util.AnimationUtils.HapticType.SUCCESS)
                    }
                    is CheckResult.Typos -> {
                        com.jerryz.poems.util.AnimationUtils.performHapticFeedback(binding.recyclerViewDictation, com.jerryz.poems.util.AnimationUtils.HapticType.PARTIAL_ERROR)
                    }
                    is CheckResult.WholeWrong -> {
                        com.jerryz.poems.util.AnimationUtils.performHapticFeedback(binding.recyclerViewDictation, com.jerryz.poems.util.AnimationUtils.HapticType.TRIPLE_ERROR)
                    }
                    null -> { /* 无结果时不震动 */ }
                }
            },
            onCheckResult = { result, view ->
                // 震动已移至 onCheck 回调中，这里保留以防需要其他处理
            }
        )
        val lm2 = LinearLayoutManager(requireContext())
        binding.recyclerViewDictation.layoutManager = lm2
        val footer = DictationFooterAdapter { dictVm.generateNewSet(3) }
        val concat = androidx.recyclerview.widget.ConcatAdapter(dictAdapter, footer)
        binding.recyclerViewDictation.adapter = concat

        dictVm.questions.observe(viewLifecycleOwner) { list ->
            dictAdapter.submitList(list)
            footer.setState(dictVm.loading.value == true, !list.isNullOrEmpty())
        }
        dictVm.loading.observe(viewLifecycleOwner) { loading ->
            footer.setState(loading == true, !(dictVm.questions.value.isNullOrEmpty()))
        }
        dictVm.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                com.google.android.material.snackbar.Snackbar.make(binding.root, err, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private val dictVm: AiDictationViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AiDictationViewModel(requireContext(), poemId, title, author, content, translation) as T
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_AUTHOR = "author"
        private const val ARG_CONTENT = "content"
        private const val ARG_TRANSLATION = "translation"

        fun newInstance(poemId: Int, title: String, author: String, content: String, translation: String) =
            AiChatBottomSheetFragment().apply {
                arguments = bundleOf(
                    ARG_ID to poemId,
                    ARG_TITLE to title,
                    ARG_AUTHOR to author,
                    ARG_CONTENT to content,
                    ARG_TRANSLATION to translation
                )
            }
    }
}
