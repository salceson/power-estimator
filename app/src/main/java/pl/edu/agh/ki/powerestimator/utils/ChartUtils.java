package pl.edu.agh.ki.powerestimator.utils;

import android.content.res.Resources;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.List;

import pl.edu.agh.ki.powerestimator.R;

public class ChartUtils {
    private static final float LINE_WIDTH = 2f;
    private static final float TEXT_SIZE = 14f;
    private static final int MAX_ENTRIES = 50;
    private static final float LEGEND_SIZE = 0.75f;

    public static final int YELLOW = Color.rgb(255, 255, 0);
    public static final int RED = Color.rgb(255, 0, 0);
    public static final int GREEN = Color.rgb(0, 255, 0);
    public static final int BLUE = Color.rgb(0, 0, 255);
    public static final int BLACK = Color.rgb(0, 0, 0);

    private ChartUtils() {
    }

    public static LineDataSet createStandardLineDataSet(List<Entry> entries, String label,
                                                        int color) {
        final LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setLineWidth(LINE_WIDTH);
        dataSet.setValueTextSize(TEXT_SIZE);
        dataSet.setValueTextColor(BLACK);
        dataSet.setColor(color);
        dataSet.setDrawFilled(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        return dataSet;
    }

    public static void setupChart(LineChart chart, Resources resources) {
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextSize(TEXT_SIZE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTitle(resources.getString(R.string.x_axis_description));

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextSize(TEXT_SIZE);
        yAxis.setTitle(resources.getString(R.string.y_axis_description));

        chart.getAxisRight().setDrawLabels(false);

        Legend legend = chart.getLegend();
        legend.setWordWrapEnabled(true);
        legend.setMaxSizePercent(LEGEND_SIZE);
        legend.setTextSize(TEXT_SIZE);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        chart.setDescription(null);
    }

    public static void removeOutdatedEntries(DataSet... dataSets) {
        for (DataSet ds : dataSets) {
            while (ds.getEntryCount() > MAX_ENTRIES) {
                ds.removeFirst();
            }
        }
    }
}
