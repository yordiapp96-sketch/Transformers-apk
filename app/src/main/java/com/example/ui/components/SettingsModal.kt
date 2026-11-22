package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BestRunRecord
import com.example.ui.theme.AutobotCrimson
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberPanel
import com.example.ui.theme.CyberPanelBorder
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.EnergonCyan
import com.example.ui.theme.MatrixGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsModal(
    record: BestRunRecord,
    onPadScaleChange: (Float) -> Unit,
    onPadOpacityChange: (Float) -> Unit,
    onCrtToggle: (Boolean) -> Unit,
    onBgmToggle: (Boolean) -> Unit,
    onSfxToggle: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid.copy(alpha = 0.86f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberNavy)
                .border(1.5.dp, EnergonCyan.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SYSTEM SETTINGS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EnergonCyan,
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_close_button"),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Settings", tint = TextPrimary)
                }
            }

            // Touch Button Size Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberPanel)
                    .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("TOUCH BUTTON SIZE", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text("${(record.padScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = EnergonCyan)
                }
                Slider(
                    value = record.padScale,
                    onValueChange = onPadScaleChange,
                    valueRange = 0.80f..1.30f,
                    colors = SliderDefaults.colors(
                        thumbColor = EnergonCyan,
                        activeTrackColor = EnergonCyan,
                        inactiveTrackColor = CyberPanelBorder,
                    ),
                    modifier = Modifier.testTag("slider_pad_size"),
                )
            }

            // Touch Opacity Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberPanel)
                    .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("CONTROL OPACITY", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text("${(record.padOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = MatrixGold)
                }
                Slider(
                    value = record.padOpacity,
                    onValueChange = onPadOpacityChange,
                    valueRange = 0.30f..0.95f,
                    colors = SliderDefaults.colors(
                        thumbColor = MatrixGold,
                        activeTrackColor = MatrixGold,
                        inactiveTrackColor = CyberPanelBorder,
                    ),
                    modifier = Modifier.testTag("slider_pad_opacity"),
                )
            }

            // Switches row (CRT Scanlines, BGM, SFX)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberPanel)
                    .border(1.dp, CyberPanelBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SettingSwitchRow(
                    label = "CRT SCANLINES",
                    checked = record.crtScanlines,
                    onCheckedChange = onCrtToggle,
                    tag = "switch_crt",
                )
                SettingSwitchRow(
                    label = "CHIPTUNE MUSIC (BGM)",
                    checked = !record.bgmMuted,
                    onCheckedChange = { enabled -> onBgmToggle(!enabled) },
                    tag = "switch_bgm",
                )
                SettingSwitchRow(
                    label = "SOUND EFFECTS (SFX)",
                    checked = !record.sfxMuted,
                    onCheckedChange = { enabled -> onSfxToggle(!enabled) },
                    tag = "switch_sfx",
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("settings_reset_button"),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESET", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = AutobotCrimson, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("settings_done_button"),
                ) {
                    Text("DONE", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberVoid,
                checkedTrackColor = EnergonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CyberVoid,
            ),
            modifier = Modifier.testTag(tag),
        )
    }
}
