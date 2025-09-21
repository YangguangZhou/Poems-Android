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
import com.jerryz.poems.databinding.FragmentAiDictationBottomSheetBinding

class AiDictationBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAiDictationBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val poemId: Int by lazy { requireArguments().getInt(ARG_ID) }
    private val title: String by lazy { requireArguments().getString(ARG_TITLE, "") }
    private val author: String by lazy { requireArguments().getString(ARG_AUTHOR, "") }
    private val content: String by lazy { requireArguments().getString(ARG_CONTENT, "") }
    private val translation: String by lazy { requireArguments().getString(ARG_TRANSLATION, "") }

    private val vm: AiDictationViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AiDictationViewModel(requireContext(), poemId, title, author, content, translation) as T
            }
        }
    }

    private lateinit var adapter: DictationAdapter

    override fun getTheme(): Int = R.style.ThemeOverlay_M3_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiDictationBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.title = getString(R.string.ai_dictation)
        binding.subtitle.text = title

        adapter = DictationAdapter(
            onTextChanged = { index, text -> vm.updateUserInput(index, text) },
            onCheck = { index ->
                hideKeyboard()
                vm.checkAnswer(index)
            }
        )
        val lm = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = lm
        binding.recyclerView.adapter = adapter

        vm.questions.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        vm.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.visibility = if (loading == true) View.VISIBLE else View.GONE
            binding.buttonGenerate.isEnabled = loading != true
            binding.buttonGenerate.text = if (loading == true) getString(R.string.generating) else getString(R.string.generate_another_set)
        }

        binding.buttonGenerate.setOnClickListener {
            vm.generateNewSet(5)
        }

        binding.topAppBar.setNavigationOnClickListener { dismiss() }

        // Auto-generate on open
        vm.generateNewSet(5)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val sheetView = dialog.findViewById<android.widget.FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        val surfaceColor = com.google.android.material.color.MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorSurfaceContainerHigh,
            0
        )
        sheetView?.background = MaterialShapeDrawable(
            ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(resources.getDimension(R.dimen.bottom_sheet_corner))
                .setTopRightCornerSize(resources.getDimension(R.dimen.bottom_sheet_corner))
                .setBottomLeftCornerSize(0f)
                .setBottomRightCornerSize(0f)
                .build()
        ).apply {
            fillColor = android.content.res.ColorStateList.valueOf(surfaceColor)
            initializeElevationOverlay(requireContext())
        }

        val dragId = resources.getIdentifier("drag_handle", "id", "com.google.android.material")
        if (dragId != 0) {
            dialog.findViewById<View>(dragId)?.backgroundTintList =
                android.content.res.ColorStateList.valueOf(surfaceColor)
        }
        val altId = resources.getIdentifier("bottom_sheet_drag_handle", "id", "com.google.android.material")
        if (altId != 0) {
            dialog.findViewById<View>(altId)?.backgroundTintList =
                android.content.res.ColorStateList.valueOf(surfaceColor)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.recyclerView.windowToken, 0)
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
            AiDictationBottomSheetFragment().apply {
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
