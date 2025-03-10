package com.jerryz.poems.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jerryz.poems.util.PoemParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// 修改为单例模式
class PoemRepository private constructor(private val context: Context) {

    // 单例实现
    companion object {
        @Volatile
        private var INSTANCE: PoemRepository? = null

        fun getInstance(context: Context): PoemRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PoemRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val _poems = MutableLiveData<List<Poem>>()
    val poems: LiveData<List<Poem>> = _poems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val client = OkHttpClient()
    private val parser = PoemParser()

    // 缓存已加载的诗词，提高性能
    private var poemsCache: List<Poem> = emptyList()

    init {
        Log.d("PoemRepository", "Repository实例已创建")
        // 初始化时不调用loadFavorites，因为_poems还是空的
    }

    /**
     * 从网络加载诗词数据
     */
    suspend fun loadPoems(url: String = "https://poems.jerryz.com.cn/poems.txt") {
        _isLoading.value = true
        _error.value = null

        try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("网络请求失败: ${response.code}")
                    }

                    response.body?.byteStream()?.let { inputStream ->
                        val poemsList = parser.parsePoems(inputStream)
                        Log.d("PoemRepository", "成功加载 ${poemsList.size} 首诗词")

                        // 打印几首诗词的ID和内容样本以便调试
                        if (poemsList.isNotEmpty()) {
                            val samplePoem = poemsList.first()
                            Log.d("PoemRepository", "样本诗词: ID=${samplePoem.id}, 标题=${samplePoem.title}, 内容行数=${samplePoem.content.size}")
                        }

                        // 应用收藏状态
                        val favoritesSet = getFavoriteIds()
                        val poemsWithFavorites = poemsList.map { poem ->
                            poem.copy(isFavorite = favoritesSet.contains(poem.id.toString()))
                        }

                        // 更新缓存
                        poemsCache = poemsWithFavorites

                        withContext(Dispatchers.Main) {
                            _poems.value = poemsWithFavorites
                            Log.d("PoemRepository", "诗词数据已更新到LiveData")
                        }
                    } ?: throw IOException("响应体为空")
                }
            }
        } catch (e: Exception) {
            Log.e("PoemRepository", "加载诗词失败", e)
            withContext(Dispatchers.Main) {
                _error.value = "加载失败: ${e.localizedMessage}"
            }
        } finally {
            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
        }
    }

    /**
     * 直接通过ID获取诗词 - 添加此方法便于直接访问
     */
    fun getPoemById(id: Int): Poem? {
        Log.d("PoemRepository", "通过ID获取诗词: $id")

        // 先从缓存中查找
        val cachedPoem = poemsCache.find { it.id == id }
        if (cachedPoem != null) {
            Log.d("PoemRepository", "在缓存中找到诗词: ${cachedPoem.title}")
            return cachedPoem
        }

        // 如果缓存中没有，从LiveData获取
        val poems = _poems.value
        val poem = poems?.find { it.id == id }

        if (poem != null) {
            Log.d("PoemRepository", "在LiveData中找到诗词: ${poem.title}")
        } else {
            Log.d("PoemRepository", "未找到ID为$id 的诗词")
        }

        return poem
    }

    /**
     * 搜索诗词
     */
    fun searchPoems(query: String): List<Poem> {
        val normalizedQuery = query.trim().lowercase()
        return _poems.value?.filter { poem ->
            poem.title.lowercase().contains(normalizedQuery) ||
                    poem.author.lowercase().contains(normalizedQuery) ||
                    poem.content.any { it.lowercase().contains(normalizedQuery) } ||
                    poem.tags.any { it.lowercase().contains(normalizedQuery) } ||
                    poem.translation.any { it.lowercase().contains(normalizedQuery) }
        } ?: emptyList()
    }

    /**
     * 获取收藏的诗词
     */
    fun getFavoritePoems(): List<Poem> {
        return _poems.value?.filter { it.isFavorite } ?: emptyList()
    }

    /**
     * 更新诗词收藏状态
     */
    fun toggleFavorite(poemId: Int) {
        val currentPoems = _poems.value?.toMutableList() ?: return
        val index = currentPoems.indexOfFirst { it.id == poemId }

        if (index != -1) {
            val poem = currentPoems[index]
            val newFavoriteStatus = !poem.isFavorite
            currentPoems[index] = poem.copy(isFavorite = newFavoriteStatus)

            // 更新缓存
            poemsCache = currentPoems.toList()

            // 更新LiveData
            _poems.value = currentPoems

            // 更新SharedPreferences
            saveFavoriteStatus(poemId.toString(), newFavoriteStatus)

            Log.d("PoemRepository", "已更新收藏状态: ID=$poemId, 收藏=${newFavoriteStatus}")
        }
    }

    /**
     * 从SharedPreferences获取收藏IDs
     */
    private fun getFavoriteIds(): Set<String> {
        val sharedPrefs = context.getSharedPreferences("PoemPrefs", Context.MODE_PRIVATE)
        val favorites = sharedPrefs.getStringSet("favorites", emptySet()) ?: emptySet()
        Log.d("PoemRepository", "获取收藏ID列表: $favorites")
        return favorites
    }

    /**
     * 保存收藏状态到SharedPreferences
     */
    private fun saveFavoriteStatus(poemId: String, isFavorite: Boolean) {
        val sharedPrefs = context.getSharedPreferences("PoemPrefs", Context.MODE_PRIVATE)
        val favorites = getFavoriteIds().toMutableSet()

        if (isFavorite) {
            favorites.add(poemId)
        } else {
            favorites.remove(poemId)
        }

        sharedPrefs.edit().putStringSet("favorites", favorites).apply()
        Log.d("PoemRepository", "已保存收藏状态: ID=$poemId, 收藏=$isFavorite")
    }

    /**
     * 检查是否已加载数据
     */
    fun isDataLoaded(): Boolean {
        return _poems.value?.isNotEmpty() == true
    }
}