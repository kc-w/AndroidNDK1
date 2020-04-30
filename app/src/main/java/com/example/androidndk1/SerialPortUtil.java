package com.example.androidndk1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;



//通过串口用于接收或发送数据
public class SerialPortUtil {

    private static Handler handler = null;
    //串口操作对象
    private SerialPort serialPort = null;
    //串口输入流
    private InputStream inputStream = null;
    //串口输出流
    private OutputStream outputStream = null;
    //接收数据线程
    private ReceiveThread mReceiveThread = null;
    //串口开启标识
    private boolean isStart = false;

    /**
     * 打开串口，接收数据
     * 通过串口，接收发送来的数据
     */
    public void openSerialPort(String path,int bit,int flag) {
        try {
            serialPort = new SerialPort(new File(path), bit, flag);
            //调用对象SerialPort方法，获取串口中"读和写"的数据流
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            isStart = true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        //接收数据
        getSerialPort();
    }

    //开启线程接收数据
    private void getSerialPort() {
        if (mReceiveThread == null) {
            mReceiveThread = new ReceiveThread();
        }
        mReceiveThread.start();
    }

    /**
     * 关闭串口
     * 关闭串口中的输入输出流
     */
    public void closeSerialPort() {
        Log.i("SerialPortUtil", "关闭串口");
        try {

            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            isStart = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





//    public void sendRecord() {
//        try {
//
//            outputStream.write("");
//            outputStream.flush();
//        } catch (IOException | StructException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 发送数据
     * 通过串口，发送数据到单片机
     *
     * @param data 要发送的数据
     */
    public void sendSerialPort(String data) {
        try {
            //将发送的十六进制字符串转换为数组
            byte[] sendData = DataUtils.HexToByteArr(data);
            if (sendData.length==2){
                System.out.println(sendData[0]+" "+sendData[1]);
            }

            //发送给下位机
            outputStream.write(sendData);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendSerialPort(byte[] data) {
        try {
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Bundle bundle = new Bundle();
    //包存放数组
    byte[] readData = new byte[1];
    //byte数组
    List<Byte> TempData = new ArrayList();



    //接收数据的线程类
    private class ReceiveThread extends Thread {
        @Override
        public void run() {

            super.run();
            //如果串口开启成功，则执行这个线程
            while (isStart) {
                if (inputStream == null ) {
                    return;
                }
                try {

                    int size = inputStream.read(readData);

                    if (size > 0) {

                        for (Byte bytes:readData){
                            TempData.add(bytes);
                        }





//                        String readString = DataUtils.ByteArrToHex(readData, 0, size);
//                        //020600110D03
//                        Log.e(getClass().getSimpleName()+"收到串口数据字节数据转换为十六进制:", readString);



                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void message(int i,String date){

        bundle.putString("msg", date);
        Message message = Message.obtain();
        message.setData(bundle);
        message.what =i ;
        handler.sendMessage(message);
    }




    public static void test(Handler handler1){
        handler=handler1;
    }

}
