package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubtitleBackgroundStyle
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.SubtitleVerticalPosition

@Composable
fun SubtitleOverlay(
    text: String?,
    config: SubtitleStyleConfig,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !text.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        val alignment = when (config.verticalPosition) {
            SubtitleVerticalPosition.TOP -> Alignment.TopCenter
            SubtitleVerticalPosition.CENTER -> Alignment.Center
            SubtitleVerticalPosition.BOTTOM -> Alignment.BottomCenter
        }

        val yOffset = when (config.verticalPosition) {
            SubtitleVerticalPosition.TOP -> config.verticalOffsetDp.dp
            SubtitleVerticalPosition.CENTER -> 0.dp
            SubtitleVerticalPosition.BOTTOM -> (-config.verticalOffsetDp).dp
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .offset(y = yOffset)
                    .then(
                        when (config.backgroundStyle) {
                            SubtitleBackgroundStyle.TRANSLUCENT_BLACK -> Modifier
                                .background(
                                    color = Color(0xB30A0A0E),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)

                            SubtitleBackgroundStyle.SOLID_BLACK -> Modifier
                                .background(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)

                            SubtitleBackgroundStyle.SHADOW_OUTLINE,
                            SubtitleBackgroundStyle.NONE -> Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        }
                    )
            ) {
                Text(
                    text = text ?: "",
                    color = Color(config.textColorHex),
                    fontSize = config.fontSizeSp.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = if (config.backgroundStyle == SubtitleBackgroundStyle.SHADOW_OUTLINE ||
                            config.backgroundStyle == SubtitleBackgroundStyle.NONE
                        ) {
                            Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        } else null
                    ),
                    modifier = Modifier.testTag("subtitle_text")
                )
            }
        }
    }
}
