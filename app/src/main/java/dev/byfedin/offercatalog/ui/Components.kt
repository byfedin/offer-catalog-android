package dev.byfedin.offercatalog.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.byfedin.offercatalog.R
import dev.byfedin.offercatalog.model.Category

@get:StringRes
val Category.label: Int
    get() =
        when (this) {
            Category.LEARNING -> R.string.category_learning
            Category.TRAVEL -> R.string.category_travel
            Category.DIGITAL -> R.string.category_digital
            Category.WELLBEING -> R.string.category_wellbeing
        }

@Composable
fun StatusPanel(
    title: String,
    message: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp).semantics {
            liveRegion = LiveRegionMode.Polite
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (message != null)
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
fun LoadingPanel(label: String = stringResource(R.string.loading)) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
