package com.dayscounter.ui.screens.createedit

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Баннер Denied (dark)"
)
@Composable
private fun ExactAlarmPermissionBannerPreviewDeniedDark() {
    JetpackDaysTheme {
        DeniedPreviewContent()
    }
}

@Preview(showBackground = true, name = "Баннер: Denied(canRequest=true)")
@Composable
fun ExactAlarmPermissionBannerPreviewDeniedCanRequest() {
    JetpackDaysTheme { DeniedPreviewContent(canRequest = true) }
}

@Preview(showBackground = true, name = "Баннер: Denied(canRequest=false)")
@Composable
fun ExactAlarmPermissionBannerPreviewDeniedCannotRequest() {
    JetpackDaysTheme { DeniedPreviewContent(canRequest = false) }
}

@Composable
private fun DeniedPreviewContent(canRequest: Boolean = true) {
    Column(
        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        DeniedCard(canRequest = canRequest, onRequestPermission = {})
    }
}
