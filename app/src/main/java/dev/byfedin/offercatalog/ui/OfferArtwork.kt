package dev.byfedin.offercatalog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import dev.byfedin.offercatalog.model.Category

// Local geometric illustrations share a 320 × 180 coordinate space.
@Composable
fun OfferArtwork(category: Category, modifier: Modifier = Modifier) {
    val background =
        when (category) {
            Category.LEARNING -> Color(0xFFE5EBCF)
            Category.TRAVEL -> Color(0xFFE1EAF0)
            Category.DIGITAL -> Color(0xFFE8E1D8)
            Category.WELLBEING -> Color(0xFFEEDFD6)
        }
    Canvas(modifier.clipToBounds().background(background)) {
        withTransform({ scale(size.width / 320f, size.height / 180f, pivot = Offset.Zero) }) {
            when (category) {
                Category.LEARNING -> {
                    drawCircle(Color(0xFFD1DBB1), 87f, Offset(268f, 32f))
                    drawCircle(Color(0xFFF9FAED), 52f, Offset(64f, 155f))
                    val left =
                        Path().apply {
                            moveTo(76f, 44f)
                            lineTo(159f, 59f)
                            lineTo(159f, 146f)
                            lineTo(76f, 130f)
                            close()
                        }
                    val right =
                        Path().apply {
                            moveTo(163f, 59f)
                            lineTo(246f, 44f)
                            lineTo(246f, 130f)
                            lineTo(163f, 146f)
                            close()
                        }
                    drawPath(left, Color(0xFF315E4C))
                    drawPath(right, Color(0xFF527E5C))
                    for (y in listOf(66f, 83f, 100f)) {
                        drawLine(Color(0xFFC9DBAE), Offset(94f, y), Offset(141f, y + 9f), 3f)
                        drawLine(Color(0xFFC9DBAE), Offset(181f, y + 9f), Offset(228f, y), 3f)
                    }
                }
                Category.TRAVEL -> {
                    drawCircle(Color(0xFFF7F3D8), 28f, Offset(241f, 45f))
                    val far =
                        Path().apply {
                            moveTo(0f, 130f)
                            lineTo(99f, 39f)
                            lineTo(196f, 149f)
                            lineTo(320f, 84f)
                            lineTo(320f, 180f)
                            lineTo(0f, 180f)
                            close()
                        }
                    drawPath(far, Color(0xFF849DA5))
                    val near =
                        Path().apply {
                            moveTo(0f, 180f)
                            lineTo(157f, 74f)
                            lineTo(265f, 180f)
                            close()
                        }
                    drawPath(near, Color(0xFF345E64))
                    val trail =
                        Path().apply {
                            moveTo(160f, 180f)
                            cubicTo(216f, 148f, 111f, 143f, 157f, 99f)
                        }
                    drawPath(trail, Color(0xFFE7E2C5), style = Stroke(5f))
                }
                Category.DIGITAL -> {
                    drawCircle(Color(0xFFD6CCC0), 80f, Offset(60f, 36f))
                    drawRect(Color(0xFF746753), Offset(93f, 37f), Size(142f, 112f))
                    drawRect(Color(0xFFF8F3E8), Offset(81f, 27f), Size(142f, 112f))
                    drawRect(Color(0xFF3F655B), Offset(95f, 42f), Size(43f, 43f))
                    drawCircle(Color(0xFFE0BE70), 13f, Offset(116f, 63f))
                    for (y in listOf(47f, 61f, 75f)) drawLine(
                        Color(0xFFB9B2A2),
                        Offset(153f, y),
                        Offset(208f, y),
                        4f,
                    )
                    for (y in listOf(103f, 117f)) drawLine(
                        Color(0xFFB9B2A2),
                        Offset(97f, y),
                        Offset(208f, y),
                        4f,
                    )
                    drawCircle(Color(0xFF3F655B), 16f, Offset(257f, 132f))
                }
                Category.WELLBEING -> {
                    drawCircle(Color(0xFFE0BFAE), 63f, Offset(242f, 54f))
                    drawCircle(Color(0xFFF9EEE3), 45f, Offset(70f, 144f))
                    drawLine(Color(0xFF415F48), Offset(161f, 159f), Offset(161f, 36f), 4f)
                    val leaf1 =
                        Path().apply {
                            moveTo(160f, 97f)
                            cubicTo(93f, 100f, 91f, 46f, 114f, 48f)
                            cubicTo(143f, 49f, 158f, 67f, 160f, 97f)
                            close()
                        }
                    val leaf2 =
                        Path().apply {
                            moveTo(163f, 127f)
                            cubicTo(211f, 132f, 235f, 82f, 207f, 79f)
                            cubicTo(179f, 78f, 166f, 99f, 163f, 127f)
                            close()
                        }
                    drawPath(leaf1, Color(0xFF66806A))
                    drawPath(leaf2, Color(0xFF415F48))
                }
            }
        }
    }
}
