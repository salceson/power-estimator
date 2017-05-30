package pl.edu.agh.ki.powerestimator.listeners;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import pl.edu.agh.ki.powerprofiles.MeasurementType;
import pl.edu.agh.ki.powerprofiles.PowerProfilesListener;

public class SummaryPowerProfilesListener implements PowerProfilesListener {

    private static final List<Integer> SUMMARY_PIDS = Collections.singletonList(NON_EXISTENT_SUMMARY_PID);

    private final Handler measurementDataHandler;

    public SummaryPowerProfilesListener(Handler measurementDataHandler) {
        this.measurementDataHandler = measurementDataHandler;
    }

    @Override
    public List<Integer> getPids() {
        return SUMMARY_PIDS;
    }

    @Override
    public int getUid() {
        return NON_EXISTENT_SUMMARY_PID;
    }

    @Override
    public List<MeasurementType> getMeasurementTypes() {
        return Arrays.asList(MeasurementType.CPU, MeasurementType.MOBILE,
                MeasurementType.WIFI, MeasurementType.SCREEN);
    }

    @Override
    public void onMeasurementData(Map<MeasurementType, Float> data) {
        final Message message = new Message();
        final Bundle messageData = new Bundle();
        messageData.putFloat(MeasurementType.SCREEN.getKey(), data.get(MeasurementType.SCREEN));
        messageData.putFloat(MeasurementType.CPU.getKey(), data.get(MeasurementType.CPU));
        messageData.putFloat(MeasurementType.WIFI.getKey(), data.get(MeasurementType.WIFI));
        messageData.putFloat(MeasurementType.MOBILE.getKey(), data.get(MeasurementType.MOBILE));
        message.setData(messageData);
        measurementDataHandler.sendMessage(message);
    }
}
