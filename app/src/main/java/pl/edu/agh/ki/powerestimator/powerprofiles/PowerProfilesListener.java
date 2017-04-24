package pl.edu.agh.ki.powerestimator.powerprofiles;

public interface PowerProfilesListener {
    int getPid();

    int getUid();

    void onNewData(float cpuUsageMAh, float wifiTransferUsageMAh, float mobileTransferUsageMAh);
}
