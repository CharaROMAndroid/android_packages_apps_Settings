/*
 * Copyright (C) 2026 CharaROM
 * SPDX-License-Identifier: Apache-2.0
 */

package com.crdroid.settings.fragments.misc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.settings.R
import com.android.settingslib.spa.framework.theme.SettingsTheme
import java.util.function.BooleanSupplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Uses the very same themed cards, search field, app rows and footer as PIF/TrickyStore.
 * Lives in Settings (where both restricted dashboard entry points live); crDroidSettings
 * sources and Settings sources are compiled into the same Settings module.
 */
object StateHidingAppPicker {
    @JvmStatic
    fun createView(
        context: Context,
        lifecycle: Lifecycle,
        key: String,
        title: Int,
        explanation: Int,
        restricted: BooleanSupplier,
    ): View = spoofComposeView(context).apply {
        setContent {
            SettingsTheme {
                StateHidingContent(context, lifecycle, key, title, explanation, restricted)
            }
        }
    }
}

@Composable
private fun StateHidingContent(
    context: Context,
    lifecycle: Lifecycle,
    key: String,
    title: Int,
    explanation: Int,
    restricted: BooleanSupplier,
) {
    var query by rememberSaveable(key) { mutableStateOf("") }
    // The old picker exposed every application UID, including system apps.
    var showSystem by rememberSaveable(key) { mutableStateOf(true) }
    var refresh by remember { mutableStateOf(0) }
    var isRestricted by remember { mutableStateOf(restricted.asBoolean) }
    var loading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<AppListEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isRestricted = restricted.asBoolean
                refresh++
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(key, refresh) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            val stored = Settings.Secure.getString(context.contentResolver, key)
            val selection = stored?.takeIf { it.isNotEmpty() }?.split(",")?.toSet()
                ?: emptySet()
            val pm = context.packageManager
            // Keep exactly the old app-UID eligibility and locale-aware label sorting.
            val entries = pm.getInstalledApplications(0)
                .filter { UserHandle.getAppId(it.uid) >= Process.FIRST_APPLICATION_UID }
                .sortedWith(ApplicationInfo.DisplayNameComparator(pm))
                .map { app ->
                    AppListEntry(
                        packageName = app.packageName,
                        label = app.loadLabel(pm).toString(),
                        icon = runCatching { app.loadIcon(pm) }.getOrNull(),
                        isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    )
                }
            selection to entries
        }
        selected = result.first
        apps = result.second
        loading = false
    }

    val filtered = remember(apps, selected, query, showSystem) {
        val needle = query.trim()
        apps.filter {
            (showSystem || !it.isSystem || it.packageName in selected) &&
                (it.label.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true))
        }
    }
    val count = apps.count { it.packageName in selected }

    Column(
        Modifier.fillMaxSize().padding(horizontal = SpoofDimens.ScreenInset),
    ) {
        Spacer(Modifier.height(8.dp))
        SpoofingHeaderCard(
            title = stringResource(title),
            subtitle = stringResource(R.string.state_hiding_apps_count, count),
        ) {
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = spoofHeaderIconTint(null),
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        AppPickerSearchField(query = query, onQueryChange = { query = it })
        Spacer(Modifier.height(8.dp))
        SpoofActionRow(
            listOf(
                SpoofAction(
                    text = stringResource(R.string.ts_system_apps),
                    icon = Icons.Rounded.PhoneAndroid,
                    selected = showSystem,
                    enabled = !loading,
                    onClick = { showSystem = !showSystem },
                ),
            ),
        )
        Spacer(Modifier.height(12.dp))
        if (loading) {
            SpoofingLoadingBox(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).toolbarNestedScroll(),
                verticalArrangement = Arrangement.spacedBy(SpoofDimens.RowGap),
                contentPadding = PaddingValues(bottom = 24.dp + spoofBottomInset()),
            ) {
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        SpoofingEmptyState(
                            icon = Icons.Rounded.Shield,
                            title = stringResource(R.string.state_hiding_no_apps),
                            description = stringResource(R.string.state_hiding_no_apps_summary),
                        )
                    }
                }
                itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                    AppPickerItem(
                        packageName = app.packageName,
                        label = app.label,
                        icon = app.icon,
                        isSystem = app.isSystem,
                        checked = app.packageName in selected,
                        enabled = !isRestricted,
                        shape = SpoofShapes.forIndex(index, filtered.size),
                        onToggle = { checked ->
                            // Recheck at the write boundary, not only when the UI was drawn.
                            isRestricted = restricted.asBoolean
                            if (!isRestricted) {
                                val next = if (checked) selected + app.packageName
                                    else selected - app.packageName
                                // Preserve invisible/uninstalled selections and comma format.
                                if (Settings.Secure.putString(
                                        context.contentResolver, key,
                                        next.sorted().joinToString(","),
                                    )) {
                                    selected = next
                                }
                            }
                        },
                    )
                }
                item(key = "explanation") {
                    SpoofFooter(stringResource(explanation))
                }
            }
        }
    }
}
