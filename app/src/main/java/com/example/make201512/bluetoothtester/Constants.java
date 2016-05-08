package com.example.make201512.bluetoothtester;

import java.util.UUID;

/**
 * Created by Tom on 2016/4/21.
 */
public class Constants {

    public static boolean CONNECTSTATE = false;

    public static final int REQUEST_ENABLE_BT = 1;

    public static final int UPDATE_PACKAGES_SENT_COUNT = 0;

    public static final int UPDATE_PACKAGES_SENT_SUCCESSFUL = 1;

    public static final int UPDATE_PACKAGES_SENT_FAIL = 2;

    public static final int UPDATE_PACKAGES_NOT_BACK = 3;

    public static final int UPDATE_BLUETOOTH_DEVICE_NAME_AND_COUNTS = 4;

    public static final int UPDATE_BLUETOOTH_DEVICES_LIST = 5;

    public static final int SCAN_DEVICES_FINISHED = 6;

    public static final int SCAN_DEVICE_FOUND = 7;

    public static final int SCAN_START = 8;

    public static final int CONNECT_STATE_CHANGED = 9;

    public static final int DEVICE_DISCONNECTED = 10;

    public static boolean IS_BLE_STATE = false;

    public static boolean shouldStopSendingData = false;

    public static int CURRENT_DEVICES_COUNT = 0;

    //测试数据
    public static final char[] TEST_DATA = {0xff,0x55,8,0,1,50,1,2,3,4,5,'\n'};

    //测试mCore左边电机数据
    public static final char[] MCORE_TEST_DATA = {0xff,0x55,6,0,2,0x0a,9,0xff,0};

    //正确的返回数据:FF 55 00 30 0D 0A
    public static final String EXPECTED_RECEIVED_DATA = "FF5500300D0A";

    //错误的返回数据:FF 55 00 00 0D 0A
    public static final String EXPECTED_RECEIVED_FAIL_DATA = "FF5500000D0A";

    //获取设备时需要使用的UUID，蓝牙模块有唯一特殊的UUID，不能随机生成，一号坑。
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //可用Service的UUID
    public static final UUID SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");

    //可用Characteristic的UUID
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");

}
