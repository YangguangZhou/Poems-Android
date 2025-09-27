package com.jerryz.poems.ui.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.jerryz.poems.R
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository
import com.jerryz.poems.databinding.FragmentFavoriteBinding
import com.jerryz.poems.ui.home.PoemAdapter
import com.google.android.material.snackbar.Snackbar

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
    
    fun getRandomFavoritePoem(): Poem? {
        val favoritePoemsList = _favoritePoems.value ?: return null
        if (favoritePoemsList.isEmpty()) return null
        
        val randomIndex = java.util.Random().nextInt(favoritePoemsList.size)
        return favoritePoemsList[randomIndex]
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.favoriteAppBarLayout) { appBar, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            appBar.updatePadding(top = statusBars.top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.favoriteAppBarLayout)

        // 初始化ViewModel
        val repository = PoemRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, FavoriteViewModelFactory(repository)).get(FavoriteViewModel::class.java)
        
        // 初始化RecyclerView
        adapter = PoemAdapter { poem -> navigateToPoemDetail(poem) }
        binding.recyclerView.adapter = adapter
        
        // 添加滚动监听器以控制FAB的展开/收缩
        binding.recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // 向下滚动时收缩FAB
                if (dy > 0) {
                    if (binding.fabRandom.isExtended) binding.fabRandom.shrink()
                }
                // 向上滚动时展开FAB
                else if (dy < 0) {
                    if (!binding.fabRandom.isExtended) binding.fabRandom.extend()
                }
            }
        })
        
        // 观察收藏诗词
        viewModel.favoritePoems.observe(viewLifecycleOwner) { favoritePoems ->
            adapter.submitList(favoritePoems)
            updateUi(favoritePoems.isEmpty())
        }
        
        // 设置随机按钮点击监听器
        binding.fabRandom.setOnClickListener {
            val randomPoem = viewModel.getRandomFavoritePoem()
            if (randomPoem != null) {
                navigateToPoemDetail(randomPoem)
            } else {
                Snackbar.make(binding.root, R.string.no_favorites, Snackbar.LENGTH_SHORT).show()
            }
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
