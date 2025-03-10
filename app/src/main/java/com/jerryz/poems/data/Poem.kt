package com.jerryz.poems.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poems")
data class Poem(
    @PrimaryKey val id: Int,
    val title: String,
    val author: String,
    val tags: List<String> = emptyList(),
    val content: List<String>,
    val translation: List<String> = emptyList(),
    val isFavorite: Boolean = false
)