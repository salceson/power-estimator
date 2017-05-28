package pl.edu.agh.ki.powerestimator.powerprofiles;

import java.util.List;
import java.util.Map;

public interface PowerProfilesListener {
    int getPid();

    int getUid();

    List<MeasurementType> getMeasurementTypes();

    void onNewData(Map<MeasurementType, Float> data);
}
