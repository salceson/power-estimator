# Power Estimator

Power Estimator application and library for Android devices.

## Application

The application is compatible with Android versions 4.4 - 6.0.

The application can show user the estimated current power consumption (in mAh) of the following
 components:

  * Screen (only consumption by the whole device),
  * CPU,
  * WiFi,
  * Mobile network,
  * Summary (sum of the above).
  
The consumption is shown both for the whole device and for any of the processes running on it.

After launching the application, a summary screen is shown to user:

![Summary screen](screenshots/summary.png?raw=true "Summary screen" =400x)

On that screen user can see the estimated power consumption of the components mentioned
 above - it contains usage from whole Android phone (all processes).
 
User can click on "Applications list" to see the applications running on the phone:

![Applications list screen](screenshots/applications-list.png?raw=true "Applications list screen" =400x)

After clicking on the application, he can see the power consumption of the CPU, WiFi,
 Mobile network of that particular application:
 
![Application screen](screenshots/application.png?raw=true "Application screen" =400x)

## Library

We provide the library that can be used if you need the estimate of the power consumption in
 your Android application.

### Usage 

Below you can find a tutorial on how to use our library.

First you need to have a `PowerProfiles` instance. You can get it with following code:

```java
import android.app.Activity;

import pl.edu.agh.ki.powerprofiles.PowerProfiles;
import pl.edu.agh.ki.powerprofiles.PowerProfilesImpl;

class MyActivity extends Activity {
    private PowerProfiles powerProfiles;
    
    // (...)
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // (Setup UI, ...)
        powerProfiles = PowerProfilesImpl.getInstance();
    }
}
```

Next, you need to create a listener that will listen to the measurements taken by our library
 and register it to the `PowerProfiles` instance created before. You can do it with the following
  code (we're assuming you want to have information about power consumption of your own
   application):

```java
import android.util.Log;
import android.os.Process;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pl.edu.agh.ki.powerprofiles.MeasurementType;
import pl.edu.agh.ki.powerprofiles.PowerProfilesListener;

class MyActivity extends Activity {
    // (As before)
    private PowerProfilesListener listener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // (Setup UI, create PowerProfiles instance, ...)
        listener = new PowerProfilesListener() {
            @Override
            List<Integer> getPids() {
                return Collections.singletonList(Process.myPid());
            }
        
            @Override
            int getUid() {
                return Process.myUid();
            }
        
            @Override
            List<MeasurementType> getMeasurementTypes() {
                return Arrays.asList(MeasurementType.CPU, MeasurementType.WIFI,
                    MeasurementType.MOBILE);
            }
        
            @Override
            void onMeasurementData(Map<MeasurementType, Float> data) {
                Log.i("Got new data:");
                Log.i("\tCPU: " + data.get(MeasurementType.CPU) + " mAh");
                Log.i("\tWifi: " + data.get(MeasurementType.WIFI) + " mAh");
                Log.i("\tMobile: " + data.get(MeasurementType.MOBILE) + " mAh");
            }
        };
        powerProfiles.addListener(listener);
    }
}
```

You can of course pass any `pids` and `uid` to the listener, but you do need to know them
 beforehand. We've designed `pids` to be a list because an application can have one UID
  and many PIDs, as it can have many processes.

Finally, you need to start the measurements:

```java

public class MyActivity extends Activity {
    // (As before)
    
    @Override
    public void onStart() {
        powerProfiles.startMeasurements();
    }
}
```

You should now see your application's power consumption measurements in the Android's log
 every 1 second.
 
### Cleaning up

When you do not need to perform measurements anymore, you can remove listener and stop
 the measurements. For example:

```java

public class MyActivity extends Activity {
    // (As before)
    
    @Override
    public void onStop() {
        powerProfiles.removeListener(listener);
        powerProfiles.stopMeasurements();
    }
}
```

### Threading

Please be advised that the `onMeasurementData` method will be called from a separate thread,
 as separate thread is used to measure power consumption.
 
Therefore, if you need to update your UI, you cannot do it in this method. You can, for example
 create an Android `Handler` to do it on the UI thread. Here is an example on how to do it:
 
```java
import android.os.Handler;

public class MyActivity extends Activity {
    // (As before)

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            final float cpuUsageMAh = data.getFloat(MeasurementType.CPU.getKey());
            final float wifiMAh = data.getFloat(MeasurementType.WIFI.getKey());
            final float mobileMAh = data.getFloat(MeasurementType.MOBILE.getKey());
            
            // Do sth with the data on UI
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // (Setup UI, create PowerProfiles instance, ...)
        listener = new PowerProfilesListener() {
            // (getPids, getUid, getMeasurementTypes methods...)
            
            @Override
            void onMeasurementData(Map<MeasurementType, Float> data) {
                final Message message = new Message();
                final Bundle messageData = new Bundle();
                messageData.putFloat(MeasurementType.CPU.getKey(), data.get(MeasurementType.CPU));
                messageData.putFloat(MeasurementType.WIFI.getKey(), data.get(MeasurementType.WIFI));
                messageData.putFloat(MeasurementType.MOBILE.getKey(), data.get(MeasurementType.MOBILE));
                message.setData(messageData);
                handler.sendMessage(message);
            }
        };
    }
    
    // (As before)
}
```
### Measurements of the whole Android device power consumption

If you want to collect the data of the whole Android device power consumption, simply put
 `NON_EXISTENT_SUMMARY_PIDS` constant as the result of the `getPid` method and
  `NON_EXISTENT_SUMMARY_UID` as result of the `getUid` method.
 
These constants are defined in `PowerProfilesListener` interface, therefore you do not need to
 import them from anywhere else.
