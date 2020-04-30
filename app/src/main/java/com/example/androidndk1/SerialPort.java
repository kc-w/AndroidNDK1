package com.example.androidndk1;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Google官方代码
 * 此类的作用为，JNI的调用，用来加载.so文件的
 * 获取串口输入输出流
 */

public class SerialPort {

    private static final String TAG = "SerialPort";
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {


//        Process process = Runtime.getRuntime().exec("/system/xbin/su");
//
//        //设置操作权限,//手动给权限x
//        if (!device.canRead() || !device.canWrite()) {
//            try {
//                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"+"exit \n";
//                //向安卓写入cmd命令
//                OutputStream outputStream = process.getOutputStream();
//                outputStream.write(cmd.getBytes());
//                //释放输出流
//                outputStream.flush();
//                outputStream.close();
//                //等待shell命令执行完成
//                process.waitFor();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new SecurityException();
//            }
//        }

        Log.d(TAG, "准备开启串口");

        //串口对象
        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Log.e(TAG, "串口开启异常!!!!!");
            throw new IOException();
        }
        //得到该串口的输入输出流对象
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }


    // JNI
    private native static FileDescriptor open(String path, int baudrate, int flags);

    public native static void close();

    static {
        Log.d(TAG, "--------引入so库---------");
        System.loadLibrary("native-lib");
    }
}
