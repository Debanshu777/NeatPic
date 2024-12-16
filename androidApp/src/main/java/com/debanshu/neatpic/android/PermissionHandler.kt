package com.debanshu.neatpic.android

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat


class PermissionHandler(private val context: Context) {
    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(READ_MEDIA_IMAGES) && checkPermission(READ_MEDIA_VIDEO)
        } else {
            checkPermission(READ_EXTERNAL_STORAGE)
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 66

        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
            } else {
                arrayOf(READ_EXTERNAL_STORAGE)
            }
        }
    }
}