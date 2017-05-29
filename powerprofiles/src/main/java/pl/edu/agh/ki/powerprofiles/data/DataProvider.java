package pl.edu.agh.ki.powerprofiles.data;

import java.util.List;

import pl.edu.agh.ki.powerprofiles.MeasurementType;

public interface DataProvider {
    List<MeasurementType> getProvidedMeasurementTypes();

    void takeMeasurements(int pid, int uid) throws Exception;

    float getMeasurement(MeasurementType measurementType, int pid, int uid) throws Exception;

    void onListenerAdded(int pid, int uid) throws Exception;

    void onListenerRemoved(int pid, int uid) throws Exception;
}
