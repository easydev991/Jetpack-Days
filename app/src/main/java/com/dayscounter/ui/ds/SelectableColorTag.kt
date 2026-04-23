package com.dayscounter.ui.ds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme

private object SelectableColorTagPreviewConstants {
    @Suppress("MagicNumber")
    val PREVIEW_COLOR = Color(0xFF43A047)
}

@Composable
internal fun SelectableColorTag(
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorTagSize = dimensionResource(R.dimen.color_tag_size)
    val borderWidth = dimensionResource(R.dimen.border_width)

    Box(
        modifier =
            modifier
                .size(colorTagSize)
                .then(
                    if (isSelected) {
                        Modifier
                            .border(
                                BorderStroke(borderWidth, MaterialTheme.colorScheme.outline),
                                CircleShape
                            ).padding(borderWidth)
                            .border(
                                BorderStroke(borderWidth, Color.White),
                                CircleShape
                            ).padding(borderWidth)
                    } else {
                        Modifier
                    }
                ).clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick)
    )
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, name = "ColorTag - Not Selected")
@Composable
private fun SelectableColorTagNotSelectedPreview() {
    JetpackDaysTheme {
        SelectableColorTag(
            color = SelectableColorTagPreviewConstants.PREVIEW_COLOR,
            isSelected = false,
            onClick = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, name = "ColorTag - Selected")
@Composable
private fun SelectableColorTagSelectedPreview() {
    JetpackDaysTheme {
        SelectableColorTag(
            color = SelectableColorTagPreviewConstants.PREVIEW_COLOR,
            isSelected = true,
            onClick = {}
        )
    }
}
