package com.debanshu.neatpic

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class PermissionHandler(private val context: Context) {
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shouldShowPermissionRationale(activity: Activity): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) ||
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VIDEO) ||
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) ||
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VIDEO)
        }

        else -> {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun handlePermissionDenial(activity: Activity) {
        if (!shouldShowPermissionRationale(activity)) {
            // User clicked "Don't ask again" or denied twice
            openAppSettings()
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 66

        fun getRequiredPermissions(): Array<String> = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )

            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun hasRequiredPermissions(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            hasFullMediaAccess() || hasPartialMediaAccess()
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            hasFullMediaAccess()
        }

        else -> {
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasFullMediaAccess(): Boolean =
        checkPermission(Manifest.permission.READ_MEDIA_IMAGES) &&
                checkPermission(Manifest.permission.READ_MEDIA_VIDEO)

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun hasPartialMediaAccess(): Boolean =
        checkPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    private fun checkPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}