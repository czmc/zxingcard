package com.czmc.zxingcard;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.czmc.zxingcard.decode.QRScanActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       Button button1 =  (Button)findViewById(R.id.button1);
       Button button2 =  (Button)findViewById(R.id.button2);
       Button button3 =  (Button)findViewById(R.id.button3);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
           case  R.id.button1:
               Intent intent = new Intent(this,CardScanActivity.class);
            startActivity(intent);
            break;
            case R.id.button2:
                Intent intent1 = new Intent(this,QRScanActivity.class);
                startActivity(intent1);
            break;
            case R.id.button3:
                Intent intent2 = new Intent(this,QRCodeGereraterActivity.class);
                startActivity(intent2);
            break;
        }
    }

}
