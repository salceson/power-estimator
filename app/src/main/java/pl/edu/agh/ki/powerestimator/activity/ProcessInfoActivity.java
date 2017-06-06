package pl.edu.agh.ki.powerestimator.activity;

import android.content.Intent;
import android.content.res.Resources;
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
import java.util.List;

import pl.edu.agh.ki.powerestimator.R;
import pl.edu.agh.ki.powerestimator.listeners.ProcessPowerProfilesListener;
import pl.edu.agh.ki.powerestimator.utils.ChartUtils;
import pl.edu.agh.ki.powerprofiles.MeasurementType;
import pl.edu.agh.ki.powerprofiles.PowerProfiles;
import pl.edu.agh.ki.powerprofiles.PowerProfilesImpl;
import pl.edu.agh.ki.powerprofiles.PowerProfilesListener;

public class ProcessInfoActivity extends AppCompatActivity {
    private static final String LOG_TAG = "PInfoActivity";
    public static final String PIDS_KEY = "pid";
    public static final String UID_KEY = "uid";
    public static final String NAME_KEY = "name";

    private LineChart chart = null;
    private LineData lineData = null;

    private LineDataSet cpuDataSet = null;
    private LineDataSet wifiDataSet = null;
    private LineDataSet mobileDataSet = null;
    private LineDataSet summaryDataSet = null;

    private float lastX = 0.0f;

    private final List<Entry> cpuEntries = new ArrayList<>();
    private final List<Entry> wifiEntries = new ArrayList<>();
    private final List<Entry> mobileEntries = new ArrayList<>();
    private final List<Entry> summaryEntries = new ArrayList<>();

    private PowerProfiles powerProfiles;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            final float cpuUsageMAh = data.getFloat(MeasurementType.CPU.getKey());
            final float wifiMAh = data.getFloat(MeasurementType.WIFI.getKey());
            final float mobileMAh = data.getFloat(MeasurementType.MOBILE.getKey());
            final float summaryMAh = cpuUsageMAh + wifiMAh + mobileMAh;

            lastX += 1.0f;

            cpuDataSet.addEntry(new Entry(lastX, cpuUsageMAh));
            wifiDataSet.addEntry(new Entry(lastX, wifiMAh));
            mobileDataSet.addEntry(new Entry(lastX, mobileMAh));
            summaryDataSet.addEntry(new Entry(lastX, summaryMAh));

            ChartUtils.removeOutdatedEntries(cpuDataSet, wifiDataSet, mobileDataSet,
                    summaryDataSet);

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

        final int uid = extra.getInt(UID_KEY);
        final String name = extra.getString(NAME_KEY);
        final List<Integer> pids = extra.getIntegerArrayList(PIDS_KEY);

        final TextView view = (TextView) findViewById(R.id.processName);
        view.setText(name);

        chart = (LineChart) findViewById(R.id.chart);

        final PowerProfilesListener listener = new ProcessPowerProfilesListener(pids, uid, handler);

        try {
            powerProfiles.addListener(listener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while adding listener", e);
        }

        cpuEntries.add(new Entry(lastX, 0.0f));
        wifiEntries.add(new Entry(lastX, 0.0f));
        mobileEntries.add(new Entry(lastX, 0.0f));
        summaryEntries.add(new Entry(lastX, 0.0f));

        Resources resources = getResources();

        cpuDataSet = ChartUtils.createStandardLineDataSet(cpuEntries,
                resources.getString(R.string.cpu_usage), ChartUtils.RED);
        wifiDataSet = ChartUtils.createStandardLineDataSet(wifiEntries,
                resources.getString(R.string.wifi_usage), ChartUtils.GREEN);
        mobileDataSet = ChartUtils.createStandardLineDataSet(mobileEntries,
                resources.getString(R.string.mobile_usage), ChartUtils.BLUE);
        summaryDataSet = ChartUtils.createStandardLineDataSet(summaryEntries,
                resources.getString(R.string.all_usage), ChartUtils.BLACK);

        lineData = new LineData(cpuDataSet, wifiDataSet, mobileDataSet, summaryDataSet);

        ChartUtils.setupChart(chart, resources);

        chart.setData(lineData);
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
