package com.example

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.GameViewModel
import com.example.game.engine.GameEngine
import com.example.game.model.GamePhase
import com.example.game.pixel.PixelSpriteBank
import com.example.ui.components.CyberHud
import com.example.ui.components.GameOverOverlay
import com.example.ui.components.PauseOverlay
import com.example.ui.components.SettingsModal
import com.example.ui.components.TitleBriefingScreen
import com.example.ui.components.VictoryOverlay
import com.example.ui.components.VirtualGamepad
import com.example.ui.theme.CyberShiftTheme
import com.example.ui.theme.CyberVoid
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            CyberShiftTheme {
                CyberShiftRoot(viewModel = gameViewModel)
            }
        }
    }
}

@Composable
fun CyberShiftRoot(viewModel: GameViewModel) {
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val bestRecord by viewModel.bestRecord.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }
    var showSettings by remember { mutableStateOf(false) }

    // Auto-pause and stop BGM on background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (viewModel.engine.phase == GamePhase.PLAYING) {
                    viewModel.pauseMission()
                }
                viewModel.audio.stopBgm()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (viewModel.engine.phase == GamePhase.PLAYING) {
                    viewModel.audio.updateBgmState(shouldPlay = true, isBoss = viewModel.hudState.value.bossActive)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Haptic feedback collector
    LaunchedEffect(Unit) {
        viewModel.hapticEvents.collect { kind ->
            when (kind) {
                GameEngine.HapticKind.LIGHT,
                GameEngine.HapticKind.MEDIUM -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                GameEngine.HapticKind.HEAVY -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Back gesture / button handling
    BackHandler(enabled = showSettings || hud.phase != GamePhase.TITLE) {
        if (showSettings) {
            showSettings = false
        } else {
            when (hud.phase) {
                GamePhase.PLAYING -> viewModel.pauseMission()
                GamePhase.PAUSED -> viewModel.resumeMission()
                GamePhase.GAME_OVER, GamePhase.VICTORY -> viewModel.abortToBase()
                GamePhase.TITLE -> Unit
            }
        }
    }

    LaunchedEffect(hud.phase, showSettings) {
        if (hud.phase == GamePhase.PLAYING && !showSettings) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberVoid)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                val isDown = event.type == KeyEventType.KeyDown
                val isUp = event.type == KeyEventType.KeyUp
                if (!isDown && !isUp) return@onPreviewKeyEvent false

                when (event.key) {
                    Key.A, Key.DirectionLeft -> {
                        viewModel.keyInput = viewModel.keyInput.copy(left = isDown)
                        true
                    }
                    Key.D, Key.DirectionRight -> {
                        viewModel.keyInput = viewModel.keyInput.copy(right = isDown)
                        true
                    }
                    Key.W, Key.DirectionUp -> {
                        viewModel.keyInput = viewModel.keyInput.copy(up = isDown)
                        true
                    }
                    Key.S, Key.DirectionDown -> {
                        viewModel.keyInput = viewModel.keyInput.copy(down = isDown)
                        true
                    }
                    Key.Spacebar, Key.Z, Key.ButtonA -> {
                        if (isDown && !viewModel.keyInput.jumpHeld) {
                            viewModel.onKeyTrigger(jumpPressed = true)
                        }
                        viewModel.keyInput = viewModel.keyInput.copy(jumpHeld = isDown)
                        true
                    }
                    Key.J, Key.X, Key.ButtonX -> {
                        if (isDown && !viewModel.keyInput.fireHeld) {
                            viewModel.onKeyTrigger(firePressed = true)
                        }
                        viewModel.keyInput = viewModel.keyInput.copy(fireHeld = isDown)
                        true
                    }
                    Key.K, Key.E, Key.ShiftLeft, Key.ButtonY -> {
                        if (isDown) viewModel.onKeyTrigger(transformPressed = true)
                        true
                    }
                    Key.L, Key.C, Key.ButtonB -> {
                        if (isDown) viewModel.onKeyTrigger(boostPressed = true)
                        true
                    }
                    Key.Escape, Key.P, Key.ButtonStart -> {
                        if (isDown) {
                            if (showSettings) showSettings = false
                            else if (hud.phase == GamePhase.PLAYING) viewModel.pauseMission()
                            else if (hud.phase == GamePhase.PAUSED) viewModel.resumeMission()
                        }
                        true
                    }
                    else -> false
                }
            },
    ) {
        if (hud.phase == GamePhase.TITLE) {
            TitleBriefingScreen(
                bestRecord = bestRecord,
                isMuted = bestRecord.isMuted,
                onToggleMute = viewModel::toggleMute,
                onOpenSettings = { showSettings = true },
                onDeploy = viewModel::startMission,
            )
        } else {
            val viewW = constraints.maxWidth.coerceAtLeast(1)
            val viewH = constraints.maxHeight.coerceAtLeast(1)
            val virtualWidth = ((GameEngine.VIRTUAL_HEIGHT.toFloat() * viewW) / viewH)
                .roundToInt()
                .coerceIn(320, 580)

            var frameBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var frameTick by remember { mutableIntStateOf(0) }
            val srcRect = remember { Rect() }
            val dstRect = remember { Rect() }

            LaunchedEffect(virtualWidth, hud.phase) {
                frameBitmap = viewModel.tickAndRender(0f, virtualWidth)
                frameTick++

                if (hud.phase == GamePhase.PLAYING) {
                    var lastNanos = 0L
                    while (true) {
                        withFrameNanos { now ->
                            val dt = if (lastNanos == 0L) 1f / 60f else (now - lastNanos) / 1_000_000_000f
                            lastNanos = now
                            frameBitmap = viewModel.tickAndRender(dt, virtualWidth)
                            frameTick++
                        }
                    }
                }
            }

            // Pixel-perfect framebuffer display
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE")
                val tick = frameTick
                val bmp = frameBitmap ?: return@Canvas
                srcRect.set(0, 0, bmp.width, bmp.height)
                dstRect.set(0, 0, size.width.roundToInt(), size.height.roundToInt())
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(bmp, srcRect, dstRect, PixelSpriteBank.nearestPaint)
                }
            }

            // Optional CRT Scanline overlay over the pixel framebuffer
            if (bestRecord.crtScanlines) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepPx = 3.dp.toPx().coerceAtLeast(3f)
                    val scanColor = Color.Black.copy(alpha = 0.16f)
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = scanColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                        y += stepPx
                    }
                }
            }

            // Cybernetic HUD
            CyberHud(
                hud = hud,
                isMuted = bestRecord.isMuted,
                onToggleMute = viewModel::toggleMute,
                onPause = viewModel::pauseMission,
            )

            // Multi-touch ergonomic controls with custom size & opacity
            if (hud.phase == GamePhase.PLAYING && !showSettings) {
                VirtualGamepad(
                    form = hud.form,
                    energonReady = hud.energon >= 30f,
                    padScale = bestRecord.padScale,
                    padOpacity = bestRecord.padOpacity,
                    onInputChanged = viewModel::updateTouchPad,
                )
            }

            when (hud.phase) {
                GamePhase.PAUSED -> if (!showSettings) {
                    PauseOverlay(
                        onResume = viewModel::resumeMission,
                        onOpenSettings = { showSettings = true },
                        onRestart = viewModel::startMission,
                        onAbort = viewModel::abortToBase,
                    )
                }
                GamePhase.GAME_OVER -> GameOverOverlay(
                    hud = hud,
                    onRetry = viewModel::startMission,
                    onAbort = viewModel::abortToBase,
                )
                GamePhase.VICTORY -> VictoryOverlay(
                    hud = hud,
                    onPlayAgain = viewModel::startMission,
                    onAbort = viewModel::abortToBase,
                )
                else -> Unit
            }
        }

        if (showSettings) {
            SettingsModal(
                record = bestRecord,
                onPadScaleChange = viewModel::setPadScale,
                onPadOpacityChange = viewModel::setPadOpacity,
                onCrtToggle = viewModel::setCrtScanlines,
                onBgmToggle = viewModel::setBgmMuted,
                onSfxToggle = viewModel::setSfxMuted,
                onResetDefaults = viewModel::resetSettings,
                onClose = { showSettings = false },
            )
        }
    }
}
