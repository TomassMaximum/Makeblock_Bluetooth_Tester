# Makeblock_Bluetooth_Tester

为Makeblock编写的一个测试蓝牙通讯性能的小app。可以与Makeblock的蓝牙模块建立连接（作为客户端），以用户设定的频率向蓝牙服务端发送数据包。监测返回的数据包是否正确，并更新到UI。
因为当前没有拿到模块，所以未进行针对性测试，可能会有bug。实测可以与mbot的蓝牙模块建立连接并发送有效数据。
后续将会更新对蓝牙4.0的支持。