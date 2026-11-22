package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.PlayerForm
import com.example.ui.theme.AutobotCrimson
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.EnergonCyan
import com.example.ui.theme.MatrixGold
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Zero-latency multi-touch virtual gamepad with customizable size & opacity.
 * Visual button sizes and multi-touch pointer hit-test geometry scale together proportionally.
 */
@Composable
fun VirtualGamepad(
    form: PlayerForm,
    energonReady: Boolean,
    padScale: Float = 1.0f,
    padOpacity: Float = 0.65f,
    onInputChanged: (
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
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = padScale.coerceIn(0.80f, 1.30f)
    val baseAlpha = (padOpacity / 0.65f).coerceIn(0.40f, 1.35f)

    var dpadLeft by remember { mutableStateOf(false) }
    var dpadRight by remember { mutableStateOf(false) }
    var dpadUp by remember { mutableStateOf(false) }
    var dpadDown by remember { mutableStateOf(false) }
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    var btnJump by remember { mutableStateOf(false) }
    var btnFire by remember { mutableStateOf(false) }
    var btnShift by remember { mutableStateOf(false) }
    var btnBoost by remember { mutableStateOf(false) }

    fun emit(
        jumpJust: Boolean = false,
        fireJust: Boolean = false,
        shiftJust: Boolean = false,
        boostJust: Boolean = false,
    ) {
        onInputChanged(
            dpadLeft,
            dpadRight,
            dpadUp,
            dpadDown,
            btnJump,
            btnFire,
            jumpJust,
            fireJust,
            shiftJust,
            boostJust,
        )
    }

    val dpadOuterSize = (148f * scale).dp
    val dpadRingSize = (134f * scale).dp
    val puckSize = (48f * scale).coerceAtLeast(42f).dp

    val shiftSize = (56f * scale).coerceAtLeast(48f)
    val boostSize = (56f * scale).coerceAtLeast(48f)
    val fireSize = (64f * scale).coerceAtLeast(52f)
    val jumpSize = (68f * scale).coerceAtLeast(56f)

    val clusterW = (186f * scale).dp
    val clusterH = (160f * scale).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        // ==================== LEFT D-PAD / SLIDE STICK ====================
        var dpadSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(dpadOuterSize)
                .alpha(padOpacity.coerceIn(0.30f, 0.98f) + 0.15f.coerceAtMost(1f - padOpacity))
                .testTag("dpad_zone")
                .onSizeChanged { dpadSize = it }
                .pointerInput(scale) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val active = event.changes.firstOrNull { it.pressed }
                            if (active == null || dpadSize.width == 0) {
                                dpadLeft = false
                                dpadRight = false
                                dpadUp = false
                                dpadDown = false
                                thumbOffset = Offset.Zero
                            } else {
                                val cx = dpadSize.width * 0.5f
                                val cy = dpadSize.height * 0.5f
                                val dx = active.position.x - cx
                                val dy = active.position.y - cy
                                val dead = dpadSize.width * 0.14f
                                dpadLeft = dx < -dead
                                dpadRight = dx > dead
                                dpadUp = dy < -dead * 1.2f && abs(dy) > abs(dx) * 0.65f
                                dpadDown = dy > dead * 1.2f && abs(dy) > abs(dx) * 0.65f

                                val maxR = dpadSize.width * 0.30f
                                val dist = hypot(dx, dy).coerceAtLeast(1f)
                                val clamped = minOf(dist, maxR) / dist
                                thumbOffset = Offset(dx * clamped, dy * clamped)
                            }
                            emit()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(dpadRingSize)
                    .clip(CircleShape)
                    .background(CyberVoid.copy(alpha = (0.55f * baseAlpha).coerceIn(0.20f, 0.85f)))
                    .border(
                        width = 1.5.dp,
                        color = if (dpadLeft || dpadRight || dpadUp || dpadDown) {
                            EnergonCyan.copy(alpha = 0.85f)
                        } else {
                            Color.White.copy(alpha = (0.28f * baseAlpha).coerceIn(0.15f, 0.65f))
                        },
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Aim Up",
                    tint = if (dpadUp) EnergonCyan else TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = (8f * scale).dp).size((24f * scale).dp),
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Drop Down",
                    tint = if (dpadDown) EnergonCyan else TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (8f * scale).dp).size((24f * scale).dp),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Move Left",
                    tint = if (dpadLeft) EnergonCyan else TextSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = (8f * scale).dp).size((28f * scale).dp),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Move Right",
                    tint = if (dpadRight) EnergonCyan else TextSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = (8f * scale).dp).size((28f * scale).dp),
                )
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                    .size(puckSize)
                    .clip(CircleShape)
                    .background(
                        if (dpadLeft || dpadRight || dpadUp || dpadDown) {
                            EnergonCyan.copy(alpha = 0.38f)
                        } else {
                            Color.White.copy(alpha = 0.16f)
                        }
                    )
                    .border(1.5.dp, EnergonCyan.copy(alpha = 0.85f), CircleShape),
            )
        }

        // ==================== RIGHT 4-BUTTON MULTI-TOUCH ARC ====================
        val density = LocalDensity.current
        var clusterSize by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = clusterW, height = clusterH)
                .alpha(padOpacity.coerceIn(0.30f, 0.98f) + 0.15f.coerceAtMost(1f - padOpacity))
                .onSizeChanged { clusterSize = it }
                .pointerInput(scale) {
                    val shiftRadiusPx = with(density) { (shiftSize * 0.58f).dp.toPx() }
                    val boostRadiusPx = with(density) { (boostSize * 0.58f).dp.toPx() }
                    val fireRadiusPx = with(density) { (fireSize * 0.58f).dp.toPx() }
                    val jumpRadiusPx = with(density) { (jumpSize * 0.58f).dp.toPx() }

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (clusterSize.width == 0) continue

                            val w = clusterSize.width.toFloat()
                            val h = clusterSize.height.toFloat()
                            val shiftCenter = Offset(
                                with(density) { (14f * scale + shiftSize * 0.5f).dp.toPx() },
                                with(density) { (6f * scale + shiftSize * 0.5f).dp.toPx() },
                            )
                            val boostCenter = Offset(
                                w - with(density) { (6f * scale + boostSize * 0.5f).dp.toPx() },
                                with(density) { (6f * scale + boostSize * 0.5f).dp.toPx() },
                            )
                            val fireCenter = Offset(
                                with(density) { (2f * scale + fireSize * 0.5f).dp.toPx() },
                                h - with(density) { (6f * scale + fireSize * 0.5f).dp.toPx() },
                            )
                            val jumpCenter = Offset(
                                w - with(density) { (2f * scale + jumpSize * 0.5f).dp.toPx() },
                                h - with(density) { (jumpSize * 0.5f).dp.toPx() },
                            )

                            var nextShift = false
                            var nextBoost = false
                            var nextFire = false
                            var nextJump = false

                            for (change in event.changes) {
                                if (!change.pressed) continue
                                val p = change.position
                                if ((p - shiftCenter).getDistance() <= shiftRadiusPx) nextShift = true
                                if ((p - boostCenter).getDistance() <= boostRadiusPx) nextBoost = true
                                if ((p - fireCenter).getDistance() <= fireRadiusPx) nextFire = true
                                if ((p - jumpCenter).getDistance() <= jumpRadiusPx) nextJump = true
                            }

                            val shiftJust = nextShift && !btnShift
                            val boostJust = nextBoost && !btnBoost
                            val fireJust = nextFire && !btnFire
                            val jumpJust = nextJump && !btnJump

                            btnShift = nextShift
                            btnBoost = nextBoost
                            btnFire = nextFire
                            btnJump = nextJump

                            emit(
                                jumpJust = jumpJust,
                                fireJust = fireJust,
                                shiftJust = shiftJust,
                                boostJust = boostJust,
                            )
                        }
                    }
                },
        ) {
            ArcadeButton(
                label = if (form == PlayerForm.MECH) "RIG" else "MECH",
                icon = Icons.Filled.Sync,
                accentColor = if (form == PlayerForm.MECH) AutobotCrimson else EnergonCyan,
                pressed = btnShift,
                sizeDp = shiftSize,
                fillAlpha = (0.58f * baseAlpha).coerceIn(0.22f, 0.88f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = (14f * scale).dp, top = (6f * scale).dp)
                    .testTag("btn_shift"),
            )

            ArcadeButton(
                label = "BOOST",
                icon = Icons.Filled.Bolt,
                accentColor = if (energonReady) ShieldEmerald else TextSecondary,
                pressed = btnBoost,
                sizeDp = boostSize,
                fillAlpha = (0.58f * baseAlpha).coerceIn(0.22f, 0.88f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = (6f * scale).dp, top = (6f * scale).dp)
                    .testTag("btn_boost"),
            )

            ArcadeButton(
                label = if (form == PlayerForm.MECH) "FIRE" else "CANNON",
                icon = Icons.Filled.LocalFireDepartment,
                accentColor = MatrixGold,
                pressed = btnFire,
                sizeDp = fireSize,
                fillAlpha = (0.58f * baseAlpha).coerceIn(0.22f, 0.88f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = (2f * scale).dp, bottom = (6f * scale).dp)
                    .testTag("btn_fire"),
            )

            ArcadeButton(
                label = "JUMP",
                icon = Icons.Filled.KeyboardArrowUp,
                accentColor = EnergonCyan,
                pressed = btnJump,
                sizeDp = jumpSize,
                fillAlpha = (0.58f * baseAlpha).coerceIn(0.22f, 0.88f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = (2f * scale).dp, bottom = 0.dp)
                    .testTag("btn_jump"),
            )
        }
    }
}

@Composable
private fun ArcadeButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    pressed: Boolean,
    sizeDp: Float,
    fillAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                if (pressed) accentColor.copy(alpha = 0.48f) else CyberVoid.copy(alpha = fillAlpha)
            )
            .border(
                width = if (pressed) 2.5.dp else 1.5.dp,
                color = if (pressed) Color.White else accentColor.copy(alpha = 0.88f),
                shape = CircleShape,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (pressed) Color.White else accentColor,
            modifier = Modifier.size((sizeDp * 0.40f).dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
            color = if (pressed) Color.White else TextPrimary.copy(alpha = 0.92f),
        )
    }
}
