package com.debanshu.neatpic

import com.debanshu.neatpic.model.MediaItem
import kotlinx.coroutines.flow.Flow

class MediaRepository(private val imageSource: ImageSource) {
    fun getMediaItems(page: Int, pageSize: Int = 20): Flow<List<MediaItem>> {
        return imageSource.loadMediaItems(page, pageSize)
    }

    suspend fun getTotalCount(): Int {
        return imageSource.getTotalMediaCount()
    }
}