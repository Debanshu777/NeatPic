package com.debanshu.neatpic.model

data class MediaItem(
    val id: String,
    val uri: String,
    val type: MediaType,
    val dateAdded: Long,
    val name: String,
    val mimeType: String
)
