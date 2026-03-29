package com.dologan.humblebrowser.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dologan.humblebrowser.data.db.dao.ExclusionRuleDao
import com.dologan.humblebrowser.data.db.entities.ExclusionRuleEntity
import com.dologan.humblebrowser.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exclusionRuleDao: ExclusionRuleDao,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val exclusionRules = exclusionRuleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val largeFileSizeBytes = appPreferences.largeFileSizeBytes

    fun setLargeFileSizeMb(mb: Long) {
        appPreferences.setLargeFileSizeMb(mb)
    }

    fun addRule(pattern: String, isRegex: Boolean) {
        viewModelScope.launch {
            exclusionRuleDao.upsert(
                ExclusionRuleEntity(pattern = pattern, isRegex = isRegex)
            )
        }
    }

    fun deleteRule(id: Int) {
        viewModelScope.launch {
            exclusionRuleDao.delete(id)
        }
    }

    fun toggleRule(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            exclusionRuleDao.setEnabled(id, enabled)
        }
    }
}
