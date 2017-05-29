package pl.edu.agh.ki.powerestimator.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pl.edu.agh.ki.powerestimator.R;
import pl.edu.agh.ki.powerestimator.powerprofiles.MeasurementType;
import pl.edu.agh.ki.powerestimator.powerprofiles.PowerProfiles;
import pl.edu.agh.ki.powerestimator.powerprofiles.PowerProfilesImpl;
import pl.edu.agh.ki.powerestimator.powerprofiles.PowerProfilesListener;
import pl.edu.agh.ki.powerestimator.utils.ChartUtils;

public class ProcessInfoActivity extends AppCompatActivity {
    private static final int MAX_ENTRIES = 50;
    private static final String LOG_TAG = "PInfoActivity";

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
            Bundle data = msg.getData();
            final float cpuUsageMAh = data.getFloat("cpu");
            final float wifiMAh = data.getFloat("wifi");
            final float mobileMAh = data.getFloat("mobile");

            lastX += 1.0f;

            cpuDataSet.addEntry(new Entry(lastX, cpuUsageMAh));
            wifiDataSet.addEntry(new Entry(lastX, wifiMAh));
            mobileDataSet.addEntry(new Entry(lastX, mobileMAh));

            ChartUtils.removeOutdatedEntries(cpuDataSet, wifiDataSet, mobileDataSet);

            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_info);

        try {
            powerProfiles = PowerProfilesImpl.getInstance(getApplicationContext());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not create PowerProfiles class", e);
        }

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
            public boolean isSummary() {
                return false;
            }

            @Override
            public List<MeasurementType> getMeasurementTypes() {
                return Arrays.asList(MeasurementType.CPU, MeasurementType.MOBILE,
                        MeasurementType.WIFI, MeasurementType.SCREEN);
            }

            @Override
            public void onNewData(Map<MeasurementType, Float> data) {
                final Message message = new Message();
                final Bundle messageData = new Bundle();
                messageData.putFloat("screen", data.get(MeasurementType.SCREEN));
                messageData.putFloat("cpu", data.get(MeasurementType.CPU));
                messageData.putFloat("wifi", data.get(MeasurementType.WIFI));
                messageData.putFloat("mobile", data.get(MeasurementType.MOBILE));
                message.setData(messageData);
                handler.sendMessage(message);
            }
        };

        try {
            powerProfiles.addListener(listener);
        } catch (Exception e) {
            Log.e(ProcessInfoActivity.class.getSimpleName(), "Error while adding listener", e);
        }

        cpuEntries.add(new Entry(lastX, 0.0f));
        wifiEntries.add(new Entry(lastX, 0.0f));
        mobileEntries.add(new Entry(lastX, 0.0f));

        cpuDataSet = ChartUtils.createStandardLineDataSet(cpuEntries, "CPU usage [mAh]", Color.rgb(255, 0, 0));
        wifiDataSet = ChartUtils.createStandardLineDataSet(wifiEntries, "WiFi usage [mAh]", Color.rgb(0, 255, 0));
        mobileDataSet = ChartUtils.createStandardLineDataSet(mobileEntries, "3G usage [mAh]", Color.rgb(0, 0, 255));

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
