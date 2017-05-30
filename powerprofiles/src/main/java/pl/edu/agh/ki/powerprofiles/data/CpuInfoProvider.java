package pl.edu.agh.ki.powerprofiles.data;

import android.annotation.SuppressLint;
import android.os.SystemClock;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pl.edu.agh.ki.powerprofiles.MeasurementType;
import pl.edu.agh.ki.powerprofiles.PowerProfilesListener;

public class CpuInfoProvider implements DataProvider {
    // _SC_CLK_TCK
    private static final int CLOCK_TICKS_PER_SECOND = 100;
    private static final int MILLIS_PER_CLOCK_TICK = 1000 / CLOCK_TICKS_PER_SECOND;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int TICKS_PER_HOUR = CLOCK_TICKS_PER_SECOND * SECONDS_PER_HOUR;
    private static final String PROC_PID_INFO_FILE_TEMPLATE = "/proc/%d/stat";
    private static final String PROC_INFO_FILE_NAME = "/proc/stat";

    private final PowerProfileObject powerProfileObject;
    private final Map<Integer, CpuInfo> previousCpuInfos = new ConcurrentHashMap<>();
    private final Map<Integer, Float> measurements = new ConcurrentHashMap<>();

    public CpuInfoProvider(PowerProfileObject powerProfileObject) {
        this.powerProfileObject = powerProfileObject;
    }

    @Override
    public List<MeasurementType> getProvidedMeasurementTypes() {
        return Collections.singletonList(MeasurementType.CPU);
    }

    @Override
    public void takeMeasurements(int pid, int uid) throws Exception {
        CpuInfo previousCpuInfo = previousCpuInfos.get(pid);
        CpuInfo nextCpuInfo = readCpuInfo(pid);
        previousCpuInfos.put(pid, nextCpuInfo);
        long cpuActiveTime = nextCpuInfo.activeTime - previousCpuInfo.activeTime;
        long cpuIdleTime = nextCpuInfo.idleTime - previousCpuInfo.idleTime;
        double cpuDrainMAh = (
                powerProfileObject.getAveragePower("cpu.idle") * cpuIdleTime / TICKS_PER_HOUR)
                + (powerProfileObject.getAveragePower("cpu.active") * cpuActiveTime / TICKS_PER_HOUR)
                + (powerProfileObject.getAveragePower("cpu.idle") * cpuActiveTime / TICKS_PER_HOUR);
        measurements.put(pid, (float) cpuDrainMAh);
    }

    @Override
    public float getMeasurement(MeasurementType measurementType,
                                int pid, int uid) throws Exception {
        if (measurementType != MeasurementType.CPU) {
            throw new IllegalArgumentException(
                    "CpuInfoProvider provides only CPU measurements, not of type: "
                            + measurementType.name()
            );
        }
        return measurements.get(pid);
    }

    @Override
    public void onListenerAdded(int pid, int uid) throws Exception {
        previousCpuInfos.put(pid, readCpuInfo(pid));
    }

    @Override
    public void onListenerRemoved(int pid, int uid) throws Exception {
        previousCpuInfos.remove(pid);
    }

    @SuppressLint("DefaultLocale")
    private CpuInfo readCpuInfo(int pid) throws IOException {
        String path;
        if (pid == PowerProfilesListener.NON_EXISTENT_SUMMARY_PID) {
            path = PROC_INFO_FILE_NAME;
        } else {
            path = String.format(PROC_PID_INFO_FILE_TEMPLATE, pid);
        }
        RandomAccessFile reader = new RandomAccessFile(path, "r");
        String line = reader.readLine();
        reader.close();

        String[] split = line.split("\\s+");

        long activeTimeTicks;
        long idleTimeTicks;

        if (pid == PowerProfilesListener.NON_EXISTENT_SUMMARY_PID) {
            // utime ntime stime
            activeTimeTicks = Long.parseLong(split[1]) + Long.parseLong(split[2])
                    + Long.parseLong(split[3]);
            idleTimeTicks = Long.parseLong(split[4]);
        } else {
            // utime stime cutime cstime
            activeTimeTicks = Long.parseLong(split[13]) + Long.parseLong(split[14])
                    + Long.parseLong(split[15]) + Long.parseLong(split[16]);
            long processStartTimeTicks = Long.parseLong(split[21]);

            long systemUptimeMillis = SystemClock.uptimeMillis();
            long processUptimeTicks = (systemUptimeMillis / MILLIS_PER_CLOCK_TICK)
                    - processStartTimeTicks;
            idleTimeTicks = processUptimeTicks - activeTimeTicks;
        }

        return new CpuInfo(activeTimeTicks, idleTimeTicks);
    }

    private static class CpuInfo {
        final long activeTime;
        final long idleTime;

        CpuInfo(long activeTime, long idleTime) {
            this.activeTime = activeTime;
            this.idleTime = idleTime;
        }
    }
}
