package com.egolite.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egolite.mobile.browser.TranslationManager
import com.egolite.mobile.model.TranslationEngine
import com.egolite.mobile.model.TranslationPopupState

@Composable
fun ChromeStyleTranslatePopup(
    state: TranslationPopupState,
    engine: TranslationEngine,
    currentHost: String,
    alwaysTranslateSite: Boolean,
    onChangeSourceLang: (String) -> Unit,
    onChangeTargetLang: (String) -> Unit,
    onToggleAlwaysTranslate: () -> Unit,
    onUndoRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    var showSrcMenu by remember { mutableStateOf(false) }
    var showDstMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        Surface(
            shadowElevation = 10.dp,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 상단: 크롬 스타일 헤더 ("페이지 번역됨" | 실행취소 | 닫기)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                state.isTranslating -> state.statusText.ifBlank { "페이지 번역 중..." }
                                state.isTranslated -> "페이지 번역됨"
                                else -> state.statusText.ifBlank { "번역 준비됨" }
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (engine == TranslationEngine.OFFLINE_MLKIT) "온디바이스 오프라인" else "온라인 Google 번역",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.isTranslated) {
                        TextButton(onClick = onUndoRestore) {
                            Text("실행취소", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", modifier = Modifier.size(18.dp))
                    }
                }

                // 중단: 팝업 안에서 바로 출발어 / 도착어 선택
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("출발어:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showSrcMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.sourceLang == "auto") state.detectedLangLabel else TranslationManager.labelFor(state.sourceLang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = showSrcMenu, onDismissRequest = { showSrcMenu = false }) {
                            TranslationManager.sourceLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.label) },
                                    onClick = {
                                        showSrcMenu = false
                                        onChangeSourceLang(lang.code)
                                    }
                                )
                            }
                        }
                    }

                    Text("→", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("도착어:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showDstMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = TranslationManager.labelFor(state.targetLang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = showDstMenu, onDismissRequest = { showDstMenu = false }) {
                            TranslationManager.targetLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.label) },
                                    onClick = {
                                        showDstMenu = false
                                        onChangeTargetLang(lang.code)
                                    }
                                )
                            }
                        }
                    }
                }

                // 하단: 이 사이트 항상 번역 토글
                if (currentHost.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleAlwaysTranslate() }
                    ) {
                        Checkbox(
                            checked = alwaysTranslateSite,
                            onCheckedChange = { onToggleAlwaysTranslate() },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$currentHost 항상 자동 번역",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
