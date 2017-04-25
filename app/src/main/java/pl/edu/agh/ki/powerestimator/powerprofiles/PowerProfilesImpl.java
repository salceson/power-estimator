package pl.edu.agh.ki.powerestimator.powerprofiles;

import android.content.Context;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.getInt;

public class PowerProfilesImpl implements PowerProfiles {
    private final Context context;

    private static final int MAX_BRIGHTNESS = 255;
    private static final int SECONDS_PER_HOUR = 3600;
    // _SC_CLK_TCK
    private static final int CLOCK_TICKS_PER_SECOND = 100;
    private static final int MILLIS_PER_CLOCK_TICK = 1000 / CLOCK_TICKS_PER_SECOND;

    private static final String LOG_TAG = "PPImpl";

    private static final String WIFI_KEY = "wifi";
    private static final String MOBILE_KEY = "mobile";
    private static final String SCREEN_KEY = "screen";

    private static final ScheduledExecutorService EXECUTOR_SERVICE =
            Executors.newSingleThreadScheduledExecutor();

    private List<PowerProfilesListener> listeners = new CopyOnWriteArrayList<>();
    private Map<Integer, CpuInfo> previousCpuInfos = new ConcurrentHashMap<>();
    private Map<Integer, TransferInfo> previousTransferInfos = new ConcurrentHashMap<>();

    private ScheduledFuture<?> future;

    public PowerProfilesImpl(Context context) {
        this.context = context;
    }

    @Override
    public void startMeasurements() {
        try {
            future = EXECUTOR_SERVICE.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (PowerProfilesListener listener : listeners) {
                        int pid = listener.getPid();
                        int uid = listener.getUid();
                        float cpuMAh = getCPUMAh(pid);
                        Map<String, Float> componentsMAh = measureComponentDrains(1.0, uid);
                        listener.onNewData(
                                componentsMAh.get(SCREEN_KEY), cpuMAh,
                                componentsMAh.get(WIFI_KEY), componentsMAh.get(MOBILE_KEY)
                        );
                    }
                }
            }, 1000, 1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while starting measurements", e);
        }
    }

    @Override
    public void stopMeasurements() {
        future.cancel(true);
    }

    @Override
    public void addListener(PowerProfilesListener listener) {
        listeners.add(listener);
        previousCpuInfos.put(listener.getPid(), new CpuInfo(0, 0));
        previousTransferInfos.put(listener.getUid(), new TransferInfo());
    }

    @Override
    public void removeListener(PowerProfilesListener listener) {
        listeners.remove(listener);
        previousCpuInfos.remove(listener.getPid());
        previousTransferInfos.remove(listener.getUid());
    }

    private float getCPUMAh(int pid) {
        try {
            CpuInfo previousCpuInfo = previousCpuInfos.get(pid);
            CpuInfo nextCpuInfo = readCpuInfo(pid);
            previousCpuInfos.put(pid, nextCpuInfo);
            long cpuActiveTime = nextCpuInfo.activeTime - previousCpuInfo.activeTime;
            long cpuIdleTime = nextCpuInfo.idleTime - previousCpuInfo.idleTime;
            int ticksPerHour = CLOCK_TICKS_PER_SECOND * SECONDS_PER_HOUR;
            double cpuDrainMAh = (getAveragePower("cpu.idle") * cpuIdleTime / ticksPerHour)
                    + (getAveragePower("cpu.active") * cpuActiveTime / ticksPerHour)
                    + (getAveragePower("cpu.idle") * cpuActiveTime / ticksPerHour);
            return (float) cpuDrainMAh;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while calculating CPU usage", e);
            return Float.NaN;
        }
    }

    private Map<String, Float> measureComponentDrains(double scale, int uid) {
        TransferInfo nextTransferInfo = new TransferInfo(uid);
        TransferInfo previousTransferInfo = previousTransferInfos.get(uid);
        previousTransferInfos.put(uid, nextTransferInfo);
        double wifiDrainMAh = 0;
        double mobileDrainMAh = 0;
        double screenDrainMAh = 0;
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm.isScreenOn()) {
                float brightness = getInt(context.getContentResolver(), SCREEN_BRIGHTNESS);
                screenDrainMAh += (getAveragePower("screen.on") + (brightness / MAX_BRIGHTNESS
                        * getAveragePower("screen.full"))) / SECONDS_PER_HOUR * scale;
            }

            if (previousTransferInfo.wasWifiReceiving(nextTransferInfo)) {
                wifiDrainMAh += getAveragePower("wifi.active") / SECONDS_PER_HOUR * scale;
            }
            if (previousTransferInfo.wasWifiTransmitting(nextTransferInfo)) {
                wifiDrainMAh += getAveragePower("wifi.active") / SECONDS_PER_HOUR * scale;
            }
            if (previousTransferInfo.wasMobileReceiving(nextTransferInfo)
                    || previousTransferInfo.wasMobileTransmitting(nextTransferInfo)) {
                mobileDrainMAh += getAveragePower("radio.active") / SECONDS_PER_HOUR * scale;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while calculating components usage", e);
        }
        Map<String, Float> componentsMAh = new HashMap<>();
        componentsMAh.put(WIFI_KEY, (float) wifiDrainMAh);
        componentsMAh.put(MOBILE_KEY, (float) mobileDrainMAh);
        componentsMAh.put(SCREEN_KEY, (float) screenDrainMAh);
        return componentsMAh;
    }

    private double getAveragePower(String componentState) throws Exception {
        return invokePowerProfileMethod("getAveragePower", Double.class,
                new Class[]{String.class}, new Object[]{componentState});
    }

    private double getAveragePower(String componentState, int level) throws Exception {
        return invokePowerProfileMethod("getAveragePower", Double.class,
                new Class[]{String.class, int.class}, new Object[]{componentState, level});
    }

    private <T> T invokePowerProfileMethod(String methodName,
                                           Class<T> returnType,
                                           Class<?>[] argTypes,
                                           Object[] args)
            throws Exception {
        final String powerProfileClass = "com.android.internal.os.PowerProfile";

        Object powerProfile = Class.forName(powerProfileClass)
                .getConstructor(Context.class)
                .newInstance(context.getApplicationContext());

        return returnType.cast(Class.forName(powerProfileClass)
                .getMethod(methodName, argTypes)
                .invoke(powerProfile, args));
    }

    private CpuInfo readCpuInfo(int pid) throws Exception {
        RandomAccessFile reader = new RandomAccessFile("/proc/" + pid + "/stat", "r");
        String line = reader.readLine();
        reader.close();

        String[] split = line.split("\\s+");

        // utime stime cutime cstime
        long activeTimeTicks = Long.parseLong(split[13]) + Long.parseLong(split[14])
                + Long.parseLong(split[15]) + Long.parseLong(split[16]);
        long processStartTimeTicks = Long.parseLong(split[21]);

        long systemUptimeMillis = SystemClock.uptimeMillis();
        long processUptimeTicks = (systemUptimeMillis / MILLIS_PER_CLOCK_TICK)
                - processStartTimeTicks;
        long idleTimeTicks = processUptimeTicks - activeTimeTicks;

        return new CpuInfo(activeTimeTicks, idleTimeTicks);
    }

    private static class CpuInfo {
        long activeTime;
        long idleTime;

        CpuInfo(long activeTime, long idleTime) {
            this.activeTime = activeTime;
            this.idleTime = idleTime;
        }
    }

    private class TransferInfo {
        long wifiRxBytes;
        long wifiTxBytes;
        long mobileRxBytes;
        long mobileTxBytes;

        TransferInfo() {
            wifiRxBytes = 0;
            wifiTxBytes = 0;
            mobileRxBytes = 0;
            mobileTxBytes = 0;
        }

        TransferInfo(int uid) {
            WifiManager wifi = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled()) {
                wifiRxBytes = TrafficStats.getUidRxBytes(uid);
                wifiTxBytes = TrafficStats.getUidTxBytes(uid);
            } else {
                mobileRxBytes = TrafficStats.getUidRxBytes(uid);
                mobileTxBytes = TrafficStats.getUidTxBytes(uid);
            }
        }

        boolean wasWifiReceiving(TransferInfo nextInfo) {
            return nextInfo.wifiRxBytes - wifiRxBytes > 0;
        }

        boolean wasWifiTransmitting(TransferInfo nextInfo) {
            return nextInfo.wifiTxBytes - wifiTxBytes > 0;
        }

        boolean wasMobileReceiving(TransferInfo nextInfo) {
            return nextInfo.mobileRxBytes - mobileRxBytes > 0;
        }

        boolean wasMobileTransmitting(TransferInfo nextInfo) {
            return nextInfo.mobileTxBytes - mobileTxBytes > 0;
        }
    }
}
