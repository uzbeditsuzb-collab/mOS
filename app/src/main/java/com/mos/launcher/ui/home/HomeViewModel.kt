package com.mos.launcher.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mos.launcher.MosApplication
import com.mos.launcher.data.apps.DefaultDockResolver
import com.mos.launcher.domain.model.AppInfo
import com.mos.launcher.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Survives rotation (ViewModel) and process death (SavedStateHandle for the
 * drawer state). App data is re-queried after process recreation.
 */
class HomeViewModel(
    private val appRepository: AppRepository,
    private val dockResolver: DefaultDockResolver,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    val apps: StateFlow<List<AppInfo>> = appRepository.observeApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dock: StateFlow<List<AppInfo>> = apps
        .map { dockResolver.resolve(it) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val drawerOpen: StateFlow<Boolean> = savedState.getStateFlow(KEY_DRAWER, false)

    fun openDrawer() {
        savedState[KEY_DRAWER] = true
    }

    fun closeDrawer() {
        savedState[KEY_DRAWER] = false
    }

    /** Home button pressed while mOS is already in front. */
    fun onHomePressed() = closeDrawer()

    fun launch(app: AppInfo): Boolean = appRepository.launch(app)

    companion object {
        private const val KEY_DRAWER = "drawer_open"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MosApplication
                HomeViewModel(
                    appRepository = app.container.appRepository,
                    dockResolver = app.container.dockResolver,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}
