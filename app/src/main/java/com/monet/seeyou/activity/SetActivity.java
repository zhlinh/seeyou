package com.monet.seeyou.activity;

/**
 * Created by Monet on 2015/6/16.
 */

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.provider.MediaStore.Images.Media;
import android.content.SharedPreferences.Editor;

import com.monet.seeyou.R;
import com.monet.seeyou.tool.MemoryCache;
import com.monet.seeyou.tool.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 设置头像和昵称的界面
 *
 */
public class SetActivity extends Activity implements OnClickListener {
    private ImageView icon;
    private Button  nicknameButton;
    private EditText nicknameEdt;

    private final String TAG = "SetAct";
    private final int CUT_PHOTO_REQUEST_CODE = 201;
    private final int SELECT_PHOTO_REQUEST_CODE = 200;

    private String iconPath;// 自己的头像的路径
    public static String iconName = "me";// 存储用户自己信息的文件名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_set);

        iconPath = getFilesDir() + File.separator + iconName;// 给用户自己的头像的存储路径赋值
        Log.d("filepath", iconPath);

        initView();//绑定部件

        // 设置头像imageView中的内容
        setIcon();

        // 设置昵称EditText中的内容，从me文档中获取个人姓名信息
        String tmpNickname = getSharedPreferences("me", MODE_PRIVATE).getString("name", "无名");
        nicknameEdt.setText(tmpNickname);
        // 设置光标的位置
        nicknameEdt.setSelection(tmpNickname.length());

        // 监听button事件
        icon.setOnClickListener(this);// 选择图片按钮
        nicknameButton.setOnClickListener(this);// 确认修改昵称按钮
    }

    private void initView() {
        // 绑定部件
        icon = (ImageView) findViewById(R.id.setactivity_icon_image);
        //iconButton = (Button) findViewById(R.id.setactivity_select_icon_btn);
        nicknameButton = (Button) findViewById(R.id.setactivity_change_nickname_btn);
        nicknameEdt = (EditText) findViewById(R.id.setactivity_nickname_edt);
    }

    private void setIcon() {
        Bitmap bitmap = MemoryCache.getInstance().get(iconName);
        // 判断是否在缓存中
        if (bitmap != null) {
            Log.d("test-image", "图片在缓存中");
            icon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap));
        } else {
            bitmap = BitmapFactory.decodeFile(iconPath);// 则打开文件路径看看是否保存在文件当中
            if (bitmap == null) {
                // 文件路径中也没有，则设一个默认图
                icon.setImageResource(R.drawable.ic_launcher);
            } else {
                Log.d("test-image", "图片在文件路径中");
                // 文件路径中存在，则
                icon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap));
                MemoryCache.getInstance().put(iconName, bitmap);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setactivity_icon_image:
                try {
                    Intent i = new Intent(Intent.ACTION_PICK,
                            Media.EXTERNAL_CONTENT_URI);
                    i.setType("image/*");
                    startActivityForResult(i, SELECT_PHOTO_REQUEST_CODE);// 打开系统相册
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.setactivity_change_nickname_btn:
                String nickName = nicknameEdt.getText().toString().trim();
                if (nickName.equals("")) {
                    Toast.makeText(getApplicationContext(), "昵称不能为空",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Editor editor = getSharedPreferences("me", MODE_PRIVATE).edit();// 把新的昵称存入文件中
                editor.putString("name", nickName);
                editor.apply();
                Toast.makeText(getApplicationContext(), "成功修改", Toast.LENGTH_LONG)
                        .show();
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK
                && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // 裁剪图片
                final Intent intent = new Intent(
                        "com.android.camera.action.CROP");
                intent.setDataAndType(uri, "image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                intent.putExtra("outputX", 100);
                intent.putExtra("outputY", 100);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, CUT_PHOTO_REQUEST_CODE);
            }
        } else if (requestCode == CUT_PHOTO_REQUEST_CODE
                && resultCode == RESULT_OK && data != null) {
            try {
                // 获取裁剪完的圆形图片
                Bitmap bitmap = data.getParcelableExtra("data");
                bitmap = Util.getRoundedCornerBitmap(bitmap);
                icon.setImageBitmap(bitmap);
                File file = new File(iconPath);
                file.delete();// 删除之前的那张图片
                file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);//转码jpeg
                outputStream.flush();
                outputStream.close();

                MemoryCache.getInstance().put(iconName, bitmap);// 加入到缓存中
                Editor editor = getSharedPreferences("me", MODE_PRIVATE).edit();
                // 标记为头像为刚更新的头像
                editor.putBoolean("is_refresh_icon", true);
                editor.apply();
                Toast.makeText(getApplicationContext(), "成功修改头像",
                        Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "操作失败",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

}
