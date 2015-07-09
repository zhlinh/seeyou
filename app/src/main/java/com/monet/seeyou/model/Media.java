package com.monet.seeyou.model;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

/**
 *
 * Created by Monet on 2015/6/16.
 * 发送和接收录音的类
 *
 */
public class Media {
    private String appPath, sendPath, receivePath;//音频文件保存地址
    private String name;//储存文件的名字
    private MediaRecorder myRecorder;
    private MediaPlayer myPlayer;
    private File saveFilePath;

    public static Random random = new Random();

    public Media() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            //设置要发送的录音的保存路径
            try {
                //建立两层目录时需要逐层建立
                appPath = Environment.getExternalStorageDirectory().
                        getCanonicalPath().toString() + "/SeeYou";
                File file1 = new File(appPath);
                if (!file1.exists()) {
                    file1.mkdir();
                }
                sendPath = appPath + "/MessageMediaSend";
                File file2 = new File(sendPath);
                if (!file2.exists()) {
                    file2.mkdir();
                }
            } catch (Exception e) {
                e.getStackTrace();
            }
        }

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            //设置接收到的录音存放的路径
            try {
                //建立两层目录时需要逐层建立
                appPath = Environment.getExternalStorageDirectory().
                        getCanonicalPath().toString() + "/SeeYou";
                File file1 = new File(appPath);
                if (!file1.exists()) {
                    file1.mkdir();
                }
                receivePath = appPath + "/MessageMediaReceive";
                File file2 = new File(receivePath);
                if (!file2.exists()) {
                    file2.mkdir();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startRecord() {
        myRecorder = new MediaRecorder();
        myRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);//设置从麦克风录音
        myRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);//设置输出格式
        myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);//设置编码格式

        this.name = "AND" + getRandomId() + new SimpleDateFormat(
                "yyyyMMddHHmmss", Locale.CHINA).format(System
                .currentTimeMillis()) + ".amr";//给文件命名：AND + 4位随机数 + 日期 + .amr

        String paths = sendPath + "/" + name;    //文件的绝对路径
        saveFilePath = new File(paths);    //获取到文件对象

        myRecorder.setOutputFile(saveFilePath.getAbsolutePath());//设置录音的输出文件

        try {
            saveFilePath.createNewFile();
            myRecorder.prepare();
        } catch (Exception e) {
            e.getStackTrace();
        }

        myRecorder.start();//开始录音
    }

    public void stopRecord() {
        if (saveFilePath.exists() && saveFilePath != null) {
            myRecorder.stop();
            myRecorder.release();
            myRecorder = null;
        }
    }

    public void startPlay(String path0) {
        myPlayer = new MediaPlayer();

        try {
            myPlayer.reset();
            myPlayer.setDataSource(path0);
            if (!myPlayer.isPlaying()) {
                myPlayer.prepare();
                myPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void deleteOldFile(String paths) {
        try {
            File file = new File(paths);
            if (file.exists()) {
               file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static String getRandomId() {
        return random.nextInt(9999) + "";
    }

    public String getName() {
        return this.name;
    }

    public String getSendPath() {
        return this.sendPath;
    }

    public String getReceivePath() {
        return this.receivePath;
    }
}

