package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.HudState
import com.example.game.model.PlayerForm
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AutobotCrimson
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberPanelBorder
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.DecepticonViolet
import com.example.ui.theme.EnergonCyan
import com.example.ui.theme.MatrixGold
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CyberHud(
    hud: HudState,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // ==================== TOP BAR ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // LEFT: Form Badge + Segmented Armor HP + Energon Gauge
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberVoid.copy(alpha = 0.72f))
                    .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Form pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hud.form == PlayerForm.MECH) {
                                    AutobotCrimson.copy(alpha = 0.25f)
                                } else {
                                    EnergonCyan.copy(alpha = 0.25f)
                                }
                            )
                            .border(
                                1.dp,
                                if (hud.form == PlayerForm.MECH) AutobotCrimson else EnergonCyan,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (hud.form == PlayerForm.MECH) {
                                Icons.Filled.PrecisionManufacturing
                            } else {
                                Icons.Filled.DirectionsCar
                            },
                            contentDescription = "Active Form",
                            tint = if (hud.form == PlayerForm.MECH) AutobotCrimson else EnergonCyan,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = if (hud.form == PlayerForm.MECH) "MECH" else "RIG",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextPrimary,
                        )
                    }

                    // 10-segment HP bar
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val hpColor = when {
                            hud.hp <= 3 -> AlertRed
                            hud.hp <= 6 -> MatrixGold
                            else -> ShieldEmerald
                        }
                        for (i in 1..hud.maxHp) {
                            Box(
                                modifier = Modifier
                                    .size(width = 9.dp, height = 11.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (i <= hud.hp) hpColor else Color.White.copy(alpha = 0.12f)),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Energon Boost Gauge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "ENG",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = EnergonCyan,
                    )
                    Box(
                        modifier = Modifier
                            .width(122.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                    ) {
                        val frac = (hud.energon / hud.maxEnergon).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .height(6.dp)
                                .background(if (hud.energon >= 30f) EnergonCyan else TextSecondary),
                        )
                    }
                }

                // Cannon Charge Meter (visible while charging Mech beam)
                if (hud.chargeRatio > 0.10f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (hud.chargeRatio >= 1f) "MAX" else "CHG",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (hud.chargeRatio >= 1f) MatrixGold else EnergonCyan,
                        )
                        Box(
                            modifier = Modifier
                                .width(122.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(hud.chargeRatio)
                                    .height(5.dp)
                                    .background(if (hud.chargeRatio >= 1f) MatrixGold else EnergonCyan),
                            )
                        }
                    }
                }
            }

            // CENTER: Timer, Progress & Matrix Relics (or Boss Health Bar)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 270.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberVoid.copy(alpha = 0.72f))
                        .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = formatTimer(hud.elapsedSeconds),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = TextPrimary,
                    )

                    // 3 Matrix Relics
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        for (idx in 0 until hud.totalRelics) {
                            val collected = idx < hud.relicsCollected
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = if (collected) "Matrix Relic Collected" else "Matrix Relic Uncollected",
                                tint = if (collected) MatrixGold else Color.White.copy(alpha = 0.20f),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    // Mini sector progress
                    if (!hud.bossActive) {
                        Text(
                            text = "${(hud.progressPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = EnergonCyan,
                        )
                    }
                }

                // Boss Health Bar
                AnimatedVisibility(
                    visible = hud.bossActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberVoid.copy(alpha = 0.85f))
                            .border(
                                1.5.dp,
                                if (hud.bossOverclock) AlertRed else DecepticonViolet,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (hud.bossOverclock) "${hud.bossName} [OVERCLOCK]" else hud.bossName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = AlertRed,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.14f)),
                        ) {
                            val bFrac = (hud.bossHp.toFloat() / hud.bossMaxHp).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bFrac)
                                    .height(8.dp)
                                    .background(AlertRed),
                            )
                        }
                    }
                }
            }

            // RIGHT: Score + Combo + Audio & Pause Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberVoid.copy(alpha = 0.72f))
                        .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = String.format(Locale.US, "%06d", hud.score),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = MatrixGold,
                    )
                    if (hud.combo > 1) {
                        Text(
                            text = "x${hud.combo} COMBO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = EnergonCyan,
                        )
                    } else {
                        Text(
                            text = "SCORE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextSecondary,
                        )
                    }
                }

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberVoid.copy(alpha = 0.72f))
                        .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                        .testTag("btn_mute"),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                        tint = if (isMuted) TextSecondary else EnergonCyan,
                    )
                }

                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberVoid.copy(alpha = 0.72f))
                        .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                        .testTag("btn_pause"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Pause Game",
                        tint = TextPrimary,
                    )
                }
            }
        }

        // ==================== ZONE BANNER, OVERCLOCK ALERT & HINT PILL ====================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (hud.bossActive) 90.dp else 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = hud.overclockBannerTimer > 0f,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AlertRed.copy(alpha = 0.25f))
                        .border(1.5.dp, AlertRed, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "WARNING // OVERCLOCK PHASE",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = AlertRed,
                    )
                }
            }

            AnimatedVisibility(
                visible = hud.zoneBannerTimer > 0f,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberNavy.copy(alpha = 0.85f))
                        .border(1.dp, EnergonCyan.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = hud.zone.label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = EnergonCyan,
                    )
                }
            }

            AnimatedVisibility(
                visible = hud.hintText != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                hud.hintText?.let { hint ->
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberVoid.copy(alpha = 0.78f))
                            .border(1.dp, MatrixGold.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MatrixGold,
                        )
                    }
                }
            }
        }
    }
}

fun formatTimer(seconds: Float): String {
    val totalTenths = (seconds * 10f).toInt().coerceAtLeast(0)
    val mins = totalTenths / 600
    val secs = (totalTenths / 10) % 60
    val tenths = totalTenths % 10
    return String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
}
