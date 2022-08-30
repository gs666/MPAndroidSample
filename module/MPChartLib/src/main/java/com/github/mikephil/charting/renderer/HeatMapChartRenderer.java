package com.github.mikephil.charting.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.BubbleDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBubbleDataSet;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.List;

/**
 * Bubble chart implementation: Copyright 2015 Pierre-Marc Airoldi Licensed
 * under Apache License 2.0 Ported by Daniel Cohen Gindi
 * 方块热力图
 *
 * @author rickon
 */
public class HeatMapChartRenderer extends BarLineScatterCandleBubbleRenderer {

    protected BubbleDataProvider mChart;

    /**
     * Y轴最大值，用于计算方块高度
     */
    private int yMaxValue;
    /**
     * X轴最大值，用于计算方块宽度
     */
    private int xMaxValue;

    /**
     * 方块间隔(像素)
     */
    private final float squareIntervalSize = 1f;

    private float dataMinValue = 0;
    private float dataMaxValue = 0;

    /**
     * 绘制的方块宽度
     */
    private float rectangleWidth = 0;
    /**
     * 绘制的方块长度
     */
    private float rectangleHeight = 0;

    public HeatMapChartRenderer(BubbleDataProvider chart, ChartAnimator animator,
                                ViewPortHandler viewPortHandler, int xMax, int yMax) {
        super(animator, viewPortHandler);
        mChart = chart;

        xMaxValue = xMax;
        yMaxValue = yMax;

        mRenderPaint.setStyle(Style.FILL);

        mHighlightPaint.setStyle(Style.STROKE);
        mHighlightPaint.setStrokeWidth(Utils.convertDpToPixel(1.5f));
    }

    @Override
    public void initBuffers() {

    }

    @Override
    public void drawData(Canvas c) {

        BubbleData bubbleData = mChart.getBubbleData();

        for (IBubbleDataSet set : bubbleData.getDataSets()) {

            if (set.isVisible())
                drawDataSet(c, set);
        }
    }

    private float[] sizeBuffer = new float[4];
    private float[] pointBuffer = new float[2];

    protected float getShapeSize(float entrySize, float maxSize, float reference, boolean normalizeSize) {
        final float factor = normalizeSize ? ((maxSize == 0f) ? 1f : (float) Math.sqrt(entrySize / maxSize)) :
                entrySize;
        final float shapeSize = reference * factor;
        return shapeSize;
    }

    protected void drawDataSet(Canvas c, IBubbleDataSet dataSet) {

        if (dataSet.getEntryCount() < 1)
            return;

        xMaxValue = dataSet.getEntryCount();
        //方块宽度 = 图表宽度/X轴最大值
        rectangleWidth = (mViewPortHandler.getContentRect().width() / xMaxValue) - squareIntervalSize;
        rectangleHeight = (mViewPortHandler.getContentRect().height() / yMaxValue) - squareIntervalSize;

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        float phaseY = mAnimator.getPhaseY();

        mXBounds.set(mChart, dataSet);

        sizeBuffer[0] = 0f;
        sizeBuffer[2] = 1f;

        trans.pointValuesToPixel(sizeBuffer);

        boolean normalizeSize = dataSet.isNormalizeSizeEnabled();

        // calcualte the full width of 1 step on the x-axis
        final float maxBubbleWidth = Math.abs(sizeBuffer[2] - sizeBuffer[0]);
        final float maxBubbleHeight = Math.abs(mViewPortHandler.contentBottom() - mViewPortHandler.contentTop());
        final float referenceSize = Math.min(maxBubbleHeight, maxBubbleWidth);

        for (int j = mXBounds.min; j <= mXBounds.range + mXBounds.min; j++) {

            final BubbleEntry entry = dataSet.getEntryForIndex(j);

            pointBuffer[0] = entry.getX();
            pointBuffer[1] = (entry.getY()) * phaseY;
            trans.pointValuesToPixel(pointBuffer);

            float shapeHalf = getShapeSize(entry.getSize(), dataSet.getMaxSize(), referenceSize, normalizeSize) / 2f;

            if (!mViewPortHandler.isInBoundsTop(pointBuffer[1] + shapeHalf)
                    || !mViewPortHandler.isInBoundsBottom(pointBuffer[1] - shapeHalf))
                continue;

            if (!mViewPortHandler.isInBoundsLeft(pointBuffer[0] + shapeHalf))
                continue;

            if (!mViewPortHandler.isInBoundsRight(pointBuffer[0] - shapeHalf))
                break;

            //根据数值确定方格的颜色
            mRenderPaint.setColor(getValueBgColor(dataMaxValue, dataMinValue, entry.getSize()));

            if (entry.getSize() > 0) {
                c.drawRect(pointBuffer[0],
                        pointBuffer[1] - rectangleHeight,
                        pointBuffer[0] + rectangleWidth,
                        pointBuffer[1],
                        mRenderPaint);
            }
        }
    }

    @Override
    public void drawValues(Canvas c) {

        BubbleData bubbleData = mChart.getBubbleData();

        if (bubbleData == null)
            return;

        // if values are drawn
//        if (isDrawingValuesAllowed(mChart)) {

        final List<IBubbleDataSet> dataSets = bubbleData.getDataSets();

        float lineHeight = Utils.calcTextHeight(mValuePaint, "1");

        for (int i = 0; i < dataSets.size(); i++) {

            IBubbleDataSet dataSet = dataSets.get(i);

            if (!shouldDrawValues(dataSet) || dataSet.getEntryCount() < 1)
                continue;

            // apply the text-styling defined by the DataSet
            applyValueTextStyle(dataSet);

            final float phaseX = Math.max(0.f, Math.min(1.f, mAnimator.getPhaseX()));
            final float phaseY = mAnimator.getPhaseY();

            mXBounds.set(mChart, dataSet);

            final float[] positions = mChart.getTransformer(dataSet.getAxisDependency())
                    .generateTransformedValuesBubble(dataSet, phaseY, mXBounds.min, mXBounds.max);

            final float alpha = phaseX == 1 ? phaseY : phaseX;

            MPPointF iconsOffset = MPPointF.getInstance(dataSet.getIconsOffset());
            iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x);
            iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y);

            for (int j = 0; j < positions.length; j += 2) {

                int valueTextColor = dataSet.getValueTextColor(j / 2 + mXBounds.min);
                valueTextColor = Color.argb(Math.round(255.f * alpha), Color.red(valueTextColor),
                        Color.green(valueTextColor), Color.blue(valueTextColor));

                float x = positions[j];
                float y = positions[j + 1];

                if (!mViewPortHandler.isInBoundsRight(x))
                    break;

                if ((!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y)))
                    continue;

                BubbleEntry entry = dataSet.getEntryForIndex(j / 2 + mXBounds.min);

                //修改文字中心，X轴加上1/2宽度，Y轴减去1/2高度
                if (dataSet.isDrawValuesEnabled()) {
                    //只有size>0时才绘制value
                    if (entry.getSize() > 0) {
                        //避免X轴数据过多导致显示重叠，此处动态设置一下字体大小
                        if (dataSet.getEntryCount() > 20) {
                            mValuePaint.setTextSize(dataSet.getValueTextSize() * 0.33f);
                        } else if (dataSet.getEntryCount() > 16) {
                            mValuePaint.setTextSize(dataSet.getValueTextSize() * 0.66f);
                        } else if (dataSet.getEntryCount() > 12) {
                            mValuePaint.setTextSize(dataSet.getValueTextSize() * 0.8f);
                        }

                        //绘制数值
                        drawValue(c, dataSet.getValueFormatter(), entry.getSize(), entry, i, x + rectangleWidth / 2,
                                y + (0.5f * lineHeight) - rectangleHeight / 2, valueTextColor);
                    }
                }

                if (entry.getIcon() != null && dataSet.isDrawIconsEnabled()) {

                    Drawable icon = entry.getIcon();

                    Utils.drawImage(
                            c,
                            icon,
                            (int) (x + iconsOffset.x),
                            (int) (y + iconsOffset.y),
                            icon.getIntrinsicWidth(),
                            icon.getIntrinsicHeight());
                }
            }

            MPPointF.recycleInstance(iconsOffset);
        }
//        }
    }

    @Override
    public void drawExtras(Canvas c) {
    }

    private float[] _hsvBuffer = new float[3];

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {

        BubbleData bubbleData = mChart.getBubbleData();

        float phaseY = mAnimator.getPhaseY();

        for (Highlight high : indices) {

            IBubbleDataSet set = bubbleData.getDataSetByIndex(high.getDataSetIndex());

            if (set == null || !set.isHighlightEnabled())
                continue;

            final BubbleEntry entry = set.getEntryForXValue(high.getX(), high.getY());

            if (entry.getY() != high.getY())
                continue;

            if (!isInBoundsX(entry, set))
                continue;

            Transformer trans = mChart.getTransformer(set.getAxisDependency());

            sizeBuffer[0] = 0f;
            sizeBuffer[2] = 1f;

            trans.pointValuesToPixel(sizeBuffer);

            boolean normalizeSize = set.isNormalizeSizeEnabled();

            // calcualte the full width of 1 step on the x-axis
            final float maxBubbleWidth = Math.abs(sizeBuffer[2] - sizeBuffer[0]);
            final float maxBubbleHeight = Math.abs(
                    mViewPortHandler.contentBottom() - mViewPortHandler.contentTop());
            final float referenceSize = Math.min(maxBubbleHeight, maxBubbleWidth);

            pointBuffer[0] = entry.getX();
            pointBuffer[1] = (entry.getY()) * phaseY;
            trans.pointValuesToPixel(pointBuffer);

            high.setDraw(pointBuffer[0], pointBuffer[1]);

            float shapeHalf = getShapeSize(entry.getSize(),
                    set.getMaxSize(),
                    referenceSize,
                    normalizeSize) / 2f;

            if (!mViewPortHandler.isInBoundsTop(pointBuffer[1] + shapeHalf)
                    || !mViewPortHandler.isInBoundsBottom(pointBuffer[1] - shapeHalf))
                continue;

            if (!mViewPortHandler.isInBoundsLeft(pointBuffer[0] + shapeHalf))
                continue;

            if (!mViewPortHandler.isInBoundsRight(pointBuffer[0] - shapeHalf))
                break;

            final int originalColor = set.getColor((int) entry.getX());

            Color.RGBToHSV(Color.red(originalColor), Color.green(originalColor),
                    Color.blue(originalColor), _hsvBuffer);
            _hsvBuffer[2] *= 0.5f;
            final int color = Color.HSVToColor(Color.alpha(originalColor), _hsvBuffer);

            mHighlightPaint.setColor(color);
            mHighlightPaint.setStrokeWidth(set.getHighlightCircleWidth());

//            c.drawCircle(pointBuffer[0], pointBuffer[1], shapeHalf, mHighlightPaint);

            c.drawRect(pointBuffer[0],
                    pointBuffer[1] - rectangleHeight,
                    pointBuffer[0] + rectangleWidth,
                    pointBuffer[1],
                    mHighlightPaint);
        }
    }

    private int getValueBgColor(float maxDataValue, float minDataValue, float currentDataValue) {
        float interval = (maxDataValue - minDataValue) / 3;
        if (currentDataValue >= minDataValue && currentDataValue < minDataValue + interval) {
            //最浅色
            return Color.parseColor("#A3D87214");
        } else if (currentDataValue >= (minDataValue + interval) && currentDataValue < (minDataValue + interval * 2)) {
            return Color.parseColor("#D87214");
        } else {
            //最深色
            return Color.parseColor("#DD425A");
        }
    }

    public void setMaxValueMinValue(float dataMaxValue, float dataMinValue) {
        this.dataMaxValue = dataMaxValue;
        this.dataMinValue = dataMinValue;
    }
}
