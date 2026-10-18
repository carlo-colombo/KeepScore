package ovh.litapp.keepscore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import ovh.litapp.keepscore.R

/**
 * Calculator dark theme for every Compose screen.
 *
 * The colour roles are read from the shared palette in `res/values/colors.xml`, so the Compose
 * screens and the View-based screens (which use [ovh.litapp.keepscore.R.style.KeepScoreMainTheme])
 * cannot drift apart: black background, dark-grey surfaces, light text, and the calculator-blue
 * and orange-operator accents.
 */
@Composable
fun KeepScoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = calculatorColorScheme(), content = content)
}

@Composable
private fun calculatorColorScheme() = darkColorScheme(
    primary = colorResource(R.color.calc_accent_blue),
    onPrimary = colorResource(R.color.calc_text_primary),
    primaryContainer = colorResource(R.color.calc_accent_blue_dark),
    onPrimaryContainer = colorResource(R.color.calc_text_primary),
    secondary = colorResource(R.color.calc_accent_orange),
    onSecondary = colorResource(R.color.calc_background),
    secondaryContainer = colorResource(R.color.calc_accent_orange_dark),
    onSecondaryContainer = colorResource(R.color.calc_text_primary),
    tertiary = colorResource(R.color.calc_accent_orange),
    onTertiary = colorResource(R.color.calc_background),
    tertiaryContainer = colorResource(R.color.calc_accent_orange_dark),
    onTertiaryContainer = colorResource(R.color.calc_text_primary),
    background = colorResource(R.color.calc_background),
    onBackground = colorResource(R.color.calc_text_primary),
    surface = colorResource(R.color.calc_surface),
    onSurface = colorResource(R.color.calc_text_primary),
    surfaceVariant = colorResource(R.color.calc_surface_alt),
    onSurfaceVariant = colorResource(R.color.calc_text_secondary),
    surfaceContainerLowest = colorResource(R.color.calc_background),
    surfaceContainerLow = colorResource(R.color.calc_surface),
    surfaceContainer = colorResource(R.color.calc_surface),
    surfaceContainerHigh = colorResource(R.color.calc_surface_alt),
    surfaceContainerHighest = colorResource(R.color.calc_surface_pressed),
    surfaceBright = colorResource(R.color.calc_surface_alt),
    surfaceDim = colorResource(R.color.calc_background),
    outline = colorResource(R.color.calc_border),
    outlineVariant = colorResource(R.color.calc_surface_alt),
    inverseSurface = colorResource(R.color.calc_text_primary),
    inverseOnSurface = colorResource(R.color.calc_background),
    inversePrimary = colorResource(R.color.calc_accent_blue_dark),
)
