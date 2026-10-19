package ovh.litapp.keepscore.ui.about

import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.ui.viewinterop.AndroidView
import ovh.litapp.keepscore.R

@Composable
fun AboutScreen(
    title: String,
    pageHtml: String,
    onOpenLink: (Uri) -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val aboutContentDescription = stringResource(R.string.text_about)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Original app and source code by Nolan Lawson.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
        AboutCreditLink("Upstream repository: Nolan Lawson", UPSTREAM_REPOSITORY, onOpenLink)
        Text(
            text = "This fork is maintained by Carlo Colombo.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
        AboutCreditLink("Fork repository", FORK_REPOSITORY, onOpenLink)
        AboutCreditLink("WTFPL Version 2 license", WTFPL_LICENSE, onOpenLink)
        AboutCreditLink("Help translate KeepScore", TRANSLATION_HELP, onOpenLink)

        // The existing raw HTML is localized (including contributor lists and changelog markup).
        // Retain it as page content only; the enclosing screen and its controls are Compose.
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            factory = {
                WebView(context).apply {
                    this.contentDescription = "$aboutContentDescription content"
                    settings.defaultTextEncodingName = "utf-8"
                    setBackgroundColor(Color.TRANSPARENT)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = openExternalLink(request.url, onOpenLink)
                    }
                    loadDataWithBaseURL(null, pageHtml, "text/html", "utf-8", null)
                }
            },
        )

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(android.R.string.ok))
        }
    }
}

@Composable
private fun AboutCreditLink(label: String, url: String, onOpenLink: (Uri) -> Unit) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .semantics { role = Role.Button }
            .clickable { onOpenLink(url.toUri()) }
            .padding(vertical = 2.dp),
    )
}

private fun openExternalLink(uri: Uri, onOpenLink: (Uri) -> Unit): Boolean =
    when (uri.scheme?.lowercase()) {
        "http", "https", "mailto", "market" -> {
            onOpenLink(uri)
            true
        }
        else -> false
    }

private const val UPSTREAM_REPOSITORY = "https://github.com/nolanlawson/KeepScore"
private const val FORK_REPOSITORY = "https://github.com/carlo-colombo/KeepScore"
private const val WTFPL_LICENSE = "http://www.wtfpl.net/"
private const val TRANSLATION_HELP = "https://github.com/nolanlawson/KeepScore#add-a-translation"
