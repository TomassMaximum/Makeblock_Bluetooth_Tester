package com.example.make201512.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Tom on 2016/4/28.
 * 对蓝牙2.0的支持类
 */
public class BluetoothClassic {

    private static final String TAG = BluetoothClassic.class.getSimpleName();

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private BluetoothSocket socket;

    private int packagesSent;

    private int packagesSentFail;

    private int packagesSentSuccessful;

    private static BluetoothClassic mBluetoothClassic = null;

    private ArrayList<DeviceBean> deviceBeans = new ArrayList<DeviceBean>(); //搜索到的设备List

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private InputStream mmInStream;
    private OutputStream mmOutStream;

    /*---------------------单例------------------------*/
    private static BluetoothClassic instance;

    public static BluetoothClassic getInstance() {
        if (instance == null) {
            synchronized (BluetoothClassic.class) {
                if (instance == null) {
                    instance = new BluetoothClassic();
                }
            }
        }
        return instance;
    }

    private BluetoothClassic() { }

    public void onReceive(Intent intent){
        String action = intent.getAction();
        switch (action) {
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                Log.e(TAG, "搜索开始");
                Message message = new Message();
                message.what = Constants.BT_START_DISCOVERY;
                EventBus.getDefault().post(new MessageEvent(message));
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.e(TAG, "搜索结束");
                Message message1 = new Message();
                message1.what = Constants.SCAN_DEVICES_FINISHED;
                EventBus.getDefault().post(new MessageEvent(message1));
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        Log.e(TAG, "配对中......");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.e(TAG, "配对成功  开始连接");
                        startConnect(new DeviceBean(device));//连接设备
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Log.e(TAG, "配对取消");
                }
            }
            break;
            case BluetoothDevice.ACTION_FOUND: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                DeviceBean bean = new DeviceBean(device);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG, "添加已配对蓝牙:" + device.getName() + " " + device.getAddress());
                } else {
                    Log.e(TAG, "添加未配对蓝牙:" + device.getName() + " " + device.getAddress());
                }
                getDeviceBeans().add(bean);
                Message message2 = new Message();
                message2.what = Constants.SCAN_DEVICE_FOUND;
                EventBus.getDefault().post(new MessageEvent(message2));
                Log.e(TAG, "*************************************************************");
            }
            break;
            case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:{
                Log.e(TAG,"ACTION_CONNECTION_STATE_CHANGED" + BluetoothAdapter.STATE_CONNECTED);
            }
            break;
        }
    }

    public void connectBluetooth(){

    }

    //主动断开
    public void disconnectBluetooth() {
        if (socket == null) {
            Log.e(TAG, "socket已经断开");
//            update4BluetoothDisconnected();
            return;
        }
        try {
            mmOutStream.close();
            mmInStream.close();
            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "断开蓝牙连接异常");
            e.printStackTrace();
            return;
        }
        mmOutStream = null;
        mmInStream = null;
        socket = null;

        Log.e(TAG, "断开蓝牙连接成功");
    }

    //蓝牙连接:开启ConnectThread线程连接
    public void startConnect(DeviceBean deviceBean) {
        if (connectThread != null && connectThread.isRunning) {
            Log.e(TAG, "connectThread 正在运行中");
        } else {
            //instance不能为null，必须先初始化
            connectThread = instance.new ConnectThread(deviceBean.getBluetoothDevice());
            connectThread.start();
        }
    }

    /*---------------------蓝牙的主动扫描，连接，断开------------------------*/
    private static int discoveryTimes = 0;

    public void stopDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            return;
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    }

    public void startDiscovery() {
        Log.e(TAG, "******startDiscovery******");
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Log.e(TAG, "此设备不支持蓝牙 return");
            return;
        }

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Message message = new Message();
            message.what = Constants.REQUEST_ENABLE_BT;
            EventBus.getDefault().post(new MessageEvent(message));
            return;
        }

        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()){
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }

        while (!BluetoothAdapter.getDefaultAdapter().startDiscovery()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            if (discoveryTimes > 3) {
                Log.e(TAG, "startDiscovery failed 3次，发送msg:BLUETOOTH_STATE_DISCOVERY_FAILED");
                Message message = new Message();
                message.what = Constants.DISCOVERY_FAILED;
                EventBus.getDefault().post(new MessageEvent(message));
                discoveryTimes = 0;
                return;
            }
            discoveryTimes++;
            Log.e(TAG, "startDiscovery failed");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "startDiscovery succeed");
        discoveryTimes = 0;
        //清空未连接的蓝牙
        if (isConnected()) {
            int count = getDeviceBeans().size();
            for (int i = 0; i < count; i++) {
                DeviceBean bean = getDeviceBeans().get(i);
                if (!bean.connected) {
                    getDeviceBeans().remove(i);
                    i--;
                    count--;
                }
            }
        } else {
            getDeviceBeans().clear();
        }
    }

    public Boolean isConnected() {
        for (int i = 0; i < deviceBeans.size(); i++) {
            DeviceBean bean = deviceBeans.get(i);
            if (bean.connected) {
                return true;
            }
        }
        return false;
    }

    /*---------------------get/set 方法------------------------*/
    public ArrayList<DeviceBean> getDeviceBeans() {
        return deviceBeans;
    }

    //蓝牙连接子线程
    private class ConnectThread extends Thread {
        private BluetoothDevice bluetoothDevice;
        private Boolean isRunning;
        private int retryTime;

        public ConnectThread(BluetoothDevice bluetoothDevice) {
            super();
            this.bluetoothDevice = bluetoothDevice;
            this.isRunning = false;
            retryTime = 0;
        }

        public void run() {
            Log.e(TAG, "connectThread start run");
            this.isRunning = true;
            //1.停止搜索
            if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            }

            retryTime++;
            if (!connect()) {
                retryTime++;
                connect();
                retryTime = 0;
            }
            this.isRunning = false;
        }

        private Boolean connect() {
            Log.e(TAG, "retryTime=" + retryTime);
            if (this.bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                try {
                    Log.e(TAG, "此设备没有配对  开始配对");
                    //lyh 跳转系统设置让用户配对
                    Message message = new Message();
                    message.what = Constants.REQUEST_PAIRING_DIALOG;
                    EventBus.getDefault().post(new MessageEvent(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            } else {
                Log.e(TAG, "此设备已经配对");
                try {
                    socket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    socket.connect();

                    Constants.CONNECT_STATE = true;
                } catch (Exception e) {
                    if (retryTime > 1) {
                        Log.e(TAG, "连接retry");
//                        EventBus.getDefault().post(new SearchEvent(SearchEvent.CONNECT_RETRY));
                    } else {
                        Log.e(TAG, "连接失败");
//                        EventBus.getDefault().post(new SearchEvent(SearchEvent.CONNECT_FAIL));
                    }
                    e.printStackTrace();
                    return false;
                }
                Log.e(TAG, "socket连接成功");

                try {
                    Log.e(TAG, "开始初始化:mmInStream,mmOutStream");
                    mmInStream = socket.getInputStream();
                    mmOutStream = socket.getOutputStream();

                } catch (Exception e) {
                    Log.e(TAG, "初始化:mmInStream,mmOutStream 异常 return");
                    if (retryTime > 1) {
                        Log.e(TAG, "连接retry");
//                        EventBus.getDefault().post(new SearchEvent(SearchEvent.CONNECT_RETRY));
                    } else {
                        Log.e(TAG, "连接失败");
//                        EventBus.getDefault().post(new SearchEvent(SearchEvent.CONNECT_FAIL));
                    }
                    e.printStackTrace();
                    return false;
                }

                Log.e(TAG, "初始化:mmInStream,mmOutStream 成功");
                update4BluetoothConnected(bluetoothDevice);
                Message message = new Message();
                message.what = Constants.BT_CONNECT_FINISHED;
                EventBus.getDefault().post(new MessageEvent(message));
                return true;
            }
        }
    }

    public void update4BluetoothConnected(BluetoothDevice bluetoothDevice){
        for (int i = 0;i < deviceBeans.size();i++){
            DeviceBean deviceBean = deviceBeans.get(i);
            BluetoothDevice device = deviceBean.getBluetoothDevice();
            if (bluetoothDevice.equals(device)){
                deviceBean.connected = true;
            }else {
                deviceBean.connected = false;
            }
        }
        if (connectedThread == null){
            connectedThread = instance.new ConnectedThread();
            connectedThread.start();
        }
    }

    public void resetData(){
        packagesSent = 0;
        packagesSentSuccessful = 0;
        packagesSentFail = 0;
    }

    /**
     * 处理收发数据流操作的线程
     * */
    private class ConnectedThread extends Thread {
        @Override
        public synchronized void run() {
            Log.e(TAG,"开始处理输入输出流");

            int[] expectedBytes = new int[]{0xff, 0x55, 0x00, 0x30, 0x0d, 0x0a};
            int[] unexpectedBytes = new int[]{0xff, 0x55, 0x00, 0x00, 0x0d, 0x0a};
            int indexForExpectedBytes = 0;
            int indexForUnexpectedBytes = 0;
            int indexForErrorBytes = 0;
            String result = "";
            boolean goElseIf = false;
            boolean goElse = false;

//            while (mSocket.isConnected()){
                try {
                    Log.e(TAG,"开始读取输入流");

                    while(true){
                        int nextByte = mmInStream.read();
                        result += Integer.toHexString(nextByte);

                        //加入ff判断,解决丢数据后排序错乱导致统计数据错误的问题
                        if (nextByte == expectedBytes[0]){
                            indexForExpectedBytes = 0;
                            indexForErrorBytes = 0;
                            indexForUnexpectedBytes = 0;

                            result = "ff";
                        }

                        if(nextByte == expectedBytes[indexForExpectedBytes] && !goElseIf && !goElse){
                            indexForExpectedBytes++;
                            indexForUnexpectedBytes++;
                            indexForErrorBytes++;
                            if(indexForExpectedBytes >= expectedBytes.length){
                                indexForExpectedBytes = 0;
                                indexForUnexpectedBytes = 0;
                                indexForErrorBytes = 0;
                                // ok count ++
                                packagesSentSuccessful++;
                                Message message = new Message();
                                message.what = Constants.UPDATE_PACKAGES_SENT_SUCCESSFUL;
                                message.obj = packagesSentSuccessful;
                                EventBus.getDefault().post(new MessageEvent(message));

                                Message msg = new Message();
                                msg.what = Constants.UPDATE_LOGS;
                                Bundle bundle = new Bundle();
                                bundle.putString("result","received:<" + result + ">");
                                msg.setData(bundle);
                                EventBus.getDefault().post(new MessageEvent(msg));

                                Log.e(TAG,"received:<" + result + ">");

                                result = "";
                            }
                        }else if (nextByte == unexpectedBytes[indexForUnexpectedBytes] && !goElse){   // dismatch between expected and failed

                            goElseIf = true;
                            indexForUnexpectedBytes++;
                            indexForErrorBytes++;

                            if (indexForUnexpectedBytes >= unexpectedBytes.length){
                                indexForExpectedBytes = 0;  // if dismatch, try match from the first byte
                                indexForUnexpectedBytes = 0;
                                indexForErrorBytes = 0;
                                goElseIf = false;

                                packagesSentFail++;
                                Message message = new Message();
                                message.what = Constants.UPDATE_PACKAGES_SENT_FAIL;
                                message.obj = packagesSentFail;
                                EventBus.getDefault().post(new MessageEvent(message));

                                Message msg = new Message();
                                msg.what = Constants.UPDATE_LOGS;
                                Bundle bundle = new Bundle();
                                bundle.putString("result","received:<" + result + ">");
                                msg.setData(bundle);
                                EventBus.getDefault().post(new MessageEvent(msg));

                                Log.e(TAG,"received:<" + result + ">");

                                result = "";
                            }

                        }else {
                            goElse = true;
                            indexForErrorBytes++;
                            if (indexForErrorBytes >= unexpectedBytes.length){
                                indexForExpectedBytes = 0;
                                indexForUnexpectedBytes = 0;
                                indexForErrorBytes = 0;
                                goElse = false;

                                Message msg = new Message();
                                msg.what = Constants.UPDATE_LOGS;
                                Bundle bundle = new Bundle();
                                bundle.putString("result","received:<" + result + ">");
                                msg.setData(bundle);
                                EventBus.getDefault().post(new MessageEvent(msg));

                                Log.e(TAG,"received:<" + result + ">");

                                result = "";
                            }

                        }

                        Message message = new Message();
                        message.what = Constants.UPDATE_PACKAGES_NOT_BACK;
                        EventBus.getDefault().post(new MessageEvent(message));

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"字节读取异常");
                }
//            }
        }
    }

    //向蓝牙模块发包的方法
    public void write(int frequency){
        Log.e(TAG,"ConnectedThread的write方法被执行");
        new SendPackagesThread(mmOutStream,frequency).start();
    }

    private class SendPackagesThread extends Thread{
        private OutputStream outputStream;
        private int frequency;

        public SendPackagesThread(OutputStream outputStream,int frequency){
            this.outputStream = outputStream;
            this.frequency = frequency;
        }

        @Override
        public void run() {
            Log.e(TAG,"SendPackageThread线程run方法执行");
            try {
                //循环发包，根据用户设定的间隔时间进行发包操作
                while (true){
                    if (!Constants.shouldStopSendingData){
                        for (int i = 0;i < Constants.TEST_DATA_BYTE_ARRAY.length;i++){
                            outputStream.write(Constants.TEST_DATA_BYTE_ARRAY[i]);
                        }
                        outputStream.flush();
                    }else {
                        break;
                    }
                    String string = bytesToHex(Constants.TEST_DATA_BYTE_ARRAY);
                    Log.e(TAG,"send:<" + string + ">");

                    Message msg = new Message();
                    msg.what = Constants.UPDATE_LOGS;
                    Bundle bundle = new Bundle();
                    bundle.putString("result","send:<" + string + ">");
                    msg.setData(bundle);
                    EventBus.getDefault().post(new MessageEvent(msg));

                    //已发的数据包数量自增
                    packagesSent++;

                    //使用Handler更新UI的数据显示
                    Message message = new Message();
                    message.what = Constants.UPDATE_PACKAGES_SENT_COUNT;
                    message.obj = packagesSent;

                    EventBus.getDefault().post(new MessageEvent(message));

                    //当前线程休眠用户设定的时间长度
                    sleep(frequency);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Message message = new Message();
                message.what = Constants.EXCEPTION_INFO;
                message.obj = e.getMessage();
                EventBus.getDefault().post(new MessageEvent(message));
                Log.e(TAG,"写入数据异常");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Message message = new Message();
                message.what = Constants.EXCEPTION_INFO;
                message.obj = e.getMessage();
                EventBus.getDefault().post(new MessageEvent(message));
                Log.e(TAG,"线程阻塞异常");
            }
        }

        final protected char[] hexArray = "0123456789ABCDEF".toCharArray();

        public String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }
    }
}
