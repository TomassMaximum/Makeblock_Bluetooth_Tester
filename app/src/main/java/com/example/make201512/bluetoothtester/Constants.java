package com.example.make201512.bluetoothtester;

import java.util.UUID;

/**
 * Created by Tom on 2016/4/21.
 */
public class Constants {

    public static boolean CONNECT_STATE = false;

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

    public static final int OK_DATA_SET_CHANGED = 11;

    public static final int ERROR_DATA_SET_CHANGED = 12;

    public static final int EXCEPTION_INFO = 13;

    public static boolean IS_BLE_STATE = false;

    public static boolean shouldStopSendingData = false;

    public static int CURRENT_DEVICES_COUNT = 0;

    //测试数据
    public static final char[] TEST_DATA = {0xff,0x55,0x08,0x00,0x01,0x50,0x01,0x02,0x03,0x04,0x05,'\n'};

    //测试数据字节数组
    public static final byte[] TEST_DATA_BYTE_ARRAY = {(byte) 0xff,(byte) 0x55,(byte) 0x08,(byte) 0x00,(byte) 0x01,(byte) 0x50,(byte) 0x01,(byte) 0x02,(byte) 0x03,(byte) 0x04,(byte) 0x05};

    //测试mCore左边电机数据
    public static final char[] MCORE_TEST_DATA = {0xff,0x55,0x06,0x00,0x02,0x0a,0x09,0xff,0x00};

    //正确的返回数据:FF 55 00 30 0D 0A
    public static final String EXPECTED_RECEIVED_DATA = "ff5500300d0a";

    //错误的返回数据:FF 55 00 00 0D 0A
    public static final String EXPECTED_RECEIVED_FAIL_DATA = "ff5500000d0a";

    //获取设备时需要使用的UUID，蓝牙模块有唯一特殊的UUID，不能随机生成，一号坑。
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //可用Service的UUID
    public static final UUID SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");

    //可用Characteristic的UUID
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");

    private final static String UUID_KEY_DATA = "0000ffe1-0000-1000-8000-00805f9b34fb";

}
