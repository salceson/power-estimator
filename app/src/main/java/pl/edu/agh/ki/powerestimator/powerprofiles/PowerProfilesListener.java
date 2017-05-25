package pl.edu.agh.ki.powerestimator.powerprofiles;

public interface PowerProfilesListener {
    int getPid();

    int getUid();

    void onNewData(float lcdUsageMAh, float cpuUsageMAh, float wifiTransferUsageMAh,
                   float mobileTransferUsageMAh);
}
