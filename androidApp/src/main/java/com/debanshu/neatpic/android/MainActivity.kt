package com.debanshu.neatpic.android

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.debanshu.neatpic.AppViewModel
import com.debanshu.neatpic.PermissionHandler
import com.debanshu.neatpic.PermissionState
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val permissionHandler by lazy { PermissionHandler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val viewModel: AppViewModel = koinViewModel()

                LaunchedEffect(Unit) {
                    if (permissionHandler.hasRequiredPermissions()) {
                        viewModel.setPermissionState(PermissionState.Granted(false))
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MediaScreen(
                        viewModel = viewModel, onRequestPermission = {
                            requestPermissions()
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            PermissionHandler.getRequiredPermissions(),
            PermissionHandler.PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        val viewModel: AppViewModel by inject()
        handlePermissionResult(requestCode, grantResults, viewModel, this, permissionHandler)
    }
}

private fun handlePermissionResult(
    requestCode: Int,
    grantResults: IntArray,
    viewModel: AppViewModel,
    activity: Activity,
    permissionHandler: PermissionHandler
) {
    if (requestCode != PermissionHandler.PERMISSION_REQUEST_CODE) return

    val isGranted = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            val hasPartialAccess = grantResults.lastOrNull() == PackageManager.PERMISSION_GRANTED
            val hasFullAccess = grantResults.take(2).all { it == PackageManager.PERMISSION_GRANTED }
            hasPartialAccess || hasFullAccess
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }

        else -> {
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        }
    }

    val isPartialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            grantResults.lastOrNull() == PackageManager.PERMISSION_GRANTED

    if (isGranted) {
        viewModel.setPermissionState(PermissionState.Granted(isPartialAccess))
    } else {
        val shouldShowRationale = permissionHandler.shouldShowPermissionRationale(activity)
        viewModel.setPermissionState(PermissionState.RequirePermission(shouldShowRationale))

        if (!shouldShowRationale) {
            // User has denied permission twice or clicked "Don't ask again"
            permissionHandler.handlePermissionDenial(activity)
        }
    }
}