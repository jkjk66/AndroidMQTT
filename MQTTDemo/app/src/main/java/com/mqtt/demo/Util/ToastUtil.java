package com.mqtt.demo.Util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by cym1497 on 2017/7/10.
 */

public class ToastUtil {

    public static void showToast(Context context, String text){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
