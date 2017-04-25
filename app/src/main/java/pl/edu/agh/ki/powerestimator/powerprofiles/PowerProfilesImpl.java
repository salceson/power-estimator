package pl.edu.agh.ki.powerestimator.powerprofiles;

import android.content.Context;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.getInt;

public class PowerProfilesImpl implements PowerProfiles {
    private final Context context;
    private final PowerProfilesListener listener;

    private static final int MAX_BRIGHTNESS = 255;
    private static final int SECONDS_PER_HOUR = 3600;
    // _SC_CLK_TCK
    private static final int CLOCK_TICKS_PER_SECOND = 100;
    private static final int MILLIS_PER_CLOCK_TICK = 1000 / CLOCK_TICKS_PER_SECOND;

    private static final String LOG_TAG = "PPImpl";

    private static final Map<String, Double> COMPONENTS_DRAIN_MAH = new HashMap<>();

    private static final ScheduledExecutorService EXECUTOR_SERVICE =
            Executors.newSingleThreadScheduledExecutor();

    private static CpuInfo previousCpuInfo;
    private static TransferInfo previousTransferInfo;
    private static long previousMeasurementMillis;

    public PowerProfilesImpl(Context context, PowerProfilesListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void startMeasurements() {
        try {
            COMPONENTS_DRAIN_MAH.put("screen", 0.0);
            COMPONENTS_DRAIN_MAH.put("wifi", 0.0);
            COMPONENTS_DRAIN_MAH.put("mobile", 0.0);

            EXECUTOR_SERVICE.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    measureComponentsDrains(1.0);
                    float cpuMAh = getCPUMAh();
                    listener.onNewData(
                            COMPONENTS_DRAIN_MAH.get("screen").floatValue(),
                            cpuMAh,
                            COMPONENTS_DRAIN_MAH.get("wifi").floatValue(),
                            COMPONENTS_DRAIN_MAH.get("mobile").floatValue()
                    );
                }
            }, 1000, 1000, TimeUnit.MILLISECONDS);

            previousTransferInfo = new TransferInfo();
            previousCpuInfo = readCpuInfo();
            previousMeasurementMillis = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while starting measurements", e);
        }
    }

    private void stopMeasurements() {
//        try {
//            CpuInfo nextCpuInfo = readCpuInfo();
//            long cpuActiveTime = nextCpuInfo.activeTime - previousCpuInfo.activeTime;
//            long cpuIdleTime = nextCpuInfo.idleTime - previousCpuInfo.idleTime;
//            int ticksPerHour = CLOCK_TICKS_PER_SECOND * SECONDS_PER_HOUR;
//            double cpuDrainMAh = (getAveragePower("cpu.idle") * cpuIdleTime / ticksPerHour)
//                    + (getAveragePower("cpu.active") * cpuActiveTime / ticksPerHour)
//                    + (getAveragePower("cpu.idle") * cpuActiveTime / ticksPerHour);
//            previousCpuInfo = nextCpuInfo;
//
//            long measurementInterval = System.currentTimeMillis() - previousMeasurementMillis;
//            if (measurementInterval > 100 && measurementInterval < 900) {
//                measureComponentsDrains(measurementInterval / 1000);
//            }
//
//            obj.put("cpuActivePower", getAveragePower("cpu.active", 3));
//            obj.put("wifiActivePower", getAveragePower("wifi.active"));
//            obj.put("mobileActivePower", getAveragePower("radio.active"));
//
//            obj.put("cpu", cpuDrainMAh);
//            obj.put("wifi", COMPONENTS_DRAIN_MAH.get("wifi"));
//            obj.put("mobile", COMPONENTS_DRAIN_MAH.get("mobile"));
//            double total = cpuDrainMAh + COMPONENTS_DRAIN_MAH.get("wifi") + COMPONENTS_DRAIN_MAH.get("mobile");
//            obj.put("total", total);
//            obj.put("total%", total / getAveragePower("battery.capacity") * 100);
//        } catch (Exception e) {
//            callbackContext.error(e.getCause() + ": " + e.getMessage());
//        }
//        callbackContext.success(obj);
    }


    @Override
    public float getCPUMAh() {
        try {
            CpuInfo nextCpuInfo = readCpuInfo();
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

    private void measureComponentsDrains(double scale) {
        double screenDrainMAh = COMPONENTS_DRAIN_MAH.get("screen");
        TransferInfo nextTransferInfo = new TransferInfo();
        double wifiDrainMAh = COMPONENTS_DRAIN_MAH.get("wifi");
        double mobileDrainMAh = COMPONENTS_DRAIN_MAH.get("mobile");
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
        } catch (Exception ignore) {
        }
        COMPONENTS_DRAIN_MAH.put("screen", screenDrainMAh);
        COMPONENTS_DRAIN_MAH.put("wifi", wifiDrainMAh);
        COMPONENTS_DRAIN_MAH.put("mobile", mobileDrainMAh);
        previousMeasurementMillis = System.currentTimeMillis();
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

    private CpuInfo readCpuInfo() throws Exception {
        RandomAccessFile reader = new RandomAccessFile("/proc/" + Process.myPid() + "/stat", "r");
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
            WifiManager wifi = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled()) {
                wifiRxBytes = TrafficStats.getUidRxBytes(Process.myUid());
                wifiTxBytes = TrafficStats.getUidTxBytes(Process.myUid());
            } else {
                mobileRxBytes = TrafficStats.getUidRxBytes(Process.myUid());
                mobileTxBytes = TrafficStats.getUidTxBytes(Process.myUid());
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
