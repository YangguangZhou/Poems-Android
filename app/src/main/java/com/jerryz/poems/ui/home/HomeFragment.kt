package com.jerryz.poems.ui.home

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jerryz.poems.R
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository
import com.jerryz.poems.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Random

// HomeViewModelFactory 定义
class HomeViewModelFactory(private val repository: PoemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// HomeViewModel 增强
class HomeViewModel(private val repository: PoemRepository) : ViewModel() {
    val poems = repository.poems
    val isLoading = repository.isLoading
    val error = repository.error

    suspend fun loadPoems(forceRefresh: Boolean = false) {
        repository.loadPoems(forceRefresh)
    }

    fun getRandomPoem(): Poem? {
        val poemsList = poems.value ?: return null
        if (poemsList.isEmpty()) return null

        val randomIndex = Random().nextInt(poemsList.size)
        return poemsList[randomIndex]
    }
}

// HomeFragment 更新后的代码
class HomeFragment : com.jerryz.poems.ui.base.BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: PoemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { appBar, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            appBar.updatePadding(top = statusBars.top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.appBarLayout)

        val repository = PoemRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, HomeViewModelFactory(repository))
            .get(HomeViewModel::class.java)

        adapter = PoemAdapter { poem -> navigateToPoemDetail(poem) }
        binding.recyclerView.adapter = adapter

        // 配置RecyclerView以获得更流畅的滚动体验
        with(binding.recyclerView) {
            // 启用嵌套滚动以获得更好的滚动性能
            isNestedScrollingEnabled = true
            // 设置更平滑的滚动行为
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                scrollIndicators = View.SCROLL_INDICATOR_TOP or View.SCROLL_INDICATOR_BOTTOM
            }
            // 预取和缓存配置
            setItemViewCacheSize(10)
            setHasFixedSize(false) // 因为卡片高度可能不同
            // 添加默认的item动画
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                // 加快动画速度，使其更响应
                addDuration = 200L
                removeDuration = 200L
                moveDuration = 200L
                changeDuration = 200L
            }
            // 添加自定义装饰器，提供优雅的间距和视觉效果
            addItemDecoration(PoemItemDecoration())
        }

        // 添加滚动监听器以控制FAB的展开/收缩
        var isScrolling = false
        binding.recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                isScrolling = newState != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
                
                // 当停止滚动时，如果在顶部则展开FAB
                if (!isScrolling && !recyclerView.canScrollVertically(-1)) {
                    if (!binding.fabRetry.isExtended) binding.fabRetry.extend()
                    if (!binding.fabRandom.isExtended) binding.fabRandom.extend()
                }
            }
            
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // 增加阈值，避免过于敏感的动画
                val threshold = 8 // 像素阈值
                
                // 向下滚动且超过阈值时收缩FAB
                if (dy > threshold) {
                    if (binding.fabRetry.isExtended) {
                        binding.fabRetry.shrink(object : com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton.OnChangedCallback() {
                            override fun onShrunken(extendedFab: com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton?) {
                                // 动画完成后的回调
                            }
                        })
                    }
                    if (binding.fabRandom.isExtended) {
                        binding.fabRandom.shrink()
                    }
                }
                // 向上滚动且超过阈值时展开FAB
                else if (dy < -threshold) {
                    if (!binding.fabRetry.isExtended) {
                        binding.fabRetry.extend()
                    }
                    if (!binding.fabRandom.isExtended) {
                        binding.fabRandom.extend()
                    }
                }
            }
        })

        // 配置下拉刷新的颜色和动画
        with(binding.swipeRefreshLayout) {
            setColorSchemeColors(
                com.google.android.material.color.MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorPrimary,
                    android.graphics.Color.BLUE
                )
            )
            setProgressBackgroundColorSchemeColor(
                com.google.android.material.color.MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorSurface,
                    android.graphics.Color.WHITE
                )
            )
            // 设置刷新动画的距离
            setDistanceToTriggerSync(150)
            setSlingshotDistance(200)
            
            setOnRefreshListener {
                // 为下拉刷新添加震动反馈
                com.jerryz.poems.util.AnimationUtils.performHapticFeedback(this@with, com.jerryz.poems.util.AnimationUtils.HapticType.MEDIUM)
                loadPoems(true)
            }
        }

        binding.buttonRetry.setOnClickListener { view -> // In error card
            com.jerryz.poems.util.AnimationUtils.animateButtonWithHaptic(view) {
                loadPoems(true)
            }
        }

        binding.fabRetry.setOnClickListener { view ->
            com.jerryz.poems.util.AnimationUtils.animateFabWithHaptic(view) {
                loadPoems(true)
            }
        }

        binding.fabRandom.setOnClickListener { view ->
            val randomPoem = viewModel.getRandomPoem()
            if (randomPoem != null) {
                com.jerryz.poems.util.AnimationUtils.animateFabWithHaptic(view) {
                    navigateToPoemDetail(randomPoem)
                }
            } else {
                Snackbar.make(binding.root, R.string.no_poems_available, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.poems.observe(viewLifecycleOwner) { poems ->
            adapter.submitList(poems)
            if (viewModel.isLoading.value == false) {
                updateUiState()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            if (isLoading) {
                // If loading starts and we don't have any poems yet, hide content views.
                // If we DO have poems (it's a refresh), keep recyclerView visible.
                if (viewModel.poems.value.isNullOrEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.visibility = View.GONE
                    binding.errorCard.visibility = View.GONE
                } else {
                    // Poems exist, this is a refresh. Keep recycler visible.
                    // Ensure other states are hidden.
                    binding.emptyView.visibility = View.GONE
                    binding.errorCard.visibility = View.GONE
                }
            } else {
                // Loading finished. updateUiState will handle final visibility.
                updateUiState()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (viewModel.isLoading.value == false) {
                updateUiState()
                // General error display, might show if poems are present and an error occurs NOT during a manual refresh.
                // Manual refresh errors are handled in loadPoems.
                if (error != null && !viewModel.poems.value.isNullOrEmpty() /*&& !isHandlingManualRefreshErrorCurrently ??? - complex to track */) {
                    // Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    //     .setAnchorView(binding.fabRandom)
                    //     .show() 
                    // To avoid double snackbars, this general one could be more conditional
                    // or removed if loadPoems handles all relevant error snackbars.
                    // For now, let's keep it as is, but be mindful of potential overlap.
                }
            }
        }

        if (!repository.isDataLoaded()) {
            loadPoems()
        } else {
            updateUiState()
        }
    }

    private fun loadPoems(forceRefresh: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadPoems(forceRefresh) // This updates isLoading, poems, error LiveData

            // After viewModel.loadPoems() completes:
            if (forceRefresh) { // Feedback specifically for manual refresh attempts
                val currentError = viewModel.error.value
                if (currentError != null) {
                    // Manual refresh failed
                    Snackbar.make(binding.root, "Refresh failed: $currentError", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.fabRandom) // Anchor to fabRandom or fabRetry
                        .show()
                } else {
                    // Manual refresh succeeded (no error)
                    if (!viewModel.poems.value.isNullOrEmpty()) { // And there's data to show
                        Snackbar.make(binding.root, R.string.refresh_successful, Snackbar.LENGTH_SHORT)
                            .setAnchorView(binding.fabRandom)
                            .show()
                    }
                }
            }
        }
    }

    private fun updateUiState() {
        val poems = viewModel.poems.value
        val error = viewModel.error.value
        val isLoading = viewModel.isLoading.value ?: false

        if (isLoading && viewModel.poems.value.isNullOrEmpty()) { // Only hide all if loading and truly empty initially
             binding.recyclerView.visibility = View.GONE
             binding.emptyView.visibility = View.GONE
             binding.errorCard.visibility = View.GONE
             return // Return early if actively loading initial data
        }
        // If not loading, or if loading but poems are present (refreshing):

        if (error != null && poems.isNullOrEmpty()) {
            binding.errorCard.visibility = View.VISIBLE
            binding.textError.text = error
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.GONE
        } else if (poems.isNullOrEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.errorCard.visibility = View.GONE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.errorCard.visibility = View.GONE
        }
    }

    private fun navigateToPoemDetail(poem: Poem) {
        val action = HomeFragmentDirections.actionHomeFragmentToPoemDetailFragment(poem.id)
        
        // 查找当前点击的卡片视图用于共享元素过渡
        val layoutManager = binding.recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        
        // 在可见范围内查找匹配的卡片
        for (i in firstVisiblePosition..lastVisiblePosition) {
            val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder != null && adapter.currentList[i].id == poem.id) {
                val cardView = viewHolder.itemView
                val extras = androidx.navigation.fragment.FragmentNavigatorExtras(
                    cardView to "poem_card_${poem.id}"
                )
                findNavController().navigate(action, extras)
                return
            }
        }
        
        // 如果没找到对应的视图，则执行普通导航
        findNavController().navigate(action)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}
