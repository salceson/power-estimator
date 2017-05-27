package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import android.os.SystemClock;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pl.edu.agh.ki.powerestimator.powerprofiles.MeasurementType;

public class CpuInfoProvider implements DataProvider {
    // _SC_CLK_TCK
    private static final int CLOCK_TICKS_PER_SECOND = 100;
    private static final int MILLIS_PER_CLOCK_TICK = 1000 / CLOCK_TICKS_PER_SECOND;
    private static final int SECONDS_PER_HOUR = 3600;

    private final PowerProfileObject powerProfileObject;
    private Map<Integer, CpuInfo> previousCpuInfos = new ConcurrentHashMap<>();
    private Map<Integer, Float> measurements = new ConcurrentHashMap<>();

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
        int ticksPerHour = CLOCK_TICKS_PER_SECOND * SECONDS_PER_HOUR;
        double cpuDrainMAh = (
                powerProfileObject.getAveragePower("cpu.idle") * cpuIdleTime / ticksPerHour)
                + (powerProfileObject.getAveragePower("cpu.active") * cpuActiveTime / ticksPerHour)
                + (powerProfileObject.getAveragePower("cpu.idle") * cpuActiveTime / ticksPerHour);
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
    public void onListenerAdded(int pid, int uid) throws Exception {
        previousCpuInfos.put(pid, readCpuInfo(pid));
    }

    @Override
    public void onListenerRemoved(int pid, int uid) throws Exception {
        previousCpuInfos.remove(pid);
    }

    private CpuInfo readCpuInfo(int pid) throws IOException {
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
}
