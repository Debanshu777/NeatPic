package com.debanshu.neatpic

import com.debanshu.neatpic.model.MediaItem
import kotlinx.coroutines.flow.Flow

expect class ImageSource {
    fun loadMediaItems(page: Int, pageSize: Int = 20): Flow<List<MediaItem>>
}
