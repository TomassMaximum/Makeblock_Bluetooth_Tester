package com.example.make201512.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 作者：小龟
 * 版本：v1.0.0
 * 当前支持测试蓝牙2.0的连接稳定性
 * */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Debug TAG
    private static final String TAG = "MainActivity";

    TextView dataSentCounts;
    TextView dataSentSuccessCounts;
    TextView dataSentWrongCounts;
    TextView dataNotBackCounts;

    EditText dataSentFrequency;
    int frequency;

    TextView blueToothName;
    TextView blueToothCounts;

    Button sendData;
    Button stopSendData;
    Button connect;
    Button disconnect;
    Button editName;

    BluetoothAdapter bluetoothAdapter;

    ConnectedThread connectedThread;

    private int packagesSent;

    private int packagesSentFail;

    private int packagesSentSuccessful;

    private int packagesNotBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化Activity
        init();

        //获取到本设备的蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //判断当前设备是否支持蓝牙功能
        if (bluetoothAdapter == null){
            //当前设备不支持蓝牙功能，吐司提示退出当前app
            Toast.makeText(this, "丫的设备并不支持蓝牙啊喂", Toast.LENGTH_SHORT).show();

            //app自焚
            finish();
        }

        //判断蓝牙是否打开，未打开则弹出提示框提醒用户打开蓝牙
        if (!bluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent,Constants.REQUEST_ENABLE_BT);
        }

        Toast.makeText(MainActivity.this, "系统版本为：" + Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();
    }

    //接收系统打开蓝牙的结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG,RESULT_OK + "结果码为：" + resultCode);
        if (resultCode != RESULT_OK){
            //未打开蓝牙，提示用户打开蓝牙，否则不允许操作app
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent,Constants.REQUEST_ENABLE_BT);
        }

        if (bluetoothAdapter.isEnabled()){
            //蓝牙打开成功，弹出snackbar提示用户蓝牙成功打开
            Snackbar.make(dataNotBackCounts,"蓝牙已经打开",Snackbar.LENGTH_SHORT).show();
        }

    }

    //为主界面的按钮设置监听
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_connect:{
                if (Constants.CONNECTSTATE){
                    Snackbar.make(v,"请先断开连接",Snackbar.LENGTH_SHORT);
                }else {
                    //当搜索按钮被点击时，弹出搜索对话框进行搜索和连接操作
                    SearchDevicesDialog dialog = new SearchDevicesDialog(new MaterialDialog.Builder(this).customView(R.layout.search_dialog, false));

                    //调用对话框的回调接口，接收传回来的已连接的Socket
                    dialog.setDialogResult(new SearchDevicesDialog.OnMyDialogResult() {
                        @Override
                        public void finish(BluetoothSocket socket,String deviceName) {
                            if (socket != null){
                                Message message = new Message();
                                message.what = Constants.UPDATE_BLUETOOTH_DEVICE_NAME;
                                message.obj = deviceName;
                                mHandler.sendMessage(message);
                                connectedThread = new ConnectedThread(socket);
                                connectedThread.start();
                            }
                        }
                    });
                    //显示对话框
                    dialog.show();
                }
                break;
            }

            case R.id.button_send_data:{
                //当发包按钮被点击时，判断设备是否已连接蓝牙，如果已连接，则进行发包操作，如果未连接，则弹出Snackbar提示用户连接蓝牙设备
                if (Constants.CONNECTSTATE){
                    //重置变量
                    Constants.shouldStopSendingData = false;
                    packagesSent = 0;
                    packagesSentSuccessful = 0;
                    packagesSentFail = 0;
                    packagesNotBack = 0;

                    //获取到用户输入的发包频率
                    if (!dataSentFrequency.getText().toString().equals("")){
                        String enteredNum = dataSentFrequency.getText().toString();

                        Log.e(TAG,"用户输入为：" + enteredNum);

                        frequency = Integer.valueOf(enteredNum);

                        //使用用户设置的频率发包
                        connectedThread.write(frequency);
                        Log.e(TAG,"消息已发送");
                    }else {
                        Snackbar.make(v,"请输入发包间隔时间",Snackbar.LENGTH_SHORT);
                    }

                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_stop_send_data:{
                //当停止发包按钮被点击时，设变量为true，停止发包
                if (Constants.CONNECTSTATE){
                    Constants.shouldStopSendingData = true;
                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_disconnect:{
                //当断开连接按钮被点击时，启动断开连接流程
                if (Constants.CONNECTSTATE){
                    connectedThread.cancel();

                    //复位各项数据
                    dataSentCounts.setText("0");
                    dataSentSuccessCounts.setText("0");
                    dataSentWrongCounts.setText("0");
                    dataNotBackCounts.setText("0");
                    Constants.shouldStopSendingData = true;
                    Constants.CONNECTSTATE = false;
                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_edit_name:{
                //当更名按钮被点击时，弹出Snackbar提示该功能尚未实现
                Snackbar.make(v,"该功能尚未实现",Snackbar.LENGTH_SHORT).show();
                break;
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
                        mHandler.sendMessage(message);
                    }else if (receivedData.equals(Constants.EXPECTED_RECEIVED_FAIL_DATA)){
                        packagesSentFail++;
                        Message message = new Message();
                        message.what = Constants.UPDATE_PACKAGES_SENT_FAIL;
                        mHandler.sendMessage(message);
                    }else {
                        packagesNotBack++;
                        Message message = new Message();
                        message.what = Constants.UPDATE_PACKAGES_NOT_BACK;
                        mHandler.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"字节读取错误");
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
                    mHandler.sendMessage(message);

                    //当前线程休眠用户设定的时间长度
                    sleep(frequency);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"写入数据错误");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG,"线程休眠错误");
            }
        }
    }

    //用于更新UI的Handler，当数据发生改变时，根据最新数据更新UI
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            int action = msg.what;
            switch (action){
                case  Constants.UPDATE_PACKAGES_SENT_COUNT:{
                    dataSentCounts.setText(packagesSent + "");
                    break;
                }
                case Constants.UPDATE_PACKAGES_SENT_SUCCESSFUL:{
                    dataSentSuccessCounts.setText(packagesSentSuccessful + "");
                    break;
                }
                case Constants.UPDATE_PACKAGES_SENT_FAIL:{
                    dataSentWrongCounts.setText(packagesSentFail + "");
                    break;
                }
                case Constants.UPDATE_PACKAGES_NOT_BACK:{
                    dataNotBackCounts.setText(packagesNotBack + "");
                    break;
                }
                case Constants.UPDATE_BLUETOOTH_DEVICE_NAME:{
                    String deviceName = (String) msg.obj;
                    blueToothName.setText(deviceName);
                    blueToothCounts.setText(Constants.CURRENT_DEVICES_COUNT + "");
                    break;
                }
            }
        }
    };


    public void init(){
        dataSentCounts = (TextView) findViewById(R.id.data_sent);
        dataSentSuccessCounts = (TextView) findViewById(R.id.data_sent_success);
        dataSentWrongCounts = (TextView) findViewById(R.id.data_sent_wrong);
        dataNotBackCounts = (TextView) findViewById(R.id.data_sent_not_back);

        dataSentFrequency = (EditText) findViewById(R.id.data_sent_time);

        blueToothName = (TextView) findViewById(R.id.bluetooth_name);
        blueToothCounts = (TextView) findViewById(R.id.device_counts);

        sendData = (Button) findViewById(R.id.button_send_data);
        stopSendData = (Button) findViewById(R.id.button_stop_send_data);
        connect = (Button) findViewById(R.id.button_connect);
        disconnect = (Button) findViewById(R.id.button_disconnect);
        editName = (Button) findViewById(R.id.button_edit_name);

        connect.setOnClickListener(this);
        sendData.setOnClickListener(this);
        stopSendData.setOnClickListener(this);
        disconnect.setOnClickListener(this);
        editName.setOnClickListener(this);

        dataSentFrequency.setText("50");
        dataSentFrequency.setSelection(dataSentFrequency.length());
    }

}
