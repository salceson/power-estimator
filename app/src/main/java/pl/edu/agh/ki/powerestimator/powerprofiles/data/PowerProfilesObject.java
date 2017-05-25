package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import android.content.Context;

public class PowerProfilesObject {
    private final static String POWER_PROFILE_CLASS_NAME = "com.android.internal.os.PowerProfile";

    private final Object powerProfilesInstance;

    public PowerProfilesObject(Context context) throws Exception {
        this.powerProfilesInstance = Class.forName(POWER_PROFILE_CLASS_NAME)
                .getConstructor(Context.class)
                .newInstance(context.getApplicationContext());
    }

    double getAveragePower(String componentState) throws Exception {
        return invokePowerProfileMethod("getAveragePower", Double.class,
                new Class[]{String.class}, new Object[]{componentState});
    }

    // QUESTION: Can it be removed? Does not seem to be used anywhere.
    double getAveragePower(String componentState, int level) throws Exception {
        return invokePowerProfileMethod("getAveragePower", Double.class,
                new Class[]{String.class, int.class}, new Object[]{componentState, level});
    }

    private <T> T invokePowerProfileMethod(String methodName,
                                           Class<T> returnType,
                                           Class<?>[] argTypes,
                                           Object[] args)
            throws Exception {
        return returnType.cast(Class.forName(POWER_PROFILE_CLASS_NAME)
                .getMethod(methodName, argTypes)
                .invoke(powerProfilesInstance, args));
    }
}
