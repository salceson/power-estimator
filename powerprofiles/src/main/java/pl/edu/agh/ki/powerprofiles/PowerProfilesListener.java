package pl.edu.agh.ki.powerprofiles;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface PowerProfilesListener {
    int NON_EXISTENT_SUMMARY_UID = -1;
    int NON_EXISTENT_SUMMARY_PID = -1;
    List<Integer> NON_EXISTENT_SUMMARY_PIDS = Collections.singletonList(NON_EXISTENT_SUMMARY_PID);

    List<Integer> getPids();

    int getUid();

    List<MeasurementType> getMeasurementTypes();

    void onMeasurementData(Map<MeasurementType, Float> data);
}
