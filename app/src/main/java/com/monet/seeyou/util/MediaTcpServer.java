package com.monet.seeyou.util;

import com.monet.seeyou.model.Media;
import com.monet.seeyou.model.UdpMessage;
import com.monet.seeyou.service.ChatService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Monet on 2015/6/16.
 * Use TCP port 2225
 */

public class MediaTcpServer{
    public static final int MEDIA_TCP_PORT = 2225;
    ChatService service = null;
    UdpMessage msg;
    String senderIp;//信息的来源

    public MediaTcpServer(UdpMessage msg, String ip, ChatService service){
        this.msg = msg;
        senderIp = ip;
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
            ServerSocket ss =new ServerSocket(MEDIA_TCP_PORT);
            Socket s = ss.accept();//开始监听


            File file = new File(new Media().getReceivePath() + "/" + msg.getMsg());//接收到的文件的存储路径
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

            //接收完消息后，刷新界面
            msg.setType(ChatService.RECEIVE_MEDIA);
            String hostAddress = senderIp;
            if (service.getMsgs().containsKey(hostAddress)) {
                service.getMsgs().get(hostAddress).add(msg);
            } else {
                Queue<UdpMessage> queue = new ConcurrentLinkedQueue<UdpMessage>();
                queue.add(msg);
                service.getMsgs().put(hostAddress, queue);
            }

            service.onReceiver(ChatService.RECEIVE_MEDIA);
            //把msg放入消息队列中
            //调用ChatService的onreceive方法来广播，通知刷新列表
        }
    }


}
