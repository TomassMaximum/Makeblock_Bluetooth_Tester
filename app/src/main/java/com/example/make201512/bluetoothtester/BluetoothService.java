package com.example.make201512.bluetoothtester;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by tom on 16/7/13.
 */
public class BluetoothService extends Service {

    public static final String TAG = BluetoothService.class.getSimpleName();
    private BroadcastReceiver broadcastReceiver;

    public BluetoothService(){}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"BluetoothService onCreate() is called");
        EventBus.getDefault().register(this);
        registerReceiver();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /*---------------------注册/注销 广播接收者------------------------*/
    public void registerReceiver() {
        if (broadcastReceiver != null) {
            return;
        }
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)  {
                BluetoothClassic.getInstance().onReceive(intent);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(broadcastReceiver, filter);
        Log.e(TAG, "registerReceiver");
    }

    public void unregisterReceiver() {
        Log.e(TAG, "unregisterReceiver");
        if (broadcastReceiver == null) {
            return;
        }
        unregisterReceiver(broadcastReceiver);
        BluetoothClassic.getInstance().disconnectBluetooth();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBluetoothSearchEvent(MessageEvent event) {
        switch (event.message.what) {
            case Constants.DEVICE_CONNECTED:
                Log.e(TAG,"##连接成功");
                break;
            case Constants.DEVICE_DISCONNECTED:
                Log.e(TAG, "##连接断开");
                break;
        }
    }
}
