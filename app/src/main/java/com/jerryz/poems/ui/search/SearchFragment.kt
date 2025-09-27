package com.jerryz.poems.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository
import com.jerryz.poems.databinding.FragmentSearchBinding
import com.jerryz.poems.ui.home.PoemAdapter

// SearchViewModelFactory 定义
class SearchViewModelFactory(private val repository: PoemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SearchViewModel(private val repository: PoemRepository) : androidx.lifecycle.ViewModel() {
    
    private val _searchResults = androidx.lifecycle.MutableLiveData<List<Poem>>()
    val searchResults: androidx.lifecycle.LiveData<List<Poem>> = _searchResults
    
    private val _allTags = androidx.lifecycle.MutableLiveData<List<String>>()
    val allTags: androidx.lifecycle.LiveData<List<String>> = _allTags
    
    fun searchPoems(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        _searchResults.value = repository.searchPoems(query)
    }

    fun loadAllTags() {
        _allTags.value = repository.getAllTags()
    }
}

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: PoemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchAppBarLayout) { appBar, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            appBar.updatePadding(top = statusBars.top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.searchAppBarLayout)

        // 初始化ViewModel
        val repository = PoemRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, SearchViewModelFactory(repository)).get(SearchViewModel::class.java)
        
        // 初始化RecyclerView
        adapter = PoemAdapter { poem -> navigateToPoemDetail(poem) }
        binding.recyclerView.adapter = adapter
        
        // 使用 AppCompat SearchView（原样式），输入即搜索
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 支持提交，但不强制；主要用变更回调实时搜索
                query?.let { viewModel.searchPoems(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchPoems(newText.orEmpty())
                return true
            }
        })

        // 调整 SearchView 文本/提示颜色以贴近 M3，并移除内部下划线
        runCatching {
            val editText = binding.searchView.findViewById<android.widget.EditText>(
                androidx.appcompat.R.id.search_src_text
            )
            val onSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, 0)
            val onSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            editText.setTextColor(onSurface)
            editText.setHintTextColor(onSurfaceVariant)
            // 去除内部 plate/submit 区域默认边框或底线
            binding.searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)?.background = null
            binding.searchView.findViewById<View>(androidx.appcompat.R.id.submit_area)?.background = null
            binding.searchView.findViewById<View>(androidx.appcompat.R.id.search_edit_frame)?.background = null
        }
        
        // 观察搜索结果
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            updateUi(results.isEmpty())
        }

        // 加载并显示所有标签（初始状态）
        viewModel.loadAllTags()
        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            displayTagChips(tags)
        }
    }
    
    private fun updateUi(isEmpty: Boolean) {
        binding.emptyCard.visibility = if (isEmpty && currentQuery().isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        binding.recyclerView.visibility = if (!isEmpty) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        binding.initialStateContainer.visibility = if (isEmpty && currentQuery().isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun displayTagChips(tags: List<String>) {
        val group = binding.chipGroupTagsSearch
        group.removeAllViews()
        if (tags.isEmpty()) return

        // 最多显示一定数量的标签，避免过多造成拥挤，可调节
        val maxTagsToShow = 50
        tags.take(maxTagsToShow).forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = false
                isClickable = true
                chipStrokeWidth = 0f

                // 使用主题色，确保明暗主题适配
                val bg = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, 0)
                val fg = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, 0)
                chipBackgroundColor = ColorStateList.valueOf(bg)
                setTextColor(fg)

                setOnClickListener {
                    // 直接以该标签进行搜索
                    binding.searchView.setQuery(tag, false)
                }
            }
            group.addView(chip)
        }
    }

    private fun currentQuery(): CharSequence {
        return binding.searchView.query ?: ""
    }
    
    private fun navigateToPoemDetail(poem: Poem) {
        val action = SearchFragmentDirections.actionSearchFragmentToPoemDetailFragment(poem.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
