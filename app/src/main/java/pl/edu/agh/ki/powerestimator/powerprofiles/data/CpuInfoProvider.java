package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import android.os.SystemClock;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pl.edu.agh.ki.powerestimator.activity.SummaryActivity;
import pl.edu.agh.ki.powerestimator.powerprofiles.MeasurementType;

public class CpuInfoProvider implements DataProvider {
    // _SC_CLK_TCK
    private static final int CLOCK_TICKS_PER_SECOND = 100;
    private static final int MILLIS_PER_CLOCK_TICK = 1000 / CLOCK_TICKS_PER_SECOND;
    private static final int SECONDS_PER_HOUR = 3600;

    private final PowerProfilesObject powerProfilesObject;
    private Map<Integer, CpuInfo> previousCpuInfos = new ConcurrentHashMap<>();
    private Map<Integer, Float> measurements = new ConcurrentHashMap<>();

    public CpuInfoProvider(PowerProfilesObject powerProfilesObject) {
        this.powerProfilesObject = powerProfilesObject;
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
        int ticksPerHour = CLOCK_TICKS_PER_SECOND * SECONDS_PER_HOUR;
        double cpuDrainMAh = (
                powerProfilesObject.getAveragePower("cpu.idle") * cpuIdleTime / ticksPerHour)
                + (powerProfilesObject.getAveragePower("cpu.active") * cpuActiveTime / ticksPerHour)
                + (powerProfilesObject.getAveragePower("cpu.idle") * cpuActiveTime / ticksPerHour);
        measurements.put(pid, (float) cpuDrainMAh);
    }

    @Override
    public float getMeasurement(MeasurementType measurementType,
                                int pid, int uid) throws Exception {
        if (measurementType != MeasurementType.CPU) {
            return Float.NaN;
        }
        return measurements.get(pid);
    }

    @Override
    public void listenerAdded(int pid, int uid) throws Exception {
        previousCpuInfos.put(pid, readCpuInfo(pid));
    }

    @Override
    public void listenerRemoved(int pid, int uid) throws Exception {
        previousCpuInfos.remove(pid);
    }

    private CpuInfo readCpuInfo(int pid) throws IOException {
        String path;
        if (pid == SummaryActivity.SUMMARY_PID) {
            path = "/proc/stat";
        } else {
            path = "/proc/" + pid + "/stat";
        }
        RandomAccessFile reader = new RandomAccessFile(path, "r");
        String line = reader.readLine();
        reader.close();

        String[] split = line.split("\\s+");

        long activeTimeTicks;
        long idleTimeTicks = 0;

        if (pid == SummaryActivity.SUMMARY_PID) {
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
        long activeTime;
        long idleTime;

        CpuInfo(long activeTime, long idleTime) {
            this.activeTime = activeTime;
            this.idleTime = idleTime;
        }
    }
}
