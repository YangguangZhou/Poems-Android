package com.jerryz.poems.ui.detail

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jerryz.poems.data.Poem
import com.jerryz.poems.data.PoemRepository

class PoemDetailViewModel(private val repository: PoemRepository, private val context: Context) : ViewModel() {

    private val _poem = MediatorLiveData<Poem?>()
    val poem: LiveData<Poem?> = _poem

    // 字体大小常量
    private val DEFAULT_TEXT_SIZE = 16f // 默认字体大小（sp）
    private val MIN_TEXT_SIZE = 12f // 最小字体大小
    private val MAX_TEXT_SIZE = 24f // 最大字体大小
    private val TEXT_SIZE_STEP = 2f // 每次调整的步长

    // 字体大小LiveData
    private val _textSize = MutableLiveData<Float>()
    val textSize: LiveData<Float> = _textSize

    init {
        // 从偏好设置初始化字体大小
        _textSize.value = getStoredTextSize()

        // 直接在初始化时设置观察源
        _poem.addSource(repository.poems) { poemsList ->
            Log.d("PoemDetailVM", "诗词列表更新，共 ${poemsList?.size ?: 0} 首诗")
        }
    }

    // 字体大小管理方法
    private fun getStoredTextSize(): Float {
        val prefs = context.getSharedPreferences("poem_preferences", Context.MODE_PRIVATE)
        return prefs.getFloat("text_size", DEFAULT_TEXT_SIZE)
    }

    private fun saveTextSize(size: Float) {
        val prefs = context.getSharedPreferences("poem_preferences", Context.MODE_PRIVATE)
        prefs.edit().putFloat("text_size", size).apply()
    }

    fun getTextSize(): Float {
        return _textSize.value ?: DEFAULT_TEXT_SIZE
    }

    fun increaseTextSize() {
        val currentSize = _textSize.value ?: DEFAULT_TEXT_SIZE
        val newSize = minOf(currentSize + TEXT_SIZE_STEP, MAX_TEXT_SIZE)
        if (newSize != currentSize) {
            _textSize.value = newSize
            saveTextSize(newSize)
        }
    }

    fun decreaseTextSize() {
        val currentSize = _textSize.value ?: DEFAULT_TEXT_SIZE
        val newSize = maxOf(currentSize - TEXT_SIZE_STEP, MIN_TEXT_SIZE)
        if (newSize != currentSize) {
            _textSize.value = newSize
            saveTextSize(newSize)
        }
    }

    fun resetTextSize() {
        if (_textSize.value != DEFAULT_TEXT_SIZE) {
            _textSize.value = DEFAULT_TEXT_SIZE
            saveTextSize(DEFAULT_TEXT_SIZE)
        }
    }

    // 加载特定ID的诗词
    fun loadPoem(id: Int) {
        Log.d("PoemDetailVM", "开始加载ID为 $id 的诗词")

        // 使用现有诗词列表查找
        val poemsList = repository.poems.value
        if (!poemsList.isNullOrEmpty()) {
            val foundPoem = poemsList.find { it.id == id }
            if (foundPoem != null) {
                Log.d("PoemDetailVM", "找到诗词: ${foundPoem.title}")
                _poem.value = foundPoem
            } else {
                Log.e("PoemDetailVM", "未找到ID为 $id 的诗词")
                _poem.value = null
            }
        } else {
            // 如果Repository没有数据，则观察数据变化
            Log.d("PoemDetailVM", "等待Repository加载数据...")
        }

        // 更新观察者逻辑
        (_poem as MediatorLiveData<Poem?>).apply {
            // 如果已有源，先移除
            removeSource(repository.poems)

            // 添加新的观察源
            addSource(repository.poems) { poems ->
                Log.d("PoemDetailVM", "收到更新，寻找ID=$id 的诗词")
                val foundPoem = poems?.find { it.id == id }
                if (foundPoem != null) {
                    Log.d("PoemDetailVM", "找到诗词: ${foundPoem.title}")
                    value = foundPoem
                } else {
                    Log.e("PoemDetailVM", "仍未找到ID为 $id 的诗词")
                }
            }
        }
    }

    fun toggleFavorite(poemId: Int) {
        repository.toggleFavorite(poemId)
    }

    override fun onCleared() {
        super.onCleared()
        (_poem as? MediatorLiveData<*>)?.let { mediator ->
            mediator.removeSource(repository.poems)
        }
    }
}