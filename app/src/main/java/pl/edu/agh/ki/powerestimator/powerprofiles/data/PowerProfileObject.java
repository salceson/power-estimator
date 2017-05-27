package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import android.content.Context;

public class PowerProfileObject {
    private final static String POWER_PROFILE_CLASS_NAME = "com.android.internal.os.PowerProfile";
    private final Class<?> powerProfileClass = Class.forName(POWER_PROFILE_CLASS_NAME);

    private final Object powerProfilesInstance;

    public PowerProfileObject(Context context) throws Exception {
        this.powerProfilesInstance = powerProfileClass
                .getConstructor(Context.class)
                .newInstance(context.getApplicationContext());
    }

    double getAveragePower(String componentState) throws Exception {
        return (double) powerProfileClass
                .getMethod("getAveragePower", String.class)
                .invoke(powerProfilesInstance, componentState);
    }
}
