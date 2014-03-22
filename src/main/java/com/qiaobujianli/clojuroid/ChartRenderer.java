package com.qiaobujianli.clojuroid;

import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.graphics.Paint.Align;

public class ChartRenderer extends XYMultipleSeriesRenderer {
	public ChartRenderer(String chartTitle, int totalDays) {
		/*
		(.setAxisTitleTextSize 16)
		(.setXTitle "")
		(.setYTitle "")
		*/
		setChartTitleTextSize(25);
		setChartTitle(chartTitle);

		setLabelsTextSize(20);
		setXLabels((int) Math.ceil(totalDays / 2f));
		setYLabels(10);
		setYLabelsAlign(Align.RIGHT);

		setLegendTextSize(20);

		setPointSize(10);

		setMargins(new int[] { 20, 60, 20, 20 });

		setZoomButtonsVisible(true);

		setShowGrid(true);

		setClickEnabled(true);

		addSeriesRenderers();
	}

	private void addSeriesRenderers() {
		XYSeriesRenderer userRenderer = new XYSeriesRenderer();
		userRenderer.setColor(0xFF56BAEC);
		userRenderer.setPointStyle(PointStyle.CIRCLE);
		userRenderer.setFillPoints(true);
		userRenderer.setLineWidth(2);
		addSeriesRenderer(userRenderer);

		XYSeriesRenderer resumeRenderer = new XYSeriesRenderer();
		resumeRenderer.setColor(0xFFFFF0AA);
		resumeRenderer.setPointStyle(PointStyle.DIAMOND);
		resumeRenderer.setFillPoints(true);
		userRenderer.setLineWidth(2);
		addSeriesRenderer(resumeRenderer);
	}
}
