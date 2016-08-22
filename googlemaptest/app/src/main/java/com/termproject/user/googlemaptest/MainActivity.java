package com.termproject.user.googlemaptest;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity  extends FragmentActivity {


    public static String wifi_address = null;
    public static int recovery_check = 0;    //app에 ble를 등록할때 전에 연결되었던 ble인지 아니면 새로운 ble를 등록하는 것인지 구분할때 사용됨
    public static Context context;
    public static int device_selected = 0;
    public static int activityCheck = 0;
    private final static String TAG = "check";
    public static  String mDeviceName;
    public static  String mDeviceAddress;
    public static BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    public DeviceScanActivity deviceScan;

    static ViewPager pager; //ViewPager 참조변수
    ActionBar actionBar;  //ActionBar 참조변수
    CustomAdapter adapter;
    NetworkInfo mobile;
    NetworkInfo wifi;

    public static FragmentManager fragmentManager;

    // Code to manage Service lifecycle.
    public static ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //finish();
            }
            // Automatically connects to the device upon successful start-up initialization.

            ConnectionReceiver.sh_Pref = MainActivity.context.getSharedPreferences("Device Information", Context.MODE_PRIVATE);
            ConnectionReceiver.toEdit = ConnectionReceiver.sh_Pref.edit();

            if (ConnectionReceiver.sh_Pref.contains("device_num")) {
                recovery_check = 1;
                int length = ConnectionReceiver.sh_Pref.getInt("device_num", 0);
                mBluetoothLeService.connect(ConnectionReceiver.sh_Pref.getString("device" + ConnectionReceiver.sh_Pref.getInt("device_num", 0), "error"));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                   mConnected = false;
                   updateConnectionState(R.string.disconnected);
                   invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private static boolean isOnline(Activity activity) { // network 연결 상태 확인
        try {
            ConnectivityManager conMan = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState(); // wifi
            if (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING) {
                return true;
            }

            NetworkInfo.State mobile = conMan.getNetworkInfo(0).getState(); // mobile ConnectivityManager.TYPE_MOBILE
            if (mobile == NetworkInfo.State.CONNECTED || mobile == NetworkInfo.State.CONNECTING) {
                return true;
            }
        } catch (NullPointerException e) {
            return false;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items

        switch (item.getItemId()) {
            case R.id.ic_action_add:
                startActivity(new Intent(this,DeviceScanActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

public  Handler handler = new Handler() {
    public void handleMessage(Message msg) {
        if(msg.arg1 == 1){

            if(BluetoothLeService.rssi_value!= 0)
              Toast.makeText(getApplicationContext(),"RSSI: "+BluetoothLeService.rssi_value, Toast.LENGTH_LONG).show();
        }
    }
};

    public void Vibe(){       //연결범위 밖으로 나갔을 시 핸드폰 진동

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);  // <- or Context.VIBRATE_SERVICE)
        vibrator.vibrate(5000);
    }

    public   String getCurrentSsid(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

            ssid  = connectionInfo.getBSSID();
        }
        return ssid;
    }

    class rssi_thread extends Thread {

        public void run() {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(BluetoothLeService.mConnectionState == 0 && activityCheck==1) //disconnected
                {
                    if(MainActivity.wifi_address!=null && MainActivity.wifi_address.equals( getCurrentSsid(context)) )
                    {
                     //wifi safe zone
                    }
                    else{
                         Vibe();
                         BluetoothLeService.rssi_value=0;
                    }
                    continue;
                }

                if (BluetoothLeService.mBluetoothGatt != null)          //BLE신호세기 읽어와서 Toast Message 띄움
                {
                    BluetoothLeService.mBluetoothGatt.readRemoteRssi();
                    Message msg = handler.obtainMessage();
                    msg.arg1 = 1;
                    handler.sendMessage(msg);
                }

            }

        }

    }
    private class ViewHolder {
        public ImageView mIcon;
        public TextView mText;
        public TextView location;
    }

    //This listviewadapter exists for adding items(time table, tip calculator..)
    public class ListViewAdapter extends BaseAdapter {
        private Context mContext = null;
        private ArrayList<ListData> mListData = new ArrayList<ListData>();

        public ListViewAdapter(Context mContext) {
            super();
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            return mListData.size();
        }

        @Override
        public Object getItem(int position) {
            return mListData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        // add item(image , title)
        public void addItem(Drawable icon, String mTitle, String location){
            ListData addInfo = null;
            addInfo = new ListData();
            addInfo.mIcon = icon;
            addInfo.mTitle = mTitle;
            addInfo.location = location;
            mListData.add(addInfo);
        }

        //override getView method
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();

                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_item, null);

                holder.mIcon = (ImageView) convertView.findViewById(R.id.mImage);
                holder.mText = (TextView) convertView.findViewById(R.id.mText);
                holder.location = (TextView) convertView.findViewById(R.id.location);

                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }

            ListData mData = mListData.get(position);

            if (mData.mIcon != null) {
                holder.mIcon.setVisibility(View.VISIBLE);
                holder.mIcon.setImageDrawable(mData.mIcon);
            }else{
                holder.mIcon.setVisibility(View.GONE);
            }

            holder.mText.setText(mData.mTitle);
            holder.location.setText(mData.location);

            return convertView;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context= this;

        if(device_selected == 0) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);     //서비스 시작

            getActionBar().setTitle(mDeviceName);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        deviceScan= new DeviceScanActivity();
        fragmentManager = getFragmentManager();

        ConnectivityManager manager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (wifi.isConnected() || mobile.isConnected()) {
            System.out.println("연결됨");

            setContentView(R.layout.activity_main);

            actionBar= getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            pager= (ViewPager)findViewById(R.id.container);
            adapter= new CustomAdapter(getSupportFragmentManager());
            pager.setAdapter(adapter);
            pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                //Page가 일정부분 넘겨져서 현재 Page가 바뀌었을 때 호출
                //이전 Page의 80%가 이동했을때 다음 Page가 현재 Position으로 설정됨.
                //파라미터 : 현재 변경된 Page의 위치
                @Override
                public void onPageSelected(int position) {
                    // TODO Auto-generated method stub

                    //ViewPager는 3개의 View를 가지고 있도록 설계하였으므로.
                    //Position도 역시 가장 왼쪽 처음부터(0,1,2 순으로 되어있음)
                    //현재 전면에 놓여지는 ViewPager의 Page에 해당하는 Position으로
                    //ActionBar의 Tab위치를 변경.
                    actionBar.setSelectedNavigationItem(position);
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onPageScrollStateChanged(int arg0) {
                    // TODO Auto-generated method stub

                }
            });


            //ActionBar에 추가 될 Tab 참조변수
            ActionBar.Tab tab=null;

            //첫번째 Tab 객체 생성 및 ActionBar에 추가하기
            tab= actionBar.newTab(); //ActionBar에 붇는 Tab객체 생성
            tab.setText("Map");    //Tab에 보여지는 글씨

            //Tab의 선택이 변경되는 것을 인지하는 TabListener 설정(아래쪽 객체 생성 코드 참고)
            tab.setTabListener(listener);

            //ActionBar에 Tab 추가
            actionBar.addTab(tab);

            //두번째 Tab 객체 생성 및 ActionBar에 추가하기
            tab= actionBar.newTab(); //ActionBar에 붇는 Tab객체 생성
            tab.setText("List");     //Tab에 보여지는 글씨
            //Tab의 선택이 변경되는 것을 인지하는 TabListener 설정(아래쪽 객체 생성 코드 참고)
            tab.setTabListener(listener);

            //ActionBar에 Tab 추가
            actionBar.addTab(tab);
            actionBar.setSelectedNavigationItem(1);
        }
        else {
            setContentView(R.layout.network_exception);
        }

        rssi_thread temp = new rssi_thread();                   //BLE 신호세기 읽어오는 쓰레드 실행
        temp.start();
    }//onCreate Method...

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        unregisterReceiver(mGattUpdateReceiver);
        unbindService(MainActivity.mServiceConnection);
        MainActivity.mBluetoothLeService = null;
        MainActivity.activityCheck = 0;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
         //       mConnectionState.setText(resourceId);
                Toast.makeText(getApplicationContext(), resourceId , Toast.LENGTH_LONG).show();
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public  class  CustomAdapter extends FragmentPagerAdapter {

        public CustomAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }
        //PagerAdapter가 가지고 잇는 View의 개수를 리턴
        //Tab에 따른 View를 보여줘야 하므로 Tab의 개수인 3을 리턴..
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return 2; //보여줄 View의 개수 리턴(Tab이 3개라서 3을 리턴)
        }

        //ViewPager가 현재 보여질 Item(View객체)를 생성할 필요가 있는 때 자동으로 호출
        //쉽게 말해, 스크롤을 통해 현재 보여져야 하는 View를 만들어냄.
        //첫번째 파라미터 : ViewPager
        //두번째 파라미터 : ViewPager가 보여줄 View의 위치(가장 처음부터 0,1,2,3...)
        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            View view=null;//현재 position에서 보여줘야할 View를 생성해서 리턴.

            switch(position) {
                case 0:
                    return Map.newInstance();
                case 1:
                    return List.newInstance();

            }

            return null;
        }

    }

    //ActionBar의 Tab 선택에 변화가 생기는 것을 인지하는 리스너(Listener)
    ActionBar.TabListener listener= new ActionBar.TabListener() {

        //Tab의 선택이 벗어날 때 호출
        //첫번째 파라미터 : 선택에서 벗어나는 Tab 객체
        //두번째 파라미터 : Tab에 해당하는 View를 Fragment로 만들때 사용하는 트랜젝션.(여기서는 사용X)
        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }

        //Tab이 선택될 때 호출
        //첫번째 파라미터 : 선택된 Tab 객체
        //두번째 파라미터 : Tab에 해당하는 View를 Fragment로 만들때 사용하는 트랜젝션.(여기서는 사용X)
        @Override
        public  void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

            //선택된 Tab객체의 위치값(왼족 처음부터 0,1,2....순으로 됨)
            int position = tab.getPosition();

            //Tab의 선택 위치에 따라 ViewPager에서 보여질 Item(View)를 설정
            //첫번째 파라미터: ViewPager가 현재 보여줄 View의 위치
            //두번째 파라미터: 변경할 때 부드럽게 이동하는가? false면 팍팍 바뀜

            if(position == 0)
            {
                if(isOnline(MainActivity.this) == false)
                {
                   android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
                    android.support.v4.app.FragmentTransaction transaction = manager.beginTransaction();

                    transaction.replace(R.id.container, new Notice2());
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            }

            pager.setCurrentItem(position,true);
        }

        //Tab이 재 선택될 때 호출
        //첫번째 파라미터 : 재 선택된 Tab 객체
        //두번째 파라미터 : Tab에 해당하는 View를 Fragment로 만들때 사용하는 트랜젝션.(여기서는 사용X)
        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }
    };

}
