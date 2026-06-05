package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.LocalProxyServer
import com.example.network.SpoofEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed interface HandshakeUiState {
    object Idle : HandshakeUiState
    object Loading : HandshakeUiState
    data class Success(val result: HandshakeHistory) : HandshakeUiState
    data class Error(val message: String, val partialResult: HandshakeHistory? = null) : HandshakeUiState
}

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database)

    // Profiles Flow
    val profiles: StateFlow<List<SniProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History Flow
    val histories: StateFlow<List<HandshakeHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Proxy Status State
    private val _isProxyRunning = MutableStateFlow(false)
    val isProxyRunning: StateFlow<Boolean> = _isProxyRunning.asStateFlow()

    private val _proxyPort = MutableStateFlow(8080)
    val proxyPort: StateFlow<Int> = _proxyPort.asStateFlow()

    private val _proxyHostSni = MutableStateFlow("www.google.com")
    val proxyHostSni: StateFlow<String> = _proxyHostSni.asStateFlow()

    private val _proxyLogs = MutableStateFlow<List<String>>(emptyList())
    val proxyLogs: StateFlow<List<String>> = _proxyLogs.asStateFlow()

    // Interactive Handshake diagnostics state
    private val _handshakeState = MutableStateFlow<HandshakeUiState>(HandshakeUiState.Idle)
    val handshakeState: StateFlow<HandshakeUiState> = _handshakeState.asStateFlow()

    private var localProxyServer: LocalProxyServer? = null

    init {
        // Pre-populate with useful templates if empty
        viewModelScope.launch {
            profiles.first() // Wait till loaded
            if (profiles.value.isEmpty()) {
                repository.insertProfile(
                    SniProfile(
                        name = "Google Bypass Template",
                        targetHost = "example.com",
                        targetPort = 443,
                        spoofedSni = "www.google.com",
                        notes = "Spoofs target SNI to www.google.com to test handshake compatibility."
                    )
                )
                repository.insertProfile(
                    SniProfile(
                        name = "Cloudflare CDN Template",
                        targetHost = "1.1.1.1",
                        targetPort = 443,
                        spoofedSni = "cloudflare.com",
                        notes = "Checks TLS handshake routing using secondary CDN headers."
                    )
                )
                repository.insertProfile(
                    SniProfile(
                        name = "Secure Sandbox Check",
                        targetHost = "github.com",
                        targetPort = 443,
                        spoofedSni = "raw.githubusercontent.com",
                        notes = "Handshakes github.com but spoofing content delivery networks server name."
                    )
                )
            }
        }
    }

    // Proxy Operations
    fun toggleProxy(port: Int, spoofSni: String) {
        if (_isProxyRunning.value) {
            stopProxy()
        } else {
            startProxy(port, spoofSni)
        }
    }

    private fun startProxy(port: Int, spoofSni: String) {
        _proxyPort.value = port
        _proxyHostSni.value = spoofSni
        
        localProxyServer?.stop()
        _proxyLogs.value = emptyList()

        addProxyLog("Initializing VanetSpoofing Proxy Engine...")
        localProxyServer = LocalProxyServer(port, spoofSni) { logLine ->
            addProxyLog(logLine)
        }
        localProxyServer?.start()
        _isProxyRunning.value = true
    }

    private fun stopProxy() {
        localProxyServer?.stop()
        localProxyServer = null
        _isProxyRunning.value = false
    }

    fun addProxyLog(log: String) {
        val formatTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$formatTime] $log"
        _proxyLogs.update { current -> listOf(formatted) + current }
    }

    fun clearProxyLogs() {
        _proxyLogs.value = emptyList()
    }

    // Diagnostics Operations
    fun performHandshakeCheck(host: String, port: Int, spoofedSni: String, trustAll: Boolean) {
        viewModelScope.launch {
            _handshakeState.value = HandshakeUiState.Loading
            val historyItem = SpoofEngine.testSniSpoof(host, port, spoofedSni, trustAll)
            
            // Persist history item inside Room DB
            repository.insertHistory(historyItem)

            if (historyItem.isSuccess) {
                _handshakeState.value = HandshakeUiState.Success(historyItem)
            } else {
                _handshakeState.value = HandshakeUiState.Error(
                    message = historyItem.errorMessage ?: "Handshake timeout",
                    partialResult = historyItem
                )
            }
        }
    }

    fun resetHandshakeState() {
        _handshakeState.value = HandshakeUiState.Idle
    }

    // Room DB Profiles Setup
    fun saveProfile(profile: SniProfile) {
        viewModelScope.launch {
            if (profile.id == 0) {
                repository.insertProfile(profile)
            } else {
                repository.updateProfile(profile)
            }
        }
    }

    fun deleteProfile(profile: SniProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        localProxyServer?.stop()
    }
}
