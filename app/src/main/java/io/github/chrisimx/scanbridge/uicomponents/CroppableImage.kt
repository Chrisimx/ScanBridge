package io.github.chrisimx.scanbridge.uicomponents

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import timber.log.Timber


/**
 * Determines if a given position is within a rect with a clearance for a given error (so that touch does not feel clunky).
 *
 * @param errorEpsilon This is allowed amount of being outside of the actual rectangle
 */
fun isPositionInRect(position: Offset, rect: Rect, errorEpsilon: Float = 40f): Boolean {
    val isHorizontallyInRect = abs(position.x - rect.center.x) < (rect.width / 2) + errorEpsilon
    val isVerticallyInRect = abs(position.y - rect.center.y) < (rect.height / 2) + errorEpsilon

    return isHorizontallyInRect && isVerticallyInRect
}

enum class Edge { TOP, LEFT, BOTTOM, RIGHT }

data class EdgeFlags(
    val top: Boolean = false,
    val left: Boolean = false,
    val bottom: Boolean = false,
    val right: Boolean = false
)

data class HandleSpec(
    val position: Offset,
    val draggedEdges: Set<Edge>
) {
    val edgeFlags: EdgeFlags
        get() = EdgeFlags(
            top = Edge.TOP in draggedEdges,
            left = Edge.LEFT in draggedEdges,
            bottom = Edge.BOTTOM in draggedEdges,
            right = Edge.RIGHT in draggedEdges
        )
}

/**
 * Create a HandleSpec based on the handle position and edges that should be moved when the handle is dragged
 */
fun Offset.asScalingHandle(
    vararg edge: Edge
) = HandleSpec(
    position = this,
    draggedEdges = edge.toSet()
)

fun Rect.applyResizeDrag(drag: Offset, size: IntSize, flags: EdgeFlags, density: Density, clearance: Dp = 30.dp): Rect {
    return with(density) {
        copy(
            left = max(left + if (flags.left) drag.x else 0f, 0f),
            right = min(max(right + if (flags.right) drag.x else 0f, left + clearance.toPx()), size.width.toFloat()),
            top = max(top + if (flags.top) drag.y else 0f, 0f),
            bottom = min(max(bottom + if (flags.bottom) drag.y else 0f, top + clearance.toPx()), size.height.toFloat())
        )
    }
}

sealed class CropDragEvent {
    data class ResizeHandleDragged(val idx: Int) : CropDragEvent()
    object DraggedOutside : CropDragEvent()
    object DraggedInside : CropDragEvent()
}

@Composable
fun CropOverlay(
    modifier: Modifier,
    onRectChange: (Rect) -> Unit = {}
) {
    var rect by remember { mutableStateOf(Rect(0f, 00f, 50f, 50f)) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    val handles by derivedStateOf {
        arrayOf(
            rect.topLeft.asScalingHandle(Edge.TOP, Edge.LEFT),
            rect.centerLeft.asScalingHandle(Edge.LEFT),
            rect.bottomLeft.asScalingHandle(Edge.LEFT, Edge.BOTTOM),
            rect.bottomCenter.asScalingHandle(Edge.BOTTOM),
            rect.bottomRight.asScalingHandle(Edge.BOTTOM, Edge.RIGHT),
            rect.centerRight.asScalingHandle(Edge.RIGHT),
            rect.topRight.asScalingHandle(Edge.TOP, Edge.RIGHT),
            rect.topCenter.asScalingHandle(Edge.TOP)
        )
    }

    var lastDragEventType by remember { mutableStateOf<CropDragEvent>(CropDragEvent.DraggedOutside) }
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .onSizeChanged {
                size = it
                rect = Rect(Offset.Zero, size.toSize())
            }
            .pointerInput(Unit) {
                detectDragGestures (
                    onDragStart = { offset ->
                        lastDragEventType = CropDragEvent.DraggedOutside

                        if (!isPositionInRect(offset, rect, 40.dp.toPx())) {
                            return@detectDragGestures
                        }

                        lastDragEventType = CropDragEvent.DraggedInside

                        handles.forEachIndexed { idx, handle ->
                            val distanceBetweenPointerAndHandle = (handle.position - offset).getDistanceSquared()
                            val isNear = distanceBetweenPointerAndHandle < 50.dp.toPx() * 50.dp.toPx()

                            if (isNear) {
                                lastDragEventType = CropDragEvent.ResizeHandleDragged(idx)
                            }
                        }
                    },
                    onDrag = { pointerInputChange, dragChange ->
                        val evenType = lastDragEventType

                        when (evenType) {
                            CropDragEvent.DraggedInside -> {
                                if (rect.left + dragChange.x < 0 || rect.right + dragChange.x > size.width) {
                                    return@detectDragGestures
                                }

                                if (rect.top + dragChange.y < 0 || rect.bottom + dragChange.y > size.height) {
                                    return@detectDragGestures
                                }

                                rect = rect.translate(dragChange.x, dragChange.y)
                            }

                            is CropDragEvent.ResizeHandleDragged -> {
                                val currentlyDraggedHandle = handles[evenType.idx]
                                val edgeFlags = currentlyDraggedHandle.edgeFlags
                                rect = rect.applyResizeDrag(dragChange, size, edgeFlags, density)
                            }

                            CropDragEvent.DraggedOutside -> return@detectDragGestures
                        }


                        onRectChange(rect)
                        pointerInputChange.consume()
                })
            }
    ) {
        // Inner transparent fill
        drawRect(Color.Cyan, rect.topLeft, rect.size, alpha = 0.1f)

        // Border
        drawRect(Color.Cyan, rect.topLeft, rect.size, alpha = 0.5f, style = Stroke(width = 2.dp.toPx()))

        // Draw handles for resizing the rect
        for (handlePoint in handles.map { it.position }) {
            drawCircle(Color.Cyan, 5.dp.toPx(), handlePoint, style = Stroke(width = 2.dp.toPx()))
            drawCircle(Color.Cyan, 5.dp.toPx(), handlePoint, alpha = 0.4f)
        }
    }
}

enum class SlotsEnum { Main, Dependent }
@Composable
fun MatchLargestChildBoxWithTouchErrorMargin(
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
    dependentContent: @Composable () -> Unit
) {
    SubcomposeLayout(modifier) { constraints ->
        val mainPlaceables = subcompose(SlotsEnum.Main, mainContent).map { it.measure(constraints) }
        val maxSize =
            mainPlaceables.fold(IntSize.Zero) { currentMax, placeable ->
                IntSize(
                    width = maxOf(currentMax.width, placeable.width),
                    height = maxOf(currentMax.height, placeable.height),
                )
            }

        val dependentConstraints = Constraints.fixed(maxSize.width, maxSize.height)
        layout(maxSize.width, maxSize.height) {
            mainPlaceables.forEach { it.placeRelative(0, 0) }
            subcompose(SlotsEnum.Dependent) { dependentContent() }
                .forEach { it.measure(dependentConstraints).placeRelative(0, 0) }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun PreviewCropOverlay() {
    Timber.plant(Timber.DebugTree())

    Scaffold {
        ScanBridgeTheme {
            var currentRect by remember { mutableStateOf(Rect(0f,0f,0f,0f)) }
            MatchLargestChildBoxWithTouchErrorMargin(
                mainContent = {
                    Image(painter = painterResource(R.drawable.icon_about_dialog), contentDescription = "Hallo", modifier = Modifier.fillMaxWidth())
                }
            ) {
                CropOverlay(Modifier) {
                    currentRect = it
                }
            }
        }
    }



}
