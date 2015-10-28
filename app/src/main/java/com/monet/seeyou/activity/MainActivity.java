package com.monet.seeyou.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.monet.seeyou.R;
import com.monet.seeyou.model.UdpMessage;
import com.monet.seeyou.model.User;
import com.monet.seeyou.service.ChatService;
import com.monet.seeyou.service.ChatService.MyBinder;
import com.monet.seeyou.tool.MemoryCache;
import com.monet.seeyou.tool.MyApplication;
import com.monet.seeyou.tool.Util;
import com.monet.seeyou.util.IconTcpServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Monet on 2015/6/16.
 */

public class MainActivity extends Activity {
    public static final String TAG = "MainAct";
    public static final int MESSAGE_PORT = 2425;
    public static final int ON_LINE = 101;
    public static final String ACTION_REFRESH= "com.monet.seeyou.refresh";
/**
 * MuticastSocket 广播的方式，UDP，标识一组D类IP地址
*/
    private MulticastSocket multicastSocket = null;
    public static MyBinder binder;
    private boolean binded = false;//标志当前是否与chatService绑定

    MyServiceConnection myServiceConnection;
    RefreshReceiver refreshReceiver = new RefreshReceiver();
    // Adapter
    private UserAdapter adapter;
    private ListView listView;

    // 顶部菜单栏的定位及设置按钮
    private Button locate;
    private Button setProfile;
    // Users列表
    private List<User> users = new ArrayList<User>();
    // 用户自己
    private User myself = new User();

    /**
     * 用于ChatService与MainActivity交互的connection
     * 用bind的方式,一旦Activity被销毁，相应的service也会被销毁
     */
    public class MyServiceConnection implements ServiceConnection {
        // 只是在连接成功那一刻调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (MyBinder) service;// 获取到ChatService对象，可从中调用想要的信息来更新activity页面
            binded = true;// 绑定Service的标志
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    /**
     * 用于定时刷新列表
     */
    private final Handler handler = new Handler();
    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            // 定时更新，单位为毫秒
            handler.postDelayed(this, 1000);
            // 更新自己的AP信息及RSSI信息
            Log.e(TAG,"auto update UI when necessary.");
            boolean updateFlag = false;
            String tmpApDesc = MyApplication.appInstance.getApDesc();
            if ( tmpApDesc != null && !myself.getApDesc().equals(tmpApDesc)) {
                myself.setApDesc(tmpApDesc);
                updateFlag = true;
            }
            if (myself.getApRssi() != MyApplication.appInstance.getApRssi()) {
                myself.setApRssi(MyApplication.appInstance.getApRssi());
                updateFlag = true;
            }
            // 广播自己上线的消息，以便自己及其他用户更新列表
            if (binder != null && updateFlag) {
                binder.sendMsg(MyApplication.appInstance.generateMyMessage("", ON_LINE), "255.255.255.255");
            }
            // 更新列表，发送上线消息，接收到后便会自动更新列表，故此处不需要重复
            //sendBroadcast(new Intent(MainActivity.ACTION_REFRESH));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化自己
        myself.setName(MyApplication.appInstance.getMyName());
        myself.setIp(MyApplication.appInstance.getLocalIp());
        myself.setDeviceCode(MyApplication.appInstance.getDeviceCode());
        myself.setApDesc(MyApplication.appInstance.getApDesc());
        myself.setApRssi(MyApplication.appInstance.getApRssi());
        // 注意未连接Wifi时可能出现的空指针问题
        if (myself.getIp() == null) {
            Log.e("Ip", "null");
        } else {
            Log.e("Ip", myself.getIp());
        }

        initService();// 初始化Service
        initTopBar(); //初始化顶部菜单栏
        initUserList();// 初始化用户列表
        initReceiver();//注册广播接收器，用于刷新用户列表的广播接收器
        handler.post(task);  //每隔1s定时刷新用户列表
    }

    /**
     * 初始化服务
     */
    private void initService() {
        Intent serviceIntent = new Intent(MainActivity.this, ChatService.class);
        startService(serviceIntent);// 开启服务
        bindService(serviceIntent, myServiceConnection = new MyServiceConnection(),
                Context.BIND_AUTO_CREATE);
    }

    /**
     * 初始化顶部菜单栏
     */
    private void initTopBar() {
        setProfile = (Button) findViewById(R.id.set_profile);
        locate = (Button) findViewById(R.id.locate);

        /**
         * 点击设置按钮，进入设置界面
         */
        setProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent setIntent = new Intent(MainActivity.this, SetActivity.class);
                startActivity(setIntent);
            }
        });
        /**
         * 点击定位按钮，进入定位界面
         */
        locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent setIntent = new Intent(MainActivity.this, LocateActivity.class);
                startActivity(setIntent);
            }
        });

    }
    /**
     * 初始化用户列表
     */
    private void initUserList() {
        listView = (ListView) findViewById(R.id.user_list);

        /**
         * 设置用户列表，使点击用户，进入聊天界面
         */
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (binded) {
                    unbindService(myServiceConnection);
                    binded = false;
                }
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                User chatter = users.get(position);
                intent.putExtra("IP", chatter.getIp());
                intent.putExtra("DeviceCode", chatter.getDeviceCode());
                intent.putExtra("Name", chatter.getName());
                intent.putExtra("ApDesc", chatter.getApDesc());
                intent.putExtra("ApRssi", chatter.getApRssi());
                startActivity(intent);
            }
        });

    }

    /**
     * 发送消息的一方，此处主要用来广播自己上线的消息
     * 已经弃用
     * 已改为在绑定的服务ChatService初始化时发送onLine消息
     */
    class MyClient extends Thread {
        @Override
        public void run() {
            super.run();
            // 打开发送消息的socket
            try {
                String tmp = MyApplication.appInstance.generateMyMessage(
                        "Hello", ON_LINE).toJOString();
                //DatagramPacket是UDP方式，Stream是TCP方式
                // 广播自己已经上线
                DatagramPacket dgp = new DatagramPacket(tmp.getBytes("UTF-8"),
                        tmp.length(), InetAddress.getByName("255.255.255.255"), MESSAGE_PORT);
                // 除了初始化之外，MulticastSocket 和 DatagramSocket 的使用方式几乎一样。而实际上前者就是后者的子类。
                multicastSocket.send(dgp);
                Log.i("发送者", "成功发送");
                interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 用户列表适配器adapter,用于ArrayList的显示
     */
    public class UserAdapter extends ArrayAdapter<User> {
        private int resourceId;

        public UserAdapter(Context context, int resource, List<User> objects) {
            super(context, resource, objects);
            resourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            User tmp = getItem(position);
            View view;
            ViewHolder viewHolder;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(resourceId,
                        null);// 获取到子布局
                viewHolder = new ViewHolder();
                viewHolder.userIcon = (ImageView) view
                        .findViewById(R.id.user_list_icon);
                viewHolder.userName = (TextView) view
                        .findViewById(R.id.user_list_name);
                viewHolder.userIp = (TextView) view
                        .findViewById(R.id.user_list_ip);
                viewHolder.userApDesc= (TextView) view.findViewById(R.id.user_list_ap_desc);
                viewHolder.userApRssi = (TextView) view.findViewById(R.id.user_list_ap_rssi);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            // 设置用户列表中的item的显示
            viewHolder.userName.setText(tmp.getName());
            viewHolder.userIp.setText(tmp.getIp());
            viewHolder.userApDesc.setText("@AP-" + tmp.getApDesc());
            viewHolder.userApRssi.setText(Util.rssi2Distance(tmp.getApRssi()));
            // 设置用户头像
            if(position == 0){
                //自己的头像
                Bitmap bitmap = MemoryCache.getInstance().get("me");
                if(bitmap == null){
                    // 缓存中没有图片，则需要从文件中读出
                    bitmap = BitmapFactory.decodeFile(MyApplication.iconPath + "me");
                    if(bitmap != null){
                        //文件中存在
                        viewHolder.userIcon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap));
                        MemoryCache.getInstance().put("me", bitmap);
                    }
                }else{
                    // 若缓存中有图片
                    viewHolder.userIcon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap));
                }
            }else{
                // 其他用户的头像
                Bitmap bitmap1 = MemoryCache.getInstance().get(tmp.getDeviceCode());//根据用户的设备码在缓存中寻找对应的头像
                if(bitmap1 == null){
                    //内存中没有,则在文件中查找
                    bitmap1 = BitmapFactory.decodeFile(MyApplication.iconPath + tmp.getDeviceCode());
                    if(bitmap1 != null){//文件中有
                        viewHolder.userIcon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap1));
                        MemoryCache.getInstance().put(tmp.getDeviceCode(), bitmap1);//放入缓存中
                        if(!tmp.isRefreshIcon()){
                            refreshIcon(tmp, viewHolder.userIcon);
                        }
                    }else{
                        //文件中也没有头像图片
                        viewHolder.userIcon.setImageResource(R.drawable.ic_launcher);
                        refreshIcon(tmp, viewHolder.userIcon);
                    }
                }else {
                    //若缓存中有头像图片
                    viewHolder.userIcon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap1));
                }
            }
            return view;
        }

        class ViewHolder {
            TextView userName;
            TextView userIp;
            TextView userApDesc;
            TextView userApRssi;
            ImageView userIcon;
        }
    }

    /**
     * 刷新用户头像
     */
    public void refreshIcon(User userTmp, View view){
        if(binder != null){
            //打开一个接收头像的服务端
            IconTcpServer ts = new IconTcpServer(userTmp);
            ts.start();

            //发送头像请求消息
            UdpMessage message = MyApplication.appInstance.generateMyMessage("", ChatService.REQUIRE_ICON);
            binder.sendMsg(message, userTmp.getIp());
        }
    }

    private void initReceiver(){
        IntentFilter filter = new IntentFilter(ACTION_REFRESH);
        registerReceiver(refreshReceiver, filter);
    }

    /**
     * 广播接收器，接收广播，刷新好友列表
     */
    class RefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(binder != null){
                users.clear();
                // 添加自己，方便测试
                users.add(myself);
                List<User> list = binder.getUsers();
                for (int i = 0; i < list.size(); i++){
                    users.add(list.get(i));
                }
                if(adapter == null){
                    adapter = new UserAdapter(MainActivity.this, R.layout.item_list, users);
                    listView.setAdapter(adapter);
                }
                adapter.notifyDataSetChanged();
            } else{
                unbindService(myServiceConnection);
                binded = false;
                bindService(new Intent(MainActivity.this, ChatService.class),
                        myServiceConnection = new MyServiceConnection(), Context.BIND_AUTO_CREATE);
            }
        }
    }

    //按两次Back键退出程序
    long oldTime;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            long currentTime=System.currentTimeMillis();
            if(currentTime-oldTime<3*1000){
                finish();
            }else{
                Toast.makeText(getApplicationContext(), "再按一次退出", Toast.LENGTH_SHORT).show();
                oldTime=currentTime;
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //修改完设置后，刷新自己的名字
        myself.setName(MyApplication.appInstance.getMyName());
        // 同时刷新IP，AP的IP以及RSSI等信息
        myself.setIp(MyApplication.appInstance.getLocalIp());
        myself.setApDesc(MyApplication.appInstance.getApDesc());
        myself.setApRssi(MyApplication.appInstance.getApRssi());
        // Restart后广播自己上线的消息，以便自己及其他用户更新列表
        if (binder != null) {
            binder.sendMsg(MyApplication.appInstance.generateMyMessage("", ON_LINE), "255.255.255.255");
        }
        // sendBroadcast(new Intent(MainActivity.ACTION_REFRESH)) // 刷新列表
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(refreshReceiver);
        if(binded){
            unbindService(myServiceConnection);
            binded = false;
        }
        stopService(new Intent(MainActivity.this, ChatService.class));
        Log.i(TAG, "Activity被销毁啊！");
    }

}
