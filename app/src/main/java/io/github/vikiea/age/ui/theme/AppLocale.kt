/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.ui.theme

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import io.github.vikiea.age.core.data.AppLanguage

@Composable
fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val systemLanguageTags = Resources.getSystem().configuration.locales.toLanguageTags()
    val languageTags = when (language) {
        AppLanguage.SYSTEM -> systemLanguageTags
        AppLanguage.ZH_CN -> "zh-CN"
        AppLanguage.ENGLISH -> "en"
    }
    val localizedConfiguration = remember(baseConfiguration, languageTags) {
        Configuration(baseConfiguration).apply {
            val locales = LocaleList.forLanguageTags(languageTags)
            setLocales(locales)
            if (!locales.isEmpty) setLayoutDirection(locales[0])
        }
    }
    val localizedResources = remember(baseContext, localizedConfiguration) {
        baseContext.createConfigurationContext(localizedConfiguration)
            .resources
    }
    val layoutDirection = if (localizedConfiguration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    SideEffect {
        val applicationResources = baseContext.applicationContext.resources
        if (applicationResources.configuration.locales.toLanguageTags() != languageTags) {
            @Suppress("DEPRECATION")
            applicationResources.updateConfiguration(
                Configuration(applicationResources.configuration).apply {
                    val locales = LocaleList.forLanguageTags(languageTags)
                    setLocales(locales)
                    if (!locales.isEmpty) setLayoutDirection(locales[0])
                },
                applicationResources.displayMetrics
            )
        }
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalResources provides localizedResources,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}
