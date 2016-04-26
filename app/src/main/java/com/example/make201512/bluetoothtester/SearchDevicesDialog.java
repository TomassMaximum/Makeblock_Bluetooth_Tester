package com.example.make201512.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 自定义对话框类，使用第三方库MaterialDialog。
 * 当用户点击搜索时弹出本对话框，自动开始搜索，将搜索的过程添加到ListView中显示。
 * 用户选择一项点击后，与选中的蓝牙设备建立连接，对话框自动消失，并将连接获取的Socket传回Activity供其互传数据使用。
 * 本类包含了蓝牙连接的线程，连接蓝牙模块时将蓝牙模块作为服务端进行连接，不需要写服务端。
 *
 * Created by make201512 on 2016/4/17.
 */
public class SearchDevicesDialog extends MaterialDialog {

    //Debug TAG
    private final String TAG = "SearchDevicesDialog";

    //对话框顶部的文本框，搜索时显示正在搜索，搜索完毕后显示为再来一次，点击可以进行新一轮的搜索
    TextView searchTextView;

    //蓝牙适配器，代表本机蓝牙射频模块
    BluetoothAdapter bluetoothAdapter;

    //用于显示搜索到的设备列表，包含设备蓝牙名称与设备蓝牙MAC地址
    ListView listView;

    //旋转进度条，第三方库。搜索时播放旋转动画，搜索完毕后自动消失
    ProgressWheel progressWheel;

    //ListView的适配器
    ArrayAdapter<String> arrayAdapter;

    //存储当前蓝牙设备的Map集合，用于存储获取到的蓝牙设备，在连接的时候使用
    Map<Integer,BluetoothDevice> devicesMap;

    //对话框回调接口，在建立连接后将Socket传回Activity
    OnMyDialogResult onMyDialogResult;

    ConnectThread connectThread;

    /*
    * 构造方法，接受DialogBuilder，初始化对话框。
    *
    * param: MaterialDialog.Builder
    * */
    protected SearchDevicesDialog(Builder builder) {
        super(builder);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //初始化Views，找到Views并创建IntentFilter用于接受各类广播
        init();

        //开始搜索
        searchStart();
    }

    /**
     * ListView监听器，当任意一项item被点击时，开启连接线程进行连接操作
     * */
    private class DeviceOnClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //如果当前仍在进行搜索状态，取消搜索操作。因为搜索过程消耗大量带宽，可能导致连接失败。二号坑。
            bluetoothAdapter.cancelDiscovery();

            //以用户点击的角标获取到Map集合中对应的蓝牙设备
            BluetoothDevice device = devicesMap.get(position);
            Log.e(TAG,"当前设备地址为：" + device.getAddress());

            //开启连接线程进行连接操作
            connectThread = new ConnectThread(device);
            connectThread.start();
        }
    }

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

                //调用对话框的接口，将获取到的Socket成功连接后传给Activity
                if (onMyDialogResult != null){
                    onMyDialogResult.finish(mSocket,deviceName);
                }

                //关闭当前对话框
                dismiss();

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
     * 搜索文本框监听器。
     * 当用户点击搜索文本框再次进行搜索时，做出相应动作。
     * */
    private class searchListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {

            //清除当前ListView中的所有内容
            arrayAdapter.clear();

            //搜索开始标识
            searchStart();
        }
    }

    //开始搜索的方法
    private void searchStart(){
        //使旋转进度条开始旋转，标识搜索正在进行中
        progressWheel.spin();

        //将搜索文本框的文字内容设为正在搜索状态
        searchTextView.setText("正在搜索蓝牙设备...");

        //设置搜索文本框为不可点击
        searchTextView.setClickable(false);

        //如果正在进行蓝牙搜索操作，则取消搜索操作。因为蓝牙搜索只能进行一次
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }

        //开始搜索周围的蓝牙设备
        bluetoothAdapter.startDiscovery();
    }

    //搜索结束的方法
    private void searchFinished(){
        //使旋转进度条消失，标识当前不在搜索过程中
        progressWheel.stopSpinning();

        //设置搜索文本框为在进行一次搜索的标识
        searchTextView.setText("再来一次");

        //为搜索文本框设置监听器
        searchTextView.setOnClickListener(new searchListener());
    }

    /**
     * 广播接收器，接收蓝牙连接方面的各种广播。
     * 当各种状态改变时，打印日志标识当前状态。
     *
     * */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        //记录当前搜索到的蓝牙设备的次序角标
        int index = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            //获取到当前广播动作
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
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:{
                    Log.e(TAG,"搜索结束");
                    searchFinished();
                    break;
                }
                case BluetoothDevice.ACTION_FOUND:{
                    Log.e(TAG,"搜索到设备" + index +" ，添加到列表");

                    //获取到当前搜索到的蓝牙设备
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    //将获取到的蓝牙设备的名称和MAC地址添加到ListView中显示
                    arrayAdapter.add(device.getName() + "\n" + device.getAddress());

                    //将获取到的设备按角标顺序存入Map集合中
                    devicesMap.put(index,device);

                    //角标自增
                    index++;

                    Constants.CURRENT_DEVICES_COUNT = index;

                    break;
                }
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:{
                    Log.e(TAG,"设备连接状态改变");

                    //设备配对成功后调用ConnectThread的run()方法
                    connectThread.run();
                    break;
                }
            }

        }
    };

    //初始化操作的方法
    public void init(){
        //找到搜索文本框，ListView和进度条
        searchTextView = (TextView) findViewById(R.id.start_search);
        listView = (ListView) findViewById(R.id.list_view);
        progressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);

        //创建ListView适配器
        arrayAdapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1);

        //获取到蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //为ListView设置适配器
        listView.setAdapter(arrayAdapter);

        //为ListView的items设置监听
        listView.setOnItemClickListener(new DeviceOnClickListener());

        //创建各种IntentFilter并注册广播接收器来监听蓝牙状态的各种变化
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getContext().registerReceiver(broadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(broadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        getContext().registerReceiver(broadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        getContext().registerReceiver(broadcastReceiver,intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getContext().registerReceiver(broadcastReceiver,intentFilter);

        //初始化蓝牙设备的Map集合
        devicesMap = new HashMap<>();
    }

    //获取到本类接口的回调
    public void setDialogResult(OnMyDialogResult dialogResult){
        onMyDialogResult = dialogResult;
    }

    //接口，用于在连接完成后将Socket回传给Activity
    public interface OnMyDialogResult{
        void finish(BluetoothSocket socket,String deviceName);
    }

    //对话框从窗口剥离的回调方法，在这里进行善后工作
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //如果当前还在进行搜索操作，则取消搜索
        if (bluetoothAdapter != null){
            bluetoothAdapter.cancelDiscovery();
        }

        //注销广播接收器，避免溢出
        getContext().unregisterReceiver(broadcastReceiver);
    }
}
