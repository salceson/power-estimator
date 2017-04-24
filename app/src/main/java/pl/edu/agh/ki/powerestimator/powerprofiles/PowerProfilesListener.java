package pl.edu.agh.ki.powerestimator.powerprofiles;

public interface PowerProfilesListener {
    void onNewData(float cpuUsageMAh, float wifiTransferUsageMAh, float mobileTransferUsageMAh);
}
