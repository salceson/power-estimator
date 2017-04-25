package pl.edu.agh.ki.powerestimator.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import pl.edu.agh.ki.powerestimator.R;

public class ProcessListItemAdapter extends BaseAdapter {
    private List<ProcessListItem> data = new CopyOnWriteArrayList<>();
    private final LayoutInflater layoutInflater;

    public ProcessListItemAdapter(Context context) {
        layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.process_row, null);
        }

        final ProcessListItem item = data.get(position);

        final TextView name = (TextView) view.findViewById(R.id.rowProcessName);
        name.setText(item.getName());

        final TextView pid = (TextView) view.findViewById(R.id.rowProcessPid);
        pid.setText(Integer.toString(item.getPid(), 10));

        final TextView uid = (TextView) view.findViewById(R.id.rowProcessUid);
        uid.setText(Integer.toString(item.getUid(), 10));

        return view;
    }

    public void changeProcesses(List<ProcessListItem> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }
}
