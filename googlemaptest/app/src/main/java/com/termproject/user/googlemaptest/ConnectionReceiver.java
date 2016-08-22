package com.termproject.user.googlemaptest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by user on 2016-08-03.
 */
public class ConnectionReceiver extends BroadcastReceiver  {
    public static SharedPreferences sh_Pref;
    public static SharedPreferences.Editor toEdit;
    public static int device_num=0;

    @Override
    public void onReceive(Context context, Intent intent) {

        String name = intent.getAction();

        if(name.equals("com.termproject.user.googlemaptest")){
            MainActivity.activityCheck = 1;
            MainActivity.device_selected = 1;
            device_num ++; //increase paired device

            if(MainActivity.recovery_check == 1)            //app에 BLE를 추가할때
            {
                List.mAdapter.addItem(context.getResources().getDrawable(R.drawable.lg_logo),ConnectionReceiver.sh_Pref.getString("device" + ConnectionReceiver.sh_Pref.getInt("device_num",0), "error") , ConnectionReceiver.sh_Pref.getString("address" + ConnectionReceiver.sh_Pref.getInt("device_num",0), "error"));
                MainActivity.recovery_check = 0;
            }
            else                                           //app에 맨처음 BLE가 등록될때
            {
                sh_Pref = context.getSharedPreferences("Device Information", Context.MODE_PRIVATE);
                toEdit = sh_Pref.edit();
                toEdit.putInt("device_num", device_num);
                toEdit.putString("device" + device_num, MainActivity.mDeviceAddress);
                toEdit.putString("address" + device_num, Map.getAddress(context));   //get address of phone
                List.mAdapter.addItem(context.getResources().getDrawable(R.drawable.lg_logo), MainActivity.mDeviceAddress, Map.getAddress(context));
            }

            toEdit.commit();

            List.mAdapter.notifyDataSetChanged();
            List.mListView.setAdapter(List.mAdapter);
        }
    }
}
