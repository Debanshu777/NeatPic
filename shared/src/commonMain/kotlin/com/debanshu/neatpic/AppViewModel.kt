package com.debanshu.neatpic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import com.debanshu.neatpic.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(repository: MediaRepository) : ViewModel() {
    private val _permissionState =
        MutableStateFlow<PermissionState>(PermissionState.RequirePermission(false))
    val permissionState = _permissionState.asStateFlow()

    val mediaItems: Flow<PagingData<MediaItem>> =
        repository.getMediaItems().cachedIn(viewModelScope)

    fun setPermissionState(permissionState: PermissionState) {
        _permissionState.tryEmit(permissionState)
    }
}

sealed interface PermissionState {
    data class RequirePermission(val shouldShowRationale: Boolean) : PermissionState
    data class Granted(val retry: Boolean) : PermissionState
}
