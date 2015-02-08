package com.example.rfduino;


import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.text.format.Time;
import android.util.Log;

public class ECGLine {

		private static final double yMin = -1000.0;
		private static final double yMax = 2000.0;
		private static final int windowSize = 200;
		
		//Just for one line
		private TimeSeries series = new TimeSeries("Line");
		private XYSeriesRenderer renderer = new XYSeriesRenderer();
		
		//contains all lines information
		private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
		private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); 
		
		//counter to have graph to only have 50 points
		private int count;
		private long interval,now ;
		
		//private static final int textSize = 12;
		
		private GraphicalView mChartView;
		private Time nowTxt = new Time(Time.getCurrentTimezone());
		
		public char status = 0;
		
		
		public void stop(){
			status =1;
			/*series.clear();
			mDataset.removeSeries(series);
			mRenderer.removeAllRenderers();
			mRenderer.clearXTextLabels();*/
			
		}
		
		public void initialize(){
			//int[] margins = {100, 25, 250, 250};
			// initializes class:
			// Add single dataset to multiple dataset
			switch(status){
			case 0:
			mDataset.addSeries(series);
			
			case 1:
				series.clear();
				mDataset.removeSeries(series);
				mRenderer.removeAllRenderers();
				mRenderer.clearXTextLabels();	
				
				mDataset.addSeries(series);
				status = 2;
			case 2:
				
			}
		
			
			// Customization time for only a single line 1!
			renderer.setColor(Color.RED);
			renderer.setPointStyle(PointStyle.CIRCLE);
			renderer.setFillPoints(true);
			renderer.setLineWidth(10f);
			
			//CHARACTERISITCS FOR ALL LINES		
			// Unable Zoom
			mRenderer.setApplyBackgroundColor(true);
			mRenderer.setBackgroundColor(Color.BLACK);
			mRenderer.setBarSpacing(10);
			mRenderer.setAxesColor(Color.GRAY);
			mRenderer.setZoomButtonsVisible(false);
			mRenderer.setZoomEnabled(false);
			//mRenderer.setMargins(margins);
			mRenderer.setChartTitle("ECG Measurement");
			mRenderer.setChartTitleTextSize(35);
			mRenderer.setXTitle("");
			mRenderer.setYTitle("");
			mRenderer.setPointSize(1);
			mRenderer.setShowGrid(false);
			mRenderer.setXLabels(0);		
			mRenderer.addSeriesRenderer(renderer);	
			mRenderer.setLabelsTextSize(25);
			mRenderer.setLegendTextSize(25);
			mRenderer.setGridColor(Color.BLACK);
			mRenderer.setXLabelsAngle(-45);
			//mRenderer.setYAxisMax(15);
			//mRenderer.setYAxisMin(4);
			mRenderer.setShowLabels(false);
			mRenderer.setShowLegend(false);
			mRenderer.setYAxisMax(yMax);
			mRenderer.setYAxisMin(yMin);
			//mRenderer.setXAxisMax(windowSize);
			//mRenderer.setXAxisMin(0);
			//mRenderer.set
			
			count =0;
			interval =0;
			nowTxt.setToNow();
			now = nowTxt.toMillis(false);
			
			
		}
		
		public void addPoint(double x, double y){
			series.add(x, y);
			
			//Log.d("ECG", String.valueOf(y));
			//nowTxt.setToNow();
			//interval = nowTxt.toMillis(false) - now;
			//mRenderer.setXAxisMax(windowSize + x);
			//mRenderer.setXAxisMin(x)
			
			if (interval == 1000){ // 1 second passed
			mRenderer.addXTextLabel(x,nowTxt.format("%k:%M:%S"));
			now = nowTxt.toMillis(false);
			}
			else
			{
				mRenderer.addXTextLabel(x,"");
			}
			
			
			
			if (count < windowSize){
				count++;
				
			}
			else{
				series.remove(0);
			}
			
			
		}
		
		public GraphicalView getView(Context context){
			mChartView = ChartFactory.getLineChartView(context, mDataset, mRenderer);
			return mChartView;
		}
		
		public void rePaint(){
			mChartView.repaint();
		}
}
