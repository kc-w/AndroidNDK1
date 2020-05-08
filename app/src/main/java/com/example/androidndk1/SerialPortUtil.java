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

import static android.content.ContentValues.TAG;


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
    //发送数据线程
    private SendThread mSendThread = null;
    private Data_class mData_class = null;
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

        if (mData_class == null) {
            mData_class = new Data_class();
        }
        if (mReceiveThread == null) {
            mReceiveThread = new ReceiveThread(mData_class);
        }
        if (mSendThread == null) {
            mSendThread = new SendThread(mData_class);
        }
        mReceiveThread.start();
        mSendThread.start();
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
    byte[] readData = new byte[1024];

    ArrayList<Byte> data=new ArrayList();
    Object o = new Object();
    Boolean f = true ; // True 时线程1执行


    public class Data_class {

        ArrayList<Byte> SendData = new ArrayList();

        public  ArrayList<Byte> getSendData() {
            return SendData;
        }

        public  void add(int size,byte[] readDatum) {
            for (int i=0;i<size;i++){
                SendData.add(readDatum[i]);
            }
            Log.d(TAG, "添加数据执行 "+SendData.toString());
        }
        public  void subtract(int i) {

            Log.d(TAG, "移除数据执行 " + SendData.toString());
            for (int j = 0; j <= i; j++) {
                SendData.remove(0);
            }
        }

    }


    //接收数据的线程类
    private class ReceiveThread extends Thread {

        Data_class data_class;
        public ReceiveThread(Data_class data_class){
            this.data_class=data_class;
        }

        @Override
        public void run() {


            Log.d(TAG, "接收数据线程执行");
            //如果串口开启成功，则执行这个线程
            while (true){
                while (isStart) {
                    if (inputStream == null ) {
                        return;
                    }
                    try {
                        int size = inputStream.read(readData);
                        if (size > 0) {
                            data_class.add(size,readData);
                            isStart=false;
                        }else {
                            data.clear();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }


    private class SendThread extends Thread {

        boolean flag = true;
        int AllLen=0;

        Data_class data_class;
        public SendThread(Data_class data_class){
            this.data_class=data_class;
        }


        @Override
        public void run() {

            Log.d(TAG, "发送数据线程执行");
            while (true){

                while (!isStart){
                    ArrayList<Byte> sendDate=data_class.getSendData();
                    int size=sendDate.size();

                    if(size>0){
                        for (int i=0;i<size;i++){
                            if (sendDate.get(i) ==2 && flag){
                                data.add(sendDate.get(i));
                                AllLen=DataUtils.HLtoInt(sendDate.get(i+2),sendDate.get(i+1));
                                flag=false;
                                continue;
                            }
                            //确定首字节后进行累加操作
                            if (!flag){
                                data.add(sendDate.get(i));
                                //累加长度达到标准正确长度
                                if (data.size()==AllLen){
                                    message("Data",data);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    data.clear();
                                    data_class.subtract(i);
                                    flag=true;
                                    isStart=true;
                                    continue;
                                }

                                //有多个数据包且循环到最后一个数据时清除接收到的数据重新从缓冲区读取数据
                                if (i+1==size){
                                    data_class.subtract(i);
                                    isStart=true;
                                }

                            }

                        }
                    }
                }
            }
        }
    }



    //发送数据
    public void message(String mesage,ArrayList tempData){
        bundle.putCharSequenceArrayList(mesage,tempData);
        Message message = Message.obtain();
        message.setData(bundle);
        message.what =1 ;
        handler.sendMessage(message);
    }




    public static void test(Handler handler1){
        handler=handler1;
    }

}
