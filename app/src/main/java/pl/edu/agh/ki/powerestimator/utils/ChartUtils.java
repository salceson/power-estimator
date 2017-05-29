package pl.edu.agh.ki.powerestimator.utils;

import android.graphics.Color;

import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.List;

public class ChartUtils {

    private static final float LINE_WIDTH = 2f;
    private static final float TEXT_SIZE = 10f;
    private static final int TEXT_COLOR = Color.rgb(0, 0, 0);
    private static final int MAX_ENTRIES = 50;

    private ChartUtils() {
    }

    public static LineDataSet createStandardLineDataSet(List<Entry> entries, String label,
                                                        int color) {
        final LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setLineWidth(LINE_WIDTH);
        dataSet.setValueTextSize(TEXT_SIZE);
        dataSet.setValueTextColor(TEXT_COLOR);
        dataSet.setColor(color);
        dataSet.setDrawFilled(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        return dataSet;
    }

    public static void removeOutdatedEntries(DataSet... dataSets) {
        for (DataSet ds : dataSets) {
            while (ds.getEntryCount() > MAX_ENTRIES) {
                ds.removeFirst();
            }
        }
    }
}
