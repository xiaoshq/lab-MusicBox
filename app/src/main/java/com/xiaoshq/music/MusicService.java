package com.xiaoshq.music;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

public class MusicService extends Service {
    public static MediaPlayer mp= new MediaPlayer();
    public static int status = 3;

    public MusicService() {
        try {
            mp.setDataSource(Environment.getExternalStorageDirectory() + "/melt.mp3");
            mp.prepare(); //进入就绪状态
            mp.setLooping(true); //设置循环播放
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
                case 101: //播放按钮，服务处理函数
                    if (mp.isPlaying()) {
                        mp.pause();
                        status = 1;
                    } else {
                        mp.start();
                        status = 2;
                    }
                    reply.writeInt(status);
                    break;
                case 102: //停止按钮，服务处理函数
                    if (mp != null) {
                        mp.stop();
                        status = 0;
                        try {
                            mp.prepare();
                            mp.seekTo(0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        reply.writeInt(status);
                    }
                    break;
                case 103: //退出按钮，服务处理函数
                    mp.release();
                    break;
                case 104: //界面刷新，服务返回数据函数
                    int i = mp.getCurrentPosition();
                    reply.writeInt(i);
                    break;
                case 105: //拖动进度条，服务处理函数
                    mp.seekTo(data.readInt());
                    break;
                case 106: //歌曲长度
                    reply.writeInt(mp.getDuration());
                    break;
                case 107: //返回状态
                    reply.writeInt(status);
                    break;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }
}