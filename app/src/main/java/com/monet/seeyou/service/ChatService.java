package com.monet.seeyou.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.monet.seeyou.activity.ChatActivity;
import com.monet.seeyou.model.UdpMessage;
import com.monet.seeyou.model.User;
import com.monet.seeyou.tool.MyApplication;
import com.monet.seeyou.activity.MainActivity;
import com.monet.seeyou.util.IconTcpClient;
import com.monet.seeyou.util.ImageTcpClient;
import com.monet.seeyou.util.ImageTcpServer;
import com.monet.seeyou.util.MediaTcpClient;
import com.monet.seeyou.util.MediaTcpServer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Monet on 2015/6/16.
 */

public class ChatService extends Service {
    public static final int MESSAGE_PORT = 2425;
    // 消息类型
    public static final int ON_LINE = 101;
    public static final int REPLY_ONLINE = 102;
    public static final int TEXT_MESSAGE = 103;
    public static final int REQUEST_SEND_MEDIA = 104;
    public static final int REPLY_SEND_MEDIA = 105;
    public static final int RECEIVE_MEDIA = 106;
    public static final int REQUIRE_ICON = 107;
    public static final int REQUEST_SEND_IMAGE = 108;
    public static final int REPLY_SEND_IMAGE = 109;
    public static final int RECEIVE_IMAGE = 110;

    private MulticastSocket multicastSocket = null;
    private ExecutorService executor = Executors.newFixedThreadPool(20);// 创建一个固定大小的线程池来发送消息，大小为20个线程

    private MyBinder myBinder = new MyBinder();
    public MyServer myServer = new MyServer();

    private List<User> users = new ArrayList<User>();
    // 用映射表Map的方式来存储信息，键为String，键值为队列Queue
    final Map<String, Queue<UdpMessage>> messages = new ConcurrentHashMap<String, Queue<UdpMessage>>();

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化socket
        try {
            multicastSocket = new MulticastSocket(MESSAGE_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 关键，开启接收消息的线程
        myServer.start();
        onLine();
    }

    /**
     * 开启Server线程，等待接收消息
     */
    class MyServer extends Thread {
        @Override
        public void run() {
            super.run();

            byte[] data = new byte[2 * 1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);

            while (!myServer.isInterrupted()) {
                try {
                    // 监听，接收消息
                    multicastSocket.receive(dp);
                    String tmp = new String(dp.getData(), 0, dp.getLength(), "UTF-8");
                    // 处理接收到的消息
                    dealMsg(tmp, dp.getAddress().getHostAddress());
                    dp.setLength(data.length);
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 处理接收到得消息
         */
        private void dealMsg(String tmp, String hostAddress) throws JSONException {
            UdpMessage msg = new UdpMessage(new JSONObject(tmp));
            boolean addUserFlag = true;
            // 根据类型对信息进行操作
            int type = msg.getType();
            switch (type) {
                case ON_LINE:
                    // 添加新用户
                    User user = new User();
                    user.setName(msg.getSenderName());
                    user.setIp(hostAddress);
                    user.setDeviceCode(msg.getDeviceCode());
                    user.setApDesc(msg.getApDesc());
                    user.setApRssi(msg.getApRssi());
                    Log.d("ReceiveOnLine", user.getIp());

                    addUserFlag = false;
                    if (user.getIp() != null) {
                        addUserFlag = true;
                        for (User userTmp : users) {
                            Log.d("UserIP", userTmp.getIp() + " - " + hostAddress);
                            if (userTmp.getIp().equals(hostAddress)) {
                                addUserFlag = false; // 不添加已存在的用户
                                // 如果User所连接的AP地址有更新，则更新User信息
                                // 注意String需要用equals来比较
                                if (!userTmp.getApDesc().equals(msg.getApDesc())) {
                                    userTmp.setApDesc(msg.getApDesc());
                                }
                                // 如果User所连接的AP的RSSI有更新，则更新User信息
                                if (userTmp.getApRssi() != msg.getApRssi()) {
                                    userTmp.setApRssi(msg.getApRssi());
                                }
                                break;
                            }
                        }
                    }

                    // 如果发送消息的源头不是自己且为新用户，则把它添加到好友列表，并且发送一条REPLY的消息
                    if (!((MyApplication.appInstance.getLocalIp()).equals(hostAddress)) && addUserFlag) {
                        users.add(user);
                        send(MyApplication.appInstance.generateMyMessage("",
                                REPLY_ONLINE).toJOString(), hostAddress);
                    }
                    break;

                case REPLY_ONLINE:
                    // 添加新用户
                    user = new User();
                    user.setName(msg.getSenderName());
                    user.setIp(hostAddress);
                    user.setDeviceCode(msg.getDeviceCode());
                    user.setApDesc(msg.getApDesc());
                    user.setApRssi(msg.getApRssi());
                    addUserFlag = false;
                    if (user.getIp() != null) {
                        addUserFlag = true;
                        for (User userTmp : users) {
                            if (userTmp.getIp().equals(hostAddress)) {
                                addUserFlag = false; //不添加已存在的用户
                                // 如果User所连接的AP地址有更新，则更新User信息
                                // 注意String需要用equals来比较
                                if (!userTmp.getApDesc().equals(msg.getApDesc())) {
                                    userTmp.setApDesc(msg.getApDesc());
                                }
                                // 如果User所连接的AP的RSSI有更新，则更新User信息
                                if (userTmp.getApRssi() != msg.getApRssi()) {
                                    userTmp.setApRssi(msg.getApRssi());
                                }
                                break;
                            }
                        }
                    }
                    if (!((MyApplication.appInstance.getLocalIp()).equals(hostAddress)) && addUserFlag) {
                        users.add(user);
                    }
                    break;

                case TEXT_MESSAGE:
                    if (messages.containsKey(hostAddress)) {
                        //获取键值Queue<UdpMessage>，再在Queue消息队列里添加消息
                        // TODO 消息队列的管理
                        messages.get(hostAddress).add(msg);
                    } else {
                        Queue<UdpMessage> queue = new ConcurrentLinkedQueue<UdpMessage>();
                        queue.add(msg);
                        messages.put(hostAddress, queue);
                    }
                    break;

                case REQUEST_SEND_MEDIA:
                    //打开tcp服务端准备接收音频
                    MediaTcpServer ts = new MediaTcpServer(msg, hostAddress, ChatService.this);
                    ts.start();

                    UdpMessage msg1 = MyApplication.appInstance.generateMyMessage(msg.getMsg(), REPLY_SEND_MEDIA);
                    send(msg1.toJOString(), hostAddress);//发送一条回应消息
                    break;

                case REPLY_SEND_MEDIA:
                    // 收到回应Media请求的信息后,打开Tcp客户端准备发送音频
                    MediaTcpClient tc = new MediaTcpClient(msg,hostAddress);
                    tc.start();
                    break;

                case REQUEST_SEND_IMAGE:
                    //打开tcp服务端准备接收图像
                    ImageTcpServer its = new ImageTcpServer(msg, hostAddress, ChatService.this);
                    its.start();

                    UdpMessage msg2 = MyApplication.appInstance.generateMyMessage(msg.getMsg(), REPLY_SEND_IMAGE);
                    send(msg2.toJOString(), hostAddress);//发送一条回应消息
                    break;

                case REPLY_SEND_IMAGE:
                    // 收到回应Image请求的信息后,打开Tcp客户端准备发送图像
                    ImageTcpClient imtc = new ImageTcpClient(msg, hostAddress);
                    imtc.start();
                    break;

                case REQUIRE_ICON:
                    IconTcpClient itc = new IconTcpClient(hostAddress);//把自己的头像发送到指定ip
                    itc.start();

            }
            // 收到任何消息，都会发送一条广播，通知刷新好友列表或聊天列表
            onReceiver(type);
        }
    }

    // 发送广播更新聊天界面UI
    public void onReceiver(int type) {
        switch(type){
            // ON_LINE和REPLY_ONLINE为同一处理方式
            case ON_LINE:
            case REPLY_ONLINE:
//                Log.i("广播", "刷新MainActivity列表界面啦");
                sendBroadcast(new Intent(MainActivity.ACTION_REFRESH));
                break;
            // 下面三种type为同一处理方式
            case TEXT_MESSAGE:
            case RECEIVE_MEDIA:
            case RECEIVE_IMAGE:
                // Log.i("广播", "刷新ChatActivity聊天界面啦");
                sendBroadcast(new Intent(ChatActivity.ACTION_NOTIFY_DATA));
                break;
        }
    }

    /**
     * 如果在局域网内广播一条自己上线的消息，则目的地址为"255.255.255.255"
     */
    public void onLine() {
        send(MyApplication.appInstance.generateMyMessage("", ON_LINE)
                .toJOString(), "255.255.255.255");
        Log.d("发送者", "我上线啦！");
    }

    /**
     * 发送一条消息到message端口
     */
    private void send(final String msg, final String destip) {
        // 用前面创建的一个固定大小的线程池executor来发送消息，大小为20个线程
        executor.execute(new Runnable() {
            public void run() {
                try {
                    DatagramPacket dps = new DatagramPacket(
                            msg.getBytes("UTF-8"), msg.length(), InetAddress
                            .getByName(destip), MESSAGE_PORT);
                    multicastSocket.send(dps);

                } catch (UnsupportedEncodingException er) {
                    er.printStackTrace();
                } catch (UnknownHostException err) {
                    err.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        multicastSocket.close();
        myServer.interrupt();
        Log.d("Service", "我被销毁啦！");
    }

    public Map<String, Queue<UdpMessage>> getMsgs() {
        return messages;
    }

    public class MyBinder extends Binder {
        public List<User> getUsers() {
            return users;
        }

        public Map<String, Queue<UdpMessage>> getMessages() {
            return messages;
        }

        public void sendMsg(UdpMessage msg, String destIp) {
            send(msg.toJOString(), destIp);
            Log.d("发送者", "点击按钮发送成功");
        }
    }
}
