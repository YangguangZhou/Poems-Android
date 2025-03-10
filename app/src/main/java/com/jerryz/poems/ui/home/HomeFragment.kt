package com.jerryz.poems.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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

    suspend fun loadPoems() {
        repository.loadPoems()
    }

    fun getRandomPoem(): Poem? {
        val poemsList = poems.value ?: return null
        if (poemsList.isEmpty()) return null

        val randomIndex = Random().nextInt(poemsList.size)
        return poemsList[randomIndex]
    }
}

// HomeFragment 更新后的代码
class HomeFragment : Fragment() {

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

        // 初始化 PoemRepository 和 ViewModel
        val repository = PoemRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, HomeViewModelFactory(repository))
            .get(HomeViewModel::class.java)

        // 初始化 RecyclerView
        adapter = PoemAdapter { poem -> navigateToPoemDetail(poem) }
        binding.recyclerView.adapter = adapter

        // 设置重试按钮
        binding.buttonRetry.setOnClickListener {
            loadPoems()
        }

        // 设置随机阅读诗词
        binding.fabRandom.setOnClickListener {
            val randomPoem = viewModel.getRandomPoem()
            if (randomPoem != null) {
                navigateToPoemDetail(randomPoem)
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.no_poems_available,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        // 观察数据变化
        viewModel.poems.observe(viewLifecycleOwner) { poems ->
            adapter.submitList(poems)
            updateUi(poems.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading && viewModel.poems.value.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (viewModel.poems.value.isNullOrEmpty()) {
                    // 如果没有数据且有错误，则显示错误卡片
                    binding.errorCard.visibility = View.VISIBLE
                    binding.textError.text = error
                } else {
                    // 如果有数据，则显示Snackbar
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.fabRandom)
                        .show()
                }
            } ?: run {
                binding.errorCard.visibility = View.GONE
            }
        }

        // 加载诗词
        if (viewModel.poems.value.isNullOrEmpty()) {
            loadPoems()
        }
    }

    private fun loadPoems() {
        binding.errorCard.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadPoems()
        }
    }

    private fun updateUi(isEmpty: Boolean) {
        binding.recyclerView.visibility = if (!isEmpty) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun navigateToPoemDetail(poem: Poem) {
        val action = HomeFragmentDirections.actionHomeFragmentToPoemDetailFragment(poem.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}