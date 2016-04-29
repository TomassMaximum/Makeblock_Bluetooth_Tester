package com.example.make201512.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tom on 2016/4/28.
 * 对蓝牙2.0的支持类
 */
public class BluetoothClassic {

    private static final String TAG = BluetoothClassic.class.getSimpleName();

    BluetoothAdapter mBluetoothAdapter;

    Context mContext;

    private int deviceIndex = 0;

    private Map<Integer,BluetoothDevice> devices;

    private BluetoothDevice device;

    private ConnectedThread connectedThread;

    private BluetoothSocket socket;

    private int packagesSent;

    private int packagesSentFail;

    private int packagesSentSuccessful;

    private int packagesNotBack;

    private static BluetoothClassic mBluetoothClassic = null;

    private BluetoothClassic(final Context context){
        mContext = context;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        devices = new HashMap<>();

        registerBroadcast();
    }

    public static BluetoothClassic getInstance(Context context){
        if (mBluetoothClassic == null){
            mBluetoothClassic = new BluetoothClassic(context);
        }
        return mBluetoothClassic;
    }

    public void scanClassic(){
        mBluetoothAdapter.cancelDiscovery();

        mBluetoothAdapter.startDiscovery();
    }

    public void connectDevice(int index){
        mBluetoothAdapter.cancelDiscovery();
        Message message = new Message();
        message.what = Constants.SCAN_DEVICES_FINISHED;
        EventBus.getDefault().post(new MessageEvent(message));

        BluetoothDevice selectedDevice = devices.get(index);
        device = selectedDevice;

        message.what = Constants.UPDATE_BLUETOOTH_DEVICE_NAME_AND_COUNTS;
        message.obj = selectedDevice.getName();
        EventBus.getDefault().post(new MessageEvent(message));

        new ConnectThread(selectedDevice).start();

    }

    public void sendPackages(int frequency){
        if (connectedThread == null){
            connectedThread = new ConnectedThread(socket);
        }
        connectedThread.start();
        connectedThread.write(frequency);
    }

    public void resetCounts(){
        packagesSent = 0;
        packagesSentSuccessful = 0;
        packagesSentFail = 0;
        packagesNotBack = 0;
    }

    public void disconnect(){
        connectedThread.cancel();
    }

    public void registerBroadcast(){
        //创建各种IntentFilter并注册广播接收器来监听蓝牙状态的各种变化
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mBroadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mContext.registerReceiver(mBroadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mBroadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        mContext.registerReceiver(mBroadcastReceiver,intentFilter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action){
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:{
                    Log.e(TAG,"搜索开始");
                    break;
                }
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:{
                    Log.e(TAG,"连接状态改变");
                    break;
                }
                case BluetoothDevice.ACTION_ACL_CONNECTED:{
                    Log.e(TAG,"已连接诶");
                    Message message = new Message();
                    message.what = Constants.CONNECT_STATE_CHANGED;
                    EventBus.getDefault().post(new MessageEvent(message));
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:{
                    Log.e(TAG,"搜索结束");
                    Message message = new Message();
                    message.what = Constants.SCAN_DEVICES_FINISHED;
                    EventBus.getDefault().post(new MessageEvent(message));
                    break;
                }
                case BluetoothDevice.ACTION_FOUND:{
                    //获取到当前搜索到的蓝牙设备
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();

                    if (deviceName == null){
                        deviceName = "无名";
                    }

                    //将获取到的蓝牙设备的名称和MAC地址添加到ListView中显示
                    String deviceInfo = deviceName + "\n" + deviceAddress;

                    if (!devices.containsValue(deviceInfo) && deviceName.equals("Makeblock")) {
                        Log.e(TAG,"搜索到设备" + deviceIndex +" ，添加到列表");

                        //将获取到的设备按角标顺序存入Map集合中
                        devices.put(deviceIndex,device);

                        Message message = new Message();
                        message.what = Constants.SCAN_DEVICE_FOUND;
                        message.obj = deviceInfo;
                        EventBus.getDefault().post(new MessageEvent(message));

                        //角标自增
                        deviceIndex++;
                        Log.e(TAG,"蓝牙设备数量为：" + deviceIndex);
                        Constants.CURRENT_DEVICES_COUNT = deviceIndex;
                    }
                    break;
                }
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:{
                    Log.e(TAG,"设备配对状态改变");

                    break;
                }
            }
        }
    };

    /**
     * 连接到蓝牙设备的线程。
     * */
    private class ConnectThread extends Thread{

        private final BluetoothDevice mDevice;
        private final BluetoothSocket mSocket;
        private String deviceName;

        //构造方法，接受希望连接到的蓝牙设备
        public ConnectThread(BluetoothDevice bluetoothDevice){
            //接收构造收到的蓝牙设备
            mDevice = bluetoothDevice;

            //获取该蓝牙设备的名称
            deviceName = mDevice.getName();

            //考虑设备蓝牙名称为空的情况，避免空指针
            if (deviceName == null){
                deviceName = "未知设备";
            }

            //连接到Makeblock的蓝牙模块需要设置PIN码，三号坑。
            setBluetoothPairingPin(mDevice);

            //因为当前Socket为final，所以创建临时Socket存储
            BluetoothSocket tmpSocket = null;

            try {
                //使用UUID从服务端蓝牙设备获取到Socket
                tmpSocket = mDevice.createRfcommSocketToServiceRecord(Constants.MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"不能获取到BluetoothSocket");
            }

            //赋值给本地的Socket
            mSocket = tmpSocket;
        }

        @Override
        public void run() {
            //判断设备是否已配对（绑定），如果已配对，则直接进行连接
            if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                Log.e(TAG,"Socket开始连接");
                try {
                    mSocket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"Socket连接异常");
                }

                Log.e(TAG,"Socket已连接");
                Constants.CONNECTSTATE = true;
                socket = mSocket;

                //如果当前蓝牙设备未配对，要手动进行配对工作。
                //利用反射获取到配对方法（createBond）。四号坑。
            }else {
                Log.e(TAG,"设备未配对，进行配对操作");
                //如果SDK版本在19以上，则直接调用系统API，19以下使用反射来进行主动配对。
                if (Build.VERSION.SDK_INT >= 19){
                    mDevice.createBond();
                }else {
                    Method method;
                    try {
                        method = mDevice.getClass().getMethod("createBond", (Class[]) null);
                        method.invoke(mDevice, (Object[]) null);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG,"绑定异常");
                    }
                }
            }
        }

        //设置PIN码的方法，来自小明在mbot中写的代码
        public void setBluetoothPairingPin(BluetoothDevice device){
            byte[] pinBytes = ("0000").getBytes();
            if (Build.VERSION.SDK_INT >= 19){
                device.setPin(pinBytes);
                device.setPairingConfirmation(true);
            }else {
                try {
                    Log.e(TAG, "Try to set the PIN");

                    Method m = device.getClass().getMethod("setPin", byte[].class);

                    m.invoke(device, pinBytes);
                    Log.e(TAG, "Success to add the PIN.");

                    try {
                        device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                        Log.e(TAG, "Success to setPairingConfirmation.");
                    } catch (Exception e) {
                        Log.e(TAG,"设置配对信息失败");
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG,"获取setPin方法错误");
                }
            }
        }
    }

    /**
     * 处理收发数据流操作的线程
     * */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        //构造，获取到对话框回传的已连接的Socket
        ConnectedThread(BluetoothSocket socket){
            mSocket = socket;
            InputStream tmpInputStream = null;
            OutputStream tmpOutputStream = null;

            try {
                tmpInputStream = mSocket.getInputStream();
                tmpOutputStream = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tmpInputStream;
            outputStream = tmpOutputStream;
        }

        @Override
        public void run() {
            Log.e(TAG,"开始处理输入输出流");

            byte[] buffer = new byte[1024];
            int bytes;

            while (mSocket.isConnected()){
                try {
                    Log.e(TAG,"开始读取输入流");
                    bytes = inputStream.read(buffer);

                    String receivedData = bytes2HexString(buffer);

                    Log.e(TAG,"接收到的数据为：" + receivedData);

                    if (receivedData.equals(Constants.EXPECTED_RECEIVED_DATA)){
                        packagesSentSuccessful++;
                        Message message = new Message();
                        message.what = Constants.UPDATE_PACKAGES_SENT_SUCCESSFUL;
                        message.obj = packagesSentSuccessful;
                        EventBus.getDefault().post(new MessageEvent(message));
                    }else if (receivedData.equals(Constants.EXPECTED_RECEIVED_FAIL_DATA)){
                        packagesSentFail++;
                        Message message = new Message();
                        message.what = Constants.UPDATE_PACKAGES_SENT_FAIL;
                        message.obj = packagesSentFail;
                        EventBus.getDefault().post(new MessageEvent(message));
                    }else {
                        packagesNotBack++;
                        Message message = new Message();
                        message.what = Constants.UPDATE_PACKAGES_NOT_BACK;
                        message.obj = packagesNotBack;
                        EventBus.getDefault().post(new MessageEvent(message));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"字节读取异常");
                }
            }
        }

        //向蓝牙模块发包的方法
        public void write(int frequency){
            new SendPackagesThread(outputStream,frequency).start();
        }

        //将字节流转换为十六进制字符串的方法
        public String bytes2HexString(byte[] b) {
            String ret = "";
            for (int i = 0; i < b.length; i++) {
                String hex = Integer.toHexString(b[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                ret += hex;
            }
            return ret;
        }

        public void cancel(){
            if (inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"无法关闭输入流");
                }
            }
            if (outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"无法关闭输出流");
                }
            }
            if (mSocket != null){
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"无法关闭Socket");
                }
            }
        }
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
            try {
                //循环发包，根据用户设定的间隔时间进行发包操作
                while (true){
                    if (!Constants.shouldStopSendingData){
                        for (int i = 0;i < Constants.TEST_DATA.length;i++){
                            outputStream.write(Constants.TEST_DATA[i]);
                        }
                    }else {
                        break;
                    }
                    Log.e(TAG,"一条消息已发送完毕");

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
                Log.e(TAG,"写入数据异常");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG,"线程阻塞异常");
            }
        }
    }

    public void unRegisterBroadcastReceiver(){
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

}
