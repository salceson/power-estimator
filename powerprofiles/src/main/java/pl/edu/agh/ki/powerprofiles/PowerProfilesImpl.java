package pl.edu.agh.ki.powerprofiles;

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

import pl.edu.agh.ki.powerprofiles.data.CpuInfoProvider;
import pl.edu.agh.ki.powerprofiles.data.DataProvider;
import pl.edu.agh.ki.powerprofiles.data.PowerProfileObject;
import pl.edu.agh.ki.powerprofiles.data.ScreenProvider;
import pl.edu.agh.ki.powerprofiles.data.TransferDataProvider;

public class PowerProfilesImpl implements PowerProfiles {
    private static final String LOG_TAG = "PPImpl";

    private static PowerProfiles instance;

    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor();

    private final List<PowerProfilesListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<MeasurementType, DataProvider> providerMap = new HashMap<>();
    private final List<DataProvider> providers = new ArrayList<>();

    private ScheduledFuture<?> future;

    private PowerProfilesImpl(Context context) throws Exception {
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

    public static PowerProfiles getInstance(Context context) throws Exception {
        if (instance == null) {
            instance = new PowerProfilesImpl(context);
        }
        return instance;
    }

    @Override
    public void startMeasurements() {
        try {
            future = executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (PowerProfilesListener listener : listeners) {
                        final List<Integer> pids = listener.getPids();
                        final int uid = listener.getUid();

                        final Map<MeasurementType, Float> measurements = new HashMap<>();
                        final List<DataProvider> providersWithMeasurementsTaken = new ArrayList<>();

                        for (MeasurementType type : listener.getMeasurementTypes()) {
                            try {
                                final DataProvider provider = providerMap.get(type);
                                if (!providersWithMeasurementsTaken.contains(provider)) {
                                    provider.takeMeasurements(pids, uid);
                                    providersWithMeasurementsTaken.add(provider);
                                }
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Error while taking measurements of type "
                                        + type.name(), e);
                            }

                            float measurement = Float.NaN;
                            try {
                                measurement = providerMap.get(type).getMeasurement(type, pids, uid);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Error while getting measurements of type "
                                        + type.name(), e);
                            }
                            measurements.put(type, measurement);
                        }
                        listener.onMeasurementData(measurements);
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
            provider.onListenerAdded(listener.getPids(), listener.getUid());
        }
    }

    @Override
    public void removeListener(PowerProfilesListener listener) throws Exception {
        listeners.remove(listener);
        for (DataProvider provider : providers) {
            provider.onListenerRemoved(listener.getPids(), listener.getUid());
        }
    }
}
