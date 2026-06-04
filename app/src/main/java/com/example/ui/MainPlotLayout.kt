package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.math.MathParser
import com.example.math.PredefinedFunctions
import com.example.ui.components.Plotter2D
import com.example.ui.components.Plotter3D

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPlotLayout(
    viewModel: MathPlotViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MathPlot 2D/3D",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Hệ thống khảo sát đồ thị tương tác",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.currentTab == 0) viewModel.resetView2D()
                        else if (uiState.currentTab == 1) viewModel.resetView3D()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Căn giữa góc nhìn",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.currentTab == 0,
                    onClick = { viewModel.updateTab(0) },
                    icon = { Icon(Icons.Default.Create, contentDescription = "Đồ thị 2D") },
                    label = { Text("Đồ thị 2D", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == 1,
                    onClick = { viewModel.updateTab(1) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Đồ thị 3D") },
                    label = { Text("Đồ thị 3D", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == 2,
                    onClick = { viewModel.updateTab(2) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Thư viện") },
                    label = { Text("Thư viện", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == 3,
                    onClick = { viewModel.updateTab(3) },
                    icon = { Icon(Icons.Default.Face, contentDescription = "Trợ lý AI") },
                    label = { Text("Trợ lý AI", fontSize = 11.sp) }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (uiState.currentTab) {
                0 -> Screen2DPlot(uiState, viewModel)
                1 -> Screen3DPlot(uiState, viewModel)
                2 -> ScreenPresetsCatalog(viewModel)
                3 -> ScreenAiAssistant(uiState, viewModel)
            }
        }
    }
}

@Composable
fun Screen2DPlot(
    uiState: MathUiState,
    viewModel: MathPlotViewModel
) {
    var textInput by remember { mutableStateOf(uiState.formula2D) }
    
    // Sync text input when state changes externally (e.g., from presets loading)
    LaunchedEffect(uiState.formula2D) {
        textInput = uiState.formula2D
    }

    // Parsing Validation check
    val parserError = remember(textInput) {
        if (textInput.isEmpty()) "Vui lòng nhập phương trình hàm số."
        else {
            try {
                val parser = MathParser(textInput)
                parser.parse(1.0) // Test run evaluation
                null
            } catch (e: Exception) {
                e.message ?: "Cú pháp không hợp lệ."
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Nhập hàm số hai chiều y = f(x):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ví dụ: sin(x) + cos(x)") },
                        singleLine = true,
                        isError = parserError != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onAny = {
                            if (parserError == null) {
                                viewModel.updateFormula2D(textInput)
                                viewModel.addToHistory(textInput)
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (parserError == null) {
                                viewModel.updateFormula2D(textInput)
                                viewModel.addToHistory(textInput)
                            }
                        },
                        enabled = parserError == null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Vẽ ngay")
                    }
                }

                if (parserError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ $parserError",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Active Plot Canvas bounds card
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            // Interactive canvas:
            Plotter2D(
                formula = uiState.formula2D,
                scale = uiState.scale2D,
                centerX = uiState.centerX2D,
                centerY = uiState.centerY2D,
                onTransform = { scale, cx, cy ->
                    viewModel.updateTransform2D(scale, cx, cy)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlays & Guides
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Đang vẽ: y = ${uiState.formula2D}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tọa độ tâm: (${String.format("%.2f", uiState.centerX2D)}, ${String.format("%.2f", uiState.centerY2D)}) | Zoom: x${String.format("%.1f", uiState.scale2D / 45f)}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "🔎 Nhón tay để Zoom | Vuốt để di chuyển",
                        color = MaterialTheme.colorScheme.primaryContainer,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun Screen3DPlot(
    uiState: MathUiState,
    viewModel: MathPlotViewModel
) {
    var textInput by remember { mutableStateOf(uiState.formula3D) }
    
    LaunchedEffect(uiState.formula3D) {
        textInput = uiState.formula3D
    }

    val parserError = remember(textInput) {
        if (textInput.isEmpty()) "Vui lòng nhập phương trình."
        else {
            try {
                val parser = MathParser(textInput)
                parser.parse(1.0, 1.0) // Test run in 3D
                null
            } catch (e: Exception) {
                e.message ?: "Cú pháp không hợp lệ."
            }
        }
    }

    var rangeInputMin by remember { mutableStateOf(uiState.rangeMin3D.toString()) }
    var rangeInputMax by remember { mutableStateOf(uiState.rangeMax3D.toString()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Nhập hàm số ba chiều z = f(x, y):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ví dụ: sin(x) * cos(y)") },
                        singleLine = true,
                        isError = parserError != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onAny = {
                            if (parserError == null) {
                                viewModel.updateFormula3D(textInput)
                                viewModel.addToHistory(textInput)
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (parserError == null) {
                                viewModel.updateFormula3D(textInput)
                                viewModel.addToHistory(textInput)
                            }
                        },
                        enabled = parserError == null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Vẽ ngay")
                    }
                }

                if (parserError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ $parserError",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Ranges parameters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phạm vi vẽ (X và Y): ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = rangeInputMin,
                        onValueChange = {
                            rangeInputMin = it
                            it.toFloatOrNull()?.let { minV ->
                                viewModel.update3DRange(minV, uiState.rangeMax3D)
                            }
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .height(50.dp),
                        singleLine = true,
                        label = { Text("Min X,Y", fontSize = 9.sp) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "đến", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = rangeInputMax,
                        onValueChange = {
                            rangeInputMax = it
                            it.toFloatOrNull()?.let { maxV ->
                                viewModel.update3DRange(uiState.rangeMin3D, maxV)
                            }
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .height(50.dp),
                        singleLine = true,
                        label = { Text("Max X,Y", fontSize = 9.sp) }
                    )
                }
            }
        }

        // Active 3D plotter canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(Color(0xFF12141C)) // Dark space canvas looks incredible for 3D color mapping lines
        ) {
            Plotter3D(
                formula = uiState.formula3D,
                scale = uiState.scale3D,
                theta = uiState.theta3D,
                phi = uiState.phi3D,
                rangeMin = uiState.rangeMin3D,
                rangeMax = uiState.rangeMax3D,
                onTransform = { scale, theta, phi ->
                    viewModel.updateTransform3D(scale, theta, phi)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlays & Guides
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Đang vẽ: z = ${uiState.formula3D}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Góc xoay: Azimuth=${(uiState.theta3D * 180 / Math.PI).roundToInt()}° Tilt=${(uiState.phi3D * 180 / Math.PI).roundToInt()}° | Phạm vi: [${uiState.rangeMin3D}, ${uiState.rangeMax3D}]",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "🔄 Vuốt 1 ngón để xoay | Bóp 2 ngón để Zoom",
                        color = Color(0xFFFFB74D),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenPresetsCatalog(viewModel: MathPlotViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        item {
            Text(
                text = "📁 Thư Viện Hàm Số Kỳ Thú",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Khám phá các phương trình hình học độc đáo được lựa chọn sẵn. Bấm nút để vẽ ngay lên bảng tương tác.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            Text(
                text = "📉 Hàm Số 2D (Mặt Phẳng Oxy)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(PredefinedFunctions.presets2D) { preset ->
            PresetCard(preset = preset, onSelect = { viewModel.selectPreset(preset) })
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Text(
                text = "📐 Hàm Số 3D (Không Gian Oxyz)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        items(PredefinedFunctions.presets3D) { preset ->
            PresetCard(preset = preset, onSelect = { viewModel.selectPreset(preset) })
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun PresetCard(
    preset: com.example.math.GraphPreset,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Badge(
                    containerColor = if (preset.is3D) Color(0xFF64B5F6) else Color(0xFF81C784),
                ) {
                    Text(
                        text = if (preset.is3D) "3D Oxyz" else "2D Oxy",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(8.dp)
            ) {
                Text(
                    text = if (preset.is3D) "z = ${preset.formula}" else "y = ${preset.formula}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (preset.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = preset.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Vẽ đồ thị này", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ScreenAiAssistant(
    uiState: MathUiState,
    viewModel: MathPlotViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "🧠 Trợ Lý Toán Học AI",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Yêu cầu trợ lý thiết kế hoặc gợi ý bất kỳ đồ thị nào. AI sẽ trả về phương trình hoàn hảo cùng lời giải thích cặn kẽ.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Prompt Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = uiState.aiPrompt,
                    onValueChange = { viewModel.updateAiPrompt(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ý tưởng hoặc câu hỏi của bạn...") },
                    placeholder = { Text("Ví dụ: gợi ý cho mình đồ thị 3D hình mũ vành rộng rực rỡ") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.executeAiRecommendation()
                        keyboardController?.hide()
                    })
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.executeAiRecommendation()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = uiState.aiPrompt.trim().isNotEmpty() && !uiState.aiLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.aiLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang suy nghĩ...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gửi yêu cầu")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Response Panel
        Text(
            text = "Kết quả đề xuất:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (uiState.aiLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                "AI đang phân tích yêu cầu toán học và xây dựng cấu trúc đồ thị phù hợp...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                } else if (uiState.aiError != null) {
                    item {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚠️ Có lỗi xảy ra:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.aiError ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else if (uiState.aiExplanationResult != null) {
                    item {
                        Column {
                            Text(
                                text = "💡 Câu trả lời từ Math AI:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.aiExplanationResult ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Quick Plot loading Card
                            uiState.aiSuggestedFormula?.let { formula ->
                                Spacer(modifier = Modifier.height(14.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🚀 Bản vẽ toán học sẵn sàng:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (uiState.aiIs3D) "z = $formula" else "y = $formula",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                viewModel.applyFromAI(
                                                    formula = formula,
                                                    is3D = uiState.aiIs3D,
                                                    min = uiState.aiRangeMin,
                                                    max = uiState.aiRangeMax
                                                )
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "⚡ TẢI LÊN & VẼ ĐỒ THỊ NGAY",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .wrapContentSize(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ví dụ: 'vẽ hình cái phễu' hoặc 'đưa ra phương trình trái tim 2D'",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
