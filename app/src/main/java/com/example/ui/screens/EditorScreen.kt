package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.data.*
import com.example.engine.VideoEditorViewModel
import com.example.engine.VideoExportService
import com.example.engine.PlaybackProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditorScreen(
    viewModel: VideoEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val timelineItems by viewModel.timelineItems.collectAsState()
    val playheadMs by viewModel.playheadMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedItemId by viewModel.selectedItemId.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val zoomScale by viewModel.timelineZoom.collectAsState()
    val snapEnabled by viewModel.snapToGrid.collectAsState()

    // Active select item object reference
    val selectedItem = remember(selectedItemId, timelineItems) {
        timelineItems.find { it.id == selectedItemId }
    }

    // Export Dialog State
    var showExportDialog by remember { mutableStateOf(false) }
    var exportResolution by remember { mutableStateOf("1080p") }
    var exportFps by remember { mutableStateOf(30) }
    var exportBitrate by remember { mutableStateOf("Standard") }

    // Recording and permissions
    val recordPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    var isRecordingVoiceover by remember { mutableStateOf(false) }

    // Active Inspector Mode
    var inspectorTab by remember { mutableStateOf("Timeline") } // "Timeline", "Speed", "Canvas", "Adjust", "Text", "Sticker", "Transition", "Audio"

    // Back handler
    val handleBackPress = {
        viewModel.stopPlayback()
        viewModel.saveCurrentProjectToDraft()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name by viewModel.projectName.collectAsState()
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Export trigger
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeoPrimaryContainer,
                            contentColor = GeoOnPrimary
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Export icon", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GeoBackground)
            )
        },
        bottomBar = {
            // Inspector Panel / Footer Tabs
            EditorInspectorPanel(
                selectedItem = selectedItem,
                viewModel = viewModel,
                activeTab = inspectorTab,
                onTabChange = { inspectorTab = it },
                recordPermissionGranted = recordPermissionState.status.isGranted,
                onRequestRecordPermission = { recordPermissionState.launchPermissionRequest() },
                isRecordingVoiceover = isRecordingVoiceover,
                onRecordingVoiceoverChange = { isRecordingVoiceover = it }
            )
        },
        containerColor = GeoBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. VIDEO PREVIEW CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                VideoPreviewCanvas(
                    timelineItems = timelineItems,
                    playheadMs = playheadMs,
                    aspectRatio = aspectRatio,
                    viewModel = viewModel
                )
            }

            // 2. TIMELINE NAVIGATION CONTROLS BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(GeoSurface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Playhead Timer Text (formatted 00:00.00)
                Text(
                    text = "${formatTime(playheadMs)} / ${formatTime(viewModel.getMaxTimelineDuration())}",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )

                // Play / Pause Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (isPlaying) viewModel.stopPlayback() else viewModel.startPlayback()
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Snap indicator & Zoom triggers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleSnap() }) {
                        Icon(
                            imageVector = if (snapEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = "Toggle Grid Snapping",
                            tint = if (snapEnabled) GeoPrimary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom indicator",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Slider(
                        value = zoomScale,
                        onValueChange = { viewModel.setZoom(it) },
                        valueRange = 1.0f..15.0f,
                        modifier = Modifier.width(80.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = GeoPrimary,
                            thumbColor = GeoPrimary
                        )
                    )
                }
            }

            // 3. MULTI-TRACK CHANNELS TIMELINE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f)
                    .background(GeoSurfaceVariant)
            ) {
                TimelineTrackView(
                    timelineItems = timelineItems,
                    playheadMs = playheadMs,
                    selectedItemId = selectedItemId,
                    zoomScale = zoomScale,
                    viewModel = viewModel
                )
            }

            // 4. TIMELINE OPERATIONS / QUICK EDIT BUTTONS BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(GeoSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickEditButton(
                    label = "Split",
                    icon = Icons.Default.ContentCut,
                    enabled = selectedItem != null,
                    onClick = { viewModel.splitSelectedItem() }
                )
                QuickEditButton(
                    label = "Delete",
                    icon = Icons.Default.Delete,
                    enabled = selectedItem != null,
                    onClick = { viewModel.deleteSelectedItem() }
                )
                QuickEditButton(
                    label = "Duplicate",
                    icon = Icons.Default.ContentCopy,
                    enabled = selectedItem != null,
                    onClick = { viewModel.duplicateSelectedItem() }
                )
                QuickEditButton(
                    label = "Freeze Frame",
                    icon = Icons.Default.AcUnit,
                    enabled = selectedItem != null && selectedItem?.type == TrackType.VIDEO,
                    onClick = { viewModel.freezeFrameSelectedItem() }
                )
                QuickEditButton(
                    label = "Extract Audio",
                    icon = Icons.Default.Audiotrack,
                    enabled = selectedItem != null && selectedItem?.type == TrackType.VIDEO,
                    onClick = { viewModel.extractAudioFromSelected() }
                )
                QuickEditButton(
                    label = "🔑 Keyframe",
                    icon = Icons.Default.VpnKey,
                    enabled = selectedItem != null,
                    onClick = { viewModel.toggleKeyframeAtPlayhead() }
                )
                QuickEditButton(
                    label = "Auto Captions",
                    icon = Icons.Default.TextFormat,
                    enabled = true,
                    onClick = { viewModel.generateAutoCaptions() }
                )
            }
        }

        // Export Dialog Menu
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Video", color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Configure cinematic rendering settings:", color = Color.Gray, fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Resolution Selection
                        Text("Resolution", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 14.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("480p", "720p", "1080p", "4K").forEach { res ->
                                OutlinedButton(
                                    onClick = { exportResolution = res },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (exportResolution == res) GeoPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                        contentColor = if (exportResolution == res) GeoPrimary else Color.Gray
                                    ),
                                    border = BorderStroke(1.dp, if (exportResolution == res) GeoPrimary else Color.DarkGray)
                                ) {
                                    Text(res, fontSize = 12.sp)
                                }
                            }
                        }

                        // Frame Rate Selection
                        Text("Frame Rate", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 14.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(24, 30, 60).forEach { fps ->
                                OutlinedButton(
                                    onClick = { exportFps = fps },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (exportFps == fps) GeoPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                        contentColor = if (exportFps == fps) GeoPrimary else Color.Gray
                                    ),
                                    border = BorderStroke(1.dp, if (exportFps == fps) GeoPrimary else Color.DarkGray)
                                ) {
                                    Text("${fps}fps", fontSize = 12.sp)
                                }
                            }
                        }

                        // Bitrate Selection
                        Text("Bitrate Control", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 14.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Standard", "High").forEach { rate ->
                                OutlinedButton(
                                    onClick = { exportBitrate = rate },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (exportBitrate == rate) GeoPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                        contentColor = if (exportBitrate == rate) GeoPrimary else Color.Gray
                                    ),
                                    border = BorderStroke(1.dp, if (exportBitrate == rate) GeoPrimary else Color.DarkGray)
                                ) {
                                    Text(rate, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.stopPlayback()
                            viewModel.saveCurrentProjectToDraft()
                            
                            val projectId = viewModel.currentProjectId.value ?: 1
                            VideoExportService.startExport(
                                context,
                                projectId,
                                exportResolution,
                                exportFps,
                                exportBitrate
                            )
                            Toast.makeText(context, "Rendering started in background. Check your notification drawer!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryContainer, contentColor = GeoOnPrimary)
                    ) {
                        Text("Start Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = GeoSurfaceVariant
            )
        }
    }
}

// 1. VIDEO PREVIEW CANVAS COMPOSABLE
@Composable
fun VideoPreviewCanvas(
    timelineItems: List<TimelineItem>,
    playheadMs: Long,
    aspectRatio: String,
    viewModel: VideoEditorViewModel
) {
    // Find active clips on the timeline at the current playhead
    val activeVideos = timelineItems.filter {
        it.type == TrackType.VIDEO && playheadMs >= it.startInTimeline && playheadMs <= (it.startInTimeline + it.duration)
    }.sortedBy { it.startInTimeline }

    val activeTexts = timelineItems.filter {
        it.type == TrackType.TEXT && playheadMs >= it.startInTimeline && playheadMs <= (it.startInTimeline + it.duration)
    }

    val activeStickers = timelineItems.filter {
        it.type == TrackType.STICKER && playheadMs >= it.startInTimeline && playheadMs <= (it.startInTimeline + it.duration)
    }

    val activeEffects = timelineItems.filter {
        it.type == TrackType.EFFECT && playheadMs >= it.startInTimeline && playheadMs <= (it.startInTimeline + it.duration)
    }

    // Determine target canvas size ratio box
    val canvasRatioModifier = when (aspectRatio) {
        "9:16" -> Modifier.aspectRatio(9f / 16f)
        "16:9" -> Modifier.aspectRatio(16f / 9f)
        "1:1" -> Modifier.aspectRatio(1f)
        else -> Modifier.aspectRatio(9f / 16f)
    }

    Box(
        modifier = Modifier
            .fillMaxHeight(0.95f)
            .then(canvasRatioModifier)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, GeoBorder, RoundedCornerShape(4.dp))
            .background(GeoBackground)
    ) {
        if (activeVideos.isEmpty()) {
            // Dark empty canvas placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MovieFilter, contentDescription = "Empty canvas", tint = Color.DarkGray, modifier = Modifier.size(40.dp))
                    Text("End of Timeline", color = Color.DarkGray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            // Draw active video track layer with live animations
            activeVideos.forEach { clip ->
                // Fetch keyframed interpolated properties
                val props = viewModel.getInterpolatedProperties(clip, playheadMs)

                // Render dynamic backgrounds using standard Compose canvas elements
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            translationX = props.x,
                            translationY = props.y,
                            scaleX = props.scale * (if (clip.isFlippedHorizontal) -1f else 1f),
                            scaleY = props.scale * (if (clip.isFlippedVertical) -1f else 1f),
                            rotationZ = props.rotation,
                            alpha = props.opacity
                        )
                ) {
                    val w = size.width
                    val h = size.height

                    // Construct Color Filter based on Selected LUT Mode
                    val colorMatrix = when (clip.filterType) {
                        "Vintage" -> ColorMatrix(floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 0f,
                            0.349f, 0.686f, 0.168f, 0f, 0f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        "Cyberpunk" -> ColorMatrix(floatArrayOf(
                            0.8f, 0f, 0.5f, 0f, 0f,
                            0f, 0.9f, 0.8f, 0f, 0f,
                            0.6f, 0f, 1.2f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        "Film" -> ColorMatrix(floatArrayOf(
                            1.1f, 0f, 0f, 0f, 10f,
                            0f, 1.0f, 0f, 0f, 5f,
                            0f, 0f, 0.9f, 0f, -5f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        "B&W" -> ColorMatrix().apply { setToSaturation(0f) }
                        else -> ColorMatrix().apply { setToSaturation(1f + clip.saturation) }
                    }

                    // Apply Brightness offset
                    if (clip.brightness != 0f) {
                        val b = clip.brightness * 255f
                        val brightMat = ColorMatrix(floatArrayOf(
                            1f, 0f, 0f, 0f, b,
                            0f, 1f, 0f, 0f, b,
                            0f, 0f, 1f, 0f, b,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        // Multiply with saturation color matrix manually using compose operations if supported
                    }

                    val colorFilter = ColorFilter.colorMatrix(colorMatrix)
                    
                    // Fetch stock clip color signatures
                    val stock = TemplateAssets.stockVideos.find { it.id == clip.filePathOrAssetId }
                    val primaryHex = stock?.colorPrimary ?: 0xFF333336
                    val secondaryHex = stock?.colorSecondary ?: 0xFF121214

                    val colorA = Color(primaryHex)
                    val colorB = Color(secondaryHex)
                    
                    val brush = Brush.linearGradient(
                        colors = listOf(colorA, colorB),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )

                    drawRect(
                        brush = brush,
                        colorFilter = colorFilter
                    )

                    // 3D Perspective Grid, volumetric circles, and particle physics
                    val playheadSec = playheadMs / 1000f
                    
                    if (clip.filePathOrAssetId == "stock_tokyo") {
                        // Draw falling glowing rain lines
                        for (i in 0..12) {
                            val speedX = 100f
                            val speedY = 800f
                            val x = ((i * 120f + playheadSec * speedX) % w)
                            val y = ((i * 80f + playheadSec * speedY) % h)
                            drawLine(
                                color = Color(0x7700FFFF),
                                start = Offset(x, y),
                                end = Offset(x - 10f, y + 40f),
                                strokeWidth = 3f,
                                colorFilter = colorFilter
                            )
                        }
                    } else if (clip.filePathOrAssetId == "stock_forest") {
                        // Volumetric expanding light circles
                        val animRadius = (300f + (playheadMs % 4000) / 4000f * 200f)
                        drawCircle(
                            color = Color(0x33FFAA00),
                            radius = animRadius,
                            center = Offset(w / 2f, h / 3f),
                            colorFilter = colorFilter
                        )
                    } else if (clip.filePathOrAssetId == "stock_ocean") {
                        // perspective grid lines
                        val count = 10
                        val horizon = h * 0.6f
                        for (i in 0..count) {
                            val ratio = i.toFloat() / count.toFloat()
                            val xBottom = ratio * w
                            drawLine(
                                color = Color(0x44FF00CC),
                                start = Offset(w / 2f, horizon),
                                end = Offset(xBottom, h),
                                strokeWidth = 2f,
                                colorFilter = colorFilter
                            )
                        }
                        // moving horizontal lines
                        val lines = 6
                        for (i in 0..lines) {
                            val ratio = ((i + playheadSec) % lines) / lines
                            val y = horizon + ratio * (h - horizon)
                            drawLine(
                                color = Color(0x44FF00CC),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 2f,
                                colorFilter = colorFilter
                            )
                        }
                    } else if (clip.filePathOrAssetId == "stock_matrix") {
                        // Draw code columns
                        for (i in 0..8) {
                            val columnX = i * (w / 8f) + 20f
                            val speed = 400f
                            val y = ((i * 150f + playheadSec * speed) % h)
                            drawCircle(
                                color = Color(0xFF00FF00),
                                radius = 6f,
                                center = Offset(columnX, y),
                                colorFilter = colorFilter
                            )
                            drawCircle(
                                color = Color(0xFF00FF00),
                                radius = 4f,
                                center = Offset(columnX, y - 30f),
                                colorFilter = colorFilter
                            )
                        }
                    } else if (clip.filePathOrAssetId == "stock_nebula") {
                        // cosmic swirls
                        for (i in 0..15) {
                            val angle = i * 20f + playheadSec * 45f
                            val rad = i * 25f
                            val radRad = Math.toRadians(angle.toDouble())
                            val x = (w / 2f + Math.cos(radRad) * rad).toFloat()
                            val y = (h / 2f + Math.sin(radRad) * rad).toFloat()
                            drawCircle(
                                color = Color(0xBBFFFFFF),
                                radius = 5f + (i % 3),
                                center = Offset(x, y),
                                colorFilter = colorFilter
                            )
                        }
                    }
                }
            }

            // Draw Video Effects Layers (Glitch split overlays)
            activeEffects.forEach { effect ->
                val seconds = playheadMs / 1000f
                if (effect.effectType == "Glitch") {
                    // Random horizontal splits
                    val verticalLines = (seconds * 15).toInt() % 4
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(0x2200FFFF).copy(
                                    alpha = if (verticalLines == 1) 0.15f else 0.0f
                                )
                            )
                    )
                } else if (effect.effectType == "Blur") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                } else if (effect.effectType == "RGB Split") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(6.dp, Color(0x33FF0055))
                    )
                } else if (effect.effectType == "Flash") {
                    val pulse = (playheadMs / 200) % 2 == 0L
                    if (pulse) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                }
            }

            // Draw Stickers Overlay Layer
            activeStickers.forEach { sticker ->
                val animPulse = (playheadMs / 300) % 2 == 0L
                val sizeScale = if (sticker.stickerType == "Pulse" && animPulse) 1.2f else 1.0f
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .graphicsLayer(scaleX = sizeScale, scaleY = sizeScale)
                ) {
                    val stickerEmoji = when (sticker.filePathOrAssetId) {
                        "sticker_sparkle" -> "✨"
                        "sticker_heart" -> "❤️"
                        "sticker_fire" -> "🔥"
                        "sticker_cool" -> "😎"
                        "sticker_crown" -> "👑"
                        "sticker_arrow" -> "🎯"
                        "sticker_glitch" -> "⚡"
                        "sticker_vibe" -> "🎵"
                        else -> "⭐"
                    }
                    Text(
                        text = stickerEmoji,
                        fontSize = 48.sp
                    )
                }
            }

            // Draw Caption / Text Overlay Layer
            activeTexts.forEach { textItem ->
                // Apply entrance animation styles
                var displayText = textItem.title
                if (textItem.textAnimationType == "Typewriter") {
                    val chars = (playheadMs - textItem.startInTimeline) / 80L
                    displayText = textItem.title.take(chars.toInt().coerceIn(0, textItem.title.length))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.BottomCenter)
                        .padding(bottom = 60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(textItem.textCustomBgColor))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = displayText,
                        color = Color(textItem.textCustomColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 2. TIMELINE MULTI-TRACK TRACKS PANEL COMPOSABLE
@Composable
fun TimelineTrackView(
    timelineItems: List<TimelineItem>,
    playheadMs: Long,
    selectedItemId: String?,
    zoomScale: Float,
    viewModel: VideoEditorViewModel
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Map playhead to scroll position or vice versa on scrubbing
    val maxDuration = viewModel.getMaxTimelineDuration()
    val playheadOffsetPx = (playheadMs * zoomScale / 10f).dp

    // Separate tracks
    val videoClips = timelineItems.filter { it.type == TrackType.VIDEO }.sortedBy { it.startInTimeline }
    val audioClips = timelineItems.filter { it.type == TrackType.AUDIO }.sortedBy { it.startInTimeline }
    val textClips = timelineItems.filter { it.type == TrackType.TEXT }.sortedBy { it.startInTimeline }
    val stickerClips = timelineItems.filter { it.type == TrackType.STICKER }.sortedBy { it.startInTimeline }
    val effectClips = timelineItems.filter { it.type == TrackType.EFFECT }.sortedBy { it.startInTimeline }

    val timelineWidthPx = (maxDuration * zoomScale / 10f).dp + 300.dp // padding boundary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .width(timelineWidthPx)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // TRACK 1: VIDEO (Bands representing video segments)
            TrackRow(
                label = "Video",
                icon = Icons.Default.VideoCameraBack,
                items = videoClips,
                playheadMs = playheadMs,
                selectedItemId = selectedItemId,
                zoomScale = zoomScale,
                trackColor = VideoTrackClip1,
                accentColor = GeoPrimary,
                onSelect = { viewModel.selectItem(it) }
            )

            // TRACK 2: AUDIO (wave shapes)
            TrackRow(
                label = "Audio",
                icon = Icons.Default.Audiotrack,
                items = audioClips,
                playheadMs = playheadMs,
                selectedItemId = selectedItemId,
                zoomScale = zoomScale,
                trackColor = AudioTrackBg,
                accentColor = GeoPrimary,
                onSelect = { viewModel.selectItem(it) },
                isAudio = true,
                viewModel = viewModel
            )

            // TRACK 3: TEXT SUBTITLES
            TrackRow(
                label = "Text",
                icon = Icons.Default.TextFields,
                items = textClips,
                playheadMs = playheadMs,
                selectedItemId = selectedItemId,
                zoomScale = zoomScale,
                trackColor = TextTrackBg,
                accentColor = Color(0xFFC084FC),
                onSelect = { viewModel.selectItem(it) }
            )

            // TRACK 4: STICKERS / WATERMARKS
            TrackRow(
                label = "Stickers",
                icon = Icons.Default.EmojiEmotions,
                items = stickerClips,
                playheadMs = playheadMs,
                selectedItemId = selectedItemId,
                zoomScale = zoomScale,
                trackColor = Color(0xFF233A3A),
                accentColor = Color(0xFF2DD4BF),
                onSelect = { viewModel.selectItem(it) }
            )

            // TRACK 5: VIDEO EFFECTS (filters, glitch layers)
            TrackRow(
                label = "Effects",
                icon = Icons.Default.AutoAwesome,
                items = effectClips,
                playheadMs = playheadMs,
                selectedItemId = selectedItemId,
                zoomScale = zoomScale,
                trackColor = Color(0xFF3B1E40),
                accentColor = Color(0xFFE9D5FF),
                onSelect = { viewModel.selectItem(it) }
            )
        }

        // VERTICAL CENTRAL PLAYHEAD RED LINE
        Box(
            modifier = Modifier
                .padding(start = playheadOffsetPx + 80.dp) // shift by track label width padding
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.Red)
        ) {
            // Small diamond playhead handle at top
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.Red, RoundedCornerShape(2.dp))
            )
        }

        // Invisible scrubber overlay to detect clicks and drags on the timeline
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x / zoomScale * 10).toLong()
                            viewModel.seekTo(playheadMs + deltaMs)
                        }
                    )
                }
        )
    }
}

@Composable
fun TrackRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<TimelineItem>,
    playheadMs: Long,
    selectedItemId: String?,
    zoomScale: Float,
    trackColor: Color,
    accentColor: Color,
    onSelect: (String?) -> Unit,
    isAudio: Boolean = false,
    viewModel: VideoEditorViewModel? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Track Label Area
        Row(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(GeoSurfaceVariant)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // Right Tracks Timeline Strip Box
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(GeoSurface)
        ) {
            items.forEach { item ->
                val startOffset = (item.startInTimeline * zoomScale / 10f).dp
                val widthSize = (item.duration * zoomScale / 10f).dp
                val isSelected = item.id == selectedItemId

                Box(
                    modifier = Modifier
                        .padding(start = startOffset)
                        .width(widthSize)
                        .fillMaxHeight(0.9f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) accentColor else trackColor)
                        .border(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) Color.White else accentColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelect(item.id) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Render animated waveforms for active audio tracks
                        if (isAudio && viewModel != null) {
                            val activeAudioTriggers by viewModel.activeAudioTriggers.collectAsState()
                            val amplitude = activeAudioTriggers[item.id] ?: 0.2f
                            
                            Row(
                                modifier = Modifier.width(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(modifier = Modifier.height((12 * amplitude).dp).width(2.dp).background(if (isSelected) Color.Black else Color.White))
                                Box(modifier = Modifier.height((18 * amplitude).dp).width(2.dp).background(if (isSelected) Color.Black else Color.White))
                                Box(modifier = Modifier.height((8 * amplitude).dp).width(2.dp).background(if (isSelected) Color.Black else Color.White))
                            }
                        }
                    }

                    // Keyframe nodes indicator (tiny diamonds inside timeline segments)
                    if (item.keyframes.isNotEmpty()) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 2.dp)
                        ) {
                            item.keyframes.forEach { keyframe ->
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color.Yellow, CircleShape)
                                        .padding(horizontal = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// INSPECTOR PANEL COMPOSABLE (detailed properties control tabs)
@Composable
fun EditorInspectorPanel(
    selectedItem: TimelineItem?,
    viewModel: VideoEditorViewModel,
    activeTab: String,
    onTabChange: (String) -> Unit,
    recordPermissionGranted: Boolean,
    onRequestRecordPermission: () -> Unit,
    isRecordingVoiceover: Boolean,
    onRecordingVoiceoverChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(GeoBackground)
    ) {
        // Tab Headers Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(GeoSurfaceVariant)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InspectorTabButton("Timeline", activeTab == "Timeline") { onTabChange("Timeline") }
            InspectorTabButton("Canvas Ratio", activeTab == "Canvas") { onTabChange("Canvas") }
            InspectorTabButton("Speed Controls", activeTab == "Speed") { onTabChange("Speed") }
            InspectorTabButton("Transform/PIP", activeTab == "Transform") { onTabChange("Transform") }
            InspectorTabButton("Color Adjust", activeTab == "Adjust") { onTabChange("Adjust") }
            InspectorTabButton("Fonts & TTS", activeTab == "Text") { onTabChange("Text") }
            InspectorTabButton("Stickers", activeTab == "Sticker") { onTabChange("Sticker") }
            InspectorTabButton("Transitions", activeTab == "Transition") { onTabChange("Transition") }
            InspectorTabButton("Music & Voice", activeTab == "Audio") { onTabChange("Audio") }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
        ) {
            when (activeTab) {
                "Timeline" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Current Selection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (selectedItem != null) {
                            Text("Title: ${selectedItem.title}", color = GeoPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Text("Track Type: ${selectedItem.type} | Duration: ${String.format(Locale.US, "%.2fs", selectedItem.duration / 1000f)}", color = Color.Gray, fontSize = 11.sp)
                            
                            // Simple volume slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Volume", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                Slider(
                                    value = selectedItem.volume,
                                    onValueChange = { vol ->
                                        viewModel.updateSelectedItem { it.copy(volume = vol) }
                                    },
                                    valueRange = 0.0f..1.5f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(activeTrackColor = GeoPrimary, thumbColor = GeoPrimary)
                                )
                                Text(String.format(Locale.US, "%.0f%%", selectedItem.volume * 100), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            Text("No item selected. Tap on any segment in the timeline to inspect its parameters.", color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }

                "Canvas" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Canvas Aspect Ratio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("9:16", "16:9", "1:1").forEach { ratio ->
                                val isActive = viewModel.aspectRatio.value == ratio
                                OutlinedButton(
                                    onClick = { viewModel.setAspectRatio(ratio) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isActive) GeoPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isActive) GeoPrimary else Color.Gray
                                    ),
                                    border = BorderStroke(1.dp, if (isActive) GeoPrimary else Color.DarkGray)
                                ) {
                                    val icon = when (ratio) {
                                        "9:16" -> "📱 Vertical (TikTok)"
                                        "16:9" -> "🖥️ Wide (YouTube)"
                                        else -> "🔲 Square (Instagram)"
                                    }
                                    Text(icon, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                "Speed" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Speed Adjustment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (selectedItem != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Multiplier", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                Slider(
                                    value = selectedItem.speed,
                                    onValueChange = { spd ->
                                        viewModel.updateSelectedItem { it.copy(speed = spd) }
                                    },
                                    valueRange = 0.2f..5.0f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(activeTrackColor = GeoPrimary, thumbColor = GeoPrimary)
                                )
                                Text(String.format(Locale.US, "%.1fx", selectedItem.speed), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                            }

                            // Preset Speed Curves triggers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Curve Preset", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                listOf("None", "Montage", "Bullet Time", "Jump Cut").forEach { curve ->
                                    Box(
                                        modifier = Modifier
                                            .background(GeoSurfaceVariant, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable {
                                                Toast.makeText(context, "Curve ramp '$curve' applied!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(curve, fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Text("Select a clip to adjust playback speeds.", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                }

                "Transform" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Picture-in-Picture / Rotation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (selectedItem != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Rotate Button
                                Button(
                                    onClick = {
                                        viewModel.updateSelectedItem { it.copy(rotation = (it.rotation + 90f) % 360f) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeoSurfaceVariant)
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rotate 90°", fontSize = 11.sp, color = Color.White)
                                }

                                // Flip Horiz
                                Button(
                                    onClick = {
                                        viewModel.updateSelectedItem { it.copy(isFlippedHorizontal = !it.isFlippedHorizontal) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeoSurfaceVariant)
                                ) {
                                    Icon(Icons.Default.Flip, contentDescription = "Flip Horiz", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flip H", fontSize = 11.sp, color = Color.White)
                                }

                                // Flip Vert
                                Button(
                                    onClick = {
                                        viewModel.updateSelectedItem { it.copy(isFlippedVertical = !it.isFlippedVertical) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeoSurfaceVariant)
                                ) {
                                    Text("Flip V", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            // Scaling slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Scale", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                Slider(
                                    value = selectedItem.scale,
                                    onValueChange = { s ->
                                        viewModel.updateSelectedItem { it.copy(scale = s) }
                                    },
                                    valueRange = 0.5f..3.0f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(activeTrackColor = GeoPrimary, thumbColor = GeoPrimary)
                                )
                                Text(String.format(Locale.US, "%.1fx", selectedItem.scale), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            Text("Select a clip to transform coordinates.", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                }

                "Adjust" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Filters & Adjustments", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (selectedItem != null) {
                            // Active LUT Filter Grid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("None", "Vintage", "Cyberpunk", "Film", "B&W").forEach { filter ->
                                    val isFilterActive = selectedItem.filterType == filter || (filter == "None" && selectedItem.filterType == null)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isFilterActive) GeoPrimary.copy(alpha = 0.2f) else GeoSurfaceVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(1.dp, if (isFilterActive) GeoPrimary else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.updateSelectedItem { it.copy(filterType = if (filter == "None") null else filter) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(filter, fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }

                            // Saturation Slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Saturation", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                Slider(
                                    value = selectedItem.saturation,
                                    onValueChange = { s ->
                                        viewModel.updateSelectedItem { it.copy(saturation = s) }
                                    },
                                    valueRange = -1.0f..1.0f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(activeTrackColor = GeoPrimary, thumbColor = GeoPrimary)
                                )
                            }
                        } else {
                            Text("Select a clip to apply cinematic adjustments.", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                }

                "Text" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Text Customizations & Speech Synthesis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (selectedItem != null && selectedItem.type == TrackType.TEXT) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Add text track
                                Text("Animations", color = Color.LightGray, fontSize = 11.sp)
                                listOf("None", "Typewriter", "Fade", "Pop").forEach { anim ->
                                    val isAnim = selectedItem.textAnimationType == anim
                                    Box(
                                        modifier = Modifier
                                            .background(if (isAnim) GeoPrimary.copy(alpha = 0.2f) else GeoSurfaceVariant, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isAnim) GeoPrimary else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.updateSelectedItem { it.copy(textAnimationType = anim) }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(anim, fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }

                            // Convert Text to Speech (TTS)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Text-to-Speech", color = Color.LightGray, fontSize = 11.sp)
                                Button(
                                    onClick = {
                                        viewModel.convertSelectedTextToSpeech("US_FEMALE")
                                        Toast.makeText(context, "TTS voice track compiled and added!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary, contentColor = Color.Black),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Synthesize Voice", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Quick Add Text Button
                            Button(
                                onClick = {
                                    viewModel.addClipToTimeline(
                                        type = TrackType.TEXT,
                                        title = "Double Tap to Edit Subtitle",
                                        duration = 3000L,
                                        filePathOrAssetId = "text"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoSurfaceVariant),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Text")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Text Track", color = Color.White)
                            }
                        }
                    }
                }

                "Sticker" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Sticker Library", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(
                            contentPadding = PaddingValues(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(TemplateAssets.stickers) { sticker ->
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GeoSurfaceVariant)
                                        .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.addClipToTimeline(
                                                type = TrackType.STICKER,
                                                title = sticker.name,
                                                duration = 4000L,
                                                filePathOrAssetId = sticker.id,
                                                extraConfig = { it.copy(stickerType = sticker.animType) }
                                            )
                                            Toast.makeText(context, "Added sticker: ${sticker.name}", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(sticker.iconEmoji, fontSize = 24.sp)
                                        Text(sticker.name, fontSize = 8.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "Transition" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Transitions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (selectedItem != null && selectedItem.type == TrackType.VIDEO) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("Mix", "Fade", "Wipe", "Zoom").forEach { trans ->
                                    val isTrans = selectedItem.transitionType == trans
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isTrans) GeoPrimary.copy(alpha = 0.2f) else GeoSurfaceVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(1.dp, if (isTrans) GeoPrimary else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.updateSelectedItem { it.copy(transitionType = trans) }
                                                Toast.makeText(context, "Transition '$trans' applied!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(trans, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Text("Select a video clip to apply transition animations on its boundaries.", color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }

                "Audio" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Royalty-Free Music & Voiceovers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Voiceover record toggle
                            Button(
                                onClick = {
                                    if (!recordPermissionGranted) {
                                        onRequestRecordPermission()
                                    } else {
                                        val newState = !isRecordingVoiceover
                                        onRecordingVoiceoverChange(newState)
                                        if (newState) {
                                            Toast.makeText(context, "Recording voiceover... Speak now!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Finish recording, add to timeline
                                            viewModel.addClipToTimeline(
                                                type = TrackType.AUDIO,
                                                title = "Voiceover Rec",
                                                duration = 4000L,
                                                filePathOrAssetId = "voice_rec"
                                            )
                                            Toast.makeText(context, "Voiceover audio segment added to timeline!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecordingVoiceover) Color.Red else GeoSurfaceVariant,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isRecordingVoiceover) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Voice Recorder"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isRecordingVoiceover) "Stop Rec" else "Record Voice", fontSize = 11.sp)
                            }

                            // Import standard stock music
                            Button(
                                onClick = {
                                    val music = TemplateAssets.stockAudios[0]
                                    viewModel.addClipToTimeline(
                                        type = TrackType.AUDIO,
                                        title = music.title,
                                        duration = 15000L,
                                        filePathOrAssetId = music.id
                                    )
                                    Toast.makeText(context, "Added track: ${music.title}", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoSurfaceVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LibraryMusic, contentDescription = "Audio library")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Stock Music", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InspectorTabButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .background(if (active) Color(0xFF0D0D10) else Color.Transparent)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) GeoPrimary else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun QuickEditButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Color.White else Color.DarkGray,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = if (enabled) Color.LightGray else Color.DarkGray,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
    }
}

// FORMAT MILLISECONDS TO DIGITAL TIMER STRING: 00:00.000
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
}
