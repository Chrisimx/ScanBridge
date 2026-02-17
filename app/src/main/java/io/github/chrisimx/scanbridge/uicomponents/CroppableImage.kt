package io.github.chrisimx.scanbridge.uicomponents

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.compose.AsyncImage
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import kotlin.math.max
import kotlin.math.min
import timber.log.Timber

enum class Edge { TOP, LEFT, BOTTOM, RIGHT }

data class EdgeFlags(val top: Boolean = false, val left: Boolean = false, val bottom: Boolean = false, val right: Boolean = false)

data class HandleSpec(val position: Offset, val draggedEdges: Set<Edge>) {
    val edgeFlags: EdgeFlags
        get() = EdgeFlags(
            top = Edge.TOP in draggedEdges,
            left = Edge.LEFT in draggedEdges,
            bottom = Edge.BOTTOM in draggedEdges,
            right = Edge.RIGHT in draggedEdges
        )
}

fun Rect.deflate(x: Float, y: Float): Rect = copy(
    left = min(left + x, left + width / 2),
    right = max(right - x, right - width / 2),
    top = min(top + y, top + height / 2),
    bottom = max(bottom - y, bottom - height / 2)
)

/**
 * Create a HandleSpec based on the handle position and edges that should be moved when the handle is dragged
 */
fun Offset.asScalingHandle(vararg edge: Edge) = HandleSpec(
    position = this,
    draggedEdges = edge.toSet()
)

fun Rect.applyResizeDrag(drag: Offset, size: IntSize, flags: EdgeFlags, density: Density, clearance: Dp = 30.dp): Rect = with(density) {
    copy(
        left = min(max(left + if (flags.left) drag.x else 0f, 0f), size.width - clearance.toPx()),
        right = min(max(right + if (flags.right) drag.x else 0f, left + clearance.toPx()), size.width.toFloat()),
        top = min(max(top + if (flags.top) drag.y else 0f, 0f), size.height - clearance.toPx()),
        bottom = min(max(bottom + if (flags.bottom) drag.y else 0f, top + clearance.toPx()), size.height.toFloat())
    )
}

sealed class CropDragEvent {
    data class ResizeHandleDragged(val idx: Int) : CropDragEvent()
    object DraggedOutside : CropDragEvent()
    object DraggedInside : CropDragEvent()
}

@Composable
fun CropOverlay(modifier: Modifier, touchPaddingAroundInPx: Int, handleTouchRadius: Dp, onRectChange: (Rect) -> Unit = {}) {
    var rect by remember { mutableStateOf(Rect(0f, 0f, 50f, 50f)) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val relativeRect by remember {
        derivedStateOf {
            Rect(
                rect.left / size.width,
                rect.top / size.height,
                rect.right / size.width,
                rect.bottom / size.height
            )
        }
    }

    val handles by remember {
        derivedStateOf {
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
    }

    var lastDragEventType by remember { mutableStateOf<CropDragEvent>(CropDragEvent.DraggedOutside) }
    val density = LocalDensity.current

    val touchErrorClearance = with(density) {
        touchPaddingAroundInPx.toDp()
    }

    Canvas(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val offset = offset - Offset(touchPaddingAroundInPx.toFloat(), touchPaddingAroundInPx.toFloat())
                        lastDragEventType = CropDragEvent.DraggedOutside

                        if (!rect.inflate(handleTouchRadius.toPx()).contains(offset)) {
                            return@detectDragGestures
                        }

                        lastDragEventType = CropDragEvent.DraggedInside

                        if (rect.deflate(rect.width / 4, rect.height / 4).contains(offset)) {
                            return@detectDragGestures
                        }

                        val nearestHandle = handles
                            .mapIndexed { idx, handle ->
                                Pair(
                                    idx,
                                    (handle.position - offset).getDistance()
                                )
                            }
                            .filter { it.second < handleTouchRadius.toPx() }
                            .minByOrNull { it.second }

                        if (nearestHandle != null) {
                            lastDragEventType = CropDragEvent.ResizeHandleDragged(nearestHandle.first)
                        }
                    },
                    onDrag = { pointerInputChange, dragChange ->
                        val evenType = lastDragEventType

                        when (evenType) {
                            CropDragEvent.DraggedInside -> {
                                val possibleXChange = if (rect.left + dragChange.x < 0) {
                                    -rect.left
                                } else if (rect.right + dragChange.x > size.width) {
                                    size.width - rect.right
                                } else {
                                    dragChange.x
                                }

                                val possibleYChange = if (rect.top + dragChange.y < 0) {
                                    -rect.top
                                } else if (rect.bottom + dragChange.y > size.height) {
                                    size.height - rect.bottom
                                } else {
                                    dragChange.y
                                }

                                rect = rect.translate(possibleXChange, possibleYChange)
                            }

                            is CropDragEvent.ResizeHandleDragged -> {
                                val currentlyDraggedHandle = handles[evenType.idx]
                                val edgeFlags = currentlyDraggedHandle.edgeFlags
                                rect = rect.applyResizeDrag(dragChange, size, edgeFlags, density)
                            }

                            CropDragEvent.DraggedOutside -> return@detectDragGestures
                        }

                        onRectChange(relativeRect)
                        pointerInputChange.consume()
                    }
                )
            }.padding(touchErrorClearance)
            .onSizeChanged {
                size = it
                rect = Rect(Offset.Zero, size.toSize())
            }.then(modifier)
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
    dependantAdditionalSpaceInPx: Int,
    mainContent: @Composable () -> Unit,
    dependentContent: @Composable () -> Unit
) {
    SubcomposeLayout(modifier) { constraints ->
        val mainPlaceables = subcompose(SlotsEnum.Main, mainContent).map { it.measure(constraints) }

        val maxSize =
            mainPlaceables.fold(IntSize.Zero) { currentMax, placeable ->
                IntSize(
                    width = maxOf(currentMax.width, placeable.width),
                    height = maxOf(currentMax.height, placeable.height)
                )
            }

        val dependentConstraints = Constraints.fixed(
            maxSize.width + 2 * dependantAdditionalSpaceInPx,
            maxSize.height + 2 * dependantAdditionalSpaceInPx
        )
        layout(maxSize.width, maxSize.height) {
            mainPlaceables.forEach { it.placeRelative(0, 0) }
            subcompose(SlotsEnum.Dependent, dependentContent)
                .forEach { it.measure(dependentConstraints).placeRelative(-dependantAdditionalSpaceInPx, -dependantAdditionalSpaceInPx) }
        }
    }
}

@Composable
fun CroppableAsyncImage(
    modifier: Modifier = Modifier,
    imageModel: Any?,
    contentDescription: String?,
    additionalTouchAreaAround: Dp,
    handleTouchRadius: Dp,
    cropRectChanged: (Rect) -> Unit
) {
    val density = LocalDensity.current
    val additionalTouchAreaAroundInPx = with(density) {
        additionalTouchAreaAround.toPx().toInt()
    }

    MatchLargestChildBoxWithTouchErrorMargin(
        modifier = modifier,
        dependantAdditionalSpaceInPx = additionalTouchAreaAroundInPx,
        mainContent = {
            AsyncImage(
                model = imageModel,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        CropOverlay(
            modifier = Modifier,
            additionalTouchAreaAroundInPx,
            handleTouchRadius
        ) {
            cropRectChanged(it)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun PreviewCropOverlay() {
    Timber.plant(Timber.DebugTree())

    Scaffold { innerPadding ->
        ScanBridgeTheme {
            val density = LocalDensity.current
            var currentRect by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
            MatchLargestChildBoxWithTouchErrorMargin(
                modifier = Modifier.padding(innerPadding).padding(top = 200.dp, start = 40.dp, end = 40.dp),
                dependantAdditionalSpaceInPx = 200,
                mainContent = {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.icon_about_dialog),
                        contentDescription = "Hallo"
                    )
                }
            ) {
                CropOverlay(modifier = Modifier, 200, 50.dp) {
                    currentRect = it
                }
            }
        }
    }
}
