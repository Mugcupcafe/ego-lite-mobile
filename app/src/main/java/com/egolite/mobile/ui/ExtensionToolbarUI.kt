package com.egolite.mobile.ui

import android.graphics.BitmapFactory
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.ExtensionCompatReport
import com.egolite.mobile.browser.ExtensionCompatStatus
import com.egolite.mobile.browser.ExtensionManager
import com.egolite.mobile.browser.InstalledExtension
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExtensionIconBadge(
    iconPath: String?,
    tint: Color,
    sizeDp: Int = 20,
    modifier: Modifier = Modifier
) {
    val bmp = remember(iconPath) {
        iconPath?.takeIf { it.isNotBlank() && File(it).isFile }?.let {
            runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull()
        }
    }
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(5.dp))
        )
    } else {
        EgoIcon(
            imageVector = Icons.Default.Extension,
            contentDescription = null,
            tint = tint,
            modifier = modifier.size(sizeDp.dp),
            symbolSize = sizeDp.dp
        )
    }
}

/**
 * 설정 > 디자인 > 주소창 툴바 맨 하단에 배치되는 동적 크롬 확장(.zip) 추가 · 진단 · 관리 UI
 */
@Composable
fun ExtensionSettingsBottomSection() {
    val context = LocalContext.current
    val extensions by ExtensionManager.extensions.collectAsState()
    val pendingReport by ExtensionManager.pendingReport.collectAsState()
    var renamingExt by remember { mutableStateOf<InstalledExtension?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // 설치된 확장 목록
        if (extensions.isNotEmpty()) {
            Text(
                text = "설치된 크롬 확장 (${extensions.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )
            extensions.forEach { ext ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ExtensionIconBadge(
                                iconPath = ext.iconPath,
                                tint = MaterialTheme.colorScheme.primary,
                                sizeDp = 22
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = ext.displayName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "v${ext.version}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${ext.compatStatus.label} · ${if (ext.showInToolbar) "상단 툴바 표시 중" else "툴바 숨김"}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { renamingExt = ext },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF8F00)),
                            contentAlignment = Alignment.Center
                        ) {
                            EgoIcon(
                                Icons.Default.Edit,
                                contentDescription = "확장 이름 변경",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp),
                                symbolSize = 13.dp
                            )
                        }
                    }
                    Switch(
                        checked = ext.enabled,
                        onCheckedChange = { c -> ExtensionManager.toggleEnabled(ext.id, c) }
                    )
                    IconButton(
                        onClick = {
                            ExtensionManager.deleteExtension(ext.id)
                            Toast.makeText(context, "${ext.name} 삭제됨", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        EgoIcon(
                            Icons.Default.Delete,
                            contentDescription = "확장 삭제",
                            tint = EgoDangerRed,
                            modifier = Modifier.size(18.dp),
                            symbolSize = 18.dp
                        )
                    }
                }
            }
        }

        // 맨 하단: 추가 툴바 버튼 (+ 크롬 확장 .zip 추가)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val launcher = BrowserActivityBridge.launchExtensionZipPicker
                    if (launcher != null) {
                        launcher()
                    } else {
                        Toast.makeText(context, "파일 선택기를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    EgoIcon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(19.dp),
                        symbolSize = 19.dp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "추가 툴바 버튼 (크롬 확장 .zip 불러오기)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = ".zip 파일을 선택하면 이식 가능 여부를 자동 진단하고 상단 툴바에 추가합니다",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }

    // 호환성 자동 진단 다이얼로그
    renamingExt?.let { ext ->
        ExtensionRenameDialog(
            ext = ext,
            onDismiss = { renamingExt = null }
        )
    }

    pendingReport?.let { report ->
        ExtensionCompatReportDialog(
            report = report,
            onInstall = {
                val installed = ExtensionManager.confirmInstall(context, report)
                if (installed != null) {
                    Toast.makeText(context, "${installed.name} 설치 완료 · 상단 툴바에 추가됨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "확장 설치에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { ExtensionManager.dismissPendingReport(cleanupTemp = true) }
        )
    }
}

@Composable
fun ExtensionCompatReportDialog(
    report: ExtensionCompatReport,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val badgeColor = when (report.status) {
        ExtensionCompatStatus.FULL -> EgoEmerald
        ExtensionCompatStatus.PARTIAL -> Color(0xFFF59E0B)
        ExtensionCompatStatus.UNSUPPORTED -> EgoDangerRed
    }
    val badgeIcon = when (report.status) {
        ExtensionCompatStatus.FULL -> Icons.Default.CheckCircle
        ExtensionCompatStatus.PARTIAL -> Icons.Default.Warning
        ExtensionCompatStatus.UNSUPPORTED -> Icons.Default.Error
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EgoIcon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp), symbolSize = 22.dp)
                Column {
                    Text(report.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("v${report.version} · ${report.status.label}", fontSize = 12.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (report.description.isNotBlank()) {
                    Text(report.description, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (report.supportedFeatures.isNotEmpty()) {
                    Text("이식 가능 항목", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EgoEmerald)
                    report.supportedFeatures.forEach { item ->
                        Text("• $item", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (report.warnings.isNotEmpty()) {
                    Text("참고 사항", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    report.warnings.forEach { item ->
                        Text("• $item", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (report.unsupportedFeatures.isNotEmpty()) {
                    Text("미지원 / 제한 항목", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EgoDangerRed)
                    report.unsupportedFeatures.forEach { item ->
                        Text("• $item", fontSize = 12.sp, color = EgoDangerRed)
                    }
                }
            }
        },
        confirmButton = {
            if (report.canInstall) {
                Button(onClick = onInstall) {
                    Text("상단 툴바에 추가")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (report.canInstall) "취소" else "닫기")
            }
        }
    )
}

/**
 * 상단 주소창 우측 툴바에 동적으로 추가되는 확장 아이콘 버튼들.
 * - 짧게 탭: popup HTML 열기 또는 현재 페이지에서 즉시 실행/재주입 (+ 물리 반응 애니메이션)
 * - 길게 누르기: 확장 퀵 관리(켜기/끄기 · 재실행 · 숨기기 · 삭제)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExtensionToolbarButtons(
    extensions: List<InstalledExtension>,
    activeIconColor: Color,
    onTapExtension: (InstalledExtension) -> Unit,
    onLongPressExtension: (InstalledExtension) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val ringCol = MaterialTheme.colorScheme.primary

    extensions.filter { it.enabled && it.showInToolbar }.forEach { ext ->
        val popAnim = remember(ext.id) { Animatable(1f) }
        val waveAnim = remember(ext.id) { Animatable(0f) }
        val spinAnim = remember(ext.id) { Animatable(0f) }

        Box(
            modifier = Modifier
                .size(38.dp)
                .drawBehind {
                    val w = waveAnim.value
                    if (w > 0.01f && w < 0.99f) {
                        drawCircle(
                            color = ringCol.copy(alpha = (1f - w) * 0.48f),
                            radius = size.minDimension * (0.24f + 0.34f * w),
                            style = Stroke(width = (2.0f * (1f - w)).dp.toPx())
                        )
                    }
                }
                .clip(CircleShape)
                .combinedClickable(
                    onClick = {
                        scope.launch {
                            popAnim.snapTo(0.80f)
                            popAnim.animateTo(1.22f, spring(dampingRatio = 0.48f, stiffness = 540f))
                            popAnim.animateTo(1f, EgoMotion.bouncySpring())
                        }
                        scope.launch {
                            spinAnim.snapTo(0f)
                            spinAnim.animateTo(18f, tween(80))
                            spinAnim.animateTo(0f, spring(dampingRatio = 0.46f, stiffness = 460f))
                        }
                        scope.launch {
                            waveAnim.snapTo(0f)
                            waveAnim.animateTo(1f, tween(400))
                            waveAnim.snapTo(0f)
                        }
                        onTapExtension(ext)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPressExtension(ext)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            ExtensionIconBadge(
                iconPath = ext.iconPath,
                tint = activeIconColor,
                sizeDp = 20,
                modifier = Modifier.graphicsLayer {
                    scaleX = popAnim.value
                    scaleY = popAnim.value
                    rotationZ = spinAnim.value
                }
            )
        }
    }
}

/**
 * 확장 아이콘 길게 누르기(Long Press) 시 뜨는 퀵 관리 다이얼로그
 */
@Composable
fun ExtensionQuickManageDialog(
    ext: InstalledExtension,
    onRunNow: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onHideFromToolbar: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtensionIconBadge(iconPath = ext.iconPath, tint = MaterialTheme.colorScheme.primary, sizeDp = 24)
                Column {
                    Text(ext.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("v${ext.version} · ${ext.compatStatus.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ext.description.isNotBlank()) {
                    Text(ext.description, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("확장 활성화", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = ext.enabled, onCheckedChange = onToggleEnabled)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onRunNow(); onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EgoIcon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp), symbolSize = 19.dp)
                    Text("현재 페이지에서 즉시 실행 / 재주입", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onHideFromToolbar(); onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EgoIcon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp), symbolSize = 19.dp)
                    Text("상단 툴바에서 숨기기", fontSize = 13.5.sp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDelete(); onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EgoIcon(Icons.Default.Delete, contentDescription = null, tint = EgoDangerRed, modifier = Modifier.size(19.dp), symbolSize = 19.dp)
                    Text("확장 프로그램 완전 삭제", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = EgoDangerRed)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

/**
 * default_popup HTML이 포함된 확장을 탭했을 때 보여주는 팝업 WebView 다이얼로그
 */
@Composable
fun ExtensionPopupWebViewDialog(
    ext: InstalledExtension,
    onDismiss: () -> Unit
) {
    val popupFile = ext.popupPath?.let { File(it) }?.takeIf { it.isFile }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtensionIconBadge(iconPath = ext.iconPath, tint = MaterialTheme.colorScheme.primary, sizeDp = 20)
                Text(ext.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (popupFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                addJavascriptInterface(ExtensionManager.PageExtensionBridge(this), "EgoExtBridge")
                                loadUrl("file://${popupFile.absolutePath}")
                            }
                        }
                    )
                }
            } else {
                Text("팝업 HTML 파일이 없는 확장입니다.", fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}
