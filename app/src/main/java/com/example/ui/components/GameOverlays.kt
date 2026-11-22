package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BestRunRecord
import com.example.game.model.HudState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AutobotCrimson
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberPanel
import com.example.ui.theme.CyberPanelBorder
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.EnergonCyan
import com.example.ui.theme.MatrixGold
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TitleBriefingScreen(
    bestRecord: BestRunRecord,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit,
    onDeploy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid),
    ) {
        // Generated 16-bit hero artwork backdrop
        Image(
            painter = painterResource(id = R.drawable.img_title_hero_1795311125821),
            contentDescription = "Vanguard Prime guarding Sector 01 Energon Viaduct",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Asymmetric cybernetic readability gradient (darkens left briefing column while showcasing Mech on right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to CyberVoid.copy(alpha = 0.94f),
                        0.48f to CyberVoid.copy(alpha = 0.78f),
                        0.82f to CyberVoid.copy(alpha = 0.25f),
                        1.0f to CyberVoid.copy(alpha = 0.40f),
                    )
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Audio & Settings buttons top-right
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberVoid.copy(alpha = 0.75f))
                        .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                        .testTag("title_settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Open Settings",
                        tint = EnergonCyan,
                    )
                }

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberVoid.copy(alpha = 0.75f))
                        .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                        .testTag("title_mute_button"),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                        tint = if (isMuted) TextSecondary else EnergonCyan,
                    )
                }
            }

            // Left Mission Briefing Column
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "CYBERSHIFT",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                    color = TextPrimary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .background(AutobotCrimson),
                    )
                    Text(
                        text = stringResource(R.string.mission_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = EnergonCyan,
                    )
                }

                // Compact Combat Legend Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberPanel.copy(alpha = 0.84f))
                        .border(1.dp, CyberPanelBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = null,
                            tint = EnergonCyan,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "SHIFT: Switch Mech (Double-Jump + Blade) & Rig (Laser Slide)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = ShieldEmerald,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "BOOST: Nitro Ram Barriers (Rig) or Phase Dash (Mech)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                    }
                }

                // Best Record Row if player has cleared before
                if (bestRecord.bestScore > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberVoid.copy(alpha = 0.80f))
                            .border(1.dp, MatrixGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("BEST SCORE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${bestRecord.bestScore}", style = MaterialTheme.typography.titleMedium, color = MatrixGold)
                        }
                        Column {
                            Text("BEST TIME", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                if (bestRecord.bestTimeSeconds > 0f) formatTimer(bestRecord.bestTimeSeconds) else "--:--",
                                style = MaterialTheme.typography.titleMedium,
                                color = EnergonCyan,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RANK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(bestRecord.bestRank, style = MaterialTheme.typography.titleMedium, color = ShieldEmerald)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = onDeploy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AutobotCrimson,
                        contentColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("deploy_button"),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_deploy),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestart: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberNavy)
                .border(1.5.dp, EnergonCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.paused_title),
                style = MaterialTheme.typography.headlineMedium,
                color = EnergonCyan,
            )

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = EnergonCyan, contentColor = CyberVoid),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button"),
            ) {
                Text(stringResource(R.string.btn_resume), style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_settings_button"),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = EnergonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SETTINGS", style = MaterialTheme.typography.labelLarge, color = EnergonCyan)
            }

            OutlinedButton(
                onClick = onRestart,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_restart_button"),
            ) {
                Text(stringResource(R.string.btn_restart), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            }

            OutlinedButton(
                onClick = onAbort,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_abort_button"),
            ) {
                Text(stringResource(R.string.btn_abort), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    hud: HudState,
    onRetry: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid.copy(alpha = 0.84f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberNavy)
                .border(1.5.dp, AlertRed, RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.game_over_title),
                style = MaterialTheme.typography.headlineMedium,
                color = AlertRed,
            )
            Text(
                text = "Reached ${(hud.progressPercent * 100).toInt()}% • Score ${hud.score}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AutobotCrimson, contentColor = TextPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("retry_button"),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_restart), style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onAbort,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("gameover_abort_button"),
            ) {
                Text(stringResource(R.string.btn_abort), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
        }
    }
}

@Composable
fun VictoryOverlay(
    hud: HudState,
    onPlayAgain: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rankColor = when (hud.rank) {
        "S" -> MatrixGold
        "A" -> EnergonCyan
        "B" -> ShieldEmerald
        else -> TextPrimary
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberNavy)
                .border(2.dp, rankColor, RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.victory_title),
                style = MaterialTheme.typography.headlineMedium,
                color = EnergonCyan,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RANK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = hud.rank,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                        color = rankColor,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    StatLine("SCORE", "${hud.score}", MatrixGold)
                    StatLine("CLEAR TIME", formatTimer(hud.elapsedSeconds), EnergonCyan)
                    StatLine("RELICS", "${hud.relicsCollected} / ${hud.totalRelics}", MatrixGold)
                    StatLine("HITS TAKEN", "${hud.damageTakenCount}", if (hud.damageTakenCount <= 2) ShieldEmerald else TextPrimary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = AutobotCrimson, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("victory_replay_button"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_restart), style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onAbort,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("victory_base_button"),
                ) {
                    Text(stringResource(R.string.btn_abort), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, valueColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}
