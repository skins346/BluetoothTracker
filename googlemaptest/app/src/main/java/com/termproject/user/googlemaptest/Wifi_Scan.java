package com.termproject.user.googlemaptest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * WIFI Scanner
 *
 * @author Seon
 *
 */
public class Wifi_Scan extends Activity  {
    public  static ListView mListView2 = null;
    public  static ListViewAdapter mAdapter2 = null;

    private class ViewHolder {
        public TextView mText;
        public TextView location;
    }

    //This listviewadapter exists for adding items(time table, tip calculator..)
    public class ListViewAdapter extends BaseAdapter {
        private Context mContext = null;
        public ArrayList<ListData> mListData = new ArrayList<ListData>();

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
        public void addItem( String mTitle, String location){
            ListData addInfo = null;
            addInfo = new ListData();
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
                convertView = inflater.inflate(R.layout.listview_item2, null);

                holder.mText = (TextView) convertView.findViewById(R.id.wifi_name);
                holder.location = (TextView) convertView.findViewById(R.id.wifi_address);

                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }

            ListData mData = mListData.get(position);



            holder.mText.setText(mData.mTitle);
            holder.location.setText(mData.location);

            return convertView;
        }
    }


    private static final String TAG = "WIFIScanner";

    // WifiManager variable
    WifiManager wifimanager;

    // UI variable
    String text = "";
    private java.util.List<ScanResult> mScanResult; // ScanResult List

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getWIFIScanResult(); // get WIFISCanResult

            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    public void getWIFIScanResult() {

        mScanResult = wifimanager.getScanResults(); // ScanResult

        for (int i = 0; i < mScanResult.size(); i++) {
            ScanResult result = mScanResult.get(i);
            mAdapter2.addItem(result.SSID.toString(), result.BSSID.toString() );
            mListView2.setAdapter(mAdapter2);
        }

    }

    public void initWIFIScan() {
        // init WIFISCAN
        text = "";
        final IntentFilter filter = new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        wifimanager.startScan();
        Log.d(TAG, "initWIFIScan()");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi__scan);

        // Setup UI
        mListView2= (ListView) findViewById(R.id.wifi_list);
        mAdapter2 = new ListViewAdapter(this);

        // Setup WIFI
        wifimanager = (WifiManager) getSystemService(WIFI_SERVICE);
        Log.d(TAG, "Setup WIfiManager getSystemService");

        // if WIFIEnabled
        if (wifimanager.isWifiEnabled() == false)
            wifimanager.setWifiEnabled(true);

        initWIFIScan(); // start WIFIScan

        mListView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ListData mData = mAdapter2.mListData.get(position);
                MainActivity.wifi_address = mData.mTitle;
                finish();
            }
        });
    }

}
