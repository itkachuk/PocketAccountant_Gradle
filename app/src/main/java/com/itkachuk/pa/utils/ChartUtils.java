package com.itkachuk.pa.utils;

import android.graphics.Color;
import android.graphics.Paint;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.util.List;

public class ChartUtils {

	/**
	 * Sets a few of the series renderer settings.
	 * 
	 * @param renderer the renderer to set the properties to
	 * @param title the chart title
	 * @param xTitle the title for the X axis
	 * @param yTitle the title for the Y axis
	 * @param xMin the minimum value on the X axis
	 * @param xMax the maximum value on the X axis
	 * @param yMin the minimum value on the Y axis
	 * @param yMax the maximum value on the Y axis
	 * @param axesColor the axes color
	 * @param labelsColor the labels color
     * @param backgroundColor the color for background
	 */
	public static void setChartRendererSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle,
                                                String yTitle, double xMin, double xMax, double yMin, double yMax, int axesColor,
                                                int labelsColor, int backgroundColor) {
		renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setXAxisMin(xMin);
		renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
        renderer.setXLabels((int) (xMax - xMin)); // number of X-axis points
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
        //renderer.setBackgroundColor(backgroundColor);
        //renderer.setApplyBackgroundColor(true);

        // Static settings
        renderer.setYLabels(10);
        renderer.setXLabelsAlign(Paint.Align.LEFT);
        renderer.setYLabelsAlign(Paint.Align.LEFT);
        renderer.setPanEnabled(true, true);
        renderer.setMargins(new int[]{30, 25, 20, 10});
        renderer.setZoomButtonsVisible(false);
        renderer.setZoomEnabled(true);
        renderer.setZoomRate(1.0f);
        renderer.setBarSpacing(0.3f);

        for (SimpleSeriesRenderer simpleRenderer : renderer.getSeriesRenderers()) {
            simpleRenderer.setDisplayChartValues(true);
            simpleRenderer.setDisplayChartValuesDistance(10); // try to set minimal distance - need to test
        }
	}

    public static void setChartRendererDynamicSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle,
                                                String yTitle, double xMin, double xMax, double yMin, double yMax) {
        renderer.setChartTitle(title);
        renderer.setXTitle(xTitle);
        renderer.setYTitle(yTitle);
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        renderer.setXLabels((int) (xMax - xMin)); // number of X-axis points

    }

    public static void setChartRendererStaticSettings(XYMultipleSeriesRenderer renderer, int backgroundColor) {
        renderer.setAxesColor(Color.GRAY);
        renderer.setLabelsColor(Color.LTGRAY);
        renderer.setBackgroundColor(backgroundColor);
        renderer.setApplyBackgroundColor(true);

        renderer.setYLabels(10);
        renderer.setXLabelsAlign(Paint.Align.LEFT);
        renderer.setYLabelsAlign(Paint.Align.LEFT);
        renderer.setPanEnabled(true, true);
        renderer.setMargins(new int[]{30, 25, 20, 10});
        renderer.setZoomButtonsVisible(false);
        renderer.setZoomEnabled(true);
        renderer.setZoomRate(1.0f);
        renderer.setBarSpacing(0.3f);

        for (SimpleSeriesRenderer simpleRenderer : renderer.getSeriesRenderers()) {
            simpleRenderer.setDisplayChartValues(true);
            simpleRenderer.setDisplayChartValuesDistance(10); // try to set minimal distance - need to test
        }
    }

	/**
	 * Builds a bar multiple series dataset using the provided values.
	 * 
	 * @param titles the series titles
	 * @param values the values
	 * @return the XY multiple bar dataset
	 */
	public static XYMultipleSeriesDataset buildBarDataset(String[] titles, List<double[]> values) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		int length = titles.length;
        int xStartIndex = (int) values.get(3)[2]; // get X min value
		for (int i = 0; i < length; i++) {
			//CategorySeries series = new CategorySeries(titles[i]); // Changed to XYSeries, CategorySeries didn't allow to set x-axis position
            XYSeries xySeries = new XYSeries(titles[i]);
			double[] v = values.get(i);
			int seriesLength = v.length;
			for (int k = 0, x = xStartIndex; k < seriesLength; k++, x++) {
				//series.add(v[k]);
                xySeries.add(x, v[k]);
			}
			//dataset.addSeries(series.toXYSeries());
			dataset.addSeries(xySeries);
		}
		return dataset;
	}

	/**
	 * Builds a bar multiple series renderer to use the provided colors.
	 * 
	 * @param colors the series renderers colors
	 * @return the bar multiple series renderer
	 */
	public static XYMultipleSeriesRenderer buildBarRenderer(int[] colors) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(15);
		renderer.setLegendTextSize(15);
		int length = colors.length;
		for (int i = 0; i < length; i++) {
			SimpleSeriesRenderer r = new SimpleSeriesRenderer();
			r.setColor(colors[i]);
			renderer.addSeriesRenderer(r);
		}
		return renderer;
	}
	
	/**
	 * Builds a bar multiple series renderer to use the provided colors.
	 * 
	 * @param color the series renderers colors
	 * @return the bar multiple series renderer
	 */
	public static XYMultipleSeriesRenderer buildBarSimpleRenderer(int color) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(15);
		renderer.setLegendTextSize(15);
		renderer.setBarSpacing(0.2);
		SimpleSeriesRenderer simpleRenderer = new SimpleSeriesRenderer();
		simpleRenderer.setColor(color);
		renderer.addSeriesRenderer(simpleRenderer);
		return renderer;
	}
}
