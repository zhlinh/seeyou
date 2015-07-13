package com.monet.seeyou.activity;


import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.monet.seeyou.R;
import com.monet.seeyou.model.Media;
import com.monet.seeyou.model.UdpMessage;
import com.monet.seeyou.model.User;
import com.monet.seeyou.service.ChatService;
import com.monet.seeyou.service.ChatService.MyBinder;
import com.monet.seeyou.tool.MyApplication;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class ChatActivity extends Activity implements OnClickListener {
    public static final String ACTION_HEARTBEAT = "com.monet.seeyou.heartbeat";
    public static final String ACTION_NOTIFY_DATA = "com.monet.seeyou.notifydata";
    private TextView chatterNameView,recordHintView;
    private ListView listView;
    private Button sendBtn, mediaBtn;
    private Button imageBtn;
    private EditText msgEdt;

    private MyConnection conn;
    private ChatService.MyBinder binder;
    private ChatAdapter adapter;
    private RefreshBroadcastReceiver receiver = new RefreshBroadcastReceiver();


    private List<UdpMessage> myMessages = new ArrayList<UdpMessage>();
    private User chatter = new User();
    private String ChatterIp, ChatterDeviceCode, ChatterName;

    private Media media = new Media();  //音频类，进行录音操作
    private String mediaName = null;
    public static final int REQUEST_SEND_IMAGE = 108;
    public static final int RECEIVE_MEDIA = 106;
    public static final int REQUEST_SEND_MEDIA = 104;
    public static final int TEXT_MESSAGE = 103;
    private boolean isVoiceRecording = false;
    private boolean isCanceled = false;
    private float downY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化聊天对方
        ChatterIp = getIntent().getStringExtra("IP");
        ChatterDeviceCode = getIntent().getStringExtra("DeviceCode");
        ChatterName = getIntent().getStringExtra("name");
        chatter.setIp(ChatterIp);
        chatter.setDeviceCode(ChatterDeviceCode);
        chatter.setName(ChatterName);

        initView();// 初始化部件
        initService();// 初始化service
        initReceiver();// 初始化广播接收器

    }

    /**
     * 初始化部件
     */
    private void initView() {
        chatterNameView = (TextView) findViewById(R.id.chatter_name);
        chatterNameView.setText(ChatterName);
        listView = (ListView) findViewById(R.id.chat_listview);
        sendBtn = (Button) findViewById(R.id.send_btn);
        mediaBtn = (Button) findViewById(R.id.send_record_btn);
        msgEdt = (EditText) findViewById(R.id.chat_edt);
        imageBtn = (Button) findViewById(R.id.send_image_btn);
        sendBtn.setOnClickListener(this);
        final Dialog dialog = new Dialog(ChatActivity.this, R.style.MyDialog);
        dialog.setContentView(R.layout.activity_record);
        recordHintView = (TextView) dialog.findViewById(R.id.record_hint);

        msgEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (msgEdt.getText().toString().length() == 0) {
                    imageBtn.setVisibility(View.VISIBLE);
                    sendBtn.setVisibility(View.INVISIBLE);
                } else {
                    imageBtn.setVisibility(View.INVISIBLE);
                    sendBtn.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mediaBtn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isVoiceRecording = true;
                        downY = event.getY();
                        //开始录音
                        recordHintView.setText("向上滑动取消发送");
                        dialog.show();
                        media.startRecord();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float moveY = event.getY();
                        if (downY - moveY > 50) {
                            isCanceled = true;
                            recordHintView.setText("松开取消发送");
                        }
                        if (downY - moveY < 20) {
                            isCanceled = false;
                            recordHintView.setText("向上滑动取消发送");
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        dialog.dismiss();
                        if (isVoiceRecording) {
                            if (isCanceled) {
                                //手指在按钮之外松开，停止 录音
                                media.stopRecord();
                                //取消发送应该删除旧录音
                                media.deleteOldFile(media.getSendPath() + "/" + media.getName());
                            } else {
                                //手指在按钮上松开，停止并发送录音
                                media.stopRecord();
                                //发送音频  sendMediaRecord(media.getName());
                                //		该函数包含（1）把音频发送到指定的ip（先发送音频通知，通知对方开通tcp端口接收）
                                //			        （2）刷新聊天界面，在界面上添加一条语音消息
                                //			        （3）设置点击事件，点击那条语音消息，就会播放语音
                                sendMediaRecord(media.getName());
                            }
                        }
                        isVoiceRecording = false;
                        break;
                }
                return false;
            }
        });



        imageBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                /**
                 * void startActivityForResult(Intent intent, int requestCode) {
                 *  @param requestCode If >= 0, this code will be returned in
                 *       onActivityResult()[在下面重载了该函数] when the activity exits.
                 */
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.send_btn:
                String txt = msgEdt.getText().toString();
                if(txt.length() <= 0 )	return ;
                try {
                    sendMsg(MyApplication.appInstance.generateMyMessage(txt, ChatService.TEXT_MESSAGE));
                    //发送消息，（1）获取聊天对象chatter
                    //		  （2）判断对方是否在线
                    //		  （3）在线，则发送消息过去
                    //    	  （4）更新聊天界面
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    // 用于发送Image的
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            Uri uri = data.getData();
            try{
                /**
                 *   CursorLoader()的各个参数
                 *   context
                 *   uri         	The URI of the content provider to query.
                 *   projection  	List of columns to return.
                 *   selection   	SQL WHERE clause.
                 *   selectionArgs	The arguments to selection, if any ?s are pesent
                 *   sortOrder   	SQL ORDER BY clause.
                 */
                String[] projection = {MediaStore.Images.Media.DATA};
                CursorLoader cursorLoader = new CursorLoader(this,uri, projection, null, null, null);
                Cursor cursor = cursorLoader.loadInBackground();
                if(cursor !=null){
                    ContentResolver cr = this.getContentResolver();
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    String path = cursor.getString(column_index);
                    //获取到图片路径，发送
                    //Toast.makeText(getApplicationContext(), path, Toast.LENGTH_SHORT).show();
                    sendImageMsg(path);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送文本消息
     */
    private void sendMsg(UdpMessage msg) throws UnknownHostException{
        if(binder != null){
            binder.sendMsg(msg, chatter.getIp());
            myMessages.add(msg);
            msgEdt.setText("");
            refresh();
        }else{
            unbindService(conn);
            Intent intent1 = new Intent(ChatActivity.this, ChatService.class);
            startService(intent1);
        }
    }

    /**
     * 发送音频消息
     */
    private void sendMediaRecord(String name){
        /*mediaName = media.getSendPath() + "/" + name;
		String m = (new File(mediaName)).getName();*/
        UdpMessage msgTmp = MyApplication.appInstance.generateMyMessage(name, REQUEST_SEND_MEDIA);
        binder.sendMsg(msgTmp, chatter.getIp());
        //发送一条包含文件名的消息 通知 对方准备好接收音频文件

        //在此处刷新自己的聊天界面
        //（1）把这条语音消息加入到消息队列
        //（2）调用refresh方法，刷新adapter，adapter根据接收到得msg类型进行刷新。
        myMessages.add(msgTmp);
        refresh();
    }

    /**
     * 发送图片消息
     */
    private void sendImageMsg(String path){
        //发送一条即将发送图片的通知
        UdpMessage msgTmp = MyApplication.appInstance.generateMyMessage(path, REQUEST_SEND_IMAGE);
        binder.sendMsg(msgTmp, chatter.getIp());

        myMessages.add(msgTmp);
        refresh();
    }


    private void initService() {
        Intent intent = new Intent(ChatActivity.this, ChatService.class);
        bindService(intent, conn = new MyConnection(), Context.BIND_AUTO_CREATE);
    }

    /**
     * 与service绑定成功后进行的操作 1、获取service对象（用来获取在线用户，未读消息，以及调用sendmsg方法）
     * 2、设置聊天界面listview（自定义一个adapter来适配） 3、获取对方发来的消息,进行分类
     */
    public class MyConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (MyBinder) service;// 获取到后台接收和发送消息的service（用来获取在线用户、未读消息，以及调用里面的方法）
            Queue<UdpMessage> queue = binder.getMessages().get(chatter.getIp()); // 获取对方发来的消息队列
            listView.setAdapter(adapter = new ChatAdapter());

            if (queue != null) {
                ergodicMessage(queue);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NOTIFY_DATA);
        filter.addAction(ACTION_HEARTBEAT);
        registerReceiver(receiver, filter);
    }

    /**
     * 广播接收器，（1）刷新数据
     *		         （2）心跳检测接收端(未实现）
     *
     */
    public class RefreshBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("接收器", "接收到刷新页面的广播，现在进行刷新。。。");
            if((intent.getAction()).equals(ACTION_NOTIFY_DATA)){

                if(adapter != null){
                    if(binder != null){
                        Queue<UdpMessage> queue = binder.getMessages().get(chatter.getIp());
                        if(queue != null){
                            Log.i("消息队列", "消息队列不为空，遍历");
                            ergodicMessage(queue);
                        }
                    }else{
                        unbindService(conn);
                        Intent it = new Intent(ChatActivity.this, ChatService.class);
                        bindService(it, conn= new MyConnection(), Context.BIND_AUTO_CREATE);
                    }
                }else{
                    adapter = new ChatAdapter();
                }
            }
        }
    }

    /**
     * 遍历从service传过来的消息队列，根据消息的type对其进行不同处理
     * TEXT_MESSAGE, RECEIVE_MEDIA, RECEIVE_IMAGE：需要显示的消息，则加入到myMessages队列
     * @param queue
     */
    private void ergodicMessage(Queue<UdpMessage> queue) {
        // An iterator(迭代器) over a sequence of objects, such as a collection.
        Iterator<UdpMessage> it = queue.iterator();
        UdpMessage message;
        while (it.hasNext()) {
            message = it.next();
            switch (message.getType()) {
                case ChatService.TEXT_MESSAGE:
                case ChatService.RECEIVE_MEDIA:
                case ChatService.RECEIVE_IMAGE:
                    myMessages.add(message);
                    break;
            }
            queue.clear();//取出消息遍历后，清空
            refresh();
        }
    }

    private void refresh(){
        adapter.notifyDataSetChanged();
        // 当有更新时显示最新的消息
        listView.setSelection(adapter.getCount());
    }

    /**
     * 聊天界面适配器，根据消息的不同类型（自己发送的、对方发送的）适配相应的item到聊天界面上
     */
    class ChatAdapter extends BaseAdapter {
        private final int owner = 0;
        private final int other = 1;
        private final int owner_media = 2;
        private final int other_media = 3;
        private final int owner_image = 4;
        private final int other_image = 5;
        private final int view_type_count = 6;

        @Override
        public int getCount() {
            if (myMessages != null)
                return myMessages.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            UdpMessage msg = myMessages.get(position);
            if (msg.isOwn() && (msg.getType()==TEXT_MESSAGE))
                return owner;
            else if(!msg.isOwn() && (msg.getType()==TEXT_MESSAGE))
                return other;
            else if(msg.isOwn() && (msg.getType()==REQUEST_SEND_MEDIA))
                return owner_media;
            else if(!msg.isOwn() && (msg.getType() == RECEIVE_MEDIA))
                return other_media;
            else if(msg.isOwn() && (msg.getType() == REQUEST_SEND_IMAGE))
                return owner_image;
            else
                return other_image;
        }

        @Override
        public int getViewTypeCount() {
            return view_type_count;
        }

        /**
         * 聊天界面各种消息的layout显示
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // ViewHolder其实就是一个自定义的结构体,定义了聊天消息layout的一些元素
            ViewHolder viewHolder = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                viewHolder = new ViewHolder();
                switch (type) {
                    case owner:
                        convertView = getLayoutInflater().inflate(
                                R.layout.chat_my_listview, null);
                        viewHolder.sendTime = (TextView) convertView.findViewById(R.id.send_time);
                        viewHolder.txt = (TextView) convertView
                                .findViewById(R.id.chat_my_txt);
                        viewHolder.chatterName = (TextView) convertView
                                .findViewById(R.id.chat_my_name);
                        break;
                    case other:
                        convertView = getLayoutInflater().inflate(
                                R.layout.chat_other_listview, null);
                        viewHolder.sendTime = (TextView) convertView.findViewById(R.id.send_time);
                        viewHolder.txt = (TextView) convertView
                                .findViewById(R.id.chat_other_txt);
                        viewHolder.chatterName = (TextView) convertView
                                .findViewById(R.id.chat_other_name);
                        break;
                    case owner_media:
                        convertView = getLayoutInflater().inflate(
                                R.layout.chat_my_media_listview,null);
                        viewHolder.sendTime = (TextView) convertView.findViewById(R.id.send_time);
                        viewHolder.media = (Button) convertView.findViewById(R.id.chat_my_media_button);
                        viewHolder.chatterName = (TextView) convertView.findViewById(R.id.chat_my_name);
                        break;
                    case other_media:
                        convertView = getLayoutInflater().inflate(R.layout.chat_other_media_listview, null);
                        viewHolder.sendTime = (TextView) convertView.findViewById(R.id.send_time);
                        viewHolder.media = (Button) convertView.findViewById(R.id.chat_other_media_button);
                        viewHolder.chatterName = (TextView) convertView.findViewById(R.id.chat_other_name);
                        break;
                    case owner_image:
                        convertView = getLayoutInflater().inflate(R.layout.chat_my_image_listview, null);
                        viewHolder.sendTime = (TextView) convertView.findViewById(R.id.send_time);
                        viewHolder.picture = (ImageView) convertView.findViewById(R.id.chat_my_image);
                        viewHolder.chatterName = (TextView) convertView.findViewById(R.id.chat_my_name);
                        break;
                    case other_image:
                        convertView = getLayoutInflater().inflate(R.layout.chat_other_image_listview, null);
                        viewHolder.sendTime = (TextView) convertView.findViewById(R.id.send_time);
                        viewHolder.picture = (ImageView) convertView.findViewById(R.id.chat_other_image);
                        viewHolder.chatterName = (TextView) convertView.findViewById(R.id.chat_other_name);
                        break;
                }
                convertView.setTag(viewHolder);
            } else {
                // converView不为空时:
                viewHolder = (ViewHolder) convertView.getTag();
            }

            /**
             * 聊天界面各种消息的内容显示
             */
            switch (type) {
                case owner:
                    UdpMessage message = myMessages.get(position);
                    viewHolder.sendTime.setText(message.getSendTime());
                    String content = message.getMsg();
                    viewHolder.txt.setText(content);
                    viewHolder.chatterName.setText(MyApplication.appInstance.getMyName());
                    break;
                case other:
                    message = myMessages.get(position);
                    viewHolder.sendTime.setText(message.getSendTime());
                    content = message.getMsg();
                    viewHolder.txt.setText(content);
                    viewHolder.chatterName.setText(chatter.getName());
                    break;
                case owner_media:
                    message = myMessages.get(position);
                    viewHolder.sendTime.setText(message.getSendTime());
                    final String mediaName = message.getMsg(); //音频的名字
                    MediaPlayer mp = MediaPlayer.create(ChatActivity.this,Uri.parse(media.getSendPath() + "/" + mediaName));
                    int duration = mp.getDuration()/1000;

                    viewHolder.media.setText(duration+"\"" + " (((");
                    viewHolder.chatterName.setText(MyApplication.appInstance.getMyName());
                    viewHolder.media.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            media.startPlay(media.getSendPath() + "/" + mediaName);
                        }
                    });
                    break;

                case other_media:
                    message = myMessages.get(position);
                    viewHolder.sendTime.setText(message.getSendTime());
                    final String mediaName1 = message.getMsg(); //音频的名字
                    mp = MediaPlayer.create(ChatActivity.this,Uri.parse(media.getReceivePath() + "/" + mediaName1));
                    duration = mp.getDuration()/1000;
                    viewHolder.media.setText("))) "+duration+"\"");
                    viewHolder.chatterName.setText(chatter.getName());
                    viewHolder.media.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            media.startPlay(media.getReceivePath()+ "/" + mediaName1);
                        }
                    });
                    break;

                case owner_image:
                    message = myMessages.get(position);
                    viewHolder.sendTime.setText(message.getSendTime());
                    String pathTmp = message.getMsg();//图片路径

                    //对图片进行缩放
                    Options opts = new Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(pathTmp, opts);
                    int imageWidth = opts.outWidth;
                    int imageHeight = opts.outHeight;
                    Display display = ChatActivity.this.getWindowManager().getDefaultDisplay();
                    int screenWidth = display.getWidth();
                    int screenHeight = display.getHeight();
                    int widthScale = imageWidth / screenWidth;
                    int heightScale = imageHeight / screenHeight;
                    int scale = (widthScale > heightScale) ? widthScale:heightScale;
                    opts.inJustDecodeBounds= false;
                    opts.inSampleSize = scale;
                    //获取缩放后的图片，显示到聊天界面上
                    Bitmap bm = BitmapFactory.decodeFile(pathTmp,opts);
                    viewHolder.picture.setImageBitmap(bm);
                    viewHolder.chatterName.setText(MyApplication.appInstance.getMyName());
                    break;

                case other_image:
                    message = myMessages.get(position);
                    viewHolder.sendTime.setText(message.getSendTime());
                    pathTmp = message.getMsg();//图片路径；
                    //对图片进行缩放
                    opts = new Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(pathTmp, opts);
                    imageWidth = opts.outWidth;
                    imageHeight = opts.outHeight;
                    display = ChatActivity.this.getWindowManager().getDefaultDisplay();
                    screenWidth = display.getWidth();
                    screenHeight = display.getHeight();
                    widthScale = imageWidth / screenWidth;
                    heightScale = imageHeight / screenHeight;
                    scale = widthScale > heightScale ? widthScale:heightScale;
                    opts.inJustDecodeBounds= false;
                    opts.inSampleSize = scale;
                    //获取缩放后的图片，显示到聊天界面上
                    bm = BitmapFactory.decodeFile(pathTmp,opts);
                    viewHolder.picture.setImageBitmap(bm);
                    Toast.makeText(getApplicationContext(), "接收到图片，保存到" + pathTmp, Toast.LENGTH_LONG);
                    viewHolder.chatterName.setText(chatter.getName());
                    break;
            }
            return convertView;
        }

        // 将class用成类似结构体
        class ViewHolder {
            public TextView sendTime;
            public ImageView icon;
            public TextView txt;
            public TextView chatterName;
            public Button media;
            public ImageView picture;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        unregisterReceiver(receiver);
    }
}
