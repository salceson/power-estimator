package pl.edu.agh.ki.powerestimator.powerprofiles.data;

import java.util.List;

import pl.edu.agh.ki.powerestimator.powerprofiles.MeasurementType;

public interface DataProvider {
    List<MeasurementType> getProvidedMeasurementTypes();

    void takeMeasurements(int pid, int uid) throws Exception;

    float getMeasurement(MeasurementType measurementType, int pid, int uid) throws Exception;

    void listenerAdded(int pid, int uid) throws Exception;

    void listenerRemoved(int pid, int uid) throws Exception;
}
