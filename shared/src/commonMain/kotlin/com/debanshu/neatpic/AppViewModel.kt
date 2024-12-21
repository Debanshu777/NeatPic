package com.debanshu.neatpic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debanshu.neatpic.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class AppViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _mediaState = MutableStateFlow<DataState<List<MediaItem>>>(DataState.Uninitialized)
    val mediaState = _mediaState.asStateFlow()

    private val _totalState = MutableStateFlow<DataState<Int>>(DataState.Uninitialized)
    val totalState = _totalState.asStateFlow()

    fun getMedia() {
        viewModelScope.launch {
            repository.getMediaItems(page = 0)
                .onStart { _mediaState.tryEmit(DataState.Loading) }
                .catch { error ->
                    val errorMessage = when (error) {
                        is MediaLoaderException.PermissionDeniedException ->
                            "Please grant permission to access media"

                        is MediaLoaderException.NoMediaFoundException ->
                            "No media found on device"

                        is MediaLoaderException.InvalidPageException ->
                            "Invalid page number"

                        else -> error.message ?: "Unknown Error"
                    }
                    _mediaState.tryEmit(DataState.Error(errorMessage))
                }.collect { items ->
                    _mediaState.tryEmit(DataState.Success(items))
                }
        }
    }

    fun getTotalMediaCount() {
        viewModelScope.launch {
            _totalState.emit(DataState.Success(repository.getTotalCount()))
        }
    }
}


sealed interface DataState<out T> {
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val error: String) : DataState<Nothing>
    data object Loading : DataState<Nothing>
    data object Uninitialized : DataState<Nothing>
    data object RequiresPermission : DataState<Nothing>
}