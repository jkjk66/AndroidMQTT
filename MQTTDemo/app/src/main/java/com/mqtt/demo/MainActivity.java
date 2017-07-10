package com.mqtt.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mqtt.demo.Util.ServiceUtil;
import com.mqtt.demo.Util.ToastUtil;
import com.mqtt.demo.service.MQTTMessage;
import com.mqtt.demo.service.MQTTService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by cym1497 on 2017/7/10.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.disconnect).setOnClickListener(this);
        findViewById(R.id.send).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        boolean isRunning = isServiceRunning();
        switch (v.getId()){
            case R.id.start:
                if (isRunning){
                    ToastUtil.showToast(this, "MQTT Service started");
                } else {
                    ToastUtil.showToast(this, "MQTT Service starting");
                    startService(new Intent(this, MQTTService.class));
                }
                break;

            case R.id.stop:
                if (!isRunning){
                    ToastUtil.showToast(this, "MQTT Service stopped");
                } else {
                    ToastUtil.showToast(this, "MQTT Service stopping");
                    stopService(new Intent(this, MQTTService.class));
                }
                break;

            case R.id.connect:
                if (!isRunning){
                    ToastUtil.showToast(this, "please start MQTT Service");
                    break;
                }

                if (MQTTService.isConnect()){
                    ToastUtil.showToast(this, "MQTT Service connected");
                    break;
                }
                ToastUtil.showToast(this, "MQTT Service connecting");
                MQTTService.connect();
                break;

            case R.id.disconnect:
                if (!isRunning){
                    ToastUtil.showToast(this, "please start MQTT Service");
                    break;
                }

                if (!MQTTService.isConnect()){
                    ToastUtil.showToast(this, "MQTT Service disconnected");
                    break;
                }
                ToastUtil.showToast(this, "MQTT Service disconnecting");
                MQTTService.disconnect();
                break;

            case R.id.send:
                if (!isRunning){
                    ToastUtil.showToast(this, "please start MQTT Service");
                    break;
                }

                if (!MQTTService.isConnect()){
                    ToastUtil.showToast(this, "please connect");
                    break;
                }

                MQTTService.publish("测试测试测试");
                break;
        }
    }

    private boolean isServiceRunning() {
        return ServiceUtil.isServiceWork(this, MQTTService.class.getName());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getMqttMessage(MQTTMessage mqttMessage){
        Log.i(MQTTService.TAG,"get message:"+mqttMessage.getMessage());
        Toast.makeText(this,mqttMessage.getMessage(),Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
