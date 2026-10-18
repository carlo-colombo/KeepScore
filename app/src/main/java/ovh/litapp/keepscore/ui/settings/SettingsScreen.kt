package ovh.litapp.keepscore.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.SettingSetHelper
import ovh.litapp.keepscore.helper.ToastHelper
import ovh.litapp.keepscore.util.IntegerUtil

/**
 * The Settings screen.
 *
 * Recreates the preference screen that used to live in res/xml/settings.xml. Every row reads and
 * writes the same SharedPreferences entries, so [ovh.litapp.keepscore.helper.PreferenceHelper] and
 * the rest of the app see exactly the values they saw before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scrollToConfigurations: Boolean,
    onNavigateToAbout: () -> Unit,
    onReload: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = remember(context) { SettingsController(context) }
    val listState = rememberLazyListState()

    var integerDialog by remember { mutableStateOf<IntegerPreference?>(null) }
    var choiceDialog by remember { mutableStateOf<ChoicePreference?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToConfigurations) {
        if (scrollToConfigurations) {
            val configIndex = controller.rows.indexOfFirst {
                it is SettingsRow.Category && it.titleRes == R.string.pref_cat_config
            }
            if (configIndex >= 0) {
                listState.scrollToItem(configIndex)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "\u2190", fontSize = 24.sp)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(controller.rows, key = { it.stableKey }) { row ->
                when (row) {
                    is SettingsRow.Category -> CategoryHeader(row.titleRes)

                    is SettingsRow.Toggle -> ToggleRow(
                        spec = row.spec,
                        checked = controller.booleanValues[row.spec.key] ?: row.spec.defaultValue,
                        onCheckedChange = { controller.setBoolean(row.spec, it) },
                    )

                    is SettingsRow.Integer -> IntegerRow(
                        spec = row.spec,
                        value = controller.integerValues[row.spec.key] ?: row.spec.defaultValue,
                        onClick = { integerDialog = row.spec },
                    )

                    is SettingsRow.Choice -> ChoiceRow(
                        spec = row.spec,
                        value = controller.listValues[row.spec.key] ?: row.spec.defaultValue,
                        onClick = { choiceDialog = row.spec },
                    )

                    is SettingsRow.Action -> ActionRow(
                        row = row,
                        onClick = {
                            when (row.action) {
                                SettingsAction.RESET -> showResetDialog = true

                                SettingsAction.SAVE_SETTINGS_SET -> showSaveDialog = true

                                SettingsAction.LOAD_SETTINGS_SET ->
                                    if (controller.availableSettingSets().isEmpty()) {
                                        ToastHelper.showShort(context, R.string.toast_no_saved_settings)
                                    } else {
                                        showLoadDialog = true
                                    }

                                SettingsAction.ABOUT -> onNavigateToAbout()
                            }
                        },
                    )
                }
            }
        }
    }

    integerDialog?.let { spec ->
        IntegerPreferenceDialog(
            spec = spec,
            initialValue = controller.integerValues[spec.key] ?: spec.defaultValue,
            onDismiss = { integerDialog = null },
            onConfirm = { text ->
                integerDialog = null
                applyIntegerChange(context, controller, spec, text)
            },
        )
    }

    choiceDialog?.let { spec ->
        ChoicePreferenceDialog(
            spec = spec,
            currentValue = controller.listValues[spec.key] ?: spec.defaultValue,
            onDismiss = { choiceDialog = null },
            onSelect = { value ->
                controller.setChoice(spec, value)
                choiceDialog = null
            },
        )
    }

    if (showSaveDialog) {
        SaveSettingSetDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                showSaveDialog = false
                if (!SettingSetHelper.isValidSettingSetName(name)) {
                    ToastHelper.showShort(context, R.string.toast_invalid_setting_set_name)
                } else {
                    controller.saveSettingSet(name)
                    ToastHelper.showShort(context, R.string.toast_saved_setting_set_name, name)
                }
            },
        )
    }

    if (showLoadDialog) {
        LoadSettingSetDialog(
            controller = controller,
            onDismiss = { showLoadDialog = false },
            onLoad = { name ->
                showLoadDialog = false
                controller.loadSettingSet(name)
                ToastHelper.showShort(context, R.string.toast_loaded_setting_set, name)
                onReload()
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.title_confirm)) },
            text = { Text(stringResource(R.string.text_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    controller.resetAll()
                    ToastHelper.showShort(context, R.string.toast_settings_reset)
                    onReload()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private fun applyIntegerChange(
    context: Context,
    controller: SettingsController,
    spec: IntegerPreference,
    text: String,
) {
    val errorRes = integerValidationError(spec.validation, text)
    if (errorRes != null) {
        ToastHelper.showLong(context, errorRes)
    } else {
        controller.setInteger(spec, text)
    }
}

/**
 * The validation the legacy integer preferences registered, returning the toast to show or null
 * when [text] is an acceptable value for [validation].
 */
internal fun integerValidationError(validation: IntegerValidation, text: String): Int? {
    val valid = IntegerUtil.validInt(text)
    val number = IntegerUtil.parseIntOrZero(text)
    return when (validation) {
        IntegerValidation.NON_ZERO ->
            if (!valid || number == 0) R.string.toast_no_zeroes else null

        IntegerValidation.ANY_INTEGER ->
            if (!valid) R.string.toast_valid_initial_score else null

        IntegerValidation.UPDATE_DELAY ->
            if (!valid || number !in 1..600) R.string.toast_valid_update_delay_values else null
    }
}

@Composable
private fun CategoryHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToggleRow(
    spec: TogglePreference,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingRow(
        title = stringResource(spec.titleRes),
        summary = spec.summaryRes?.let { stringResource(it) },
        onClick = { onCheckedChange(!checked) },
        trailing = { Checkbox(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun IntegerRow(
    spec: IntegerPreference,
    value: String,
    onClick: () -> Unit,
) {
    val summary = spec.summaryRes?.let { stringResource(it) } ?: value
    SettingRow(title = stringResource(spec.titleRes), summary = summary, onClick = onClick)
}

@Composable
private fun ChoiceRow(
    spec: ChoicePreference,
    value: String,
    onClick: () -> Unit,
) {
    val entries = stringArrayResource(spec.entriesRes)
    val entryValues = stringArrayResource(spec.entryValuesRes)
    val index = entryValues.indexOf(value)
    val summary = if (index in entries.indices) entries[index] else value
    SettingRow(title = stringResource(spec.titleRes), summary = summary, onClick = onClick)
}

@Composable
private fun ActionRow(row: SettingsRow.Action, onClick: () -> Unit) {
    val summary = row.summaryText ?: row.summaryRes?.let { stringResource(it) }
    SettingRow(title = stringResource(row.titleRes), summary = summary, onClick = onClick)
}

@Composable
private fun SettingRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun IntegerPreferenceDialog(
    spec: IntegerPreference,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }

    val trailingIcon: (@Composable () -> Unit)? = if (spec.allowNegative) {
        {
            TextButton(onClick = { text = toggleSign(text) }) {
                Text(text = "+/\u2212")
            }
        }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(spec.titleRes)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = sanitizeIntegerInput(it, spec.allowNegative) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm(text) }),
                trailingIcon = trailingIcon,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ChoicePreferenceDialog(
    spec: ChoicePreference,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val entries = stringArrayResource(spec.entriesRes)
    val entryValues = stringArrayResource(spec.entryValuesRes)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(spec.titleRes)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(entryValues[index]) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = entryValues[index] == currentValue,
                            onClick = { onSelect(entryValues[index]) },
                        )
                        Text(text = entry, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun SaveSettingSetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_save_setting_set)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.text_enter_saved_settings_name)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm(text) }),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun LoadSettingSetDialog(
    controller: SettingsController,
    onDismiss: () -> Unit,
    onLoad: (String) -> Unit,
) {
    val settingSets = remember {
        mutableStateListOf<String>().apply { addAll(controller.availableSettingSets()) }
    }
    var contentsToShow by remember { mutableStateOf<String?>(null) }
    var deleteToConfirm by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_load_settings_name)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                settingSets.forEach { name ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { contentsToShow = name }
                                .padding(vertical = 12.dp),
                        )
                        IconButton(onClick = { deleteToConfirm = name }) {
                            Text(text = "\u2715")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )

    contentsToShow?.let { name ->
        val contents = remember(name) { controller.settingSetContents(name) }

        AlertDialog(
            onDismissRequest = { contentsToShow = null },
            title = { Text(stringResource(R.string.title_load_setting_set, name)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    contents.forEach { (title, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Text(text = title, modifier = Modifier.weight(1f))
                            Text(text = value, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    contentsToShow = null
                    onLoad(name)
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { contentsToShow = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    deleteToConfirm?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteToConfirm = null },
            title = { Text(stringResource(R.string.title_confirm_delete)) },
            text = { Text(stringResource(R.string.text_delete_saved_settings, name)) },
            confirmButton = {
                TextButton(onClick = {
                    controller.deleteSettingSet(name)
                    settingSets.remove(name)
                    deleteToConfirm = null
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteToConfirm = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

/** Keeps only digits (and, when allowed, one leading sign), like the legacy DigitsKeyListener. */
internal fun sanitizeIntegerInput(input: String, allowNegative: Boolean): String {
    val negative = allowNegative && input.startsWith("-")
    val digits = input.filter { it.isDigit() }
    return if (negative) "-$digits" else digits
}

internal fun toggleSign(text: String): String =
    if (text.startsWith("-")) text.removePrefix("-") else "-$text"
