package pl.edu.agh.ki.powerprofiles.data;

import java.util.List;

import pl.edu.agh.ki.powerprofiles.MeasurementType;

public interface DataProvider {
    List<MeasurementType> getProvidedMeasurementTypes();

    void takeMeasurements(List<Integer> pids, int uid) throws Exception;

    float getMeasurement(MeasurementType measurementType, List<Integer> pids, int uid) throws Exception;

    void onListenerAdded(List<Integer> pids, int uid) throws Exception;

    void onListenerRemoved(List<Integer> pids, int uid) throws Exception;
}
