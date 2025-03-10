package com.jerryz.poems.ui.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository
import com.jerryz.poems.databinding.FragmentFavoriteBinding
import com.jerryz.poems.ui.home.PoemAdapter

// FavoriteViewModel 定义
class FavoriteViewModelFactory(private val repository: PoemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class FavoriteViewModel(private val repository: PoemRepository) : androidx.lifecycle.ViewModel() {
    
    private val _favoritePoems = androidx.lifecycle.MutableLiveData<List<Poem>>()
    val favoritePoems: androidx.lifecycle.LiveData<List<Poem>> = _favoritePoems
    
    fun loadFavoritePoems() {
        _favoritePoems.value = repository.getFavoritePoems()
    }
    
    // 监听诗词数据变化，更新收藏列表
    init {
        repository.poems.observeForever {
            loadFavoritePoems()
        }
    }
}

class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: FavoriteViewModel
    private lateinit var adapter: PoemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化ViewModel
        val repository = PoemRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, FavoriteViewModelFactory(repository)).get(FavoriteViewModel::class.java)
        
        // 初始化RecyclerView
        adapter = PoemAdapter { poem -> navigateToPoemDetail(poem) }
        binding.recyclerView.adapter = adapter
        
        // 观察收藏诗词
        viewModel.favoritePoems.observe(viewLifecycleOwner) { favoritePoems ->
            adapter.submitList(favoritePoems)
            updateUi(favoritePoems.isEmpty())
        }
        
        // 加载收藏诗词
        viewModel.loadFavoritePoems()
    }
    
    private fun updateUi(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun navigateToPoemDetail(poem: Poem) {
        val action = FavoriteFragmentDirections.actionFavoriteFragmentToPoemDetailFragment(poem.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}