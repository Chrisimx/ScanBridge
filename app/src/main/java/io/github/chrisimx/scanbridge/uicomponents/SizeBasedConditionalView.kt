package io.github.chrisimx.scanbridge.uicomponents

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import timber.log.Timber

@Composable
fun SizeBasedConditionalView(modifier: Modifier = Modifier, largeView: @Composable () -> Unit, smallView: @Composable () -> Unit) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val relaxedWidthConstraints = Constraints(
            minWidth = 0,
            maxWidth = Constraints.Infinity,
            minHeight = 0,
            maxHeight = constraints.maxHeight
        )

        val placeablesA = subcompose("A", largeView).map {
            it.measure(relaxedWidthConstraints)
        }

        val totalHeightA = placeablesA.maxOfOrNull { it.height } ?: 0
        Timber.d(totalHeightA.toString())
        val totalWidthA = placeablesA.maxOfOrNull { it.width } ?: 0
        Timber.d(totalWidthA.toString())

        val fitsInBounds = totalWidthA <= constraints.maxWidth &&
            totalHeightA <= constraints.maxHeight

        val placeablesToUse = if (fitsInBounds) {
            subcompose("UsedA", largeView).map { it.measure(constraints) }
        } else {
            subcompose("UsedB", smallView).map { it.measure(constraints) }
        }

        val layoutWidth = placeablesToUse.maxOfOrNull { it.width } ?: 0
        val layoutHeight = placeablesToUse.maxOfOrNull { it.height } ?: 0

        layout(layoutWidth, layoutHeight) {
            placeablesToUse.forEach {
                it.place(0, 0)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SizeBasedConditionalViewPreview() {
    SizeBasedConditionalView(
        largeView = {
            Text("This is dfsdffsddsfsdfdsfdsfsdffffffffffffffffffffdsssssssssssssssssss")
        },
        smallView = {
            Text("Fallback content")
        }
    )
}
