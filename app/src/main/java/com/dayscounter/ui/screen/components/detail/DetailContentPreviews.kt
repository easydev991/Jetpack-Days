@file:Suppress("MagicNumber")

package com.dayscounter.ui.screen.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.di.FormatterModule
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.ui.theme.jetpackDaysTheme
import java.time.Duration
import java.time.Instant

// ==================== PREVIEWS ====================

private val PREVIEW_DAYS_12 = Duration.ofDays(12)
private val PREVIEW_DAYS_60 = Duration.ofDays(60)
private val PREVIEW_DAYS_120 = Duration.ofDays(120)

@Preview(showBackground = true, name = "Секция цветовой метки")
@Composable
fun colorTagSectionPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_regular)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            colorTagSection(android.graphics.Color.RED)
        }
    }
}

@Preview(showBackground = true, name = "ReadSectionView - Title")
@Composable
fun readSectionViewTitlePreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.Center,
        ) {
            readSectionView(
                headerText = "Title",
                bodyText = "День рождения",
            )
        }
    }
}

@Preview(showBackground = true, name = "ReadSectionView - Details")
@Composable
fun readSectionViewDetailsPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.Center,
        ) {
            readSectionView(
                headerText = "Details",
                bodyText = "Праздничный день с друзьями и семьей",
            )
        }
    }
}

@Preview(showBackground = true, name = "DetailDatePicker - прошлое (DAY)")
@Composable
fun detailDatePickerPastDayPreview() {
    val context = LocalContext.current
    val resourceProvider = FormatterModule.createResourceProvider(context)
    val daysFormatter = FormatterModule.createDaysFormatter()
    val formatDaysTextUseCase =
        com.dayscounter.domain.usecase
            .FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase =
        com.dayscounter.domain.usecase
            .CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        FormatterModule.createGetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            resourceProvider,
        )
    val getDaysAnalysisTextUseCase =
        FormatterModule.createGetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase,
            resourceProvider,
        )

    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.Center,
        ) {
            val timestamp =
                Instant
                    .now()
                    .minus(PREVIEW_DAYS_12)
                    .toEpochMilli()
            detailDatePicker(
                item =
                    Item(
                        id = 1L,
                        title = "День рождения",
                        details = "",
                        timestamp = timestamp,
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
            )
        }
    }
}

@Preview(showBackground = true, name = "DetailDatePicker - будущее (MONTH_DAY)")
@Composable
fun detailDatePickerFutureMonthDayPreview() {
    val context = LocalContext.current
    val resourceProvider = FormatterModule.createResourceProvider(context)
    val daysFormatter = FormatterModule.createDaysFormatter()
    val formatDaysTextUseCase =
        com.dayscounter.domain.usecase
            .FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase =
        com.dayscounter.domain.usecase
            .CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        FormatterModule.createGetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            resourceProvider,
        )
    val getDaysAnalysisTextUseCase =
        FormatterModule.createGetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase,
            resourceProvider,
        )

    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.Center,
        ) {
            val timestamp =
                Instant
                    .now()
                    .plus(PREVIEW_DAYS_60)
                    .toEpochMilli()
            detailDatePicker(
                item =
                    Item(
                        id = 1L,
                        title = "Новый год",
                        details = "",
                        timestamp = timestamp,
                        colorTag = null,
                        displayOption = DisplayOption.MONTH_DAY,
                    ),
                getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
            )
        }
    }
}

@Preview(showBackground = true, name = "DetailDatePicker - прошлое (YEAR_MONTH_DAY)")
@Composable
fun detailDatePickerPastYearMonthDayPreview() {
    val context = LocalContext.current
    val resourceProvider = FormatterModule.createResourceProvider(context)
    val daysFormatter = FormatterModule.createDaysFormatter()
    val formatDaysTextUseCase =
        com.dayscounter.domain.usecase
            .FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase =
        com.dayscounter.domain.usecase
            .CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        FormatterModule.createGetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            resourceProvider,
        )
    val getDaysAnalysisTextUseCase =
        FormatterModule.createGetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase,
            resourceProvider,
        )

    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.Center,
        ) {
            val timestamp =
                Instant
                    .now()
                    .minus(PREVIEW_DAYS_120)
                    .toEpochMilli()
            detailDatePicker(
                item =
                    Item(
                        id = 1L,
                        title = "Свадьба",
                        details = "",
                        timestamp = timestamp,
                        colorTag = null,
                        displayOption = DisplayOption.YEAR_MONTH_DAY,
                    ),
                getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
            )
        }
    }
}

@Preview(showBackground = true, name = "DetailDisplayOptionPicker")
@Composable
fun detailDisplayOptionPickerPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.Center,
        ) {
            detailDisplayOptionPicker(displayOption = DisplayOption.MONTH_DAY)
        }
    }
}

@Preview(showBackground = true, name = "DetailContentInner - полная запись")
@Composable
fun detailContentInnerPreview() {
    val context = LocalContext.current
    val resourceProvider = FormatterModule.createResourceProvider(context)
    val daysFormatter = FormatterModule.createDaysFormatter()
    val formatDaysTextUseCase =
        com.dayscounter.domain.usecase
            .FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase =
        com.dayscounter.domain.usecase
            .CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        FormatterModule.createGetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            resourceProvider,
        )
    val getDaysAnalysisTextUseCase =
        FormatterModule.createGetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase,
            resourceProvider,
        )

    jetpackDaysTheme {
        val timestamp =
            Instant
                .now()
                .minus(PREVIEW_DAYS_120)
                .toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "День рождения",
                details = "Праздничный день с друзьями и семьей",
                timestamp = timestamp,
                colorTag = android.graphics.Color.YELLOW,
                displayOption = DisplayOption.MONTH_DAY,
            )
        detailContentInner(
            item = item,
            getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
        )
    }
}

@Preview(showBackground = true, name = "DetailContentInner - без деталей и цвета")
@Composable
fun detailContentInnerSimplePreview() {
    val context = LocalContext.current
    val resourceProvider = FormatterModule.createResourceProvider(context)
    val daysFormatter = FormatterModule.createDaysFormatter()
    val formatDaysTextUseCase =
        com.dayscounter.domain.usecase
            .FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase =
        com.dayscounter.domain.usecase
            .CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        FormatterModule.createGetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            resourceProvider,
        )
    val getDaysAnalysisTextUseCase =
        FormatterModule.createGetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase,
            resourceProvider,
        )

    jetpackDaysTheme {
        val timestamp =
            Instant
                .now()
                .minus(PREVIEW_DAYS_60)
                .toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Новая запись",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DEFAULT,
            )
        detailContentInner(
            item = item,
            getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
        )
    }
}
