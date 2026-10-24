package com.phoenix.qiur.utils

import com.phoenix.qiur.BuildConfig
import com.phoenix.qiur.data.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateState(
    val updateStatus: UpdateStatus = UpdateStatus.IDLE,
    val nextUpdateVersion: String = ""
)

sealed interface UpdateResult {
    data class Success(val isUpdateAvailable: Boolean, val nextUpdateVersion: String): UpdateResult
    data class Error(val error: String?): UpdateResult
}

enum class UpdateStatus {
    IDLE, CHECKING, AVAILABLE, LATEST, FAILED
}

class UpdateStateHolder(val dataStoreManager: DataStoreManager) {

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState = _updateState.asStateFlow()

    suspend fun collectLastData() {
        val isUpdateAvailable = dataStoreManager.isUpdateAvailable.first()
        val nextUpdateVersion = dataStoreManager.nextUpdateVersion.first()
        _updateState.update {
            it.copy(
                updateStatus = if (isUpdateAvailable) UpdateStatus.AVAILABLE else UpdateStatus.IDLE,
                nextUpdateVersion = nextUpdateVersion
            )
        }
    }

    suspend fun checkForUpdates() {
        _updateState.update { it.copy(updateStatus = UpdateStatus.CHECKING) }
        val result = requestLatestVersion()
        _updateState.update { it.copy(updateStatus = UpdateStatus.IDLE) }
        when(result) {
            is UpdateResult.Error -> {
                _updateState.update { it.copy(updateStatus = UpdateStatus.FAILED) }
            }
            is UpdateResult.Success -> {
                setUpdateState(
                    updateStatus = if (result.isUpdateAvailable) UpdateStatus.AVAILABLE else UpdateStatus.LATEST,
                    nextUpdateVersion = result.nextUpdateVersion
                )
            }
        }
    }

    suspend fun setUpdateState(
        updateStatus: UpdateStatus,
        nextUpdateVersion: String?
    ) {
        _updateState.update {
            it.copy(
                updateStatus = updateStatus,
                nextUpdateVersion = nextUpdateVersion ?: ""
            )
        }
        dataStoreManager.setUpdateState(
            available = updateStatus == UpdateStatus.AVAILABLE,
            version = nextUpdateVersion
        )
    }

    fun cleanUpdateState() {
        _updateState.update { UpdateState() }
    }

}


suspend fun requestLatestVersion(): UpdateResult = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("https://api.github.com/repos/alibardide-dev/QiuR/releases/latest")
            .build()

        val response = OkHttpClient().newCall(request).execute()
        if (response.code != 200)
            return@withContext UpdateResult.Error("Request Failed: ${response.code}")

        val json = response.body?.string() ?: throw NullPointerException()
        val version = JSONObject(json).getString("name")

        return@withContext UpdateResult.Success(
            isUpdateAvailable = BuildConfig.VERSION_NAME < version,
            nextUpdateVersion = version
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext UpdateResult.Error(e.message)
    }
}