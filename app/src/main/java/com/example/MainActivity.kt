package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scheduler.NotificationHelper
import com.example.ui.TimeDeleteMainShell
import com.example.ui.theme.TimeDeleteTheme
import com.example.ui.viewmodel.TimeDeleteViewModel
import com.example.util.LocalAppLocale
import com.example.util.LocalLocalizedContext
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val viewModel: TimeDeleteViewModel by viewModels()
    private var deepLinkDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current
            val appLocale = remember(settings.appLanguage, baseContext) {
                LocaleHelper.resolveLocale(baseContext, settings.appLanguage)
            }
            val localizedContext = remember(settings.appLanguage, baseContext) {
                LocaleHelper.localizedContext(baseContext, settings.appLanguage)
            }
            val layoutDirection = if (appLocale.language.equals("ar", ignoreCase = true)) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(
                LocalLocalizedContext provides localizedContext,
                LocalAppLocale provides appLocale,
                LocalLayoutDirection provides layoutDirection
            ) {
                TimeDeleteTheme(themeMode = settings.themeMode) {
                    TimeDeleteMainShell(
                        viewModel = viewModel,
                        deepLinkDestination = deepLinkDestination,
                        onDeepLinkConsumed = { deepLinkDestination = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dest = intent?.getStringExtra(NotificationHelper.EXTRA_NAV_DESTINATION)
        if (!dest.isNullOrBlank()) {
            deepLinkDestination = dest
        }
    }
}
