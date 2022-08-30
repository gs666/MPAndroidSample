package com.rickon.mpandroidsample

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BubbleData
import com.github.mikephil.charting.data.BubbleDataSet
import com.github.mikephil.charting.data.BubbleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter

import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBubbleDataSet
import com.github.mikephil.charting.renderer.HeatMapChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.rickon.mpandroidsample.databinding.LayoutHeatBubbleChartBinding
import java.text.DecimalFormat
import java.util.ArrayList
import kotlin.math.roundToInt

/**
 * @Description: 评分分析-热力图封装
 * @Author:      Rickon
 * @CreateDate:  2021/11/11 10:02 上午
 * @Email:       gaoshuo521@foxmail.com
 */
class HeatBubbleChartView : ConstraintLayout {

    private val viewBinding: LayoutHeatBubbleChartBinding = LayoutHeatBubbleChartBinding
        .inflate(LayoutInflater.from(context), this, true)
    private var mContext: Context = context

    //车速、档位配合-0，转速、档位配合-1
    private var dataType = 0

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context)
    }

    private fun initView(context: Context) {
        configBarChart(context)
    }

    private fun configBarChart(context: Context) {
        with(viewBinding.bubbleChart) {
            this.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white))
            this.setNoDataText("无数据")
            this.setNoDataTextColor(ContextCompat.getColor(mContext, R.color.black))

            // no description text
            this.description.isEnabled = false

            //是否支持触摸
            this.setTouchEnabled(true)

            //是否支持拖拽
            this.isDragEnabled = true

            //放大缩小
            this.setScaleEnabled(false)

            //是否支持x,y轴同时缩放
            this.setPinchZoom(false)

            //是否在绘图区域绘制背景矩形
            this.setDrawGridBackground(false)

            //避免X轴文本被切割，底部增加5dp偏移空间
            this.extraBottomOffset = 5f

            val x: XAxis = this.xAxis
            x.isEnabled = true
            //是否显示X轴线
            x.setDrawAxisLine(true)
            //X轴线颜色
            x.axisLineColor = ContextCompat.getColor(context, R.color.color_1a393c43)
            //X轴线宽度
            x.axisLineWidth = 1f
            x.setAvoidFirstLastClipping(true)
            x.axisMinimum = 0f
            x.textColor = ContextCompat.getColor(context, R.color.black_hint)
            x.textSize = 9f
            x.setDrawLabels(true)
            x.position = XAxis.XAxisPosition.BOTTOM
            x.setDrawGridLines(false)
            x.setCenterAxisLabels(true)

            val y: YAxis = this.axisLeft
            //显示左边竖线
            y.setDrawAxisLine(true)
            y.axisLineColor = ContextCompat.getColor(context, R.color.color_1a393c43)
            y.axisLineWidth = 1f
            //是否绘制横向网格线
            y.setDrawGridLines(false)
            //网格线采用虚线
            y.enableGridDashedLine(8f, 8f, 0f)
            y.axisMinimum = 0f
            y.textColor = ContextCompat.getColor(context, R.color.color_B66)
            y.textSize = 9f
            //Y轴文字往上偏移
            y.yOffset = -6f

            this.axisRight.isEnabled = false

            //图例
            this.legend.isEnabled = false

            //渲染动画时间
            this.animateXY(350, 350)

            // don't forget to refresh the drawing
            this.invalidate()
        }

        //模拟数据
        val xLabelList = mutableListOf("0", "10", "20", "30", "40", "50", "60", "70", "80", "90")
        val yLabelList = mutableListOf("空挡", "1挡", "2挡", "3挡", "4挡", "5挡", "6挡", "7挡", "8挡", "9挡")
        val dataList: MutableList<List<Double>> = mutableListOf()
        for (i in xLabelList.indices) {
            for (j in yLabelList.indices) {
                dataList.add(listOf(i.toDouble(), j.toDouble(), (i + j).toDouble()))
            }
        }
        setChartData(0, xLabelList, yLabelList, dataList)
    }

    private fun setChartData(
        currentDataType: Int, xLabelList: MutableList<String>, yLabelList: MutableList<String>,
        dataList: List<List<Double>>
    ) {
        dataType = currentDataType

        val x: XAxis = viewBinding.bubbleChart.xAxis

        val heatMapChartRenderer = HeatMapChartRenderer(
            viewBinding.bubbleChart,
            viewBinding.bubbleChart.animator,
            viewBinding.bubbleChart.viewPortHandler,
            xLabelList.size,
            yLabelList.size
        )
        viewBinding.bubbleChart.renderer = heatMapChartRenderer

        if (xLabelList.isEmpty() || yLabelList.isEmpty() || dataList.isEmpty()) {
            //如果数据异常的话就不再执行
            return
        }
        //设置X格式
        x.setLabelCount(xLabelList.size, false)
        x.axisMaximum = xLabelList.size.toFloat()
        x.valueFormatter = CustomXValueFormatter(xLabelList)

        val y: YAxis = viewBinding.bubbleChart.axisLeft
        y.axisMaximum = yLabelList.size.toFloat()
        y.setLabelCount(yLabelList.size, false)
        y.valueFormatter = CustomYValueFormatter(yLabelList)


        val totalListOfList = mutableListOf<ArrayList<BubbleEntry>>()
        val totalListDataSet = mutableListOf<BubbleDataSet?>()

        val yAxisMax = yLabelList.size
        var dataMaxValue = 0f
        var dataMinValue = 0f

        for (i in 0 until yAxisMax) {
            val singleHoriEntryList = ArrayList<BubbleEntry>()
            with(singleHoriEntryList) {
                for (j in xLabelList.indices) {
                    val tempDataValue = dataList[i * xLabelList.size + j][2].toFloat()
                    this.add(BubbleEntry(j.toFloat(), i.toFloat(), tempDataValue))
                    //记录最大值和最小值，用于修改颜色
                    dataMaxValue = dataMaxValue.coerceAtLeast(tempDataValue)
                    dataMinValue = dataMinValue.coerceAtMost(tempDataValue)
                }
            }

            totalListOfList.add(singleHoriEntryList)

            val set1: BubbleDataSet? = null
            totalListDataSet.add(set1)
        }

        //传递给渲染器
        heatMapChartRenderer.setMaxValueMinValue(dataMaxValue, dataMinValue)

        if (viewBinding.bubbleChart.data != null && viewBinding.bubbleChart.data.dataSetCount > 0) {

            for (i in 0 until yAxisMax) {
                totalListDataSet[i] =
                    viewBinding.bubbleChart.data.getDataSetByIndex(i) as BubbleDataSet
                totalListDataSet[i]?.let {
                    it.entries = totalListOfList[i]
                }
            }

            viewBinding.bubbleChart.data.notifyDataChanged()
            viewBinding.bubbleChart.notifyDataSetChanged()
        } else {
            val dataSets = ArrayList<IBubbleDataSet>()

            for (i in 0 until yAxisMax) {
                totalListDataSet[i] = BubbleDataSet(totalListOfList[i], "heat")
                totalListDataSet[i]?.let {
                    //绘制图标
                    it.setDrawIcons(false)
                    //绘制数值
                    it.setDrawValues(true)
                    it.valueTextColor = Color.WHITE
                    it.valueTextSize = 11f
                    it.color = ContextCompat.getColor(mContext, R.color.purple_200)
                    //禁止选中/高亮
                    it.isHighlightEnabled = false
                    dataSets.add(it)
                }
            }

            val data = BubbleData(dataSets)
            data.setValueTextSize(10f)
            data.setValueFormatter(CurrentValueFormatter())
            viewBinding.bubbleChart.data = data
        }

        viewBinding.bubbleChart.animateX(350)

        viewBinding.bubbleChart.invalidate()
    }

    internal class CustomXValueFormatter(currentLabelList: MutableList<String>) : ValueFormatter() {
        private val xLabelList = currentLabelList
        override fun getFormattedValue(value: Float): String {
            val index = value.roundToInt()
            return if (index >= 0 && index < xLabelList.size) {
                xLabelList[index]
            } else {
                ""
            }
        }
    }

    internal class CustomYValueFormatter(currentLabelList: MutableList<String>) : ValueFormatter() {
        private val yLabelList = currentLabelList
        override fun getFormattedValue(value: Float): String {
            val intValue = value.roundToInt()
            return when {
                intValue == 0 -> {
                    "空挡"
                }
                intValue >= 0 && intValue < yLabelList.size -> {
                    "${intValue}挡"
                }
                else -> {
                    ""
                }
            }
        }
    }

    inner class CurrentValueFormatter internal constructor() : IValueFormatter {
        private val mFormat: DecimalFormat = DecimalFormat("0.0")
        override fun getFormattedValue(
            value: Float,
            entry: Entry,
            dataSetIndex: Int,
            viewPortHandler: ViewPortHandler
        ): String {
            return mFormat.format(value.toDouble())
        }
    }

}