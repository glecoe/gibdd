package com.gibdd.eyewitness.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gibdd.eyewitness.data.AppPreferences
import com.gibdd.eyewitness.data.IncidentRepository
import com.gibdd.eyewitness.network.IncidentOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportUiState(
    val description: String = "",
    val mediaUris: List<Uri> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sending: Boolean = false,
    val sentOk: Boolean = false,
    val error: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)
    private val repo = IncidentRepository(app, prefs)

    private val _report = MutableStateFlow(ReportUiState())
    val report: StateFlow<ReportUiState> = _report.asStateFlow()

    private val _incidents = MutableStateFlow<List<IncidentOut>>(emptyList())
    val incidents: StateFlow<List<IncidentOut>> = _incidents.asStateFlow()

    private val _loadingList = MutableStateFlow(false)
    val loadingList: StateFlow<Boolean> = _loadingList.asStateFlow()

    private val _serverUrl = MutableStateFlow(AppPreferences.DEFAULT_BASE_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    init {
        viewModelScope.launch {
            _serverUrl.value = prefs.getBaseUrl()
        }
    }

    fun onDescriptionChange(value: String) {
        _report.value = _report.value.copy(description = value)
    }

    fun addMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _report.value = _report.value.copy(mediaUris = _report.value.mediaUris + uris)
    }

    fun removeMedia(uri: Uri) {
        _report.value = _report.value.copy(mediaUris = _report.value.mediaUris - uri)
    }

    fun setLocation(lat: Double, lon: Double) {
        _report.value = _report.value.copy(latitude = lat, longitude = lon)
    }

    fun clearLocation() {
        _report.value = _report.value.copy(latitude = null, longitude = null)
    }

    fun clearError() {
        _report.value = _report.value.copy(error = null)
    }

    fun consumeSentOk() {
        _report.value = _report.value.copy(sentOk = false)
    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            prefs.saveBaseUrl(url)
            _serverUrl.value = prefs.getBaseUrl()
        }
    }

    fun submit() {
        val state = _report.value
        if (state.description.isBlank() && state.mediaUris.isEmpty()) {
            _report.value = state.copy(error = "Добавьте описание или хотя бы одно фото/видео")
            return
        }
        viewModelScope.launch {
            _report.value = state.copy(sending = true, error = null)
            try {
                repo.sendIncident(
                    description = state.description,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    mediaUris = state.mediaUris,
                )
                // Сбрасываем форму после успеха
                _report.value = ReportUiState(sentOk = true)
            } catch (e: Exception) {
                _report.value = _report.value.copy(
                    sending = false,
                    error = "Не удалось отправить: ${e.message ?: "ошибка сети"}",
                )
            }
        }
    }

    fun loadMyIncidents() {
        viewModelScope.launch {
            _loadingList.value = true
            try {
                _incidents.value = repo.myIncidents()
            } catch (_: Exception) {
                // молча — список просто останется прежним
            } finally {
                _loadingList.value = false
            }
        }
    }
}
