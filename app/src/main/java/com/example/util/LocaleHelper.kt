package com.example.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.data.prefs.AppLanguage
import java.util.Locale

val LocalLocalizedContext = staticCompositionLocalOf<Context?> { null }
val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault() }

@Composable
@ReadOnlyComposable
fun appString(@StringRes resId: Int): String {
    val ctx = LocalLocalizedContext.current ?: LocalContext.current
    return ctx.getString(resId)
}

@Composable
@ReadOnlyComposable
fun appString(@StringRes resId: Int, vararg formatArgs: Any): String {
    val ctx = LocalLocalizedContext.current ?: LocalContext.current
    return ctx.getString(resId, *formatArgs)
}

object LocaleHelper {

    fun resolveLocale(context: Context, language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.ARABIC -> Locale("ar")
            AppLanguage.SYSTEM -> {
                val cfg = context.resources.configuration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cfg.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    cfg.locale
                }
            }
        }
    }

    fun isArabic(context: Context, language: AppLanguage): Boolean {
        return resolveLocale(context, language).language.equals("ar", ignoreCase = true)
    }

    fun localizedContext(context: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return context
        val locale = resolveLocale(context, language)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }
}
