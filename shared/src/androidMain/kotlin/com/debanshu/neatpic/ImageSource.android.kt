package com.debanshu.neatpic

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.debanshu.neatpic.model.MediaItem
import com.debanshu.neatpic.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

actual class ImageSource(private val context: Context) {
    actual fun loadMediaItems(page: Int, pageSize: Int): Flow<List<MediaItem>> = flow {
        // Validate input parameters
        if (page < 0) throw MediaLoaderException.InvalidPageException(page)
        if (pageSize <= 0) throw IllegalArgumentException("Page size must be positive")

        // Check permissions first
        if (!hasRequiredPermissions()) {
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
                MediaStore.MediaColumns.SIZE, // Additional field to check if file exists
                MediaStore.MediaColumns.MIME_TYPE
            )

            // Try to load images first
            tryQueryMedia(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                offset,
                pageSize,
                MediaType.IMAGE,
                mediaItems
            )

            // If we haven't filled the page with images, add videos
            if (mediaItems.size < pageSize) {
                val remainingItems = pageSize - mediaItems.size
                tryQueryMedia(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    offset,
                    remainingItems,
                    MediaType.VIDEO,
                    mediaItems
                )
            }

            // If no media items found after both queries
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
            val selection = "${MediaStore.MediaColumns.SIZE} > 0" // Only get existing files
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            context.contentResolver.query(
                contentUri,
                projection,
                selection,
                null,
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
        // Move cursor to the offset position
        if (offset > 0 && !cursor.moveToPosition(offset)) {
            // If we can't move to offset, cursor is too small
            return
        }

        var itemsRetrieved = 0
        do {
            if (itemsRetrieved >= limit) break

            try {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                val dateAdded =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                val mimeType =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))

                // Skip items with invalid mime types
                if (mimeType == null || !isValidMimeType(mimeType, type)) {
                    continue
                }

                val uri = ContentUris.withAppendedId(contentUri, id)

                // Verify the file exists and is readable
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
            } catch (e: IllegalArgumentException) {
                // Skip items with missing columns
                continue
            }
        } while (cursor.moveToNext())
    }

    private fun isMediaAccessible(uri: android.net.Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidMimeType(mimeType: String, type: MediaType): Boolean {
        return when (type) {
            MediaType.IMAGE -> mimeType.startsWith("image/")
            MediaType.VIDEO -> mimeType.startsWith("video/")
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(android.Manifest.permission.READ_MEDIA_IMAGES) &&
                    checkPermission(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun getTotalMediaCount(): Int {
        if (!hasRequiredPermissions()) {
            throw MediaLoaderException.PermissionDeniedException()
        }

        var totalCount = 0
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.SIZE} > 0",
                null,
                null
            )?.use { cursor ->
                totalCount += cursor.count
            }

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.SIZE} > 0",
                null,
                null
            )?.use { cursor ->
                totalCount += cursor.count
            }
        } catch (e: Exception) {
            throw MediaLoaderException.MediaAccessException(e)
        }

        return totalCount
    }
}