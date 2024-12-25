package com.debanshu.neatpic.android

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.debanshu.neatpic.AppViewModel
import com.debanshu.neatpic.PermissionState
import com.debanshu.neatpic.model.MediaItem

@Composable
fun MediaScreen(
    viewModel: AppViewModel,
    onRequestPermission: () -> Unit
) {
    val pagingData = viewModel.mediaItems.collectAsLazyPagingItems()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (val state = permissionState) {
        is PermissionState.Granted -> {
            if (state.retry) {
                pagingData.refresh()
                viewModel.setPermissionState(PermissionState.Granted(false))
            } else {
                when (val loadState = pagingData.loadState.refresh) {
                    is LoadState.Loading -> {
                        CircularProgressIndicator()
                    }

                    is LoadState.Error -> {
                        ErrorMessage(message = loadState.error.message!!)
                    }

                    is LoadState.NotLoading -> {
                        MediaGrid(mediaItems = pagingData)
                    }
                }
            }
        }

        is PermissionState.RequirePermission -> {
            PermissionRequest(
                shouldShowRationale = state.shouldShowRationale,
                onRequestPermission = onRequestPermission,
                onOpenSettings = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", "com.debanshu.neatpic.android", null)
                        }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionRequest(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Photo Library Access",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "This app needs access to your photo library to display your media.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AnimatedVisibility(!shouldShowRationale) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Access")
            }
        }

        TextButton(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
}

@Composable
fun MediaGrid(mediaItems: LazyPagingItems<MediaItem>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(mediaItems.itemCount) { index ->
            mediaItems[index]?.let {
                MediaItemCard(it)
            }
        }
    }
}

@Composable
fun MediaItemCard(item: MediaItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                item.uri,
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    CircularProgressIndicator()
}

@Composable
fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(16.dp)
    )
}