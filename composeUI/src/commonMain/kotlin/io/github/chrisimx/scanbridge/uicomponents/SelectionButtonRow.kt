package io.github.chrisimx.scanbridge.uicomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun <T> SelectionButtonRow(
    title: String,
    options: List<T>,
    onSet: (T?) -> Unit,
    stringify: @Composable T.() -> String,
    value: T?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, option ->
                val name = option.stringify()
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = {
                        onSet(option)
                    },
                    selected = option == value
                ) {
                    Text(name)
                }
            }
        }
    }
}
