package com.plcoding.cryptotracker.crypto.presentation.coin_detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.cryptotracker.crypto.domain.CoinPrice
import com.plcoding.cryptotracker.ui.theme.CryptoTrackerTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun LineChart(
    dataPoints: List<DataPoint>,
    style: ChartStyle,
    visibleDataPointsIndices: IntRange,
    unit: String,
    modifier: Modifier = Modifier,
    selectedDataPoint: DataPoint? = null,
    onSelectedDataPoint: (DataPoint) -> Unit = {},
    onXLabelWidthChange: (Float) -> Unit = {},
    showHelperLines: Boolean = true
) {
    val textStyle = LocalTextStyle.current.copy(
        fontSize = style.labelFontSize
    )

    val visibleDataPoints = remember(dataPoints, visibleDataPointsIndices) {
        dataPoints.slice(visibleDataPointsIndices)
    }

    val maxYValue = remember(visibleDataPoints) {
        visibleDataPoints.maxOfOrNull { it.y } ?: 0f
    }
    val minYValue = remember(visibleDataPoints) {
        visibleDataPoints.minOfOrNull { it.y } ?: 0f
    }

    val measurer = rememberTextMeasurer()

    var xLabelWidth by remember {
        mutableFloatStateOf(0f)
    }
    LaunchedEffect(key1 = xLabelWidth) {
        onXLabelWidthChange(xLabelWidth)
    }

    val selectedDataPointIndex = remember(selectedDataPoint) {
        dataPoints.indexOf(selectedDataPoint)
    }

    var drawPoints by remember {
        mutableStateOf(listOf<DataPoint>())
    }
    var isShowingDataPoints by remember {
        mutableStateOf(selectedDataPoint != null)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(drawPoints, xLabelWidth) {
                detectHorizontalDragGestures { change, _ ->
                    val newSelectedDataPointIndex = getSelectedDataPointIndex(
                        touchOffsetX = change.position.x,
                        triggerWidth = xLabelWidth,
                        drawPoints = drawPoints
                    )
                    // Check to see if the gesture was within the visibleDataPoints
                    isShowingDataPoints =
                        (newSelectedDataPointIndex + visibleDataPointsIndices.first) in
                                visibleDataPointsIndices
                    // If so then update onSelectedDataPoint.
                    if (isShowingDataPoints) {
                        onSelectedDataPoint(dataPoints[newSelectedDataPointIndex])
                    }
                }
            }
    ) {
        val minLabelSpacingYPx = style.minYLabelSpacing.toPx()
        val verticalPaddingPx = style.verticalPadding.toPx()
        val horizontalPaddingPx = style.horizontalPadding.toPx()
        val xAxisLabelSpacingPx = style.xAxisLabelSpacing.toPx()

        // Loops over all of the xLabels in order to measure the size of
        // each as they would appear with the applied textStyle.
        val xLabelTextLayoutResults = visibleDataPoints.map {
            measurer.measure(
                text = it.xLabel,
                style = textStyle.copy(textAlign = TextAlign.Center)
            )
        }
        val maxXLabelWidth = xLabelTextLayoutResults.maxOfOrNull { it.size.width } ?: 0
        val maxXLabelHeight = xLabelTextLayoutResults.maxOfOrNull { it.size.height } ?: 0
        val maxXLabelLineCount = xLabelTextLayoutResults.maxOfOrNull { it.lineCount } ?: 0
        // EXAMPLE: if the largest label is 99px high at 3 lines... 99 / 3 = 33.
        // It appears that TextMeasurer's TextLayoutResult does not include a .lineHeight
        // for getting a single line height.
        val labelLineHeight = if(maxXLabelLineCount > 0) {
            maxXLabelHeight / maxXLabelLineCount
        } else 0

        // The viewPort is the chart itself within the Canvas. Because the labels and
        // padding are included inside the Canvas but are not part of the viewport,
        // we need to subtract those dimensions to make room for them. size.height is
        // the total Canvas height.
        val viewPortHeightPx = size.height -
                (maxXLabelHeight + 2 * verticalPaddingPx
                        // For the top label that shows the price of the selected DataPoint.
                        + labelLineHeight
                        + xAxisLabelSpacingPx)

        // Y-LABEL CALCULATION
        val labelViewPortHeightPx = viewPortHeightPx + labelLineHeight
        // Using toInt() to strip decimal and also exclude the last label.
        val labelCountExcludingLastLabel = ((labelViewPortHeightPx / (labelLineHeight + minLabelSpacingYPx))).toInt()

        // EXAMPLE: (2,659 - 2,583) / 2 = 36
        val valueIncrement = (maxYValue - minYValue) / labelCountExcludingLastLabel

        val yLabels = (0..labelCountExcludingLastLabel).map {
            ValueLabel(
                value = maxYValue - (valueIncrement * it),
                unit = unit
            )
        }

        val yLabelTextLayoutResults = yLabels.map {
            measurer.measure(
                text = it.formatted(),
                style = textStyle
            )
        }
        // Get the width of the widest Y label
        val maxYLabelWidth = yLabelTextLayoutResults.maxOfOrNull { it.size.width } ?: 0

        // DRAW THE VIEWPORT
        val viewPortTopY = verticalPaddingPx + labelLineHeight + 10f
        val viewPortRightX = size.width
        val viewPortBottomY = viewPortTopY + viewPortHeightPx
        val viewPortLeftX = 2f * horizontalPaddingPx + maxYLabelWidth

        // DRAW THE X-LABELS
        xLabelWidth = maxXLabelWidth + xAxisLabelSpacingPx
        xLabelTextLayoutResults.forEachIndexed { index, result ->
            val x = viewPortLeftX + xAxisLabelSpacingPx / 2f +
                    xLabelWidth * index
            drawText(
                textLayoutResult = result,
                topLeft = Offset(
                    x = x,
                    y = viewPortBottomY + xAxisLabelSpacingPx
                ),
                color = if(index == selectedDataPointIndex) {
                    style.selectedColor
                } else {
                    style.unselectedColor
                }
            )

            // VERTICAL HELPERLINES
            if (showHelperLines) {
                drawLine(
                    color = if (selectedDataPointIndex == index) {
                        style.selectedColor
                    } else style.unselectedColor,
                    start = Offset(
                        x = x + result.size.width / 2f,
                        y = viewPortBottomY
                    ),
                    end = Offset(
                        x = x + result.size.width / 2f,
                        y = viewPortTopY
                    ),
                    strokeWidth = if (selectedDataPointIndex == index) {
                        style.helperLinesThicknessPx * 1.8f
                    } else style.helperLinesThicknessPx
                )
            }

            // DRAW TOP PRICE OF SELECTED DATAPOINT
            if (selectedDataPointIndex == index) {
                val valueLabel = ValueLabel(
                    value = visibleDataPoints[index].y,
                    unit = unit
                )
                val valueResult = measurer.measure(
                    text = valueLabel.formatted(),
                    style = textStyle.copy(
                        color = style.selectedColor
                    ),
                    maxLines = 1
                )

                // If the selectedDataPoint is the last on the chart then left align
                // else center it.
                val textPositionX = if (selectedDataPointIndex == visibleDataPointsIndices.last) {
                    x - valueResult.size.width
                } else {
                    x - valueResult.size.width / 2f
                // x is the left of the xLabel that is associated with the selectedDataPoint
                // so we add half of that xLabel's width.
                } + result.size.width / 2f

                val isTextInVisibleRange =
                    (size.width - textPositionX).roundToInt() in 0..size.width.roundToInt()
                if (isTextInVisibleRange) {
                    drawText(
                        textLayoutResult = valueResult,
                        topLeft = Offset(
                            x = textPositionX,
                            y = viewPortTopY - valueResult.size.height - 10f
                        )
                    )
                }
            }
        }

        val heightRequiredForLabels = labelLineHeight *
                (labelCountExcludingLastLabel + 1)
        val remainingHeightForLabels = labelViewPortHeightPx - heightRequiredForLabels
        val spaceBetweenLabels = remainingHeightForLabels / labelCountExcludingLastLabel

        // DRAW THE Y-LABELS
        yLabelTextLayoutResults.forEachIndexed { index, result ->
            // Right align the labels by getting the difference between largest and
            // the current Y label. Then add the horizontal padding.
            val x = maxYLabelWidth - result.size.width.toFloat() + horizontalPaddingPx
            val y = viewPortTopY +
                    // plus the height of the previous label and its spacing
                    index * (labelLineHeight + spaceBetweenLabels) -
                    // minus half of a line height
                    labelLineHeight / 2f
            drawText(
                textLayoutResult = result,
                topLeft = Offset(
                    x = x,
                    y = y
                ),
                color = style.unselectedColor
            )

            // HORIZONTAL HELPERLINES
            if (showHelperLines) {
                drawLine(
                    color = style.unselectedColor,
                    start = Offset(
                        x = viewPortLeftX,
                        y = y + result.size.height.toFloat() / 2f
                    ),
                    end = Offset(
                        x = viewPortRightX,
                        y = y + result.size.height.toFloat() / 2f
                    ),
                    strokeWidth = style.helperLinesThicknessPx
                )
            }
        }

        drawPoints = visibleDataPointsIndices.map {
            val x = viewPortLeftX + // left edge of the viewPort (the graph)
                    // In case the visibleDataPoints don't start on 0 we subtract
                    // .first to offset the current index. Then multiply this
                    // offset index by 1 xLabelWidth.
                    (it - visibleDataPointsIndices.first) * xLabelWidth +
                    xLabelWidth / 2f // plust half the xLabelWidth to center

            // Interval mapping to find the exact vertical point we need to draw
            // the dataPoint.
            // [minYValue: maxYValue] -> [0: 1]
            // EXAMPLE: If min is $0, max is $1000 or [0: 1000] and dataPoint
            // is for $500 then
            // (500 - 0 = 500) / (1000 - 0 = 1000)
            // 500 / 1000 = 0.5 (exactly 50% of the viewPort)
            val ratio = (dataPoints[it].y - minYValue) / (maxYValue - minYValue)
            val y = viewPortBottomY - (ratio * viewPortHeightPx)
            DataPoint(
                x = x,
                y = y,
                xLabel = dataPoints[it].xLabel
            )
        }

        // Find the connection points between the dataPoints to be used
        // in drawing the bezier curves.
        val conPoints1 = mutableListOf<DataPoint>()
        val conPoints2 = mutableListOf<DataPoint>()
        for (i in 1 until drawPoints.size) {
            val p0 = drawPoints[i - 1]
            val p1 = drawPoints[i]

            // The x position between 2 drawPoint coordinates
            val x = (p1.x + p0.x) / 2f
            val y1 = p0.y
            val y2 = p1.y

            conPoints1.add(DataPoint(x, y1, ""))
            conPoints2.add(DataPoint(x, y2, ""))
        }

        val linePath = Path().apply {
            if (drawPoints.isNotEmpty()) {
                moveTo(drawPoints.first().x, drawPoints.first().y)

                // Feature idea: allow user to toggle bezier curves.
                // This hardcoded variable would need to propagated up.
                val hasBezierCurves = true
                for (i in 1 until drawPoints.size) {
                    if (hasBezierCurves) {
                        // Feature idea: implement true bezier curves
                        cubicTo(
                            x1 = conPoints1[i - 1].x,
                            y1 = conPoints1[i - 1].y,
                            x2 = conPoints2[i - 1].x,
                            y2 = conPoints2[i - 1].y,
                            x3 = drawPoints[i].x,
                            y3 = drawPoints[i].y
                        )
                    } else {
                        lineTo(
                            x = drawPoints[i].x,
                            y = drawPoints[i].y
                        )
                    }
                }
            }
        }
        drawPath(
            path = linePath,
            color = style.chartLineColor,
            style = Stroke(
                width = 5f,
                cap = StrokeCap.Round
            )
        )

        drawPoints.forEachIndexed { index, point ->
            if(isShowingDataPoints) {
                val circleOffset = Offset(
                    x = point.x,
                    y = point.y
                )
                // All drawPoints
                drawCircle(
                    color = style.selectedColor,
                    radius = 10f,
                    center = circleOffset
                )
                // Selected drawPoint
                if (selectedDataPointIndex == index) {
                    drawCircle(
                        color = Color.White,
                        radius = 15f,
                        center = circleOffset
                    )
                    // Outline
                    drawCircle(
                        color = style.selectedColor,
                        radius = 15f,
                        center = circleOffset,
                        style = Stroke(
                            width = 3f
                        )
                    )
                }
            }
        }
    }
}

private fun getSelectedDataPointIndex(
    touchOffsetX: Float,
    triggerWidth: Float,
    drawPoints: List<DataPoint>
): Int {
    val triggerRangeLeft = touchOffsetX - triggerWidth / 2f
    val triggerRangeRight = touchOffsetX + triggerWidth / 2f
    // returns the drawPoint within that triggerRange
    return drawPoints.indexOfFirst {
        it.x in triggerRangeLeft..triggerRangeRight
    }
}

@Preview(widthDp = 1200)
@Composable
private fun LineChartPreview() {
    CryptoTrackerTheme {
        val coinHistoryRandomized = remember {
            (1..20).map {
                CoinPrice(
                    priceUsd = Random.nextFloat() * 1000.0,
                    dateTime = ZonedDateTime.now().plusHours(it.toLong())
                )
            }
        }
        val style = ChartStyle(
            chartLineColor = Color.Black,
            unselectedColor = Color(0xFF7C7C7C),
            selectedColor = Color.Black,
            helperLinesThicknessPx = 1f,
            axisLinesThicknessPx = 5f,
            labelFontSize = 14.sp,
            minYLabelSpacing = 25.dp,
            verticalPadding = 8.dp,
            horizontalPadding = 8.dp,
            xAxisLabelSpacing = 8.dp
        )

        val dataPoints = remember {
            coinHistoryRandomized.map {
                DataPoint(
                    x = it.dateTime.hour.toFloat(),
                    y = it.priceUsd.toFloat(),
                    xLabel = DateTimeFormatter
                        .ofPattern("ha\nM/d")
                        .format(it.dateTime)
                )
            }
        }

        LineChart(
            dataPoints = dataPoints,
            style = style,
            visibleDataPointsIndices = 0..19,
            unit = "$",
            modifier = Modifier
                .width(700.dp)
                .height(300.dp)
                .background(Color.White),
            selectedDataPoint = dataPoints[1]
        )
    }
}