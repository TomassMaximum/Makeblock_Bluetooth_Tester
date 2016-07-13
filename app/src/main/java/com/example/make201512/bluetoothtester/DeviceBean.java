package com.example.make201512.bluetoothtester;

import android.bluetooth.BluetoothDevice;

/**
 * 蓝牙设备的Bean
 * @author liuming
 */
public class DeviceBean {
	
	private BluetoothDevice bluetoothDevice;
	public Boolean connected;

	public DeviceBean(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
		this.connected = false;
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}
	public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}
	
}
