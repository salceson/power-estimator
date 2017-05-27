package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import android.content.Context;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pl.edu.agh.ki.powerestimator.powerprofiles.MeasurementType;

public class TransferDataProvider implements DataProvider {
    private static final int SECONDS_PER_HOUR = 3600;

    private final PowerProfileObject powerProfileObject;
    private final Map<Integer, TransferInfo> previousTransferInfos = new ConcurrentHashMap<>();
    private final Map<Integer, Map<MeasurementType, Float>> measurements =
            new ConcurrentHashMap<>();
    private final WifiManager wifi;

    public TransferDataProvider(PowerProfileObject powerProfileObject, Context context) {
        this.powerProfileObject = powerProfileObject;
        this.wifi = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public List<MeasurementType> getProvidedMeasurementTypes() {
        return Arrays.asList(MeasurementType.MOBILE, MeasurementType.WIFI);
    }

    @Override
    public void takeMeasurements(int pid, int uid) throws Exception {
        TransferInfo nextTransferInfo = new TransferInfo(uid);
        TransferInfo previousTransferInfo = previousTransferInfos.get(uid);
        previousTransferInfos.put(uid, nextTransferInfo);

        double mobileDrainMAh = 0;
        double wifiDrainMAh = 0;

        if (previousTransferInfo.wasWifiReceiving(nextTransferInfo) ||
                previousTransferInfo.wasWifiTransmitting(nextTransferInfo)) {
            wifiDrainMAh += powerProfileObject.getAveragePower("wifi.active") / SECONDS_PER_HOUR;
        }

        if (previousTransferInfo.wasMobileReceiving(nextTransferInfo)
                || previousTransferInfo.wasMobileTransmitting(nextTransferInfo)) {
            mobileDrainMAh +=
                    powerProfileObject.getAveragePower("radio.active") / SECONDS_PER_HOUR;
        }

        Map<MeasurementType, Float> newMeasurementsForUid = new HashMap<>();
        newMeasurementsForUid.put(MeasurementType.MOBILE, (float) mobileDrainMAh);
        newMeasurementsForUid.put(MeasurementType.WIFI, (float) wifiDrainMAh);
        measurements.put(uid, newMeasurementsForUid);
    }

    @Override
    public float getMeasurement(MeasurementType measurementType,
                                int pid, int uid) throws Exception {
        switch (measurementType) {
            case MOBILE:
            case WIFI:
                return measurements.get(uid).get(measurementType);
            default:
                return Float.NaN;
        }
    }

    @Override
    public void onListenerAdded(int pid, int uid) throws Exception {
        previousTransferInfos.put(uid, new TransferInfo(uid));
    }

    @Override
    public void onListenerRemoved(int pid, int uid) throws Exception {
        previousTransferInfos.remove(uid);
    }

    private class TransferInfo {
        long wifiRxBytes;
        long wifiTxBytes;
        long mobileRxBytes;
        long mobileTxBytes;

        TransferInfo(int uid) {
            if (wifi.isWifiEnabled()) {
                wifiRxBytes = TrafficStats.getUidRxBytes(uid);
                wifiTxBytes = TrafficStats.getUidTxBytes(uid);
            } else {
                mobileRxBytes = TrafficStats.getUidRxBytes(uid);
                mobileTxBytes = TrafficStats.getUidTxBytes(uid);
            }
        }

        boolean wasWifiReceiving(TransferInfo nextInfo) {
            return nextInfo.wifiRxBytes - wifiRxBytes > 0;
        }

        boolean wasWifiTransmitting(TransferInfo nextInfo) {
            return nextInfo.wifiTxBytes - wifiTxBytes > 0;
        }

        boolean wasMobileReceiving(TransferInfo nextInfo) {
            return nextInfo.mobileRxBytes - mobileRxBytes > 0;
        }

        boolean wasMobileTransmitting(TransferInfo nextInfo) {
            return nextInfo.mobileTxBytes - mobileTxBytes > 0;
        }
    }
}
