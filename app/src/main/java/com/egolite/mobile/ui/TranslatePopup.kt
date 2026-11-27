package com.egolite.mobile.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.LangOption
import com.egolite.mobile.browser.TranslationManager
import com.egolite.mobile.model.TranslationEngine
import com.egolite.mobile.model.TranslationPopupState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.ProgressBarStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ChromeStyleTranslatePopup(
    state: TranslationPopupState,
    engine: TranslationEngine,
    currentHost: String,
    alwaysTranslateSite: Boolean,
    pinnedLangs: List<String>,
    onChangeSourceLang: (String) -> Unit,
    onChangeTargetLang: (String) -> Unit,
    onTogglePinLang: (String) -> Unit,
    onToggleAlwaysTranslate: () -> Unit,
    onUndoRestore: () -> Unit,
    onDismiss: (VanishSnapshot) -> Unit
) {
    var pickingForSource by remember { mutableStateOf<Boolean?>(null) }
    var capsuleRect by remember { mutableStateOf<Rect?>(null) }
    val capsuleCaptureLayer = rememberGraphicsLayer()
    val capsuleCaptureScope = rememberCoroutineScope()
    val prefs by SpaceManager.preferences.collectAsState()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 92.dp.toPx() }
    val swipeX = remember(state.visible) { Animatable(0f) }

    fun dismissCapsuleWithCapture() {
        capsuleCaptureScope.launch {
            val bmp = runCatching { capsuleCaptureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            val origin = capsuleRect?.let { Offset(it.right - 22f, it.center.y) }
            onDismiss(VanishSnapshot(rect = capsuleRect, touchRoot = origin, bitmap = bmp))
        }
    }

    AnimatedVisibility(
        visible = state.visible,
        enter = slideInVertically(EgoMotion.intOffsetSpring) { -it } + fadeIn(tween(180)),
        exit = ExitTransition.None
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val cardBg = if (isDark) Color(0xFF292C33) else Color(0xFFF3F6FC)
        val invTextCol = if (isDark) Color(0xFF090D16) else Color.White
        val trProg = state.progress.coerceIn(0.06f, 0.99f)
        val trActive = state.isTranslating

        // 다운로드 배너와 동일한 22dp 알약 카드 + 좌우 스와이프 닫기 + 하모니OS 7 방식 B 번역 진행 광감
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .onGloballyPositioned { coords -> capsuleRect = coords.boundsInRoot() }
                .drawWithContent {
                    capsuleCaptureLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(capsuleCaptureLayer)
                }
                .graphicsLayer {
                    translationX = swipeX.value
                    alpha = (1f - (abs(swipeX.value) / (dismissThresholdPx * 2.2f))).coerceIn(0.15f, 1f)
                }
                .pointerInput(state.visible) {
                    val vt = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { vt.resetTracking() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            vt.addPosition(change.uptimeMillis, change.position)
                            capsuleCaptureScope.launch { swipeX.snapTo(swipeX.value + dragAmount) }
                        },
                        onDragEnd = {
                            val vx = vt.calculateVelocity().x
                            capsuleCaptureScope.launch {
                                if (abs(swipeX.value) >= dismissThresholdPx || abs(vx) >= 950f) {
                                    val dir = if (abs(vx) >= 950f) (if (vx >= 0f) 1f else -1f) else (if (swipeX.value >= 0f) 1f else -1f)
                                    swipeX.animateTo(
                                        targetValue = dir * dismissThresholdPx * 4.5f,
                                        animationSpec = EgoMotion.responsiveSpring(),
                                        initialVelocity = vx
                                    )
                                    dismissCapsuleWithCapture()
                                } else {
                                    swipeX.animateTo(0f, animationSpec = EgoMotion.responsiveSpring(), initialVelocity = vx)
                                }
                            }
                        },
                        onDragCancel = {
                            capsuleCaptureScope.launch { swipeX.animateTo(0f, animationSpec = EgoMotion.responsiveSpring()) }
                        }
                    )
                }
                .harmonyRimGlow(enabled = prefs.rimGlowEnabled, cornerRadius = 22.dp, glowColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // 하모니OS 7 방식 B 광감 진행바 (번역 진행률 실시간 반영, 완료 시 깔끔히 소멸)
                HarmonyHorizontalProgressCanvas(
                    style = ProgressBarStyle.HARMONY_OPTICAL,
                    progress = if (trActive) trProg else 1f,
                    isActive = trActive,
                    accentColor = MaterialTheme.colorScheme.primary,
                    lightIntensity = prefs.progressLightIntensity,
                    particleDensity = prefs.progressParticleDensity,
                    cornerRadiusDp = 22.dp,
                    modifier = Modifier.matchParentSize()
                )

                @Composable
                fun TranslateBannerContent(
                    primaryCol: Color,
                    onSurfCol: Color,
                    mutedCol: Color,
                    chipBgCol: Color,
                    targetChipBgCol: Color
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = EgoMotion.intSizeSpring)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EgoIcon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = if (state.isTranslated || trActive) primaryCol else mutedCol,
                            modifier = Modifier.size(21.dp),
                            symbolSize = 21.dp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                // 출발어 칩
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(chipBgCol)
                                        .clickable { pickingForSource = true }
                                        .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (state.sourceLang == "auto") state.detectedLangLabel else TranslationManager.labelFor(state.sourceLang),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onSurfCol,
                                        maxLines = 1
                                    )
                                    EgoIcon(Icons.Default.ArrowDropDown, contentDescription = null, tint = onSurfCol, modifier = Modifier.size(14.dp), symbolSize = 14.dp)
                                }
                                Text("→", fontSize = 11.5.sp, color = mutedCol)
                                // 도착어 칩
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(targetChipBgCol)
                                        .clickable { pickingForSource = false }
                                        .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = TranslationManager.labelFor(state.targetLang),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryCol,
                                        maxLines = 1
                                    )
                                    EgoIcon(Icons.Default.ArrowDropDown, contentDescription = null, tint = primaryCol, modifier = Modifier.size(14.dp), symbolSize = 14.dp)
                                }
                            }

                            val subLine = when {
                                trActive -> "${state.statusText.ifBlank { "페이지 번역 중..." }} · ${(trProg * 100).roundToInt()}%"
                                state.isTranslated -> "번역 완료 · 스와이프하여 닫기"
                                state.statusText.isNotBlank() -> state.statusText
                                else -> "번역 준비됨"
                            }
                            Text(
                                text = subLine,
                                fontSize = 11.5.sp,
                                color = if (!trActive && !state.isTranslated && state.statusText.isNotBlank()) EgoDangerRed else mutedCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (state.isTranslated && !trActive) {
                            Text(
                                text = "원문",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryCol,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onUndoRestore() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (currentHost.isNotBlank()) {
                            IconButton(
                                onClick = onToggleAlwaysTranslate,
                                modifier = Modifier.size(28.dp)
                            ) {
                                EgoIcon(
                                    imageVector = if (alwaysTranslateSite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "이 사이트 항상 번역",
                                    tint = if (alwaysTranslateSite) primaryCol else mutedCol,
                                    modifier = Modifier.size(18.dp),
                                    symbolSize = 18.dp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { dismissCapsuleWithCapture() },
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                Icons.Default.Close,
                                contentDescription = "닫기",
                                modifier = Modifier.size(16.dp),
                                tint = mutedCol,
                                symbolSize = 16.dp
                            )
                        }
                    }
                }

                TranslateBannerContent(
                    primaryCol = MaterialTheme.colorScheme.primary,
                    onSurfCol = MaterialTheme.colorScheme.onSurface,
                    mutedCol = MaterialTheme.colorScheme.onSurfaceVariant,
                    chipBgCol = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    targetChipBgCol = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                )

                // 방식 B: 지나간 영역은 선명한 검정 글자 유지, 100% 완료 시 자연스럽게 원복
                HarmonyModeBClippedOverlay(
                    enabled = trActive,
                    progress = trProg,
                    isActive = trActive
                ,
                    modifier = Modifier.matchParentSize()
                ) {
                    TranslateBannerContent(
                        primaryCol = invTextCol,
                        onSurfCol = invTextCol,
                        mutedCol = invTextCol.copy(alpha = 0.85f),
                        chipBgCol = invTextCol.copy(alpha = 0.14f),
                        targetChipBgCol = invTextCol.copy(alpha = 0.18f)
                    )
                }
            }
        }
    }

    // 크롬 수준 58개 언어 선택 + 검색 + 자주 쓰는 언어 상단 핀 고정 다이얼로그
    pickingForSource?.let { isSource ->
        LanguagePickerDialog(
            title = if (isSource) "출발어 선택" else "도착어 선택",
            includeAuto = isSource,
            selectedCode = if (isSource) state.sourceLang else state.targetLang,
            pinnedCodes = pinnedLangs,
            onSelect = { code ->
                pickingForSource = null
                if (isSource) onChangeSourceLang(code) else onChangeTargetLang(code)
            },
            onTogglePin = onTogglePinLang,
            onDismiss = { pickingForSource = null }
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    title: String,
    includeAuto: Boolean,
    selectedCode: String,
    pinnedCodes: List<String>,
    onSelect: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allTargets = TranslationManager.targetLanguages
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allTargets
        else allTargets.filter { it.label.lowercase().contains(q) || it.code.lowercase().contains(q) }
    }
    val pinnedItems = remember(filtered, pinnedCodes) {
        pinnedCodes.mapNotNull { code -> filtered.firstOrNull { it.code.equals(code, ignoreCase = true) } }
    }
    val pinnedSet = remember(pinnedCodes) { pinnedCodes.map { it.lowercase() }.toSet() }
    val unpinnedItems = remember(filtered, pinnedSet) {
        filtered.filterNot { it.code.lowercase() in pinnedSet }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("언어 검색 (예: 영어, 일본어, en)...", fontSize = 13.sp) },
                    leadingIcon = { EgoIcon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), symbolSize = 18.dp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (includeAuto && query.isBlank()) {
                        item(key = "auto") {
                            LanguageRow(
                                option = LangOption("auto", "자동 감지"),
                                isSelected = selectedCode == "auto",
                                isPinned = false,
                                pinnable = false,
                                onSelect = { onSelect("auto") },
                                onTogglePin = {}
                            )
                        }
                    }

                    if (pinnedItems.isNotEmpty()) {
                        item(key = "hdr_pinned") {
                            Text(
                                text = "자주 쓰는 언어 (상단 고정)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }
                        items(pinnedItems, key = { "pin_${it.code}" }) { lang ->
                            LanguageRow(
                                option = lang,
                                isSelected = selectedCode.equals(lang.code, ignoreCase = true),
                                isPinned = true,
                                pinnable = true,
                                onSelect = { onSelect(lang.code) },
                                onTogglePin = { onTogglePin(lang.code) }
                            )
                        }
                        item(key = "div_pinned") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    item(key = "hdr_all") {
                        Text(
                            text = "전체 언어 (${unpinnedItems.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                        )
                    }
                    items(unpinnedItems, key = { "all_${it.code}" }) { lang ->
                        LanguageRow(
                            option = lang,
                            isSelected = selectedCode.equals(lang.code, ignoreCase = true),
                            isPinned = false,
                            pinnable = true,
                            onSelect = { onSelect(lang.code) },
                            onTogglePin = { onTogglePin(lang.code) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) { Text("닫기") }
        }
    )
}

@Composable
private fun LanguageRow(
    option: LangOption,
    isSelected: Boolean,
    isPinned: Boolean,
    pinnable: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option.label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = option.code,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (pinnable) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(28.dp)
            ) {
                EgoIcon(
                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (isPinned) "고정 해제" else "상단 고정",
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                , symbolSize = 16.dp)
            }
        }
    }
}


/**
 * 주소줄 번역 버튼 길게 누르기(Long-press) 시 표시되는 번역 설정 팝업
 */
@Composable
fun TranslateSettingsPopupDialog(
    engine: TranslationEngine,
    defaultTargetLang: String,
    offerTranslate: Boolean,
    currentHost: String,
    alwaysTranslateSite: Boolean,
    onChangeEngine: (TranslationEngine) -> Unit,
    onChangeDefaultTarget: (String) -> Unit,
    onToggleOfferTranslate: (Boolean) -> Unit,
    onToggleAlwaysTranslateSite: () -> Unit,
    anchorRect: Rect? = null,
    onDismiss: (VanishSnapshot) -> Unit
) {
    val quickLangs = listOf(
        "ko" to "한국어",
        "en" to "영어",
        "ja" to "일본어",
        "zh-CN" to "중국어(간체)",
        "es" to "스페인어"
    )
    var cardVisible by remember { mutableStateOf(false) }
    var cardRect by remember { mutableStateOf<Rect?>(null) }
    val captureLayer = rememberGraphicsLayer()
    val captureScope = rememberCoroutineScope()
    fun dismissWithCapture(touchRoot: Offset?) {
        captureScope.launch {
            val bmp = runCatching { captureLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            onDismiss(VanishSnapshot(rect = cardRect, touchRoot = touchRoot, bitmap = bmp))
        }
    }
    LaunchedEffect(Unit) { cardVisible = true }

    // 전체 반투명 검정 배경 없이, 번역 버튼을 덮으며 우상단에서 모프 확장되는 M3 팝업 카드
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset -> dismissWithCapture(offset) }
            }
    ) {
        HarmonyAnchoredMorphContainer(
            anchorRect = anchorRect,
            cardWidth = 336.dp,
            fallbackTopPadding = 96.dp,
            fallbackEndPadding = 24.dp
        ) { morphCorner ->
            val cardShape = RoundedCornerShape(morphCorner)
            Surface(
                shape = cardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> cardRect = coords.boundsInRoot() }
                    .drawWithContent {
                        captureLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(captureLayer)
                    }
                    .heightIn(max = 500.dp)
                    .harmonyRimGlow(enabled = true, shape = cardShape, glowColor = MaterialTheme.colorScheme.primary)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f), cardShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EgoIcon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                , symbolSize = 19.dp)
                                Text("번역 설정", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { dismissWithCapture(cardRect?.let { Offset(it.right - 24f, it.top + 24f) }) }, modifier = Modifier.size(28.dp)) {
                                EgoIcon(Icons.Default.Close, contentDescription = "닫기", modifier = Modifier.size(18.dp), symbolSize = 18.dp)
                            }
                        }

                        Text(
                            text = "번역 엔진",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf(
                                TranslationEngine.ONLINE_GOOGLE to ("온라인 Google 번역" to "빠르고 자연스러운 실시간 번역"),
                                TranslationEngine.OFFLINE_MLKIT to ("오프라인 ML Kit 번역" to "네트워크 없이 기기 내 자체 번역")
                            ).forEach { (eng, labels) ->
                                val sel = engine == eng
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onChangeEngine(eng) }
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = sel, onClick = { onChangeEngine(eng) })
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(labels.first, fontSize = 13.5.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                                        Text(labels.second, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                        Text(
                            text = "기본 번역 언어",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickLangs.forEach { (code, name) ->
                                val sel = defaultTargetLang == code
                                Surface(
                                    shape = CircleShape,
                                    color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onChangeDefaultTarget(code) }
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("번역 제안 표시", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                Text("외국어 페이지 방문 시 번역 안내", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = offerTranslate, onCheckedChange = onToggleOfferTranslate)
                        }

                        if (currentHost.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("이 사이트 항상 자동 번역", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                    Text(currentHost, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = alwaysTranslateSite, onCheckedChange = { onToggleAlwaysTranslateSite() })
                            }
                        }
                    }
                }
        }
    }
}
