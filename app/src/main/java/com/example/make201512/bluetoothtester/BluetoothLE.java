package com.example.make201512.bluetoothtester;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private SendPackagesThread mSendPackagesThread;

    private BluetoothLE(Context context){
        mContext = context;

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        devices = new HashMap<>();
    }

    public static BluetoothLE getInstance(final Context context){
        if (mBluetoothLE == null){
            mBluetoothLE = new BluetoothLE(context);
        }
        return mBluetoothLE;
    }

    public void scanLE(final boolean enable){

        devices.clear();
        deviceIndex = 0;

        if (enable){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    Message message = new Message();
                    message.what = Constants.SCAN_DEVICES_FINISHED;
                    EventBus.getDefault().post(new MessageEvent(message));
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
            if (!devices.containsValue(device)){
                devices.put(deviceIndex,device);
                deviceIndex++;

                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                String deviceInfo = deviceName + "\n" + deviceAddress;

                Message message = new Message();
                message.what = Constants.SCAN_DEVICE_FOUND;
                message.obj = deviceInfo;
                EventBus.getDefault().post(new MessageEvent(message));
                Log.e(TAG,deviceAddress + "的信号强度为：" + rssi);
            }
        }
    };

    public void connect(int position){
        if (mScanning){
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        BluetoothDevice device = devices.get(position);
        mBluetoothGatt = device.connectGatt(mContext,true,mBluetoothGattCallback);

        Constants.CONNECT_STATE = true;
    }

    public void write(int frequency){
        mSendPackagesThread = new SendPackagesThread(frequency,null,null);
        mSendPackagesThread.start();
    }

    public void disconnect(){
        mBluetoothGatt.disconnect();
        Constants.CONNECT_STATE = false;
        mSendPackagesThread = null;
    }

    private class SendPackagesThread extends Thread{

        int frequency;
        BluetoothGattCharacteristic characteristic;
        BluetoothGatt bluetoothGatt;

        public SendPackagesThread(int frequency,BluetoothGattCharacteristic characteristic,BluetoothGatt bluetoothGatt){
            this.frequency = frequency;
            this.characteristic = characteristic;
            this.bluetoothGatt = bluetoothGatt;
        }

        @Override
        public void run() {
//            String str = new String(Constants.MCORE_TEST_DATA);
//            Log.e(TAG,"STR为：" + str);
//            byte[] msg = str.getBytes();

//            byte[] bs = new byte[Constants.MCORE_TEST_DATA.length];
//            for (int i = 0;i < Constants.MCORE_TEST_DATA.length;i++){
//                byte b = (byte) Constants.MCORE_TEST_DATA[i];
//
////                bs[i] = b;
//            }

            String data = new String(Constants.MCORE_TEST_DATA);

            byte[] bytes = null;
            try {
                bytes = data.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.e(TAG,"字符转换异常");
            }

            characteristic.setValue(bytes);
            Log.e(TAG,"发出的字符串为：" + data);
            boolean flag = bluetoothGatt.writeCharacteristic(characteristic);

            if (flag){
                Log.e(TAG,"写入数据成功");
            }else {
                Log.e(TAG,"写入数据失败");
            }

            //写入数据后mbot没有响应。
//            while (true){
//                characteristic.setValue(bs);
//            BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(Constants.MY_UUID,10);
//            descriptor.setValue(bs);
//                byte[] info = descriptor.getValue();
//                String s = new String(info);
//                Log.e(TAG,"默认信息为：" + s);
//
//            characteristic.addDescriptor(descriptor);
//                boolean flag = mBluetoothGatt.writeCharacteristic(characteristic);
//                if (flag){
//                    Log.e(TAG,"写入成功");
//                }else {
//                    Log.e(TAG,"写入失败");
//                }
//                try {
//                    sleep(frequency);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    Log.e(TAG,"线程sleep异常");
//                }
//            }
        }
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG,"连接状态改变：" + status + "新状态：" + newState);
            mBluetoothGatt.discoverServices();
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            Log.e(TAG,"Services被发现" + "status为：" + status);
//            List<BluetoothGattService> services;
//            services = gatt.getServices();
//            for (BluetoothGattService service : services){
//                List<BluetoothGattCharacteristic> characteristics;
//                characteristics = service.getCharacteristics();
//                UUID uuid = service.getUuid();
//                Log.e(TAG,"当前Service的UUID为：" + uuid.toString());
//                for (BluetoothGattCharacteristic characteristic : characteristics){
//                    int permission = characteristic.getPermissions();
//                    int property = characteristic.getProperties();
//
////                    new SendPackagesThread(50,characteristic).start();
//                    characteristic.setValue("FF550600020A09FF00");
//                    boolean flag = mBluetoothGatt.writeCharacteristic(characteristic);
//                    if (flag){
//                        Log.e(TAG,"写入成功");
//                    }else {
//                        Log.e(TAG,"写入失败");
//                    }
//                    Log.e(TAG,"UUID为：" + characteristic.getUuid().toString() + "Permission为：" + permission + ".Property为：" + property);
//                }
//            }
            BluetoothGattService service = gatt.getService(Constants.SERVICE_UUID);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CHARACTERISTIC_UUID);

            new SendPackagesThread(50,characteristic,gatt).start();
//            characteristic.setValue("FF550600020A09FF00");
//            boolean flag = gatt.writeCharacteristic(characteristic);
//            if (flag){
//                Log.e(TAG,"写入成功");
//            }else {
//                Log.e(TAG,"写入失败");
//            }

            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG,"characteristic被读取，status为：" + status);
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG,"被写入");
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG,"Characteristic被改变");
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };


}
