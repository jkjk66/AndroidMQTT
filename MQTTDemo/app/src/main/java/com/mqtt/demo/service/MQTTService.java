package com.mqtt.demo.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.mqtt.demo.Util.MacAddressUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;


/**
 * MQTT长连接服务
 * Created by cym1497 on 2017/7/10.
 */
public class MQTTService extends Service {
    public static final String TAG = MQTTService.class.getSimpleName();
    private static MQTTService instance;
    private static MqttAndroidClient client;

    private MqttConnectOptions conOpt;
    private String host = "tcp://192.168.0.102:61613";
    private String userName = "admin";
    private String passWord = "password";
    private static String myTopic = "topic";
    private String clientId = "test2";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        instance = null;
        try {
            client.disconnect();
            client.unregisterResources();

        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public static void publish(String msg){
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        try {
            client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    // MQTT监听连接情况
    private IMqttActionListener iMqttActionListener = new ActionListener();

    // MQTT监听并且接受消息
    private MqttCallback mqttCallback = new CallBack();

    private void init() {
        clientId = MacAddressUtil.getLocalMacAddress(this);

        // 服务器地址（协议+地址+端口号）
        String uri = host;
        client = new MqttAndroidClient(this, uri, clientId);
        // 设置MQTT监听并且接受消息
        client.setCallback(mqttCallback);

        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(true);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(10);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(20);
        // 用户名
        conOpt.setUserName(userName);
        // 密码
        conOpt.setPassword(passWord.toCharArray());

        // last will message
        boolean doConnect = true;
        String message = "{\"terminal_uid\":\"" + clientId + "\"}";
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        if (TextUtils.isEmpty(message) || TextUtils.isEmpty(topic)) {
            // 最后发送的消息
            try {
                conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
            } catch (Exception e) {
                Log.i(TAG, "Exception Occured", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }

        if (doConnect) {
            doClientConnection();
        }

    }

    /** 连接MQTT服务器 */
    private void doClientConnection() {
        if (!client.isConnected() && isConnectIsNomarl()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /** 判断网络是否连接 */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT 没有可用网络");
            return false;
        }
    }

    public static boolean isConnect(){
        if (instance != null && client != null){
            return instance.client.isConnected();
        }
        return false;
    }

    public static void connect(){
        if (instance != null && client != null){
            instance.doClientConnection();
        }
    }

    public static void disconnect(){
        if (instance != null){
            try {
                instance.client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ActionListener implements IMqttActionListener {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                // 订阅myTopic话题
                client.subscribe(myTopic,1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            // 连接失败，重连
            Log.i("mtqq", "onFailure");
        }
    }


    public static class CallBack implements MqttCallback{

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String str1 = new String(message.getPayload());
            MQTTMessage msg = new MQTTMessage();
            msg.setMessage(str1);
            EventBus.getDefault().post(msg);
            String str2 = "topic:" + topic + ", qos:" + message.getQos() + ", retained:" + message.isRetained();
            Log.i(TAG, "messageArrived:" + str1);
            Log.i(TAG, str2);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.i("mtqq", "deliveryComplete");
        }

        @Override
        public void connectionLost(Throwable arg0) {
            // 失去连接，重连
            String message = "null";
            if (arg0 != null) {
                message = arg0.getMessage();
            }
            Log.i("mtqq", "connectionLost:"+message);
        }
    }
}
