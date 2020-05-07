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
    private SendThread mData_class = null;
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
        if (mSendThread == null) {
            mSendThread = new SendThread();
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
    byte[] readData = new byte[1024];

    //byte数组
    ArrayList<Byte> SendData = new ArrayList();




    //接收数据的线程类
    private class ReceiveThread extends Thread {

        private boolean wp=false;
        //创建synchronized关键字对象
        private Object obj=new Object();
        public void blocked() {
            wp=true;
            //obj.wait();如果wait()方法写在这相当于主线程调用wait()方法而不是子线程
        }
        public void wakeup() {
            wp=false;
            synchronized(obj) {
                obj.notifyAll();
            }
        }

        @Override
        public void run() {

            //如果串口开启成功，则执行这个线程
            while (isStart) {

                if(wp) {
                    try {
                        //notifyAll和wait语句一定要编写在synchronized(){}块中
                        synchronized(obj) {
                            obj.wait();//子线程调用wait()
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                if (inputStream == null ) {
                    return;
                }

                int size = 0;

                try {

                    Log.e(TAG, "锁住效验数据线程");
                    mSendThread.blocked();

                    size = inputStream.read(readData);
                    if (size > 0) {

                        for (int i=0;i<size;i++){
                            SendData.add(readData[i]);
                        }

                        Log.e(TAG, SendData.toString());
                        Log.e(TAG, "解锁效验数据线程");
                        mSendThread.wakeup();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    ArrayList<Byte> data=new ArrayList();
    int AllLen=0;


    private class SendThread extends Thread {


        private boolean wp=false;
        //创建synchronized关键字对象
        private Object obj=new Object();
        public void blocked() {
            wp=true;
            //obj.wait();如果wait()方法写在这相当于主线程调用wait()方法而不是子线程
        }
        public void wakeup() {
            wp=false;
            synchronized(obj) {
                obj.notifyAll();
            }
        }

        @Override
        public void run() {

            while (isStart){

                if(wp) {
                    try {
                        //notifyAll和wait语句一定要编写在synchronized(){}块中
                        synchronized(obj) {
                            obj.wait();//子线程调用wait()
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                Log.e(TAG, "锁住接收数据线程");
                mReceiveThread.blocked();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int size=SendData.size();
                boolean flag = true;

                if(SendData.size()>0){

                    for (int i=0;i<size;i++){

                        //第一个
                        if (SendData.get(i) ==2 && flag){
                            data.add(SendData.get(i));
                            AllLen=DataUtils.HLtoInt(SendData.get(i+2),SendData.get(i+1));
                            flag=false;
                        }
                        if (!flag){
                            data.add(SendData.get(i));
                            if (i+1==AllLen){

                                for (int j=0;j<i+1;j++){
                                    SendData.remove(0);
                                }
                                message("Data",data);
                                data.clear();
                            }


                        }

                        if (i==size-1){
                            Log.e(TAG, "解锁接收数据线程");
                            mReceiveThread.wakeup();
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
