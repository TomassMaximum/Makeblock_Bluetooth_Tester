package com.example.make201512.bluetoothtester;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Message;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Debug TAG
    private static final String TAG = MainActivity.class.getSimpleName();

    TextView dataSentCounts;
    TextView dataSentSuccessCounts;
    TextView dataSentWrongCounts;
    TextView dataNotBackCounts;
    TextView connectVersion;

    TextView blueToothName;
    TextView blueToothCounts;

    Button sendDataIn20;
    Button sendDataIn50;
    Button stopSendData;
    Button connect;
    Button disconnect;
    Button editName;
    Button connectLE;

    BluetoothAdapter bluetoothAdapter;

    BluetoothClassic mBluetoothClassic;

    BluetoothLE mBluetoothLE;

    ResultFragment resultFragment;

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

            case R.id.button_connect_le:{

                Snackbar.make(v,"尚不支持蓝牙4.0",Snackbar.LENGTH_SHORT).show();

                //当前不支持蓝牙4.0
//                if (bluetoothAdapter.isEnabled()){
//                    if (Constants.CONNECTSTATE){
//                        Snackbar.make(v,"请先断开连接",Snackbar.LENGTH_SHORT).show();
//                    }else {
//                        mBluetoothLE = BluetoothLE.getInstance(this);
//                        //判断当前设备是否支持BLE
//                        if (Build.VERSION.SDK_INT >= 18){
//                            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
//                                Constants.IS_BLE_STATE = true;
//                                //当搜索按钮被点击时，弹出搜索对话框进行搜索和连接操作
//                                SearchDevicesDialog dialog = new SearchDevicesDialog(new MaterialDialog.Builder(this).customView(R.layout.search_dialog, false));
//
//                                //显示对话框
//                                dialog.show();
//                            }else {
//                                Snackbar.make(v,"当前设备不支持蓝牙4.0",Snackbar.LENGTH_SHORT).show();
//                            }
//                        }else {
//                            Snackbar.make(v,"当前设备不支持蓝牙4.0",Snackbar.LENGTH_SHORT).show();
//                        }
//                    }
//                }else {
//                    Snackbar.make(v,"请先打开蓝牙",Snackbar.LENGTH_SHORT).show();
//                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    startActivityForResult(enableBluetoothIntent,Constants.REQUEST_ENABLE_BT);
//                }

                break;
            }

            case R.id.button_send_data_in_20:{
                //当用户点击20ms发包时
                if (Constants.CONNECT_STATE){
                    Constants.shouldStopSendingData = false;
                    sendPackages(20);
                    Log.e(TAG,"消息已发送");

                    if (!resultFragment.isAdded()){
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.add(R.id.fragment_container,resultFragment).addToBackStack("").commit();
                    }

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

                    if (!resultFragment.isAdded()){
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.add(R.id.fragment_container,resultFragment).addToBackStack("").commit();
                    }

                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }

                break;
            }

            case R.id.button_stop_send_data:{
                //当停止发包按钮被点击时，设变量为true，停止发包
                if (Constants.CONNECT_STATE){
                    Constants.shouldStopSendingData = true;

                }else {
                    Snackbar.make(v,"蓝牙未连接设备",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_disconnect:{
                //当断开连接按钮被点击时，启动断开连接流程
                if (Constants.CONNECT_STATE){

                    //复位各项数据
                    dataSentCounts.setText("0");
                    dataSentSuccessCounts.setText("0");
                    dataSentWrongCounts.setText("0");
                    dataNotBackCounts.setText("0");
                    blueToothName.setText("未连接");
                    connectVersion.setText("未连接");
                    Constants.shouldStopSendingData = true;
                    Constants.CONNECT_STATE = false;
                    disconnect();
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

    @Override
    public void onBackPressed() {
        if (resultFragment.isAdded()){
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.remove(resultFragment).commit();
        }else {
            finish();
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEvent(MessageEvent event){
        Message message = event.message;
        int action = message.what;
        switch (action){
            case  Constants.UPDATE_PACKAGES_SENT_COUNT:{
                int packagesSent = (int) message.obj;
                dataSentCounts.setText(packagesSent + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_SENT_SUCCESSFUL:{
                int packagesSentSuccessful = (int) message.obj;
                dataSentSuccessCounts.setText(packagesSentSuccessful + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_SENT_FAIL:{
                int packagesSentFail = (int) message.obj;
                dataSentWrongCounts.setText(packagesSentFail + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_NOT_BACK:{
                int packagesNotBack = (int) message.obj;
                dataNotBackCounts.setText(packagesNotBack + "");
                break;
            }
            case Constants.UPDATE_BLUETOOTH_DEVICE_NAME_AND_COUNTS:{
                String deviceName = (String) message.obj;
                blueToothName.setText(deviceName);
                blueToothCounts.setText(Constants.CURRENT_DEVICES_COUNT + "");
                if (Constants.IS_BLE_STATE){
                    connectVersion.setText("蓝牙4.0");
                }else {
                    connectVersion.setText("蓝牙2.0");
                }
                break;
            }
            case Constants.EXCEPTION_INFO:{
                String exceptionInfo = message.obj.toString();
                Snackbar.make(dataNotBackCounts,"出现异常,请重新连接",Snackbar.LENGTH_SHORT).show();
                break;
            }
        }
    }

    public void init(){
        dataSentCounts = (TextView) findViewById(R.id.data_sent);
        dataSentSuccessCounts = (TextView) findViewById(R.id.data_sent_success);
        dataSentWrongCounts = (TextView) findViewById(R.id.data_sent_wrong);
        dataNotBackCounts = (TextView) findViewById(R.id.data_sent_not_back);
        connectVersion = (TextView) findViewById(R.id.connect_version);

        sendDataIn20 = (Button) findViewById(R.id.button_send_data_in_20);
        sendDataIn50 = (Button) findViewById(R.id.button_send_data_in_50);

        blueToothName = (TextView) findViewById(R.id.bluetooth_name);
        blueToothCounts = (TextView) findViewById(R.id.device_counts);

        stopSendData = (Button) findViewById(R.id.button_stop_send_data);
        connect = (Button) findViewById(R.id.button_connect_classic);
        disconnect = (Button) findViewById(R.id.button_disconnect);
        editName = (Button) findViewById(R.id.button_edit_name);
        connectLE = (Button) findViewById(R.id.button_connect_le);

        connect.setOnClickListener(this);
        sendDataIn20.setOnClickListener(this);
        sendDataIn50.setOnClickListener(this);
        stopSendData.setOnClickListener(this);
        disconnect.setOnClickListener(this);
        editName.setOnClickListener(this);
        connectLE.setOnClickListener(this);
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

    public static class ResultFragment extends Fragment{

        RecyclerView resultListError;
        RecyclerView.Adapter errorAdapter;

        List<String> errorResults = new ArrayList<>();

        public ResultFragment(){
            EventBus.getDefault().register(this);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_result,container,false);

            resultListError = (RecyclerView) rootView.findViewById(R.id.result_list_error);

            resultListError.setLayoutManager(new LinearLayoutManager(getActivity()));

            errorAdapter = new ResultErrorAdapter();

            resultListError.setAdapter(errorAdapter);

            return rootView;
        }

        private class ResultErrorAdapter extends RecyclerView.Adapter{

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
                mHolder.result.setText(errorResults.get(position));
            }

            @Override
            public int getItemCount() {
                return errorResults.size();
            }
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void handleMessage(MessageEvent event){
            if (event.message.what == Constants.EXCEPTION_INFO){
                String exceptionInfo = event.message.obj.toString();
                errorResults.add(exceptionInfo);
                errorAdapter.notifyDataSetChanged();
            }else if (event.message.what == Constants.ERROR_DATA_SET_CHANGED){
                Bundle bundle = event.message.getData();
                String result = "<" + bundle.getString("result") + ">";
                errorResults.add(result);
                errorAdapter.notifyDataSetChanged();
            }
        }

    }
}
