package com.debanshu.neatpic

import androidx.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import com.debanshu.neatpic.model.MediaItem
import kotlinx.coroutines.flow.Flow

class MediaRepository(private val imageSource: ImageSource) {
    fun getMediaItems(pageSize: Int = 20): Flow<PagingData<MediaItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize
            ),
            pagingSourceFactory = { MediaPagingSource(imageSource) }
        ).flow
    }
}