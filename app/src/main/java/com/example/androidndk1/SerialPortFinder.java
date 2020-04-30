package com.example.androidndk1;

import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

/**
 * Google官方代码
 * 此类的作用为，寻找得到有效的串口的物理地址。
 * 如果你本身就知道串口的地址如：ttyS1、ttyS2，那么这个类就可以不用了。
 *
 */

public class SerialPortFinder {


	private static final String TAG = "SerialPortFinder";
	private Vector<Driver> mDrivers = null;
	private Iterator<Driver>  serials = null;


	//读取可用的串口设备
	public Vector<Driver> getDrivers() throws IOException {

		mDrivers = new Vector<Driver>();
		//读文件
		LineNumberReader r = new LineNumberReader(new FileReader("/proc/tty/drivers"));
		String line;
		//遍历读取到的串口
		while((line = r.readLine()) != null) {

			//正则匹配分隔符,以一个或多个空格作为分隔符
			String[] w = line.split(" +");

			//找出最后一个数组为'serial'的串口设备
			if ((w.length >= 5) && (w[w.length-1].equals("serial"))) {

				//从下标为0开始截取到下标为19的字符串,再去掉字符串两端Unicode编码小于等于32（\u0020,含空格)的所有字符
				//获得设备名
				String drivername = line.substring(0,20).trim();
				//得到设备路径
				String path = w[w.length-4];

				//设备名及设备路径
				Log.d(TAG, "设备名:"+drivername + "     路径:"+ path);

				Driver driver = new Driver(drivername, path);
				//将设备名及设备路径传入
				mDrivers.add(driver);
			}
		}
		//关闭读取流
		r.close();
		return mDrivers;
	}



	public ArrayList<String>  getDevices(Iterator<Driver>  serials) {

		ArrayList<String> serialList = new ArrayList<String>();

		File dev = new File("/dev");
		//返回一个抽象路径名数组
		File[] files = dev.listFiles();

		//对传过来的串口对象进行迭代
		while (serials.hasNext()) {
			Driver driver = serials.next();

			//循环这个目录下所有文件
			for (int i = 0; i < files.length; i++) {
				//绝对路径和指定的路径做计较,判断是否以指定路径开头
				if (files[i].getAbsolutePath().startsWith(driver.getPath())) {
					//得到可用的串口
					Log.d(TAG, "可用串口路径: " + files[i]);
					//将可用的串口加入到迭代集合
					serialList.add(files[i].toString());
				}
			}
		}

		return serialList;
	}




	//得到所有串口设备
	public void getAllDevicesPath() throws IOException {
		serials = getDrivers().iterator();
		getDevices(serials);
	}

}
