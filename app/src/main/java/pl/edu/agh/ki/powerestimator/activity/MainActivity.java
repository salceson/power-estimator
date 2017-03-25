package pl.edu.agh.ki.powerestimator.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pl.edu.agh.ki.powerestimator.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LineChart chart = (LineChart) findViewById(R.id.chart);
        Random random = new Random();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            entries.add(new Entry(i, random.nextFloat() * 50 + 50));
        }
        LineDataSet dataSet = new LineDataSet(entries, "CPU usage");
        dataSet.setLineWidth(2f);
        dataSet.setValueTextColor(Color.rgb(0, 0, 0));
        dataSet.setValueTextSize(10f);
        dataSet.setColor(Color.rgb(255, 0, 0));
        dataSet.setFillColor(Color.argb(200, 255, 0, 0));
        dataSet.setDrawFilled(true);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getAxisLeft().setValueFormatter(new PercentFormatter());
        chart.getAxisRight().setDrawLabels(false);
        chart.setDescription(null);
        chart.invalidate();
    }
}
