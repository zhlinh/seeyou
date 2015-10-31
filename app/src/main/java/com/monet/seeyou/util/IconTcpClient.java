package com.monet.seeyou.util;

import com.monet.seeyou.tool.MyApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

/**
 * Created by Monet on 2015/6/16.
 * Use TCP port 2223, same as IconTcpServer.
 */

public class IconTcpClient {
    public static final int ICON_TCP_PORT = IconTcpServer.ICON_TCP_PORT;
    private String destIp;

    public IconTcpClient(String destIp){
        this.destIp = destIp;
    }

    public void start(){
        Client c = new Client();
        c.start();
    }

    class Client extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                createClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void createClient() throws Exception{
            Socket s = new Socket(destIp, ICON_TCP_PORT);

            File file = new File(MyApplication.iconPath + "me");

            BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream os =new BufferedOutputStream( s.getOutputStream());

            byte[] data = new byte[1024 * 5];
            int len = -1;
            while((len=is.read(data))!= -1){
                os.write(data,0,len);
            }

            is.close();
            os.flush();
            os.close();
        }
    }
}
