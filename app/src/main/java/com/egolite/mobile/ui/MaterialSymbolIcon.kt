package com.egolite.mobile.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
private object MaterialSymbolsFontHolder {
    @Volatile
    private var initialized = false
    private var outlineFamily: FontFamily? = null
    private var filledFamily: FontFamily? = null

    fun getFamily(context: Context, filled: Boolean): FontFamily? {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val am = context.applicationContext.assets
                    val exists = runCatching {
                        am.open("fonts/MaterialSymbolsRounded.ttf").use { true }
                    }.getOrDefault(false)
                    if (exists) {
                        outlineFamily = runCatching {
                            FontFamily(
                                Font(
                                    path = "fonts/MaterialSymbolsRounded.ttf",
                                    assetManager = am,
                                    weight = FontWeight.Normal,
                                    variationSettings = FontVariation.Settings(
                                        FontVariation.weight(400),
                                        FontVariation.Setting("FILL", 0f),
                                        FontVariation.Setting("GRAD", 0f),
                                        FontVariation.Setting("opsz", 24f)
                                    )
                                )
                            )
                        }.getOrNull()
                        filledFamily = runCatching {
                            FontFamily(
                                Font(
                                    path = "fonts/MaterialSymbolsRounded.ttf",
                                    assetManager = am,
                                    weight = FontWeight.Normal,
                                    variationSettings = FontVariation.Settings(
                                        FontVariation.weight(400),
                                        FontVariation.Setting("FILL", 1f),
                                        FontVariation.Setting("GRAD", 0f),
                                        FontVariation.Setting("opsz", 24f)
                                    )
                                )
                            )
                        }.getOrNull()
                    }
                    initialized = true
                }
            }
        }
        return if (filled) filledFamily else outlineFamily
    }
}

private val VECTOR_TO_SYMBOL_MAP: Map<String, Pair<String, Boolean>> = mapOf(
    "ArrowBack" to ("arrow_back" to false),
    "ArrowForward" to ("arrow_forward" to false),
    "KeyboardArrowLeft" to ("chevron_left" to false),
    "KeyboardArrowRight" to ("chevron_right" to false),
    "KeyboardArrowUp" to ("keyboard_arrow_up" to false),
    "KeyboardArrowDown" to ("keyboard_arrow_down" to false),
    "ChevronRight" to ("chevron_right" to false),
    "ArrowDropDown" to ("arrow_drop_down" to false),
    "Send" to ("send" to false),
    "Add" to ("add" to false),
    "AddBox" to ("add_box" to false),
    "AddToHomeScreen" to ("add_to_home_screen" to false),
    "Archive" to ("folder_zip" to false),
    "AudioFile" to ("audio_file" to false),
    "AutoAwesome" to ("auto_awesome" to false),
    "Block" to ("block" to false),
    "BookmarkAdd" to ("bookmark_add" to false),
    "BookmarkBorder" to ("bookmark" to false),
    "CameraAlt" to ("photo_camera" to false),
    "ChatBubbleOutline" to ("chat_bubble" to false),
    "Check" to ("check" to false),
    "CheckBox" to ("check_box" to true),
    "CheckBoxOutlineBlank" to ("check_box_outline_blank" to false),
    "Close" to ("close" to false),
    "Code" to ("code" to false),
    "ContentCopy" to ("content_copy" to false),
    "Contrast" to ("contrast" to false),
    "Cookie" to ("cookie" to false),
    "CreateNewFolder" to ("create_new_folder" to false),
    "DarkMode" to ("dark_mode" to false),
    "LightMode" to ("light_mode" to false),
    "Delete" to ("delete" to false),
    "DeleteOutline" to ("delete" to false),
    "Description" to ("description" to false),
    "DesktopWindows" to ("desktop_windows" to false),
    "Devices" to ("devices" to false),
    "Download" to ("download" to false),
    "DownloadDone" to ("download_done" to false),
    "Edit" to ("edit" to false),
    "FindInPage" to ("find_in_page" to false),
    "Folder" to ("folder" to false),
    "FontDownload" to ("font_download" to false),
    "GridView" to ("grid_view" to false),
    "HighlightOff" to ("cancel" to false),
    "History" to ("history" to false),
    "Home" to ("home" to false),
    "Hub" to ("hub" to false),
    "Image" to ("image" to false),
    "InsertDriveFile" to ("draft" to false),
    "Language" to ("language" to false),
    "LinkOff" to ("link_off" to false),
    "LocationOn" to ("location_on" to false),
    "Lock" to ("lock" to false),
    "MenuBook" to ("menu_book" to false),
    "Mic" to ("mic" to false),
    "MoreVert" to ("more_vert" to false),
    "Newspaper" to ("newspaper" to false),
    "NorthWest" to ("north_west" to false),
    "OpenInNew" to ("open_in_new" to false),
    "Person" to ("person" to false),
    "Public" to ("public" to false),
    "PushPin" to ("keep" to false),
    "Refresh" to ("refresh" to false),
    "Remove" to ("remove" to false),
    "Search" to ("search" to false),
    "Settings" to ("settings" to false),
    "Share" to ("share" to false),
    "Shield" to ("shield" to false),
    "SmartToy" to ("smart_toy" to false),
    "Star" to ("star" to true),
    "StarBorder" to ("star" to false),
    "ThumbUpOffAlt" to ("thumb_up" to false),
    "Translate" to ("translate" to false),
    "TrendingUp" to ("trending_up" to false),
    "Tune" to ("tune" to false),
    "UploadFile" to ("upload_file" to false),
    "VideoFile" to ("video_file" to false),
    "VisibilityOff" to ("visibility_off" to false),
    "ZoomIn" to ("zoom_in" to false)
)

@Composable
fun EgoIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean? = null,
    symbolSize: Dp = 22.dp
) {
    val context = LocalContext.current
    val shortName = remember(imageVector.name) {
        imageVector.name.substringAfterLast('.')
    }
    val mapped = VECTOR_TO_SYMBOL_MAP[shortName]
    val useFilled = filled ?: (mapped?.second == true)
    val fontFamily = remember(useFilled) { MaterialSymbolsFontHolder.getFamily(context, useFilled) }

    if (mapped != null && fontFamily != null) {
        val density = LocalDensity.current
        // fontScale 영향을 받지 않도록 dp 기준 고정 sp 계산 (아이콘 크기 왜곡 방지)
        val glyphSp = with(density) { (symbolSize.toPx() / (density.density * density.fontScale)).sp }
        val semMod = if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else Modifier
        Box(
            modifier = modifier.then(semMod),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mapped.first,
                fontFamily = fontFamily,
                fontSize = glyphSp,
                lineHeight = glyphSp,
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(fontFeatureSettings = "liga")
            )
        }
    } else {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}
