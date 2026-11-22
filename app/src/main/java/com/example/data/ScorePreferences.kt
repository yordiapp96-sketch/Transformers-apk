package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cyberShiftDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cybershift_prefs"
)

data class BestRunRecord(
    val bestScore: Int = 0,
    val bestTimeSeconds: Float = 0f,
    val bestRank: String = "-",
    val bestRelics: Int = 0,
    val isMuted: Boolean = false,
    val bgmMuted: Boolean = false,
    val sfxMuted: Boolean = false,
    val padScale: Float = 1.0f,
    val padOpacity: Float = 0.65f,
    val crtScanlines: Boolean = true,
)

class ScorePreferences(private val context: Context) {

    private object Keys {
        val BEST_SCORE = intPreferencesKey("best_score")
        val BEST_TIME = floatPreferencesKey("best_time_seconds")
        val BEST_RANK = stringPreferencesKey("best_rank")
        val BEST_RELICS = intPreferencesKey("best_relics")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val BGM_MUTED = booleanPreferencesKey("bgm_muted")
        val SFX_MUTED = booleanPreferencesKey("sfx_muted")
        val PAD_SCALE = floatPreferencesKey("pad_scale")
        val PAD_OPACITY = floatPreferencesKey("pad_opacity")
        val CRT_SCANLINES = booleanPreferencesKey("crt_scanlines")
    }

    val recordFlow: Flow<BestRunRecord> = context.cyberShiftDataStore.data.map { prefs ->
        val legacyMute = prefs[Keys.IS_MUTED] ?: false
        val bgmMuted = prefs[Keys.BGM_MUTED] ?: legacyMute
        val sfxMuted = prefs[Keys.SFX_MUTED] ?: legacyMute
        BestRunRecord(
            bestScore = prefs[Keys.BEST_SCORE] ?: 0,
            bestTimeSeconds = prefs[Keys.BEST_TIME] ?: 0f,
            bestRank = prefs[Keys.BEST_RANK] ?: "-",
            bestRelics = prefs[Keys.BEST_RELICS] ?: 0,
            isMuted = bgmMuted && sfxMuted,
            bgmMuted = bgmMuted,
            sfxMuted = sfxMuted,
            padScale = (prefs[Keys.PAD_SCALE] ?: 1.0f).coerceIn(0.80f, 1.30f),
            padOpacity = (prefs[Keys.PAD_OPACITY] ?: 0.65f).coerceIn(0.30f, 0.95f),
            crtScanlines = prefs[Keys.CRT_SCANLINES] ?: true,
        )
    }

    suspend fun setMuted(muted: Boolean) {
        context.cyberShiftDataStore.edit { prefs ->
            prefs[Keys.IS_MUTED] = muted
            prefs[Keys.BGM_MUTED] = muted
            prefs[Keys.SFX_MUTED] = muted
        }
    }

    suspend fun setBgmMuted(muted: Boolean) {
        context.cyberShiftDataStore.edit { prefs ->
            prefs[Keys.BGM_MUTED] = muted
            val sfx = prefs[Keys.SFX_MUTED] ?: false
            prefs[Keys.IS_MUTED] = muted && sfx
        }
    }

    suspend fun setSfxMuted(muted: Boolean) {
        context.cyberShiftDataStore.edit { prefs ->
            prefs[Keys.SFX_MUTED] = muted
            val bgm = prefs[Keys.BGM_MUTED] ?: false
            prefs[Keys.IS_MUTED] = muted && bgm
        }
    }

    suspend fun setPadScale(scale: Float) {
        context.cyberShiftDataStore.edit { prefs ->
            prefs[Keys.PAD_SCALE] = scale.coerceIn(0.80f, 1.30f)
        }
    }

    suspend fun setPadOpacity(opacity: Float) {
        context.cyberShiftDataStore.edit { prefs ->
            prefs[Keys.PAD_OPACITY] = opacity.coerceIn(0.30f, 0.95f)
        }
    }

    suspend fun setCrtScanlines(enabled: Boolean) {
        context.cyberShiftDataStore.edit { prefs ->
            prefs[Keys.CRT_SCANLINES] = enabled
        }
    }

    suspend fun resetControlAndVisualDefaults() {
        context.cyberShiftDataStore.edit { prefs ->
            prefs[Keys.PAD_SCALE] = 1.0f
            prefs[Keys.PAD_OPACITY] = 0.65f
            prefs[Keys.CRT_SCANLINES] = true
            prefs[Keys.BGM_MUTED] = false
            prefs[Keys.SFX_MUTED] = false
            prefs[Keys.IS_MUTED] = false
        }
    }

    suspend fun saveRunIfBest(score: Int, timeSeconds: Float, rank: String, relics: Int) {
        context.cyberShiftDataStore.edit { prefs ->
            val prevScore = prefs[Keys.BEST_SCORE] ?: 0
            if (score > prevScore) {
                prefs[Keys.BEST_SCORE] = score
            }
            val prevTime = prefs[Keys.BEST_TIME] ?: 0f
            if (prevTime == 0f || (timeSeconds > 0f && timeSeconds < prevTime)) {
                prefs[Keys.BEST_TIME] = timeSeconds
            }
            val prevRank = prefs[Keys.BEST_RANK] ?: "-"
            if (rankValue(rank) > rankValue(prevRank)) {
                prefs[Keys.BEST_RANK] = rank
            }
            val prevRelics = prefs[Keys.BEST_RELICS] ?: 0
            if (relics > prevRelics) {
                prefs[Keys.BEST_RELICS] = relics
            }
        }
    }

    private fun rankValue(rank: String): Int = when (rank) {
        "S" -> 4
        "A" -> 3
        "B" -> 2
        "C" -> 1
        else -> 0
    }
}
