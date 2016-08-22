package com.termproject.user.googlemaptest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AddInfo extends Activity implements View.OnClickListener {

    Button btn, btn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_info);

        btn = (Button)findViewById(R.id.register);
        btn2 = (Button)findViewById(R.id.btn2);

        btn.setOnClickListener(this);
        btn2.setOnClickListener(this);

    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.register) {
            startActivity(new Intent(this,Wifi_Scan.class));
        }
        else if(v.getId() == R.id.btn2)
            finish();
    }
}
