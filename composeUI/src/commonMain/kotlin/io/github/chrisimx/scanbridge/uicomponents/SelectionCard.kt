package io.github.chrisimx.scanbridge.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.default_string

@Composable
fun <T> SelectionCard(
    title: String,
    options: List<T>,
    onSet: (T?) -> Unit,
    stringify: @Composable T.() -> String,
    value: T?,
    isSmallRowAbove: Boolean = false,
    hasDefaultOption: Boolean = false
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = if (isSmallRowAbove) 30.dp else 15.dp, bottom = 15.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            FlowRow(
                Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                options.forEach { option ->
                    val name = option.stringify()
                    InputChip(
                        onClick = {
                            onSet(option)
                        },
                        label = { Text(name) },
                        selected = value == option
                    )
                }
                if (hasDefaultOption) {
                    InputChip(
                        onClick = {
                            onSet(null)
                        },
                        label = { Text(stringResource(Res.string.default_string)) },
                        selected = value == null
                    )
                }
            }
        }
    }
}
