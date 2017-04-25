package pl.edu.agh.ki.powerestimator.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.ki.powerestimator.R;
import pl.edu.agh.ki.powerestimator.powerprofiles.PowerProfiles;
import pl.edu.agh.ki.powerestimator.powerprofiles.PowerProfilesImpl;
import pl.edu.agh.ki.powerestimator.powerprofiles.PowerProfilesListener;

public class MainActivity extends AppCompatActivity {
    private static final int MAX_ENTRIES = 50;

    private LineChart chart = null;
    private LineData lineData = null;

    private LineDataSet screenDataSet = null;
    private LineDataSet cpuDataSet = null;
    private LineDataSet wifiDataSet = null;
    private LineDataSet mobileDataSet = null;

    private float lastX = 0.0f;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            float screenUsageMAh = data.getFloat("screen");
            float cpuUsageMAh = data.getFloat("cpu");
            float wifiMAh = data.getFloat("wifi");
            float mobileMAh = data.getFloat("mobile");

            lastX += 1.0f;

            screenDataSet.addEntry(new Entry(lastX, screenUsageMAh));
            cpuDataSet.addEntry(new Entry(lastX, cpuUsageMAh));
            wifiDataSet.addEntry(new Entry(lastX, wifiMAh));
            mobileDataSet.addEntry(new Entry(lastX, mobileMAh));

            while (screenDataSet.getEntryCount() > MAX_ENTRIES) {
                screenDataSet.removeFirst();
            }
            while (cpuDataSet.getEntryCount() > MAX_ENTRIES) {
                cpuDataSet.removeFirst();
            }
            while (wifiDataSet.getEntryCount() > MAX_ENTRIES) {
                wifiDataSet.removeFirst();
            }
            while (mobileDataSet.getEntryCount() > MAX_ENTRIES) {
                mobileDataSet.removeFirst();
            }

            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    };

    private PowerProfilesListener listener = new PowerProfilesListener() {
        @Override
        public void onNewData(float screenUsageMAh,
                              float cpuUsageMAh,
                              float wifiTransferUsageMAh,
                              float mobileTransferUsageMAh) {
            Message message = new Message();
            Bundle data = new Bundle();
            data.putFloat("screen", screenUsageMAh);
            data.putFloat("cpu", cpuUsageMAh);
            data.putFloat("wifi", wifiTransferUsageMAh);
            data.putFloat("mobile", mobileTransferUsageMAh);
            message.setData(data);
            handler.sendMessage(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = (LineChart) findViewById(R.id.chart);

        PowerProfiles powerProfiles = new PowerProfilesImpl(getApplicationContext(), listener);

        List<Entry> screenEntries = new ArrayList<>();
        List<Entry> cpuEntries = new ArrayList<>();
        List<Entry> wifiEntries = new ArrayList<>();
        List<Entry> mobileEntries = new ArrayList<>();

        screenEntries.add(new Entry(lastX, 0.0f));
        cpuEntries.add(new Entry(lastX, 0.0f));
        wifiEntries.add(new Entry(lastX, 0.0f));
        mobileEntries.add(new Entry(lastX, 0.0f));

        screenDataSet = new LineDataSet(screenEntries, "Screen usage [mAh]");
        screenDataSet.setLineWidth(2f);
        screenDataSet.setValueTextColor(Color.rgb(0, 0, 0));
        screenDataSet.setValueTextSize(10f);
        screenDataSet.setColor(Color.rgb(255, 255, 0));
        screenDataSet.setDrawFilled(false);
        screenDataSet.setDrawValues(false);
        screenDataSet.setDrawCircles(false);
        screenDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        screenDataSet.setCubicIntensity(0.1f);

        cpuDataSet = new LineDataSet(cpuEntries, "CPU usage [mAh]");
        cpuDataSet.setLineWidth(2f);
        cpuDataSet.setValueTextColor(Color.rgb(0, 0, 0));
        cpuDataSet.setValueTextSize(10f);
        cpuDataSet.setColor(Color.rgb(255, 0, 0));
        cpuDataSet.setDrawFilled(false);
        cpuDataSet.setDrawValues(false);
        cpuDataSet.setDrawCircles(false);
        cpuDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        cpuDataSet.setCubicIntensity(0.1f);

        wifiDataSet = new LineDataSet(wifiEntries, "WiFi usage [mAh]");
        wifiDataSet.setLineWidth(2f);
        wifiDataSet.setValueTextColor(Color.rgb(0, 0, 0));
        wifiDataSet.setValueTextSize(10f);
        wifiDataSet.setColor(Color.rgb(0, 255, 0));
        wifiDataSet.setDrawFilled(false);
        wifiDataSet.setDrawValues(false);
        wifiDataSet.setDrawCircles(false);
        wifiDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        wifiDataSet.setCubicIntensity(0.1f);

        mobileDataSet = new LineDataSet(mobileEntries, "3G usage [mAh]");
        mobileDataSet.setLineWidth(2f);
        mobileDataSet.setValueTextColor(Color.rgb(0, 0, 0));
        mobileDataSet.setValueTextSize(10f);
        mobileDataSet.setColor(Color.rgb(0, 0, 255));
        mobileDataSet.setDrawFilled(false);
        mobileDataSet.setDrawValues(false);
        mobileDataSet.setDrawCircles(false);
        mobileDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        mobileDataSet.setCubicIntensity(0.1f);

        lineData = new LineData(screenDataSet, cpuDataSet, wifiDataSet, mobileDataSet);
        chart.setData(lineData);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDescription(null);
        chart.invalidate();

        powerProfiles.startMeasurements();
    }
}
