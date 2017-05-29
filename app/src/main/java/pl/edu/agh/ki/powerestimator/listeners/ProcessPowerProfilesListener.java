package pl.edu.agh.ki.powerestimator.listeners;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pl.edu.agh.ki.powerprofiles.MeasurementType;
import pl.edu.agh.ki.powerprofiles.PowerProfilesListener;

public class ProcessPowerProfilesListener implements PowerProfilesListener {

    private final int pid;
    private final int uid;
    private final Handler measurementDataHandler;

    public ProcessPowerProfilesListener(int pid, int uid, Handler measurementDataHandler) {
        this.pid = pid;
        this.uid = uid;
        this.measurementDataHandler = measurementDataHandler;
    }

    @Override
    public int getPid() {
        return pid;
    }

    @Override
    public int getUid() {
        return uid;
    }

    @Override
    public List<MeasurementType> getMeasurementTypes() {
        return Arrays.asList(MeasurementType.CPU, MeasurementType.MOBILE, MeasurementType.WIFI);
    }

    @Override
    public void onMeasurementData(Map<MeasurementType, Float> data) {
        final Message message = new Message();
        final Bundle messageData = new Bundle();
        messageData.putFloat(MeasurementType.CPU.getKey(), data.get(MeasurementType.CPU));
        messageData.putFloat(MeasurementType.WIFI.getKey(), data.get(MeasurementType.WIFI));
        messageData.putFloat(MeasurementType.MOBILE.getKey(), data.get(MeasurementType.MOBILE));
        message.setData(messageData);
        measurementDataHandler.sendMessage(message);
    }
}
