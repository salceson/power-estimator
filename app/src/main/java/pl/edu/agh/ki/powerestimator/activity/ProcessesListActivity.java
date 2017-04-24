package pl.edu.agh.ki.powerestimator.activity;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pl.edu.agh.ki.powerestimator.R;
import pl.edu.agh.ki.powerestimator.model.ProcessListItem;
import pl.edu.agh.ki.powerestimator.model.ProcessListItemAdapter;

public class ProcessesListActivity extends AppCompatActivity {

    private ActivityManager activityManager;
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> future;
    private final List<ProcessListItem> items = new CopyOnWriteArrayList<>();
    private ProcessListItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processes_list);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        ListView processesListView = (ListView) findViewById(R.id.processesListView);
        adapter = new ProcessListItemAdapter(getApplicationContext(), items);
        processesListView.setAdapter(adapter);
        processesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ProcessListItem item = items.get(position);
                final Intent intent =
                        new Intent(getApplicationContext(), ProcessInfoActivity.class);
                intent.putExtra("pid", item.getPid());
                intent.putExtra("uid", item.getUid());
                intent.putExtra("name", item.getName());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        future = service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                items.clear();
                for (RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
                    final ProcessListItem item = new ProcessListItem(
                            processInfo.uid, processInfo.pid, processInfo.processName
                    );
                    items.add(item);
                    Collections.sort(items, new Comparator<ProcessListItem>() {
                        @Override
                        public int compare(ProcessListItem o1, ProcessListItem o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
                    adapter.notifyDataSetChanged();
                }
            }
        }, 10, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        future.cancel(true);
    }
}
