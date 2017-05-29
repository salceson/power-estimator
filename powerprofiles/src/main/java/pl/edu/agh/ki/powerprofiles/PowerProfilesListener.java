package pl.edu.agh.ki.powerprofiles;

import java.util.List;
import java.util.Map;

public interface PowerProfilesListener {
    int NON_EXISTENT_SUMMARY_PID = -1;

    int getPid();

    int getUid();

    List<MeasurementType> getMeasurementTypes();

    void onMeasurementData(Map<MeasurementType, Float> data);
}
