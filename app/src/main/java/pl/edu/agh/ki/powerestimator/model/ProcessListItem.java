package pl.edu.agh.ki.powerestimator.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ProcessListItem implements Parcelable {
    private final int uid;
    private final int pid;
    private final String name;

    public static final Parcelable.Creator<ProcessListItem> CREATOR =
            new Creator<ProcessListItem>() {
                @Override
                public ProcessListItem createFromParcel(Parcel source) {
                    int uid = source.readInt();
                    int pid = source.readInt();
                    String name = source.readString();
                    return new ProcessListItem(uid, pid, name);
                }

                @Override
                public ProcessListItem[] newArray(int size) {
                    return new ProcessListItem[size];
                }
            };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(uid);
        dest.writeInt(pid);
        dest.writeString(name);
    }
}
