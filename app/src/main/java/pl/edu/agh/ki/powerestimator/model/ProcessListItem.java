package pl.edu.agh.ki.powerestimator.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.primitives.Ints;

import java.util.List;

public class ProcessListItem implements Parcelable {
    private final int uid;
    private final List<Integer> pids;
    private final String name;

    public static final Parcelable.Creator<ProcessListItem> CREATOR =
            new Creator<ProcessListItem>() {
                @Override
                public ProcessListItem createFromParcel(Parcel source) {
                    int uid = source.readInt();
                    int pidCount = source.readInt();
                    int[] pids = new int[pidCount];
                    source.readIntArray(pids);
                    String name = source.readString();
                    return new ProcessListItem(uid, Ints.asList(pids), name);
                }

                @Override
                public ProcessListItem[] newArray(int size) {
                    return new ProcessListItem[size];
                }
            };

    public ProcessListItem(int uid, List<Integer> pids, String name) {
        this.uid = uid;
        this.pids = pids;
        this.name = name;
    }

    public int getUid() {
        return uid;
    }

    public List<Integer> getPids() {
        return pids;
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
        dest.writeInt(pids.size());
        dest.writeIntArray(Ints.toArray(pids));
        dest.writeString(name);
    }
}
