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
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 诗词数据仓库 (单例模式)
 *
 * 优化逻辑：
 * 1. 首次从网络加载数据并持久化到本地文件。
 * 2. 后续优先从本地文件加载，实现离线访问和快速启动。
 * 3. 提供强制刷新功能，用于下拉刷新等场景。
 */
class PoemRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: PoemRepository? = null

        // 文件名常量
        private const val POEMS_FILE_NAME = "poems_data.txt"

        fun getInstance(context: Context): PoemRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PoemRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // LiveData 用于观察数据变化
    private val _poems = MutableLiveData<List<Poem>>()
    val poems: LiveData<List<Poem>> = _poems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // 网络和解析工具
    private val client = OkHttpClient()
    private val parser = PoemParser()

    // 内存缓存，提高访问性能
    private var poemsCache: List<Poem> = emptyList()

    init {
        Log.d("PoemRepository", "Repository instance created.")
    }

    /**
     * 加载诗词数据。
     * 优先从本地文件加载，如果文件不存在或强制刷新，则从网络获取。
     * @param forceRefresh 是否强制从网络刷新数据，默认为 false。
     */
    suspend fun loadPoems(forceRefresh: Boolean = false) {
        _isLoading.postValue(true)
        _error.postValue(null)

        withContext(Dispatchers.IO) {
            // 如果不强制刷新，并且数据已在内存中，则直接使用内存缓存
            if (!forceRefresh && poemsCache.isNotEmpty()) {
                Log.d("PoemRepository", "Data already in memory cache. Skipping load.")
                _isLoading.postValue(false)
                return@withContext
            }

            // 如果不强制刷新，尝试从本地文件加载
            val localFile = File(context.filesDir, POEMS_FILE_NAME)
            if (!forceRefresh && localFile.exists()) {
                Log.d("PoemRepository", "Loading poems from local file.")
                try {
                    localFile.inputStream().use { inputStream ->
                        processAndUpdatePoems(inputStream)
                    }
                } catch (e: Exception) {
                    Log.e("PoemRepository", "Failed to load from local file, will try network.", e)
                    // 如果本地加载失败，则尝试从网络加载
                    fetchFromNetwork()
                }
            } else {
                // 如果需要强制刷新或本地文件不存在，则从网络加载
                Log.d("PoemRepository", "Fetching poems from network. ForceRefresh: $forceRefresh")
                fetchFromNetwork()
            }
        }
    }

    /**
     * 从网络获取数据，并保存到本地文件。
     */
    private suspend fun fetchFromNetwork(url: String = "https://poems.jerryz.com.cn/poems.txt") {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Network request failed: ${response.code}")
                }

                val responseBody = response.body ?: throw IOException("Response body is null")

                // 将网络数据流保存到本地文件
                val file = File(context.filesDir, POEMS_FILE_NAME)
                responseBody.byteStream().use { inputStream ->
                    file.outputStream().use { fileOutputStream ->
                        inputStream.copyTo(fileOutputStream)
                    }
                }
                Log.d("PoemRepository", "Poems saved to local file: ${file.absolutePath}")

                // 从新保存的文件中读取并处理数据
                file.inputStream().use { savedInputStream ->
                    processAndUpdatePoems(savedInputStream)
                }
            }
        } catch (e: Exception) {
            Log.e("PoemRepository", "Failed to load poems from network", e)
            _error.postValue("加载失败: ${e.localizedMessage}")
        } finally {
            _isLoading.postValue(false)
        }
    }

    /**
     * 从输入流解析诗词，应用收藏状态，并更新 LiveData 和缓存。
     * @param inputStream 包含诗词数据的输入流。
     */
    private suspend fun processAndUpdatePoems(inputStream: InputStream) {
        val poemsList = parser.parsePoems(inputStream)
        Log.d("PoemRepository", "Successfully parsed ${poemsList.size} poems.")

        // 应用收藏状态
        val favoritesSet = getFavoriteIds()
        val poemsWithFavorites = poemsList.map { poem ->
            poem.copy(isFavorite = favoritesSet.contains(poem.id.toString()))
        }

        // 更新内存缓存
        poemsCache = poemsWithFavorites

        // 切换到主线程更新 LiveData
        withContext(Dispatchers.Main) {
            _poems.value = poemsWithFavorites
            _isLoading.value = false
            Log.d("PoemRepository", "Poems data updated to LiveData.")
        }
    }


    /**
     * 直接通过ID获取诗词 - 添加此方法便于直接访问
     */
    fun getPoemById(id: Int): Poem? {
        Log.d("PoemRepository", "Getting poem by ID: $id")
        // 优先从内存缓存中查找
        return poemsCache.find { it.id == id }
    }

    /**
     * 搜索诗词
     */
    fun searchPoems(query: String): List<Poem> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.trim().lowercase()
        // 从内存缓存中搜索
        return poemsCache.filter { poem ->
            poem.title.lowercase().contains(normalizedQuery) ||
                    poem.author.lowercase().contains(normalizedQuery) ||
                    poem.content.any { it.lowercase().contains(normalizedQuery) } ||
                    poem.tags.any { it.lowercase().contains(normalizedQuery) } ||
                    poem.translation.any { it.lowercase().contains(normalizedQuery) }
        }
    }

    /**
     * 获取所有诗词标签
     */
    fun getAllTags(): List<String> {
        // 从内存缓存中提取所有标签并去重
        return poemsCache.flatMap { it.tags }.distinct().sorted()
    }

    /**
     * 获取收藏的诗词
     */
    fun getFavoritePoems(): List<Poem> {
        // 从内存缓存中过滤
        return poemsCache.filter { it.isFavorite }
    }

    /**
     * 更新诗词收藏状态
     */
    fun toggleFavorite(poemId: Int) {
        val index = poemsCache.indexOfFirst { it.id == poemId }

        if (index != -1) {
            val poem = poemsCache[index]
            val newFavoriteStatus = !poem.isFavorite
            val updatedPoem = poem.copy(isFavorite = newFavoriteStatus)

            // 更新内存缓存
            val mutableCache = poemsCache.toMutableList()
            mutableCache[index] = updatedPoem
            poemsCache = mutableCache.toList()

            // 更新LiveData
            _poems.postValue(poemsCache)

            // 持久化收藏状态
            saveFavoriteStatus(poemId.toString(), newFavoriteStatus)
            Log.d("PoemRepository", "Toggled favorite status: ID=$poemId, isFavorite=$newFavoriteStatus")
        }
    }

    /**
     * 从SharedPreferences获取收藏IDs
     */
    private fun getFavoriteIds(): Set<String> {
        val sharedPrefs = context.getSharedPreferences("PoemPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getStringSet("favorites", emptySet()) ?: emptySet()
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
    }

    /**
     * 检查是否已加载数据
     */
    fun isDataLoaded(): Boolean {
        return poemsCache.isNotEmpty()
    }
}
