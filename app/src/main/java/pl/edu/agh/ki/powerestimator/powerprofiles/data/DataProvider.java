package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import java.util.List;

import pl.edu.agh.ki.powerestimator.powerprofiles.MeasurementType;

public interface DataProvider {
    List<MeasurementType> getProvidedMeasurementTypes();

    void takeMeasurements(int pid, int uid) throws Exception;

    float getMeasurement(MeasurementType measurementType, int pid, int uid) throws Exception;

    void onListenerAdded(int pid, int uid, boolean summary) throws Exception;

    void onListenerRemoved(int pid, int uid) throws Exception;
}
