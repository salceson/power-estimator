package pl.edu.agh.ki.powerestimator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pl.edu.agh.ki.powerestimator.R;
import pl.edu.agh.ki.powerestimator.model.ProcessListItem;
import pl.edu.agh.ki.powerestimator.model.ProcessListItemAdapter;

public class ProcessesListActivity extends AppCompatActivity {
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> future;
    private ProcessListItemAdapter adapter;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            List<ProcessListItem> newProcesses = msg.getData().getParcelableArrayList("processes");
            adapter.changeProcesses(newProcesses);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processes_list);

        ListView processesListView = (ListView) findViewById(R.id.processesListView);
        adapter = new ProcessListItemAdapter(getApplicationContext());
        processesListView.setAdapter(adapter);
        processesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ProcessListItem item = (ProcessListItem) adapter.getItem(position);
                final Intent intent =
                        new Intent(ProcessesListActivity.this, ProcessInfoActivity.class);
                intent.putExtra("pid", item.getPid());
                intent.putExtra("uid", item.getUid());
                intent.putExtra("name", item.getName());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
        future = service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                ArrayList<ProcessListItem> newProcesses = new ArrayList<>();
                final List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
                for (AndroidAppProcess process : processes) {
                    final ProcessListItem item = new ProcessListItem(
                            process.uid, process.pid, process.name
                    );
                    newProcesses.add(item);
                }
                Collections.sort(newProcesses, new Comparator<ProcessListItem>() {
                    @Override
                    public int compare(ProcessListItem o1, ProcessListItem o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                Message message = new Message();
                Bundle data = new Bundle();
                data.putParcelableArrayList("processes", newProcesses);
                message.setData(data);
                handler.sendMessage(message);
            }
        }, 100, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        future.cancel(true);
        future = null;
    }
}
