package com.dayscounter.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.dayscounter.BuildConfig
import com.dayscounter.R
import com.dayscounter.navigation.Screen
import com.dayscounter.ui.theme.JetpackDaysTheme
import com.dayscounter.util.AppConstants

/** Экран с дополнительными функциями и настройками приложения. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavHostController? = null) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.spacing_xsmall)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Кнопки настроек
            SettingsButtons(navController)

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))

            // Кнопки действий
            ActionButtons(context)

            Spacer(modifier = Modifier.weight(1f))

            // Версия приложения (снизу с учетом paddingValues)
            AppVersionText()
        }
    }
}

/** Кнопки настроек (заглушки). */
@Composable
private fun SettingsButtons(navController: NavHostController?) {
    // Кнопка "Тема и иконка"
    MoreButton(
        text = stringResource(R.string.app_theme_and_icon),
        onClick = {
            navController?.navigate(Screen.ThemeIcon.route) {
                popUpTo(Screen.More.route)
            }
        },
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))

    // Кнопка "Данные приложения"
    MoreButton(
        text = stringResource(R.string.app_data),
        onClick = {
            navController?.navigate(Screen.AppData.route) {
                popUpTo(Screen.More.route)
            }
        },
    )
}

/** Кнопки действий. */
@Composable
private fun ActionButtons(context: Context) {
    // Кнопка "Отправить отзыв"
    MoreButton(
        text = stringResource(R.string.send_feedback),
        onClick = { sendFeedback(context) },
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))

    // Кнопка "Оценить приложение"
    MoreButton(
        text = stringResource(R.string.rate_the_app),
        onClick = { rateApp(context) },
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))

    // Кнопка "Поделиться приложением"
    MoreButton(
        text = stringResource(R.string.share_the_app),
        onClick = { shareApp(context) },
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))

    // Кнопка "Страница на GitHub"
    MoreButton(
        text = stringResource(R.string.github_page),
        onClick = { openGitHub(context) },
    )
}

/** Текст версии приложения. */
@Composable
private fun AppVersionText() {
    Text(
        text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_xsmall)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
    )
}

/** Кнопка для экрана More. */
@Composable
private fun MoreButton(
    text: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding =
            PaddingValues(
                horizontal = dimensionResource(R.dimen.button_horizontal_padding),
            ),
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                ),
        )
    }
}

/** Отправляет отзыв по электронной почте. */
private fun sendFeedback(context: Context) {
    val appName =
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(context.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w("MoreScreen", "Не удалось получить название приложения: ${e.message}")
            "JetpackDays"
        }

    val subject = context.getString(R.string.feedback_subject, appName)
    val body =
        context.getString(
            R.string.feedback_body,
            Build.VERSION.RELEASE,
            BuildConfig.VERSION_NAME,
        )

    val encodedSubject = Uri.encode(subject)
    val encodedBody = Uri.encode(body)
    val uri = "mailto:easy_dev991@mail.ru?subject=$encodedSubject&body=$encodedBody".toUri()
    val intent = Intent(Intent.ACTION_SENDTO, uri)

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("MoreScreen", "Почтовый клиент не найден", e)
        Toast.makeText(context, R.string.no_email_client, Toast.LENGTH_LONG).show()
    }
}

/** Открывает страницу приложения для оценки. */
private fun rateApp(context: Context) {
    val intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(AppConstants.APP_RATE_URL),
        )

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("MoreScreen", "Не удалось открыть страницу приложения: ${e.message}")
    }
}

/** Делится ссылкой на приложение через ShareSheet. */
private fun shareApp(context: Context) {
    val intent =
        Intent(
            Intent.ACTION_SEND,
        ).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(
                    R.string.share_text,
                    context.getString(R.string.app_display_name),
                ) + "\n" + AppConstants.APP_SHARE_URL,
            )
            putExtra(Intent.EXTRA_TITLE, context.getString(R.string.share_chooser_title))
        }

    try {
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_chooser_title),
            ),
        )
    } catch (e: ActivityNotFoundException) {
        Log.e("MoreScreen", "Не удалось открыть ShareSheet: ${e.message}")
    }
}

/** Открывает репозиторий приложения на GitHub. */
private fun openGitHub(context: Context) {
    val intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(AppConstants.GITHUB_REPOSITORY_URL),
        )

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("MoreScreen", "Не удалось открыть GitHub: ${e.message}")
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "More Screen")
@Composable
fun MoreScreenPreview() {
    JetpackDaysTheme { MoreScreen(null) }
}
