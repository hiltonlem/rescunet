package com.ibm.rescunet

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.support.constraint.ConstraintLayout
import android.support.v4.graphics.ColorUtils
import android.util.AttributeSet
import android.view.View
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.DataPointInterface
import com.jjoe64.graphview.series.PointsGraphSeries
import kotlinx.android.synthetic.main.view_device_plot.view.*
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class DevicePlotView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var points = listOf<PlottedPoint>()
    private var clusters = listOf<PlottedCluster>()
    var devices = listOf<DeviceInfo>()
    private var isFrozen = false
        set(value) {
            field = value
            button_zoom_in.isEnabled = !value && zoomLevel < 8
            button_zoom_out.isEnabled = !value && zoomLevel > 1
        }
    private var zoomLevel = 1
    var heading: Float = 0f
    set(value) {compass.heading = value; field = value}

    interface DeviceUpdateListener {
        fun onDeviceNameUpdate(list: List<DeviceInfo>)
        fun onMyGPSUpdate(lat: Int, long: Int)
    }

    var updateListener: DeviceUpdateListener? = null

    private class PlottedPoint(val x: Double, val y: Double, val color: Int, val device: DeviceInfo? = null) {
//        infix fun distTo(other: PlottedPoint) =
//            Math.sqrt((other.x - this.x) * (other.x - this.x) + (other.y - this.y) * (other.y - this.y))
//
//        infix fun distTo(other: PlottedCluster) =
//            Math.sqrt((other.x - this.x) * (other.x - this.x) + (other.y - this.y) * (other.y - this.y))
    }

    private class PlottedCluster(val points: MutableList<PlottedPoint>) : DataPointInterface {
        constructor(vararg points: PlottedPoint) : this(points.toMutableList())

        override fun toString(): String {
            return "$x, $y"
        }

        override fun getX() = points.map { it.x }.average()
        override fun getY() = points.map { it.y }.average()
        val color: Int
            get() {
                return when {
                    points.size == 1 -> points[0].color
                    else -> Color.rgb(51, 51, 255)
                }
            }
        val count: Int get() = points.size
        var focus: Boolean = false
//        infix fun distTo(other: PlottedPoint) =
//            Math.sqrt((other.x - this.x) * (other.x - this.x) + (other.y - this.y) * (other.y - this.y))

        infix fun distTo(other: PlottedCluster) =
            Math.sqrt((other.x - this.x) * (other.x - this.x) + (other.y - this.y) * (other.y - this.y))

        fun mergeWith(other: PlottedCluster) {
            points.addAll(other.points)
        }
    }

    /**
     * Helper function for clustering, only does one pass
     */
    private fun getClustersOnce(points: List<PlottedCluster>, minDistance: Double): List<PlottedCluster> {
        val list = points.toMutableList()
        var index = 0
        while (index < list.size - 1) {
            for (i in index + 1 until list.size) {
                if (list[index] distTo list[i] < minDistance) {
                    list[index].mergeWith(list[i])
                    list.removeAt(i)
                    --index
                    break
                }
                if (list[i].x - list[index].x >= minDistance) break
            }
            ++index
        }
        return list
    }

    /**
     * Clusters points according to the specified minimum distance between points
     */
    private fun getClusters(points: List<PlottedPoint>, minDistance: Double): List<PlottedCluster> {
        var prevList =
            points.sortedWith(compareBy<PlottedPoint> { it.x }.thenBy { it.y }).map { PlottedCluster(it) }
        while (true) {
            val currList = getClustersOnce(prevList, minDistance)
            if (prevList.size == currList.size)
                return prevList.sortedBy { it.x }
            prevList = currList
        }
    }

    private val customRenderer = PointsGraphSeries.CustomShape { canvas, paint, x, y, point ->
        if (point is PlottedCluster) {
            paint.isAntiAlias = true
            if (point.focus) {
                paint.color = point.color
                canvas.drawCircle(x, y, if (point.count < 2) 21f else 28f, paint)
                paint.color = Color.parseColor("#323232")
                canvas.drawCircle(x, y, if (point.count < 2) 18f else 24f, paint)
            }
            paint.color = point.color
            canvas.drawCircle(x, y, if (point.count < 2) 15f else 20f, paint)
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 24f
            paint.typeface = Typeface.DEFAULT_BOLD
            if (point.count > 1)
                canvas.drawText(
                    point.count.toString(),
                    x,
                    y - paint.ascent() + (paint.ascent() - paint.descent()) / 2,
                    paint
                )
        }
    }

    private fun renderClusters(isZoom: Boolean) {
        val arr = this.clusters.toTypedArray()
        val series = PointsGraphSeries(arr).apply {
            setCustomShape(customRenderer)
            setOnDataPointTapListener { _, cluster ->
                handler.post {
                    this@DevicePlotView.clusters.forEach { it.focus = false }
                    (cluster as PlottedCluster).focus = true
                    updateListener?.onDeviceNameUpdate(cluster.points.mapNotNull { it.device }.sortedBy { it.id })
                    renderClusters(false)
                    isFrozen = true
                }
            }
        }
        val corners = PointsGraphSeries(
            arrayOf(
                PlottedCluster(PlottedPoint(-5.0, -5.0, Color.TRANSPARENT)),
                PlottedCluster(PlottedPoint(-5.0, 5.0, Color.TRANSPARENT)),
                PlottedCluster(PlottedPoint(5.0, 5.0, Color.TRANSPARENT)),
                PlottedCluster(PlottedPoint(5.0, -5.0, Color.TRANSPARENT))
            )
        ).apply {
            setCustomShape(customRenderer)
        }

        val radar = PointsGraphSeries(arrayOf(DataPoint(0.0, 0.0))).apply {
            setCustomShape { canvas, paint, x, y, dataPoint ->
                val radarColors = listOf(
                    Color.parseColor("#9c27b0"),
                    Color.parseColor("#4caf50"),
                    Color.parseColor("#f44336"),
                    Color.parseColor("#03a9f4")
                )
                val SIZE = 115
                val density = context.resources.displayMetrics.density
                paint.color = Color.argb(17, 0, 51, 255)
                paint.strokeWidth = context.resources.displayMetrics.density * 2

                var scale = 1
                for (i in 0..3) {
                    paint.style = Paint.Style.STROKE
                    paint.color = ColorUtils.setAlphaComponent(radarColors[i], 50)
                    canvas.drawCircle(x, y, density * SIZE / scale * zoomLevel, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = ColorUtils.setAlphaComponent(radarColors[i], 30)
                    canvas.drawCircle(x, y, density * SIZE / scale * zoomLevel, paint)
                    scale *= 2
                }
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(205, 153, 153, 255)
                paint.textSize = 30f
                canvas.drawText("${400 / zoomLevel}m", x + 2, y / 5, paint)
            }
        }

        handler.post {
            if (isZoom) {
                graph_radar.apply {
                    removeAllSeries()
                    addSeries(radar)
                }
            }
            graph.apply {
                removeAllSeries()
                addSeries(series)
                addSeries(corners)
                // addSeries(compass)
            }
        }
    }

    /**
     * Get a list of plotted points from the device info. At default zoom one grid space is within 1 percent of 100 meters
     */
    private fun getPointsFromDeviceInfo(): List<PlottedPoint> {
        try {
            devices.let { devices ->
                return if (devices.isNotEmpty() && devices[0].id == 0) devices.filter { it.id > 0 }.sortedBy { it.id }.map {
                    PlottedPoint(
                        (it.long.toDouble() - devices[0].long.toDouble()) / 100 * 1.11 * zoomLevel * cos(devices[0].lat.toDouble() / 36000000 * 2 * PI),
                        (it.lat.toDouble() -
                                devices[0].lat.toDouble()) / 100 * 1.11 * zoomLevel,
                        Color.rgb(45 + it.id % 16 * 14, 45 + it.id / 16 % 16 * 14, 45 + it.id / 16 / 16 * 14),
                        it
                    )
                }.filter { it.x < 5 && it.y < 5 && it.x > -5 && it.y > -5 }
                else listOf(PlottedPoint(0.0, 0.0, 0, null))
            }
        } catch (e: IndexOutOfBoundsException) {
            return listOf(PlottedPoint(0.0, 0.0, 0, null))
        }
    }

    /**
     * Renders the given PlottedPoints after clustering
     */
    fun renderPoints(isZoom: Boolean = false) {
        if (isFrozen) return
        Thread {
            getPointsFromDeviceInfo().apply upper@{
                this@DevicePlotView.points = this
                clusters = getClusters(this, 0.6)
                handler.post {
                    renderClusters(isZoom)
                    updateListener?.onDeviceNameUpdate(this@upper.mapNotNull { it.device }.sortedBy { it.id })
                }
            }

        }.start()
        if (devices.isNotEmpty())
            updateListener?.onMyGPSUpdate(devices[0].lat, devices[0].long)
    }

    fun undoClick() {
        isFrozen = false
        renderPoints()
    }

    init {
        View.inflate(context, R.layout.view_device_plot, this)

        graph.gridLabelRenderer.apply {
            isHorizontalLabelsVisible = false
            isVerticalLabelsVisible = false
            gridStyle = GridLabelRenderer.GridStyle.BOTH
            numHorizontalLabels = 11
            numVerticalLabels = 11
            gridColor = Color.argb(33, 255, 255, 255)
        }

        graph.viewport.apply {
            isXAxisBoundsManual = true
            isYAxisBoundsManual = true
            setMinX(-5.0)
            setMaxX(5.0)
            setMinY(-5.0)
            setMaxY(5.0)
        }

        graph_radar.gridLabelRenderer.apply {
            isHorizontalLabelsVisible = false
            isVerticalLabelsVisible = false
            gridStyle = GridLabelRenderer.GridStyle.NONE
            numHorizontalLabels = 11
            numVerticalLabels = 11
        }

        graph_radar.viewport.apply {
            isXAxisBoundsManual = true
            isYAxisBoundsManual = true
            setMinX(-5.0)
            setMaxX(5.0)
            setMinY(-5.0)
            setMaxY(5.0)
        }
        button.setOnClickListener {
            devices = listOf(
                DeviceInfo(0, 45_34053, -75_69044, 0),
                *Array(100) {
                    DeviceInfo(
                        Random.nextInt(1, 4096),
                        Random.nextInt(45_33652, 45_34454),
                        Random.nextInt(-75_69445, -75_68643),
                        Date().time
                    )
                })
//            DeviceInfo(Random.nextInt(1, 4096), 45_34270, -75_69056, 0),
//            DeviceInfo(Random.nextInt(1, 4096), 45_34098, -75_68934, 0),
//            DeviceInfo(Random.nextInt(1, 4096), 45_34055, -75_68732, 0))
//            devices.addAll(List(100) {
//                DeviceInfo(it + 1, Random.nextInt(10000), Random.nextInt(10000), Date().time)
//            })
            renderPoints()
            Thread {
                for (i in 0..300) {
                    handler.post {
                        devices[0].history[0] = devices[0].history[0].copy(
                            first = devices[0].history[0].first + 2,
                            second = devices[0].history[0].second + 1
                        )
                        renderPoints()
                    }
                    Thread.sleep(10)
                }
            }.start()
        }

        button_zoom_in.setOnClickListener {
            if (zoomLevel < 8) {
                zoomLevel *= 2
                button_zoom_out.isEnabled = true
            }
            if (zoomLevel == 8) button_zoom_in.isEnabled = false
            renderPoints(true)
        }
        button_zoom_out.apply { isEnabled = false }.setOnClickListener {
            if (zoomLevel > 1) {
                zoomLevel /= 2
                button_zoom_in.isEnabled = true
            }
            if (zoomLevel == 1) button_zoom_out.isEnabled = false
            renderPoints(true)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        renderClusters(true)
    }
}