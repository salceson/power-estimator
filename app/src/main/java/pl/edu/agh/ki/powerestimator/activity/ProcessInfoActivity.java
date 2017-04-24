package pl.edu.agh.ki.powerestimator.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

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

public class ProcessInfoActivity extends AppCompatActivity {
    private static final int MAX_ENTRIES = 50;

    private LineChart chart = null;
    private LineData lineData = null;

    private LineDataSet cpuDataSet = null;
    private LineDataSet wifiDataSet = null;
    private LineDataSet mobileDataSet = null;

    private float lastX = 0.0f;

    private final List<Entry> cpuEntries = new ArrayList<>();
    private final List<Entry> wifiEntries = new ArrayList<>();
    private final List<Entry> mobileEntries = new ArrayList<>();

    private PowerProfiles powerProfiles;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final Bundle data = msg.getData();
            final float cpuUsageMAh = data.getFloat("cpu");
            final float wifiMAh = data.getFloat("wifi");
            final float mobileMAh = data.getFloat("mobile");

            lastX += 1.0f;

            cpuDataSet.addEntry(new Entry(lastX, cpuUsageMAh));
            wifiDataSet.addEntry(new Entry(lastX, wifiMAh));
            mobileDataSet.addEntry(new Entry(lastX, mobileMAh));

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_info);

        powerProfiles = new PowerProfilesImpl(getApplicationContext());

        final Intent intent = getIntent();
        final Bundle extra = intent.getExtras();

        final int pid = extra.getInt("pid");
        final int uid = extra.getInt("uid");
        final String name = extra.getString("name");

        final TextView view = (TextView) findViewById(R.id.processName);
        view.setText(name);

        chart = (LineChart) findViewById(R.id.chart);

        final PowerProfilesListener listener = new PowerProfilesListener() {
            @Override
            public int getPid() {
                return pid;
            }

            @Override
            public int getUid() {
                return uid;
            }

            @Override
            public void onNewData(float cpuUsageMAh,
                                  float wifiTransferUsageMAh,
                                  float mobileTransferUsageMAh) {
                final Message message = new Message();
                final Bundle data = new Bundle();
                data.putFloat("cpu", cpuUsageMAh);
                data.putFloat("wifi", wifiTransferUsageMAh);
                data.putFloat("mobile", mobileTransferUsageMAh);
                message.setData(data);
                handler.sendMessage(message);
            }
        };

        powerProfiles.addListener(listener);

        cpuEntries.add(new Entry(lastX, 0.0f));
        wifiEntries.add(new Entry(lastX, 0.0f));
        mobileEntries.add(new Entry(lastX, 0.0f));

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

        lineData = new LineData(cpuDataSet, wifiDataSet, mobileDataSet);
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
}
