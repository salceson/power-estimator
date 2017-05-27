package pl.edu.agh.ki.powerestimator.powerprofiles;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pl.edu.agh.ki.powerestimator.powerprofiles.data.CpuInfoProvider;
import pl.edu.agh.ki.powerestimator.powerprofiles.data.DataProvider;
import pl.edu.agh.ki.powerestimator.powerprofiles.data.PowerProfileObject;
import pl.edu.agh.ki.powerestimator.powerprofiles.data.ScreenProvider;
import pl.edu.agh.ki.powerestimator.powerprofiles.data.TransferDataProvider;

public class PowerProfilesImpl implements PowerProfiles {
    private static final String LOG_TAG = "PPImpl";

    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor();

    private final List<PowerProfilesListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<MeasurementType, DataProvider> providerMap = new HashMap<>();
    private final List<DataProvider> providers = new ArrayList<>();

    private ScheduledFuture<?> future;

    public PowerProfilesImpl(Context context) throws Exception {
        PowerProfileObject powerProfileObject = new PowerProfileObject(context);

        providers.add(new CpuInfoProvider(powerProfileObject));
        providers.add(new TransferDataProvider(powerProfileObject, context));
        providers.add(new ScreenProvider(powerProfileObject, context));

        for (DataProvider provider : providers) {
            for (MeasurementType type : provider.getProvidedMeasurementTypes()) {
                providerMap.put(type, provider);
            }
        }
    }

    @Override
    public void startMeasurements() {
        try {
            future = executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (PowerProfilesListener listener : listeners) {
                        int pid = listener.getPid();
                        int uid = listener.getUid();
                        Map<MeasurementType, Float> measurements = new HashMap<>();
                        List<DataProvider> providersWithMeasurementsTaken = new ArrayList<>();
                        for (MeasurementType type : listener.getMeasurementTypes()) {
                            try {
                                DataProvider provider = providerMap.get(type);
                                if (!providersWithMeasurementsTaken.contains(provider)) {
                                    provider.takeMeasurements(pid, uid);
                                    providersWithMeasurementsTaken.add(provider);
                                }
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Error while taking measurements of type "
                                        + type.name(), e);
                            }
                        }
                        for (MeasurementType type : listener.getMeasurementTypes()) {
                            float measurement = Float.NaN;
                            try {
                                measurement = providerMap.get(type).getMeasurement(type, pid, uid);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Error while getting measurements of type "
                                        + type.name(), e);
                            }
                            measurements.put(type, measurement);
                        }
                        listener.onNewData(measurements);
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
    public void addListener(PowerProfilesListener listener) throws Exception {
        listeners.add(listener);
        for (DataProvider provider : providers) {
            provider.onListenerAdded(listener.getPid(), listener.getUid());
        }
    }

    @Override
    public void removeListener(PowerProfilesListener listener) throws Exception {
        listeners.remove(listener);
        for (DataProvider provider : providers) {
            provider.onListenerRemoved(listener.getPid(), listener.getUid());
        }
    }
}
