package pl.edu.agh.ki.powerestimator.model;

public class ProcessListItem {
    private final int uid;
    private final int pid;
    private final String name;

    public ProcessListItem(int uid, int pid, String name) {
        this.uid = uid;
        this.pid = pid;
        this.name = name;
    }

    public int getUid() {
        return uid;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }
}
