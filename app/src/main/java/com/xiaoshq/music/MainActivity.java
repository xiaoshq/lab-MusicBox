package com.xiaoshq.music;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    ImageView img;
    TextView status;
    TextView currentTime;
    TextView totalTime;
    SeekBar seekbar;
    Button playBTN;
    Button stopBTN;
    Button quitBTN;

    IBinder ibinder;
    ServiceConnection serviceConnection;
    DateFormat pos;
    ObjectAnimator objectAnimator;
    int playStatus;
    int isInitiated;
    int isChanging; //是否正在拖动进度条改变位置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions();
        setContentView(R.layout.activity_main);
        Log.d("test","bp");
        img = (ImageView) findViewById(R.id.img);
        status = (TextView) findViewById(R.id.status);
        currentTime = (TextView) findViewById(R.id.currentTime);
        totalTime = (TextView) findViewById(R.id.totalTime);
        seekbar = (SeekBar) findViewById(R.id.seekbar);
        playBTN = (Button) findViewById(R.id.playBTN);
        stopBTN = (Button) findViewById(R.id.stopBTN);
        quitBTN = (Button) findViewById(R.id.quitBTN);
        pos = new SimpleDateFormat("mm:ss", Locale.CHINA);

        playBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int code = 101;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    ibinder.transact(code, data, reply, 0);
                    playStatus = reply.readInt();
                    refresh();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        stopBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int code = 102;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    ibinder.transact(code, data, reply, 0);
                    playStatus = reply.readInt();
                    refresh();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        quitBTN.setOnClickListener(new View.OnClickListener() { //停止服务时，解除绑定
            @Override
            public void onClick(View view) {
                unbindService(serviceConnection);
                serviceConnection = null;
                try {
                    MainActivity.this.finish();
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // bindService成功后回调onServiceConnected函数，
        // 通过IBinder获取Service对象，实现Activity与Service绑定
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                Log.d("service","connected");
                ibinder = service;
                try { //通过IBinder与Service通信
                    int code = 106; //歌曲长度
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    ibinder.transact(code, data, reply, 0);
                    int p = reply.readInt();
                    seekbar.setMax(p);// seekbar设置最大进度
                    totalTime.setText(pos.format(p));// 显示歌曲总时长
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                serviceConnection = null;
            }
        };
        // 调用bindService保持与Service的通信，Activity启动时绑定Service
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        isChanging = 0;
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //监听SeekBar进度值的改变
                currentTime.setText(pos.format(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //监听SeekBar开始拖动
                isChanging = 1;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //监听SeekBar停止拖动
                //获取拖动结束之后的位置，跳转到某个位置播放
                try {
                    int code = 105;
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInt(seekbar.getProgress());
                    ibinder.transact(code, data, reply, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isChanging = 0;
            }
        });

        //通过Handler更新UI上的组件状态
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 101:
                        try {
                            int code = 104;
                            Parcel data = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            ibinder.transact(code, data, reply, 0);
                            seekbar.setProgress(reply.readInt());
                            //确保播放时退出再进来图像继续旋转
                            data = Parcel.obtain();
                            reply = Parcel.obtain();
                            ibinder.transact(107, data, reply, 0);
                            playStatus = reply.readInt();
                            refresh();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        };

        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (serviceConnection != null) {
                        handler.obtainMessage(101).sendToTarget();
                    }
                }
            }
        };
        thread.start();

        isInitiated = 1;
        objectAnimator = ObjectAnimator.ofFloat(img,"rotation",0f,360f);
        objectAnimator.setDuration(20000);
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator.setRepeatMode(ObjectAnimator.RESTART);
    }

    private void verifyStoragePermissions()
    {
        try { //检测是否有读取的权限
            int permissions = ActivityCompat.checkSelfPermission
                    (MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE);
            if(permissions != PackageManager.PERMISSION_GRANTED) { //没有读取权限，弹出对话框申请读取权限
                ActivityCompat.requestPermissions
                        (MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //权限被用户同意，可以做想做的事
        } else {
            //权限被用户拒绝
            System.exit(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void refresh() {
        switch (playStatus) {
            case 0:
                status.setText("Stopped");
                playBTN.setText("PLAY");
                objectAnimator.end();
                isInitiated = 1;
                break;
            case 1:
                status.setText("Paused");
                playBTN.setText("PLAY");
                objectAnimator.pause();
                break;
            case 2:
                status.setText("Playing");
                playBTN.setText("PAUSE");
                if (isInitiated == 1) objectAnimator.start();
                else objectAnimator.resume();
                isInitiated = 0;
                break;
            default:
                status.setText("");
                break;
        }
    }
}
