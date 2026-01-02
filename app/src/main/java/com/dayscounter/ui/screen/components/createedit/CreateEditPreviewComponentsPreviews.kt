package com.dayscounter.ui.screen.components.createedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.ui.theme.jetpackDaysTheme

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Предпросмотр дней (сегодня)")
@Composable
fun previewDaysContentTodayPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_extra_large)),
            verticalArrangement = Arrangement.Center,
        ) {
            previewDaysContentInner(java.time.LocalDate.now())
        }
    }
}

@Preview(showBackground = true, name = "Предпросмотр дней (прошедшее событие)")
@Composable
fun previewDaysContentPastPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_extra_large)),
            verticalArrangement = Arrangement.Center,
        ) {
            previewDaysContentInner(
                java.time.LocalDate
                    .now()
                    .minusDays(PreviewComponentsConstants.PREVIEW_PAST_DAYS),
            )
        }
    }
}

@Preview(showBackground = true, name = "Предпросмотр дней (будущее событие)")
@Composable
fun previewDaysContentFuturePreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_extra_large)),
            verticalArrangement = Arrangement.Center,
        ) {
            previewDaysContentInner(
                java.time.LocalDate
                    .now()
                    .plusDays(PreviewComponentsConstants.PREVIEW_FUTURE_DAYS),
            )
        }
    }
}
