package com.debanshu.neatpic

sealed class MediaLoaderException(message: String) : Exception(message) {
    class PermissionDeniedException : MediaLoaderException("Permission to access media storage is denied")
    class MediaAccessException(cause: Throwable) : MediaLoaderException("Failed to access media: ${cause.message}")
    class InvalidPageException(page: Int) : MediaLoaderException("Invalid page number: $page")
    class NoMediaFoundException : MediaLoaderException("No media items found")
    class InitializationException : MediaLoaderException("Media library not initialized")
    class CursorAccessException(cause: Throwable) : MediaLoaderException("Cursor exception: ${cause.message}")
}