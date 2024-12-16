package com.debanshu.neatpic

import com.debanshu.neatpic.model.MediaItem
import com.debanshu.neatpic.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSSortDescriptor
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaSubtypeNone
import platform.Photos.PHAssetMediaSubtypePhotoHDR
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHAssetSourceTypeUserLibrary
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHCachingImageManager
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHVideoRequestOptions
import platform.Photos.PHVideoRequestOptionsDeliveryModeHighQualityFormat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class ImageSource {
    private val imageManager = PHImageManager.defaultManager()
    private val cachingImageManager = PHCachingImageManager.defaultManager()

    init {
        cachingImageManager
    }

    actual fun loadMediaItems(page: Int, pageSize: Int): Flow<List<MediaItem>> = flow {
        if (page < 0) throw MediaLoaderException.InvalidPageException(page)
        if (pageSize <= 0) throw IllegalArgumentException("Page size must be positive")

        when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusNotDetermined -> {
                throw MediaLoaderException.InitializationException()
            }

            PHAuthorizationStatusRestricted, PHAuthorizationStatusDenied -> {
                throw MediaLoaderException.PermissionDeniedException()
            }

            else -> { /* Authorized, continue */
            }
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
    }.flowOn(Dispatchers.Default)

    private fun loadMediaItemsInternal(page: Int, pageSize: Int): List<MediaItem> {
        val startIndex = page * pageSize
        val mediaItems = mutableListOf<MediaItem>()

        val fetchOptions = PHFetchOptions().apply {
            sortDescriptors = listOf(
                NSSortDescriptor.sortDescriptorWithKey(
                    key = "creationDate",
                    ascending = false
                )
            )
            includeHiddenAssets = false
            includeAllBurstAssets = false
        }

        val fetchResult = PHAsset.fetchAssetsWithOptions(fetchOptions)

        if (fetchResult.count.toInt() == 0) {
            if (page == 0) throw MediaLoaderException.NoMediaFoundException()
            return emptyList()
        }

        if (startIndex >= fetchResult.count.toInt()) {
            throw MediaLoaderException.InvalidPageException(page)
        }

        val endIndex = minOf(startIndex + pageSize, fetchResult.count.toInt())

        for (i in startIndex until endIndex) {
            fetchResult.objectAtIndex(i.toULong())?.let { asset ->
                asset as PHAsset
                try {
                    processAsset(asset)?.let { mediaItems.add(it) }
                } catch (e: Exception) {
                    println("Error processing asset: ${e.message}")
                }
            }
        }

        return mediaItems
    }

    private fun processAsset(asset: PHAsset): MediaItem? {
        if (!asset.isReadyForPlayback()) {
            return null
        }

        val type = when {
            asset.mediaType == PHAssetMediaTypeImage -> MediaType.IMAGE
            asset.mediaType == PHAssetMediaTypeVideo -> MediaType.VIDEO
            else -> return null
        }

        val filename = asset.localIdentifier
        val mimeType = getMimeType(asset)

        return MediaItem(
            id = asset.localIdentifier,
            uri = asset.localIdentifier,
            type = type,
            dateAdded = asset.creationDate?.timeIntervalSince1970?.toLong()
                ?: (NSDate().timeIntervalSince1970.toLong()),
            name = filename,
            mimeType = mimeType
        )
    }

    private fun getMimeType(asset: PHAsset): String {
        return when {
            asset.mediaType == PHAssetMediaTypeImage -> {
                when {
                    asset.isHEIC() -> "image/heic"
                    asset.isPNG() -> "image/png"
                    else -> "image/jpeg"
                }
            }

            asset.mediaType == PHAssetMediaTypeVideo -> {
                "video/mp4"
            }

            else -> "application/octet-stream"
        }
    }

    private fun PHAsset.isReadyForPlayback(): Boolean {
        return this.sourceType == PHAssetSourceTypeUserLibrary
    }

    private fun PHAsset.isHEIC(): Boolean {
        return this.mediaSubtypes == PHAssetMediaSubtypePhotoHDR
    }

    private fun PHAsset.isPNG(): Boolean {
        return this.mediaSubtypes == PHAssetMediaSubtypeNone
    }

    actual suspend fun getTotalMediaCount(): Int {
        when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusNotDetermined -> {
                throw MediaLoaderException.InitializationException()
            }

            PHAuthorizationStatusRestricted,
            PHAuthorizationStatusDenied -> {
                throw MediaLoaderException.PermissionDeniedException()
            }

            else -> { /* Authorized, continue */
            }
        }

        try {
            val fetchOptions = PHFetchOptions()
            val fetchResult = PHAsset.fetchAssetsWithOptions(fetchOptions)
            return fetchResult.count.toInt()
        } catch (e: Exception) {
            throw MediaLoaderException.MediaAccessException(e)
        }
    }

    suspend fun loadImageData(
        asset: PHAsset,
    ): NSData? = suspendCoroutine { continuation ->
        val options = PHImageRequestOptions().apply {
            networkAccessAllowed = true
            synchronous = false
            deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
        }

        imageManager.requestImageDataAndOrientationForAsset(
            asset = asset,
            options = options
        ) { imageData, _, _, _ ->
            continuation.resume(imageData)
        }
    }

    suspend fun loadVideoURL(asset: PHAsset): NSURL? = suspendCoroutine { continuation ->
        val options = PHVideoRequestOptions().apply {
            networkAccessAllowed = true
            deliveryMode = PHVideoRequestOptionsDeliveryModeHighQualityFormat
        }

        imageManager.requestAVAssetForVideo(
            asset = asset,
            options = options
        ) { avAsset, _, _ ->
            val urlAsset = avAsset as? AVURLAsset
            continuation.resume(urlAsset?.URL)
        }
    }
}