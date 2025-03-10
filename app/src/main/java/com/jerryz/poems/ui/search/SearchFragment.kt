package com.jerryz.poems.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository
import com.jerryz.poems.databinding.FragmentSearchBinding
import com.jerryz.poems.ui.home.HomeViewModel
import com.jerryz.poems.ui.home.HomeViewModelFactory
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
    
    fun searchPoems(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        _searchResults.value = repository.searchPoems(query)
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
        
        // 初始化ViewModel
        val repository = PoemRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, SearchViewModelFactory(repository)).get(SearchViewModel::class.java)
        
        // 初始化RecyclerView
        adapter = PoemAdapter { poem -> navigateToPoemDetail(poem) }
        binding.recyclerView.adapter = adapter
        
        // 设置搜索视图
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchPoems(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    viewModel.searchPoems("")
                }
                return true
            }
        })
        
        // 观察搜索结果
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            updateUi(results.isEmpty())
        }
    }
    
    private fun updateUi(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty && binding.searchView.query.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        binding.recyclerView.visibility = if (!isEmpty) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        binding.initialStateView.visibility = if (isEmpty && binding.searchView.query.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
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