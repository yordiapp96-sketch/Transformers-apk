package com.example.game

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RetroAudioEngine
import com.example.data.BestRunRecord
import com.example.data.ScorePreferences
import com.example.game.engine.GameEngine
import com.example.game.model.GamePhase
import com.example.game.model.HudState
import com.example.game.model.InputState
import com.example.game.pixel.PixelWorldRenderer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ScorePreferences(application)
    val audio = RetroAudioEngine()

    private val _hapticEvents = MutableSharedFlow<GameEngine.HapticKind>(extraBufferCapacity = 16)
    val hapticEvents = _hapticEvents.asSharedFlow()

    val engine = GameEngine(
        audio = audio,
        onHapticEvent = { kind -> _hapticEvents.tryEmit(kind) },
    )
    private val renderer = PixelWorldRenderer()

    private val _hudState = MutableStateFlow(engine.toHudState())
    val hudState: StateFlow<HudState> = _hudState.asStateFlow()

    val bestRecord: StateFlow<BestRunRecord> = prefs.recordFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BestRunRecord(),
    )

    private var savedVictoryForRun = false
    private var hudSyncAccum = 0f

    @Volatile
    var touchInput = InputState()
    @Volatile
    var keyInput = InputState()

    private var pendingJumpPress = false
    private var pendingFirePress = false
    private var pendingTransformPress = false
    private var pendingBoostPress = false

    init {
        viewModelScope.launch {
            prefs.recordFlow.collect { rec ->
                audio.sfxMuted = rec.sfxMuted
                audio.bgmMuted = rec.bgmMuted
                syncBgm()
            }
        }
    }

    private fun syncBgm() {
        val snap = _hudState.value
        audio.updateBgmState(
            shouldPlay = snap.phase == GamePhase.PLAYING,
            isBoss = snap.bossActive,
        )
    }

    fun startMission() {
        savedVictoryForRun = false
        clearPendingButtons()
        engine.startNewRun()
        _hudState.value = engine.toHudState()
        syncBgm()
    }

    fun pauseMission() {
        engine.setPaused(true)
        _hudState.value = engine.toHudState()
        syncBgm()
    }

    fun resumeMission() {
        clearPendingButtons()
        engine.setPaused(false)
        _hudState.value = engine.toHudState()
        syncBgm()
    }

    fun abortToBase() {
        clearPendingButtons()
        engine.returnToTitle()
        _hudState.value = engine.toHudState()
        syncBgm()
    }

    fun toggleMute() {
        val next = !(audio.bgmMuted && audio.sfxMuted)
        audio.isMuted = next
        viewModelScope.launch { prefs.setMuted(next) }
        syncBgm()
    }

    fun setBgmMuted(muted: Boolean) {
        audio.bgmMuted = muted
        viewModelScope.launch { prefs.setBgmMuted(muted) }
        syncBgm()
    }

    fun setSfxMuted(muted: Boolean) {
        audio.sfxMuted = muted
        viewModelScope.launch { prefs.setSfxMuted(muted) }
    }

    fun setPadScale(scale: Float) {
        viewModelScope.launch { prefs.setPadScale(scale) }
    }

    fun setPadOpacity(opacity: Float) {
        viewModelScope.launch { prefs.setPadOpacity(opacity) }
    }

    fun setCrtScanlines(enabled: Boolean) {
        viewModelScope.launch { prefs.setCrtScanlines(enabled) }
    }

    fun resetSettings() {
        viewModelScope.launch { prefs.resetControlAndVisualDefaults() }
    }

    fun updateTouchPad(
        left: Boolean,
        right: Boolean,
        up: Boolean,
        down: Boolean,
        jumpHeld: Boolean,
        fireHeld: Boolean,
        jumpJustPressed: Boolean,
        fireJustPressed: Boolean,
        transformJustPressed: Boolean,
        boostJustPressed: Boolean,
    ) {
        if (jumpJustPressed) pendingJumpPress = true
        if (fireJustPressed) pendingFirePress = true
        if (transformJustPressed) pendingTransformPress = true
        if (boostJustPressed) pendingBoostPress = true
        touchInput = InputState(
            left = left,
            right = right,
            up = up,
            down = down,
            jumpHeld = jumpHeld,
            fireHeld = fireHeld,
        )
    }

    fun onKeyTrigger(
        jumpPressed: Boolean = false,
        firePressed: Boolean = false,
        transformPressed: Boolean = false,
        boostPressed: Boolean = false,
    ) {
        if (jumpPressed) pendingJumpPress = true
        if (firePressed) pendingFirePress = true
        if (transformPressed) pendingTransformPress = true
        if (boostPressed) pendingBoostPress = true
    }

    private fun clearPendingButtons() {
        pendingJumpPress = false
        pendingFirePress = false
        pendingTransformPress = false
        pendingBoostPress = false
        touchInput = InputState()
        keyInput = InputState()
    }

    fun tickAndRender(dtSeconds: Float, virtualWidth: Int): Bitmap {
        val mergedInput = InputState(
            left = touchInput.left || keyInput.left,
            right = touchInput.right || keyInput.right,
            up = touchInput.up || keyInput.up,
            down = touchInput.down || keyInput.down,
            jumpHeld = touchInput.jumpHeld || keyInput.jumpHeld,
            jumpJustPressed = pendingJumpPress,
            fireHeld = touchInput.fireHeld || keyInput.fireHeld,
            fireJustPressed = pendingFirePress,
            transformJustPressed = pendingTransformPress,
            boostJustPressed = pendingBoostPress,
        )
        pendingJumpPress = false
        pendingFirePress = false
        pendingTransformPress = false
        pendingBoostPress = false

        val prevPhase = engine.phase
        val prevBossActive = _hudState.value.bossActive
        engine.stepFrame(dtSeconds, mergedInput, virtualWidth)

        hudSyncAccum += dtSeconds
        if (hudSyncAccum >= 0.05f || engine.phase != prevPhase) {
            hudSyncAccum = 0f
            val snapshot = engine.toHudState()
            _hudState.value = snapshot
            if (snapshot.phase != prevPhase || snapshot.bossActive != prevBossActive) {
                syncBgm()
            }
            if (snapshot.phase == GamePhase.VICTORY && !savedVictoryForRun) {
                savedVictoryForRun = true
                viewModelScope.launch {
                    prefs.saveRunIfBest(
                        score = snapshot.score,
                        timeSeconds = snapshot.elapsedSeconds,
                        rank = snapshot.rank,
                        relics = snapshot.relicsCollected,
                    )
                }
            }
        }

        return renderer.render(engine, virtualWidth)
    }

    override fun onCleared() {
        super.onCleared()
        audio.release()
    }
}
