package pl.edu.agh.ki.powerprofiles;

public interface PowerProfiles {
    void startMeasurements();

    void stopMeasurements();

    void addListener(PowerProfilesListener listener) throws Exception;

    void removeListener(PowerProfilesListener listener) throws Exception;
}
