package com.example.receiptscanner.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.receiptscanner.domain.model.ParsedReceiptFields
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    parsedFields: ParsedReceiptFields,
    photoPath: String,
    photoNumber: Int,
    onSaved: () -> Unit,
    onDiscard: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    LaunchedEffect(parsedFields) {
        viewModel.initialize(parsedFields, photoPath, photoNumber)
    }

    val isSaved by viewModel.isSaved.collectAsState()
    LaunchedEffect(isSaved) {
        if (isSaved) onSaved()
    }

    val category by viewModel.category.collectAsState()
    val supplier by viewModel.supplier.collectAsState()
    val description by viewModel.description.collectAsState()
    val amountWithTax by viewModel.amountWithTax.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()
    val photoNumber by viewModel.photoNumber.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("読み取り結果", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDiscard) {
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
            // レシートサムネイル
            if (photoPath.isNotEmpty() && File(photoPath).exists()) {
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = "レシート画像",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 科目ドロップダウン
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("科目") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.setCategory(cat)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 取引先
            OutlinedTextField(
                value = supplier,
                onValueChange = viewModel::setSupplier,
                label = { Text("取引先") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 内容
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::setDescription,
                label = { Text("内容") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 税込み価格
            OutlinedTextField(
                value = amountWithTax,
                onValueChange = viewModel::setAmountWithTax,
                label = { Text("税込み価格 (円)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                prefix = { Text("¥") }
            )

            // 消費税率
            Text("消費税率", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = taxRate == 10,
                    onClick = { viewModel.setTaxRate(10) },
                    label = { Text("10%（標準）") }
                )
                FilterChip(
                    selected = taxRate == 8,
                    onClick = { viewModel.setTaxRate(8) },
                    label = { Text("8%（軽減）") }
                )
            }

            // 写真番号（読み取り専用）
            OutlinedTextField(
                value = "%04d".format(photoNumber),
                onValueChange = {},
                readOnly = true,
                label = { Text("写真番号") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            Spacer(Modifier.height(8.dp))

            // ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("破棄")
                }
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.weight(2f)
                ) {
                    Text("保存して次へ →")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
