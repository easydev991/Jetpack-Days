package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.ui.theme.JetpackDaysTheme

@Preview(showBackground = true, name = "Баннер: Denied(canRequest=true)")
@Composable
fun ExactAlarmPermissionBannerPreviewDeniedCanRequest() {
    JetpackDaysTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeniedCard(canRequest = true, onRequestPermission = {})
        }
    }
}

@Preview(showBackground = true, name = "Баннер: Denied(canRequest=false)")
@Composable
fun ExactAlarmPermissionBannerPreviewDeniedCannotRequest() {
    JetpackDaysTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeniedCard(canRequest = false, onRequestPermission = {})
        }
    }
}
