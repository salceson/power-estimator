package pl.edu.agh.ki.powerprofiles;

public enum MeasurementType {
    CPU("cpu"),
    WIFI("wifi"),
    MOBILE("mobile"),
    SCREEN("screen");

    private final String key;

    MeasurementType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
