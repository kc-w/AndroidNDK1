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
    //第一个包判断
    Boolean flag = true;
    //首字节判断
    Boolean flag1 = true;
    //首字节通过标识
    Boolean flag2 = false;
    //包长度标识
    int len=0;
    //总包长度
    int AllLen ;

    //包存放数组
    byte[] readData = new byte[1024];

    //包存放数组
    byte[] tempData = new byte[1024];
    //byte数组
    ArrayList<Byte> SendData = new ArrayList();


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

                        checkFull(size,readData);



                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void checkFull(int size,byte[] readData) throws InterruptedException {


        //包长
        len = len+size;

        for(int i =0;i<size;i++){
            //第一个包
            if(flag){

                //过滤掉干扰字节,接收到第一个字节
                if(readData[i] ==2 && flag1){
                    len = len-i;
                    //连接数据
                    SendData.add(readData[i]);
                    //得到总包字节长度
                    AllLen = DataUtils.HLtoInt(readData[i+2],readData[i+1]);
                    //首字节标识
                    flag1=false;
                    //首字节通过标识
                    flag2=true;
                    //首字节判断通过后直接跳过本次循环
                    continue;
                }

                //第一个完整包判断
                if (AllLen==len-(size-i)+1){

                    if(readData[i]==3){
                        //只有一个包
                        SendData.add(readData[i]);
                        message("Data", SendData);
                        Thread.sleep(1000);
                        initialize();

                        //如果末尾还有包
                        if (size>i+1){
                            int x = 0;
                            for (int j=i+1;j<=size;j++){
                                tempData[x]=readData[j];
                                x++;
                            }
                            checkFull(x+1,tempData);
                        }else {
                            continue;
                        }
                    }

                }

                //02开头得到确认后开始加入数据
                if (flag2){
                    SendData.add(readData[i]);
                }

                //标识第一个包结束,还有其他包
                if (AllLen>len){
                    flag =false;
                }

            }else {

                //一个完整的数据包
                if (AllLen==len-(size-i)+1){

                    //包尾数据为3的话
                    if(readData[i]==3){

                        SendData.add(readData[i]);
                        message("Data", SendData);
                        Thread.sleep(1000);
                        initialize();


                        if (size>i+1){
                            int x = 0;
                            for (int j=i+1;j<=size;j++){
                                tempData[x]=readData[j];
                                x++;
                            }
                            checkFull(x+1,tempData);
                        }

                    }

                }

                SendData.add(readData[i]);

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

    //初始化数据
    public void initialize(){
        //清空数据
        SendData.clear();
        //第一个包判断
        flag = true;
        //首字节判断
        flag1 = true;
        //首字节通过标识
        flag2 = false;
        //包长度标识
        len=0;
    }


    public static void test(Handler handler1){
        handler=handler1;
    }

}
