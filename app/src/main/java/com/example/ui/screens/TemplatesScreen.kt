package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrackType
import com.example.data.TemplateAssets
import com.example.engine.VideoEditorViewModel
import com.example.ui.theme.*

data class VideoTemplate(
    val title: String,
    val durationText: String,
    val speedStyle: String,
    val colors: List<Color>,
    val musicId: String,
    val videoIds: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: VideoEditorViewModel,
    onNavigateToEditor: () -> Unit
) {
    val context = LocalContext.current
    val templates = listOf(
        VideoTemplate("Vlog Beat Sync", "15s", "Standard 1.0x", listOf(Color(0xFF8000FF), Color(0xFF00FFFF)), "audio_synthwave", listOf("stock_tokyo", "stock_forest")),
        VideoTemplate("Cinematic Lofi Trip", "25s", "Slow Curve", listOf(Color(0xFFFF5500), Color(0xFFFFAA00)), "audio_lofi", listOf("stock_ocean", "stock_forest", "stock_nebula")),
        VideoTemplate("Bullet Time Velocity", "10s", "Curve Speed", listOf(Color(0xFFFF0055), Color(0xFFFF5500)), "audio_epic", listOf("stock_matrix", "stock_tokyo")),
        VideoTemplate("Glitch Art Sync", "18s", "Fast Ramping", listOf(Color(0xFF00FF00), Color(0xFF002200)), "audio_synthwave", listOf("stock_matrix", "stock_nebula")),
        VideoTemplate("Space Odyssey Trailer", "20s", "Epic Bezier", listOf(Color(0xFF8800FF), Color(0xFF000033)), "audio_epic", listOf("stock_nebula", "stock_matrix")),
        VideoTemplate("Nature Minimalist Vlog", "12s", "Standard", listOf(Color(0xFF336600), Color(0xFF99CC33)), "audio_lofi", listOf("stock_forest", "stock_ocean"))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GeoBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Video Templates",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Tap a template to load predefined multi-track edits",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(templates) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable {
                            // Load template on timeline
                            viewModel.createNewProject(template.title)
                            
                            var currentStart = 0L
                            template.videoIds.forEach { id ->
                                val stock = TemplateAssets.stockVideos.find { v -> v.id == id }
                                val duration = stock?.durationMs ?: 5000L
                                viewModel.addClipToTimeline(
                                    type = TrackType.VIDEO,
                                    title = stock?.title ?: "Clip",
                                    duration = duration,
                                    filePathOrAssetId = id
                                ) { clip ->
                                    clip.copy(startInTimeline = currentStart)
                                }
                                currentStart += duration
                            }

                            // Load audio
                            val audio = TemplateAssets.stockAudios.find { a -> a.id == template.musicId }
                            if (audio != null) {
                                viewModel.addClipToTimeline(
                                    type = TrackType.AUDIO,
                                    title = audio.title,
                                    duration = currentStart,
                                    filePathOrAssetId = template.musicId
                                )
                            }

                            Toast.makeText(context, "Loaded template: ${template.title}", Toast.LENGTH_SHORT).show()
                            onNavigateToEditor()
                        },
                    colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Artistic Gradient Graphic representing preview
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(template.colors))
                        )

                        // Info panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = template.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = "Music note", tint = GeoPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(template.durationText, color = Color.LightGray, fontSize = 10.sp)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(GeoPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = template.speedStyle,
                                        fontSize = 8.sp,
                                        color = GeoPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Sparkle Badge representing AI Sync
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Trending icon",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}
