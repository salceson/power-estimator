package pl.edu.agh.ki.powerestimator.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.ki.powerestimator.R;
import pl.edu.agh.ki.powerestimator.listeners.SummaryPowerProfilesListener;
import pl.edu.agh.ki.powerestimator.utils.ChartUtils;
import pl.edu.agh.ki.powerprofiles.MeasurementType;
import pl.edu.agh.ki.powerprofiles.PowerProfiles;
import pl.edu.agh.ki.powerprofiles.PowerProfilesImpl;
import pl.edu.agh.ki.powerprofiles.PowerProfilesListener;

public class SummaryActivity extends AppCompatActivity {

    private static final String LOG_TAG = "SummaryActivity";

    private LineChart chart = null;
    private LineData lineData = null;

    private LineDataSet screenDataSet = null;
    private LineDataSet cpuDataSet = null;
    private LineDataSet wifiDataSet = null;
    private LineDataSet mobileDataSet = null;

    private float lastX = 0.0f;

    private final List<Entry> cpuEntries = new ArrayList<>();
    private final List<Entry> wifiEntries = new ArrayList<>();
    private final List<Entry> mobileEntries = new ArrayList<>();
    private final List<Entry> screenEntries = new ArrayList<>();

    private PowerProfiles powerProfiles;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            final float screenUsageMAh = data.getFloat(MeasurementType.SCREEN.getKey());
            final float cpuUsageMAh = data.getFloat(MeasurementType.CPU.getKey());
            final float wifiMAh = data.getFloat(MeasurementType.WIFI.getKey());
            final float mobileMAh = data.getFloat(MeasurementType.MOBILE.getKey());

            lastX += 1.0f;

            screenDataSet.addEntry(new Entry(lastX, screenUsageMAh));
            cpuDataSet.addEntry(new Entry(lastX, cpuUsageMAh));
            wifiDataSet.addEntry(new Entry(lastX, wifiMAh));
            mobileDataSet.addEntry(new Entry(lastX, mobileMAh));

            ChartUtils.removeOutdatedEntries(screenDataSet, cpuDataSet, wifiDataSet, mobileDataSet);

            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        try {
            powerProfiles = PowerProfilesImpl.getInstance(getApplicationContext());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not create PowerProfiles class", e);
        }

        chart = (LineChart) findViewById(R.id.chart);

        final PowerProfilesListener listener = new SummaryPowerProfilesListener(handler);

        try {
            powerProfiles.addListener(listener);
        } catch (Exception e) {
            Log.e(ProcessInfoActivity.class.getSimpleName(), "Error while adding listener", e);
        }

        screenEntries.add(new Entry(lastX, 0.0f));
        cpuEntries.add(new Entry(lastX, 0.0f));
        wifiEntries.add(new Entry(lastX, 0.0f));
        mobileEntries.add(new Entry(lastX, 0.0f));

        screenDataSet = ChartUtils.createStandardLineDataSet(screenEntries, "Screen usage [mAh]",
                Color.rgb(255, 255, 0));
        cpuDataSet = ChartUtils.createStandardLineDataSet(cpuEntries, "CPU usage [mAh]",
                Color.rgb(255, 0, 0));
        wifiDataSet = ChartUtils.createStandardLineDataSet(wifiEntries, "WiFi usage [mAh]",
                Color.rgb(0, 255, 0));
        mobileDataSet = ChartUtils.createStandardLineDataSet(mobileEntries, "3G usage [mAh]",
                Color.rgb(0, 0, 255));

        lineData = new LineData(screenDataSet, cpuDataSet, wifiDataSet, mobileDataSet);

        chart.setData(lineData);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDescription(null);
        chart.invalidate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        powerProfiles.startMeasurements();
    }

    @Override
    protected void onStop() {
        super.onStop();
        powerProfiles.stopMeasurements();
    }

    public void showProcessesList(View view) {
        final Intent intent = new Intent(SummaryActivity.this, ProcessesListActivity.class);
        startActivity(intent);
    }
}
