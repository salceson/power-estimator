package pl.edu.agh.ki.powerestimator.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pl.edu.agh.ki.powerestimator.R;
import pl.edu.agh.ki.powerestimator.model.ProcessListItem;
import pl.edu.agh.ki.powerestimator.model.ProcessListItemAdapter;

public class ProcessesListActivity extends AppCompatActivity {

    private static final String LOG_TAG = ProcessesListActivity.class.getSimpleName();

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
                intent.putExtra(ProcessInfoActivity.UID_KEY, item.getUid());
                intent.putExtra(ProcessInfoActivity.NAME_KEY, item.getName());
                intent.putIntegerArrayListExtra(ProcessInfoActivity.PIDS_KEY, Lists.newArrayList(item.getPids()));
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
                final Multimap<AppInfo, Integer> appProcesses = TreeMultimap.create(new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo o1, AppInfo o2) {
                        return o1.name.compareTo(o2.name);
                    }
                }, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        return o1.compareTo(o2);
                    }
                });

                final List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
                for (AndroidAppProcess process : processes) {
                    String processName;
                    try {
                        processName = process.getPackageInfo(getApplicationContext(), 0).applicationInfo.loadLabel(getPackageManager()).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(LOG_TAG, e.getMessage());
                        processName = process.getPackageName();
                    }

                    final AppInfo appInfo = new AppInfo(process.uid, processName);
                    appProcesses.put(appInfo, process.pid);
                }

                final ArrayList<ProcessListItem> newProcesses = new ArrayList<>();
                for (Map.Entry<AppInfo, Collection<Integer>> appInfoCollectionEntry : appProcesses.asMap().entrySet()) {
                    final AppInfo appInfo = appInfoCollectionEntry.getKey();
                    final List<Integer> pids = Lists.newArrayList(appInfoCollectionEntry.getValue());
                    final ProcessListItem item = new ProcessListItem(
                            appInfo.uid, pids, appInfo.name
                    );
                    newProcesses.add(item);
                }
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

    private static class AppInfo {
        final int uid;
        final String name;

        private AppInfo(int uid, String name) {
            this.uid = uid;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AppInfo appInfo = (AppInfo) o;
            return uid == appInfo.uid &&
                    Objects.equal(name, appInfo.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(uid, name);
        }
    }
}
