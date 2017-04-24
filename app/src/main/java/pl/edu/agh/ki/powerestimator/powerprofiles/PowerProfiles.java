package pl.edu.agh.ki.powerestimator.powerprofiles;

public interface PowerProfiles {
    void startMeasurements();

    void stopMeasurements();

    void addListener(PowerProfilesListener listener);

    void removeListener(PowerProfilesListener listener);
}
