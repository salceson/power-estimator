package pl.edu.agh.ki.powerestimator.powerprofiles;

public interface PowerProfilesListener {
    void onNewData(float lcdUsageMAh, float cpuUsageMAh, float wifiTransferUsageMAh,
                   float mobileTransferUsageMAh);
}
