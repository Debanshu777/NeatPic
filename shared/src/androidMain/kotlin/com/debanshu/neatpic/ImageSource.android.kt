package com.debanshu.neatpic

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.debanshu.neatpic.model.MediaItem
import com.debanshu.neatpic.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

actual class ImageSource(private val context: Context) {
    private val permissionHandler = PermissionHandler(context)
    private val imageContentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore
        .Images.Media
        .getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media
        .EXTERNAL_CONTENT_URI
    private val videoContentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore
        .Video.Media
        .getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media
        .EXTERNAL_CONTENT_URI

    actual fun loadMediaItems(page: Int, pageSize: Int): Flow<List<MediaItem>> = flow {
        if (page < 0) throw MediaLoaderException.InvalidPageException(page)
        if (pageSize <= 0) throw IllegalArgumentException("Page size must be positive")

        if (!permissionHandler.hasRequiredPermissions()) {
            throw MediaLoaderException.PermissionDeniedException()
        }

        try {
            val mediaItems = loadMediaItemsInternal(page, pageSize)
            emit(mediaItems)
        } catch (e: Exception) {
            when (e) {
                is MediaLoaderException -> throw e
                else -> throw MediaLoaderException.MediaAccessException(e)
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun loadMediaItemsInternal(page: Int, pageSize: Int): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val mediaItems = mutableListOf<MediaItem>()
            val offset = page * pageSize

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE
            )

            tryQueryMedia(
                imageContentUri,
                projection,
                offset,
                pageSize,
                MediaType.IMAGE,
                mediaItems
            )

            if (mediaItems.size < pageSize) {
                val remainingItems = pageSize - mediaItems.size
                tryQueryMedia(
                    videoContentUri,
                    projection,
                    offset,
                    remainingItems,
                    MediaType.VIDEO,
                    mediaItems
                )
            }

            if (mediaItems.isEmpty() && page == 0) {
                throw MediaLoaderException.NoMediaFoundException()
            }

            return@withContext mediaItems
        }

    private fun tryQueryMedia(
        contentUri: android.net.Uri,
        projection: Array<String>,
        offset: Int,
        limit: Int,
        type: MediaType,
        mediaItems: MutableList<MediaItem>
    ) {
        try {
            val selection = StringBuilder().apply {
                append("${MediaStore.MediaColumns.SIZE} > 0")
                append(" AND ")
                append("${MediaStore.MediaColumns.MIME_TYPE} LIKE ?")
            }.toString()

            val selectionArgs = arrayOf(
                when (type) {
                    MediaType.IMAGE -> "image/%"
                    MediaType.VIDEO -> "video/%"
                }
            )

            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            context.contentResolver.query(
                contentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                handleCursor(cursor, offset, limit, type, contentUri, mediaItems)
            } ?: throw MediaLoaderException.CursorAccessException(Exception("Cursor is null"))
        } catch (e: Exception) {
            when (e) {
                is MediaLoaderException -> throw e
                is SecurityException -> throw MediaLoaderException.PermissionDeniedException()
                else -> throw MediaLoaderException.MediaAccessException(e)
            }
        }
    }

    private fun handleCursor(
        cursor: Cursor,
        offset: Int,
        limit: Int,
        type: MediaType,
        contentUri: android.net.Uri,
        mediaItems: MutableList<MediaItem>
    ) {
        try {
            if (cursor.count == 0) {
                return
            }

            if (!cursor.moveToFirst()) {
                return
            }

            cursor.moveToPosition(offset)

            if (cursor.isAfterLast) {
                return
            }

            var itemsRetrieved = 0
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            do {
                try {
                    if (itemsRetrieved >= limit) {
                        break
                    }
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)

                    if (name != null && mimeType != null && isValidMimeType(mimeType, type)) {
                        val uri = ContentUris.withAppendedId(contentUri, id)
                        if (isMediaAccessible(uri)) {
                            mediaItems.add(
                                MediaItem(
                                    id = id.toString(),
                                    uri = uri.toString(),
                                    type = type,
                                    dateAdded = dateAdded,
                                    name = name,
                                    mimeType = mimeType
                                )
                            )
                            itemsRetrieved++
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    println("Error processing media item: ${e.message}")
                    continue
                } catch (e: Exception) {
                    println("Unexpected error processing media item: ${e.message}")
                    continue
                }
            } while (cursor.moveToNext() && itemsRetrieved < limit)
        } catch (e: Exception) {
            println("Error handling cursor: ${e.message}")
            throw MediaLoaderException.CursorAccessException(e)
        }
    }

    private fun isMediaAccessible(uri: android.net.Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidMimeType(mimeType: String, type: MediaType): Boolean {
        return when (type) {
            MediaType.IMAGE -> mimeType.startsWith("image/")
            MediaType.VIDEO -> mimeType.startsWith("video/")
        }
    }
}