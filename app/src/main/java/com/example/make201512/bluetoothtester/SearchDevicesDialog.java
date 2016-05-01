package com.example.make201512.bluetoothtester;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * 自定义对话框类，使用第三方库MaterialDialog。
 * 当用户点击搜索时弹出本对话框，自动开始搜索，将搜索的过程添加到ListView中显示。
 * 用户选择一项点击后，与选中的蓝牙设备建立连接，对话框自动消失，并将连接获取的Socket传回Activity供其互传数据使用。
 * 本类包含了蓝牙连接的线程，连接蓝牙模块时将蓝牙模块作为服务端进行连接，不需要写服务端。
 *
 * Created by Tom on 2016/4/17.
 */
public class SearchDevicesDialog extends MaterialDialog {

    //Debug TAG
    private static final String TAG = SearchDevicesDialog.class.getSimpleName();

    //对话框顶部的文本框，搜索时显示正在搜索，搜索完毕后显示为再来一次，点击可以进行新一轮的搜索
    TextView searchTextView;

    //用于显示搜索到的设备列表，包含设备蓝牙名称与设备蓝牙MAC地址
    ListView listView;

    //旋转进度条，第三方库。搜索时播放旋转动画，搜索完毕后自动消失
    ProgressWheel progressWheel;

    //ListView的适配器
    ArrayAdapter<String> arrayAdapter;

    BluetoothClassic mBluetoothClassic;

    BluetoothLE mBluetoothLE;

    //构造方法，接受DialogBuilder，初始化对话框。
    protected SearchDevicesDialog(Builder builder) {
        super(builder);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //初始化Views，获取到相应的蓝牙适配器
        init();

        //开始搜索
        searchStart();

        //注册EventBus
        EventBus.getDefault().register(this);

    }


    /**
     * ListView监听器，当任意一项item被点击时，开启连接线程进行连接操作
     * */
    private class DeviceOnClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            connectDevice(position);
            dismiss();
        }
    }

    /**
     * 搜索文本框监听器。
     * 当用户点击搜索文本框再次进行搜索时，做出相应动作。
     * */
    private class searchListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            //搜索开始
            searchStart();
        }
    }

    //开始搜索的方法
    private void searchStart(){
        //清除当前ListView中的所有内容
        arrayAdapter.clear();

        //使旋转进度条开始旋转，标识搜索正在进行中
        progressWheel.spin();

        //将搜索文本框的文字内容设为正在搜索状态
        searchTextView.setText("正在搜索蓝牙设备...");

        //设置搜索文本框为不可点击
        searchTextView.setClickable(false);

        scanDevices();

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

    //初始化操作的方法
    public void init(){
        //找到搜索文本框，ListView和进度条
        searchTextView = (TextView) findViewById(R.id.start_search);
        listView = (ListView) findViewById(R.id.list_view);
        progressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);

        //创建ListView适配器
        arrayAdapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1);

        //为ListView设置适配器
        listView.setAdapter(arrayAdapter);

        //为ListView的items设置监听
        listView.setOnItemClickListener(new DeviceOnClickListener());

        if (Constants.IS_BLE_STATE){
            mBluetoothLE = BluetoothLE.getInstance(getContext());
        }else {
            mBluetoothClassic = BluetoothClassic.getInstance(getContext());
        }
    }

    @Subscribe
    public void handleEvent(MessageEvent event){
        Message message = event.message;
        switch (message.what){
            case Constants.SCAN_START:{
                //开始搜索
                searchStart();
                break;
            }
            case Constants.SCAN_DEVICE_FOUND:{
                //找到蓝牙设备，更新List
                String devicesInfo = message.obj.toString();
                arrayAdapter.add(devicesInfo);
                arrayAdapter.notifyDataSetChanged();
                break;
            }
            case Constants.SCAN_DEVICES_FINISHED:{
                //搜索动作完成，
                searchFinished();
                break;
            }
            case Constants.CONNECT_STATE_CHANGED:{
                //连接设备成功
                break;
            }
        }
    }

    //对话框从窗口剥离的回调方法，在这里进行善后工作
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        if (!Constants.IS_BLE_STATE){
            unRegisterBroadcastReceiver();
        }
    }

    public void scanDevices(){
        if (Constants.IS_BLE_STATE){
            mBluetoothLE.scanLE(true);
        }else {
            Log.e(TAG,"对话框的scanClassic被执行到");
            mBluetoothClassic.scanClassic();
            mBluetoothClassic.registerBroadcast();
        }
    }

    public void connectDevice(int index){
        if (Constants.IS_BLE_STATE){
            mBluetoothLE.connect(index);
        }else {
            mBluetoothClassic.connectDevice(index);
        }
    }

    public void unRegisterBroadcastReceiver(){
        mBluetoothClassic.unRegisterBroadcastReceiver();
    }
}
