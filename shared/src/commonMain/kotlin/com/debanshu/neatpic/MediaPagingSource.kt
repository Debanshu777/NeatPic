package com.debanshu.neatpic

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingState
import com.debanshu.neatpic.model.MediaItem

class MediaPagingSource(
    private val imageSource: ImageSource
) : PagingSource<Int, MediaItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, MediaItem> {
        val page = params.key ?: 0
        val pageSize = params.loadSize

        try {
            var mediaItems: List<MediaItem> = emptyList()
            imageSource.loadMediaItems(page, pageSize).collect { items ->
                mediaItems = items
            }

            val nextKey =
                if (mediaItems.isEmpty() || mediaItems.size < pageSize) null else page + 1
            val prevKey = if (page > 0) page - 1 else null

            return LoadResult.Page(
                data = mediaItems,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: MediaLoaderException.PermissionDeniedException) {
            return LoadResult.Error(
                e
            )
        } catch (e: Exception) {
            return when (e) {
                is MediaLoaderException -> {
                    LoadResult.Error(
                        e
                    )
                }

                else -> {
                    LoadResult.Error(
                        e
                    )
                }
            }
        }
    }
}

