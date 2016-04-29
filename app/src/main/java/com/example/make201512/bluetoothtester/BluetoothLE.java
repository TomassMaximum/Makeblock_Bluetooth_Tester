package com.example.make201512.bluetoothtester;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by make201512 on 2016/4/27.
 * 对蓝牙4.0低功耗的支持类
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLE {

    private static final String TAG = BluetoothLE.class.getSimpleName();

    Context mContext;

    final BluetoothManager mBluetoothManager;

    BluetoothAdapter mBluetoothAdapter;

    private static final long scanPeriod = 10000;

    private boolean mScanning;

    BluetoothLeScanner mBluetoothLEScanner;

    private Map<Integer,BluetoothDevice> devices;

    private int deviceIndex = 0;

    private BluetoothGatt mBluetoothGatt;

    private static BluetoothLE mBluetoothLE = null;

    private BluetoothLE(Context context){
        mContext = context;

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    public static BluetoothLE getInstance(final Context context){
        if (mBluetoothLE == null){
            mBluetoothLE = new BluetoothLE(context);
        }
        return mBluetoothLE;
    }

    public void scanLE(final boolean enable){

        devices = new HashMap<>();

        if (enable){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            },scanPeriod);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            devices.put(deviceIndex,device);
            deviceIndex++;

            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            Message message = new Message();
            message.what = Constants.UPDATE_BLUETOOTH_DEVICES_LIST;
            message.obj = deviceName + "\n" + deviceAddress;
            EventBus.getDefault().post(new MessageEvent(message));

            Log.e(TAG,deviceAddress + "的信号强度为：" + rssi);
        }
    };

    public void connect(int position){
        BluetoothDevice device = devices.get(position);
        mBluetoothGatt = device.connectGatt(mContext,true,mBluetoothGattCallback);



    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }
    };


}
