package com.example.receiptscanner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val skipReview by viewModel.skipReview.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val stabilityDuration by viewModel.stabilityDuration.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // スキャン設定
            SettingsCard(title = "スキャン設定") {
                // 効果音
                SettingsToggleRow(
                    title = "効果音",
                    subtitle = "レシート読み取り完了時に音を鳴らす",
                    checked = soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 確認画面スキップ
                SettingsToggleRow(
                    title = "確認画面をスキップ（高速モード）",
                    subtitle = "Gemini解析後、確認なしで自動保存する",
                    checked = skipReview,
                    onCheckedChange = viewModel::setSkipReview
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 安定判定時間
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "安定判定時間: ${"%.1f".format(stabilityDuration)}秒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = stabilityDuration,
                        onValueChange = viewModel::setStabilityDuration,
                        valueRange = 0.5f..3.0f,
                        steps = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // AI設定
            SettingsCard(title = "AI解析設定 (Google Gemini)") {
                Text(
                    "Gemini API キーを設定するとAIによる高精度な科目・取引先・内容の自動抽出が使えます。" +
                    "Google AI Studio (aistudio.google.com) で無料のAPIキーを取得できます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = viewModel::setGeminiApiKey,
                    label = { Text("Gemini API キー") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    placeholder = { Text("AIzaSy...") }
                )
            }

            // 科目設定
            SettingsCard(title = "科目リスト") {
                Text(
                    "カンマ区切りで科目を設定してください",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = categories.joinToString(", "),
                    onValueChange = { text ->
                        val cats = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        viewModel.setCategories(cats)
                    },
                    label = { Text("科目リスト") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    minLines = 3
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
