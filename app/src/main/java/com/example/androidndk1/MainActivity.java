package com.example.androidndk1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Boolean flag =true;


    //串口操作对象
    private SerialPortUtil serialPortUtil;

    private TextView textView;


    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                //更新ui
                Bundle bundle = msg.getData();
                ArrayList date=(ArrayList)bundle.get("Data");

                textView.setText(date.toString());
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.t1);


        serialPortUtil.test(handler);









        // Example of a call to a native method
        Button b1 = findViewById(R.id.b1);
        b1.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {

                //获取可用串口
                try {
                    SerialPortFinder prot = new SerialPortFinder();
                    prot.getAllDevicesPath();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

        });

        Button b2 = findViewById(R.id.b2);
        b2.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {

                serialPortUtil = new SerialPortUtil();
                //开启串口传入串口名,波特率
                serialPortUtil.openSerialPort("/dev/ttyS1",9600,0);


            }

        });



        Button b3 = findViewById(R.id.b3);
        b3.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {

                Log.d("MainActivity", "发出命令 ");

//                serialPortUtil.sendRecord();



//                //发送系统参数
                serialPortUtil.sendSerialPort("0219001A");
                //keyword
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(100000,4));
                //EnIDcount
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(0,2));
                //Start_pos
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(1390,2));
                //WinWidth
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(80,2));
                //TestStep
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(-700,2));
                //MoveSpeed
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(90,2));
                //TestSpeed
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(160,2));
                //MaxStep
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(12000,2));
                //Bak
                serialPortUtil.sendSerialPort(DataUtils.InttoLH(0,2));

                serialPortUtil.sendSerialPort("03");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

//                serialPortUtil.sendSerialPort("0205001603");


            }

        });

        Button b4 = findViewById(R.id.b4);
        b4.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {

                serialPortUtil.closeSerialPort();


            }

        });


    }




}
