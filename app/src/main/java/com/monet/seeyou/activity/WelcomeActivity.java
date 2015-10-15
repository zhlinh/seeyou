package com.monet.seeyou.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.monet.seeyou.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Monet on 2015/6/26.
 */
public class WelcomeActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        //通过一个时间控制函数Timer，实现一个活动与另一个活动的跳转。
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startActivity(new Intent(WelcomeActivity.this,MainActivity.class));
                finish();
            }
        }, 2000);//这里停留时间为2秒，1000=1s。
    }
}
