package com.example.make201512.bluetoothtester;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：小龟
 * 版本：v1.0.0
 * 当前支持测试蓝牙2.0的连接稳定性
 * */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    //Debug TAG
    private static final String TAG = MainActivity.class.getSimpleName();

    TextView dataSentCounts;
    TextView dataSentSuccessCounts;
    TextView dataSentWrongCounts;
    TextView dataNotBackCounts;

    TextView blueToothName;

    Button sendDataIn20;
    Button sendDataIn50;
    Button sendDataIn30;
    Button stopSendData;
    Button connect;
    Button disconnect;

    Switch showLog;
    Switch resetData;

    BluetoothAdapter bluetoothAdapter;

    BluetoothClassic mBluetoothClassic;

    BluetoothLE mBluetoothLE;

    ResultFragment resultFragment;

    ProgressDialog progressDialog;

    private int packagesSent;
    private int packagesExpected;
    private int packagesUnexpected;

    Dialog pairAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化Activity
        init();

        resultFragment = new ResultFragment();

        EventBus.getDefault().register(this);

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

        showLog.setChecked(true);
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
            case R.id.button_connect_classic:{
                if (bluetoothAdapter.isEnabled()){
                    if (Constants.CONNECT_STATE){
                        Snackbar.make(v,"请先断开连接",Snackbar.LENGTH_SHORT).show();
                    }else {
                        mBluetoothClassic = BluetoothClassic.getInstance(this);
                        Constants.IS_BLE_STATE = false;
                        //当搜索按钮被点击时，弹出搜索对话框进行搜索和连接操作
                        SearchDevicesDialog dialog = new SearchDevicesDialog(new MaterialDialog.Builder(this).customView(R.layout.search_dialog, false));

                        //显示对话框
                        dialog.show();
                    }
                }else {
                    Snackbar.make(v,"请先打开蓝牙",Snackbar.LENGTH_SHORT).show();
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent,Constants.REQUEST_ENABLE_BT);
                }

                break;
            }

            case R.id.button_send_data_in_20:{
                //当用户点击20ms发包时
                if (Constants.CONNECT_STATE){
                    Constants.shouldStopSendingData = false;
                    sendPackages(20);
                    Log.e(TAG,"消息已发送");
                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_send_data_in_30:{
                if (Constants.CONNECT_STATE){
                    Constants.shouldStopSendingData = false;
                    sendPackages(30);
                    Log.e(TAG,"消息已发送");
                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_send_data_in_50:{
                if (Constants.CONNECT_STATE){
                    Constants.shouldStopSendingData = false;
                    sendPackages(50);
                    Log.e(TAG,"消息已发送");
                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_stop_send_data:{
                //当停止发包按钮被点击时，设变量为true，停止发包
                if (Constants.CONNECT_STATE){
                    Constants.shouldStopSendingData = true;
                    int packagesNotBack = packagesSent - packagesExpected - packagesUnexpected;
                    dataNotBackCounts.setText(packagesNotBack + "");
                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_disconnect:{
                //当断开连接按钮被点击时，启动断开连接流程
                if (Constants.CONNECT_STATE){
                    //复位各项数据
                    resetCounts();
                    blueToothName.setText("未连接");
                    Constants.shouldStopSendingData = true;
                    disconnect();
                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEvent(MessageEvent event){
        Message message = event.message;
        int action = message.what;
        switch (action){
            case  Constants.UPDATE_PACKAGES_SENT_COUNT:{
                int packagesSent = (int) message.obj;
                this.packagesSent = packagesSent;
                dataSentCounts.setText(packagesSent + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_SENT_SUCCESSFUL:{
                int packagesSentSuccessful = (int) message.obj;
                this.packagesExpected = packagesSentSuccessful;
                dataSentSuccessCounts.setText(packagesSentSuccessful + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_SENT_FAIL:{
                int packagesSentFail = (int) message.obj;
                this.packagesUnexpected = packagesSentFail;
                dataSentWrongCounts.setText(packagesSentFail + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_NOT_BACK:{
                int packagesNotBack = packagesSent - packagesExpected - packagesUnexpected;
                dataNotBackCounts.setText(packagesNotBack + "");
                break;
            }
            case Constants.UPDATE_BLUETOOTH_DEVICE_NAME_AND_COUNTS:{
                String deviceName = (String) message.obj;
                blueToothName.setText(deviceName);
                break;
            }
            case Constants.EXCEPTION_INFO:{
                String exceptionInfo = message.obj.toString();
                Snackbar.make(dataNotBackCounts,"出现异常,请重新连接",Snackbar.LENGTH_SHORT).show();
                break;
            }
            case Constants.BT_CONNECT_START:{
                //start progress bar spin
                progressDialog.show();
                break;
            }
            case Constants.BT_CONNECT_FINISHED:{
                //dismiss spinning progress bar
                progressDialog.dismiss();
            }
            case Constants.SHOW_PAIRING_REQUEST_DIALOG:{
                if (pairAlertDialog == null) {
                    pairAlertDialog = new AlertDialog.Builder(this).setTitle("请进入系统设置进行配对")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                } else {
                    pairAlertDialog.show();
                }
            }
        }
    }

    public void init(){
        dataSentCounts = (TextView) findViewById(R.id.data_sent);
        dataSentSuccessCounts = (TextView) findViewById(R.id.data_sent_success);
        dataSentWrongCounts = (TextView) findViewById(R.id.data_sent_wrong);
        dataNotBackCounts = (TextView) findViewById(R.id.data_sent_not_back);

        sendDataIn20 = (Button) findViewById(R.id.button_send_data_in_20);
        sendDataIn30 = (Button) findViewById(R.id.button_send_data_in_30);
        sendDataIn50 = (Button) findViewById(R.id.button_send_data_in_50);

        blueToothName = (TextView) findViewById(R.id.bluetooth_name);

        stopSendData = (Button) findViewById(R.id.button_stop_send_data);
        connect = (Button) findViewById(R.id.button_connect_classic);
        disconnect = (Button) findViewById(R.id.button_disconnect);

        resetData = (Switch) findViewById(R.id.switch_reset_data);
        showLog = (Switch) findViewById(R.id.switch_show_log);

        connect.setOnClickListener(this);
        sendDataIn20.setOnClickListener(this);
        sendDataIn30.setOnClickListener(this);
        sendDataIn50.setOnClickListener(this);
        stopSendData.setOnClickListener(this);
        disconnect.setOnClickListener(this);

        resetData.setOnCheckedChangeListener(this);
        showLog.setOnCheckedChangeListener(this);

        progressDialog = new ProgressDialog(this);
    }

    public void sendPackages(int frequency){
        Constants.shouldStopSendingData = false;
        mBluetoothClassic.sendPackages(frequency);
    }

    public void resetCounts(){
        dataSentCounts.setText("0");
        dataSentSuccessCounts.setText("0");
        dataSentWrongCounts.setText("0");
        dataNotBackCounts.setText("0");
        mBluetoothClassic.resetCounts();

        Message message = new Message();
        message.what = Constants.RESET_FRAGMENT_LOGS;
        EventBus.getDefault().post(new MessageEvent(message));
    }

    public void disconnect(){
        if (Constants.IS_BLE_STATE){
            mBluetoothLE.disconnect();
        }else {
            mBluetoothClassic.disconnect();
        }
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.switch_reset_data:{
                //reset data
                if (isChecked){
                    resetCounts();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            buttonView.setChecked(false);
                        }
                    },500);
                }
                break;
            }

            case R.id.switch_show_log:{
                //show log fragment
                if (isChecked){
                    if (resultFragment.isAdded()){
                        Log.e(TAG,"resultFragment已经被添加");
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.show(resultFragment).commit();
                    }else {
                        Log.e(TAG,"resultFragment未被添加");
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                        fragmentTransaction.add(R.id.fragment_container,resultFragment).commit();
                    }

                }else {
                    Log.e(TAG,"switch关闭,隐藏fragment");
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.hide(resultFragment).commit();
                }
                break;
            }
        }

    }

    public static class ResultFragment extends Fragment{

        RecyclerView resultList;
        RecyclerView.Adapter resultListAdapter;

        List<String> results = new ArrayList<>();

        public ResultFragment(){
            EventBus.getDefault().register(this);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_result,container,false);

            resultList = (RecyclerView) rootView.findViewById(R.id.result_list_error);

            resultList.setLayoutManager(new LinearLayoutManager(getActivity()));

            resultListAdapter = new ResultAdapter();

            resultList.setAdapter(resultListAdapter);

            return rootView;
        }

        private class ResultAdapter extends RecyclerView.Adapter{

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                Holder holder = new Holder(LayoutInflater.from(getActivity()).inflate(R.layout.result_item,parent,false));

                return holder;
            }

            private class Holder extends RecyclerView.ViewHolder{

                TextView result;

                public Holder(View itemView) {
                    super(itemView);
                    result = (TextView) itemView.findViewById(R.id.result_item);
                }
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                Holder mHolder = (Holder) holder;
                mHolder.result.setText(results.get(position));
            }

            @Override
            public int getItemCount() {
                return results.size();
            }
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void handleMessage(MessageEvent event){
            switch (event.message.what){
                case Constants.OK_DATA_SET_CHANGED:{
                    String result = event.message.getData().getString("result");
                    results.add(result);
                    resultListAdapter.notifyDataSetChanged();
                    break;
                }
                case Constants.RESET_FRAGMENT_LOGS:{
                    results.clear();
                    resultListAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    private class ProgressDialog extends Dialog{

        public ProgressDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.dialog_spinning_progress_bar);
        }
    }
}
