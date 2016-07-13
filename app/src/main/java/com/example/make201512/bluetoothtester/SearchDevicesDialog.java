package com.example.make201512.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

import java.util.ArrayList;

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

    //构造方法，接受DialogBuilder，初始化对话框。
    protected SearchDevicesDialog(Builder builder) {
        super(builder);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //初始化Views，获取到相应的蓝牙适配器
        init();

        //注册EventBus
        EventBus.getDefault().register(this);

    }


    /**
     * ListView监听器，当任意一项item被点击时，开启连接线程进行连接操作
     * */
    private class DeviceOnClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DeviceBean bean = BluetoothClassic.getInstance().getDeviceBeans().get(position);
            BluetoothClassic.getInstance().startConnect(bean);

            Message message = new Message();
            message.what = Constants.BT_CONNECT_START;
            EventBus.getDefault().post(new MessageEvent(message));

            BluetoothClassic.getInstance().stopDiscovery();

            dismiss();
        }
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

        //start discovery
        BluetoothClassic.getInstance().startDiscovery();
    }

    @Subscribe
    public void handleEvent(MessageEvent event){
        Message message = event.message;
        switch (message.what){
            case Constants.SCAN_DEVICES_FINISHED:{
                //discover finished
                progressWheel.stopSpinning();
                searchTextView.setText("再来一次");
                searchTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        searchTextView.setText("正在搜索蓝牙设备");
                        progressWheel.spin();
                        arrayAdapter.clear();
                        arrayAdapter.notifyDataSetChanged();

                        BluetoothClassic.getInstance().startDiscovery();
                    }
                });
                break;
            }

            case Constants.SCAN_DEVICE_FOUND:{
                //更新ListView
                ArrayList<DeviceBean> beans = BluetoothClassic.getInstance().getDeviceBeans();
                arrayAdapter.clear();
                for (int i = 0;i < beans.size();i++){
                    DeviceBean deviceBean = beans.get(i);
                    BluetoothDevice device = deviceBean.getBluetoothDevice();
                    String name = device.getName();
                    String mac = device.getAddress();
                    String deviceInfo = name + "\n" + mac;
                    arrayAdapter.add(deviceInfo);
                }
                arrayAdapter.notifyDataSetChanged();

                break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
