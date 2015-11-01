package com.monet.seeyou.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.monet.seeyou.service.ChatService;
import com.monet.seeyou.tool.MemoryCache;
import com.monet.seeyou.tool.MyApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Monet on 2015/6/16.
 * Use TCP port 2223
 */

public class IconTcpServer{
    public static final int ICON_TCP_PORT = 2223;
    private String deviceCode = null;
    ChatService service = null;

    public IconTcpServer(String deviceCode, ChatService service){
        this.deviceCode = deviceCode;
        this.service = service;
    }

    public void start(){
        server s= new server();
        s.start();
    }

    class server extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                createServer();//创建tcp服务端
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void createServer() throws IOException, Exception{
            ServerSocket ss =new ServerSocket(ICON_TCP_PORT);
            Socket s = ss.accept();//开始监听


            File file = new File(MyApplication.iconPath + deviceCode);//接收到的头像的存储路径
            if(file.exists()){//把之前的头像删除掉
                file.delete();
            }
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }


            BufferedInputStream is = new BufferedInputStream(s.getInputStream()); // 读进
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));// 写出
            Thread.sleep(1000);

            byte[] data = new byte[1024 * 5];
            int len = -1;

            while((len = is.read(data)) != -1){
                os.write(data,0,len);
            }

            is.close();
            os.flush();
            os.close();
            s.close();
            ss.close();

            // 将接收到的Icon文件存到缓存中
            Bitmap bitmap1 = BitmapFactory.decodeFile(MyApplication.iconPath + deviceCode);
            if (bitmap1 != null) {
                MemoryCache.getInstance().put(deviceCode, bitmap1);//放入缓存中
            }
            // 调用ChatService的onreceive方法来广播，通知刷新列表
            service.onReceiver(ChatService.RECEIVE_ICON);
        }
    }

}
