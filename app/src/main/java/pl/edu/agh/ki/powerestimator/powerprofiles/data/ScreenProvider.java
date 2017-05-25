package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import android.content.Context;
import android.os.PowerManager;

import java.util.Collections;
import java.util.List;

import pl.edu.agh.ki.powerestimator.powerprofiles.MeasurementType;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.getInt;

public class ScreenProvider implements DataProvider {
    private static final int MAX_BRIGHTNESS = 255;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final double SCALE = 1.0;

    private final PowerProfilesObject powerProfilesObject;
    private float measurement = Float.NaN;
    private final PowerManager powerManager;
    private final Context context;

    public ScreenProvider(PowerProfilesObject powerProfilesObject, Context context) {
        this.powerProfilesObject = powerProfilesObject;
        this.context = context;
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }


    @Override
    public List<MeasurementType> getProvidedMeasurementTypes() {
        return Collections.singletonList(MeasurementType.SCREEN);
    }

    @Override
    public void takeMeasurements(int pid, int uid) throws Exception {
        double screenDrainMAh = 0;
        if (powerManager.isScreenOn()) {
            float brightness = getInt(context.getContentResolver(), SCREEN_BRIGHTNESS);
            screenDrainMAh += (powerProfilesObject.getAveragePower("screen.on") +
                    (brightness / MAX_BRIGHTNESS * powerProfilesObject
                            .getAveragePower("screen.full"))) / SECONDS_PER_HOUR * SCALE;
        }
        measurement = (float) screenDrainMAh;
    }

    @Override
    public float getMeasurement(MeasurementType measurementType,
                                int pid, int uid) throws Exception {
        if (measurementType != MeasurementType.SCREEN) {
            return Float.NaN;
        }
        return measurement;
    }

    @Override
    public void listenerAdded(int pid, int uid) throws Exception {
    }

    @Override
    public void listenerRemoved(int pid, int uid) throws Exception {
    }
}
