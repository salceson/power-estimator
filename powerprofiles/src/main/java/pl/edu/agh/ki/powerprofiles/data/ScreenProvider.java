package pl.edu.agh.ki.powerprofiles.data;

import android.content.Context;
import android.os.PowerManager;

import java.util.Collections;
import java.util.List;

import pl.edu.agh.ki.powerprofiles.MeasurementType;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.getInt;

public class ScreenProvider implements DataProvider {
    private static final int MAX_BRIGHTNESS = 255;
    private static final int SECONDS_PER_HOUR = 3600;

    private final PowerProfileObject powerProfileObject;
    private float measurement = Float.NaN;
    private final PowerManager powerManager;
    private final Context context;

    public ScreenProvider(PowerProfileObject powerProfileObject, Context context) {
        this.powerProfileObject = powerProfileObject;
        this.context = context;
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }


    @Override
    public List<MeasurementType> getProvidedMeasurementTypes() {
        return Collections.singletonList(MeasurementType.SCREEN);
    }

    @Override
    public void takeMeasurements(List<Integer> pids, int uid) throws Exception {
        double screenDrainMAh = 0;
        if (powerManager.isScreenOn()) {
            float brightness = getInt(context.getContentResolver(), SCREEN_BRIGHTNESS);
            screenDrainMAh += (powerProfileObject.getAveragePower("screen.on") +
                    (brightness / MAX_BRIGHTNESS * powerProfileObject
                            .getAveragePower("screen.full"))) / SECONDS_PER_HOUR;
        }
        measurement = (float) screenDrainMAh;
    }

    @Override
    public float getMeasurement(MeasurementType measurementType,
                                List<Integer> pids, int uid) throws Exception {
        if (measurementType != MeasurementType.SCREEN) {
            throw new IllegalArgumentException(
                    "ScreenProvider provides only SCREEN measurements, not of type: "
                            + measurementType.name()
            );
        }
        return measurement;
    }

    @Override
    public void onListenerAdded(List<Integer> pids, int uid) throws Exception {
    }

    @Override
    public void onListenerRemoved(List<Integer> pids, int uid) throws Exception {
    }
}
