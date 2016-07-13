package com.example.make201512.bluetoothtester;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
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

    ResultFragment resultFragment;

    ProgressDialog progressDialog;

    SearchDevicesDialog searchDevicesDialog;

    Dialog pairAlertDialog;

    private int packagesSentCount;
    private int packagesBackSuccess;
    private int packagesBackFail;
    private int packagesNotBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG,"Activity onCreate()");

        //initialize Activity
        init();

        //get BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //if current device support Bluetooth
        if (bluetoothAdapter == null){
            //does not support Bluetooth,toast to inform
            Toast.makeText(this, "丫的设备并不支持蓝牙啊喂", Toast.LENGTH_SHORT).show();

            //app finish
            finish();
        }

        //pop an dialog to the user to enable bluetooth
        if (!bluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent,Constants.REQUEST_ENABLE_BT);
        }

        //set showLog switch to true
        showLog.setChecked(true);

        //start Bluetooth service
        Intent intent = new Intent(this,BluetoothService.class);
        bindService(intent,serviceConnection,BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        Log.e(TAG,"onPause()");
        super.onPause();
    }

    @Override
    protected void onStart() {
        Log.e(TAG,"onStart()");
        //register EventBus
        EventBus.getDefault().register(this);
        if (BluetoothClassic.getInstance().isConnected()){
            update4ConnectSuccess();
        }else {
            blueToothName.setText("未连接");
        }
        super.onStart();
    }

    public void update4ConnectSuccess(){
        progressDialog.dismiss();
        for (int i = 0;i < BluetoothClassic.getInstance().getDeviceBeans().size();i++){
            DeviceBean deviceBean = BluetoothClassic.getInstance().getDeviceBeans().get(i);
            if (deviceBean.connected){
                BluetoothDevice bluetoothDevice = deviceBean.getBluetoothDevice();
                String name = bluetoothDevice.getName();
                blueToothName.setText(name);
                return;
            }
        }
    }

    public void update4ConnectCancel(){
        blueToothName.setText("未连接");
        Message message = new Message();
        message.what = Constants.RESET_DATA_AND_LOGS;
        EventBus.getDefault().post(new MessageEvent(message));
    }

    public void resetData(){
        BluetoothClassic.getInstance().resetData();
        packagesSentCount = 0;
        packagesBackSuccess = 0;
        packagesBackFail = 0;
        packagesNotBack = 0;

        dataSentCounts.setText("0");
        dataSentSuccessCounts.setText("0");
        dataSentWrongCounts.setText("0");
        dataNotBackCounts.setText("0");
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
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
                MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
                builder.customView(R.layout.search_dialog,false);
                searchDevicesDialog = new SearchDevicesDialog(builder);
                searchDevicesDialog.show();
                break;
            }

            case R.id.button_send_data_in_20:{
                if (BluetoothClassic.getInstance().isConnected()){
                    Constants.shouldStopSendingData = false;
                    BluetoothClassic.getInstance().write(20);
                }else {
                    Snackbar.make(v,"请先建立蓝牙连接",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_send_data_in_30:{
                if (BluetoothClassic.getInstance().isConnected()){
                    Constants.shouldStopSendingData = false;
                    BluetoothClassic.getInstance().write(30);
                }else {
                    Snackbar.make(v,"请先建立蓝牙连接",Snackbar.LENGTH_SHORT).show();
                }

                break;
            }

            case R.id.button_send_data_in_50:{
                if (BluetoothClassic.getInstance().isConnected()){
                    Constants.shouldStopSendingData = false;
                    BluetoothClassic.getInstance().write(50);
                }else {
                    Snackbar.make(v,"请先建立蓝牙连接",Snackbar.LENGTH_SHORT).show();
                }

                break;
            }

            case R.id.button_stop_send_data:{
                if (BluetoothClassic.getInstance().isConnected()) {
                    Constants.shouldStopSendingData = true;
                }
                else {
                    Snackbar.make(v,"请先建立蓝牙连接",Snackbar.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.button_disconnect:{
                if (BluetoothClassic.getInstance().isConnected()){
                    Constants.shouldStopSendingData = true;
                    BluetoothClassic.getInstance().disconnectBluetooth();
                    update4ConnectCancel();
                }else {
                    Snackbar.make(v,"请先建立蓝牙连接",Snackbar.LENGTH_SHORT).show();
                }

                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.switch_reset_data:{
                //reset data
                Message message = new Message();
                message.what = Constants.RESET_DATA_AND_LOGS;
                EventBus.getDefault().post(new MessageEvent(message));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetData.setChecked(false);
                    }
                },1000);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEvent(MessageEvent event){
        Message message = event.message;
        int action = message.what;
        switch (action){
            case Constants.BT_CONNECT_START:{
                Log.e(TAG,"start connecting");
                searchDevicesDialog.dismiss();
                progressDialog.show();
                break;
            }
            case Constants.BT_CONNECT_FINISHED:{
                Log.e(TAG,"connecting finished");
                update4ConnectSuccess();
                break;
            }
            case Constants.REQUEST_PAIRING_DIALOG:{
                if (pairAlertDialog == null) {
                    pairAlertDialog = new AlertDialog.Builder(this).setTitle("请去系统设置配对蓝牙")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.e(TAG,"点击取消,消灭动画");
                                }
                            }).show();
                } else {
                    pairAlertDialog.show();
                }
                break;
            }
            case Constants.UPDATE_PACKAGES_SENT_COUNT:{
                packagesSentCount = (int) message.obj;
                dataSentCounts.setText(packagesSentCount + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_SENT_SUCCESSFUL:{
                packagesBackSuccess = (int) message.obj;
                dataSentSuccessCounts.setText(packagesBackSuccess + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_SENT_FAIL:{
                packagesBackFail = (int) message.obj;
                dataSentWrongCounts.setText(packagesBackFail + "");
                break;
            }
            case Constants.UPDATE_PACKAGES_NOT_BACK:{
                packagesNotBack = packagesSentCount - packagesBackSuccess - packagesBackFail;
                dataNotBackCounts.setText(packagesNotBack + "");
                break;
            }
            case Constants.RESET_DATA_AND_LOGS:{
                resetData();
                break;
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
        resultFragment = new ResultFragment();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
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
                case Constants.UPDATE_LOGS:{
                    String log = event.message.getData().getString("result");
                    results.add(log);
                    resultListAdapter.notifyDataSetChanged();
                    break;
                }
                case Constants.RESET_DATA_AND_LOGS:{
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
