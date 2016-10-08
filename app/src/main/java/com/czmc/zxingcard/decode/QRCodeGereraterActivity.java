package com.czmc.zxingcard.decode;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.czmc.zxingcard.Intents;
import com.czmc.zxingcard.R;
import com.czmc.zxingcard.decode.Contents;
import com.google.zxing.BarcodeFormat;

public class QRCodeGereraterActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_gererater);
        et_text =  (EditText)findViewById(R.id.et_text);
        Button button =  (Button)findViewById(R.id.btn_generator);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case  R.id.btn_generator:
                if(et_text==null) return;
                if(!TextUtils.isEmpty(et_text.getText().toString()))
                     launchSearch(et_text.getText().toString());
                break;
        }
    }

    private void launchSearch(String text) {
        Intent intent = new Intent(Intents.Encode.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
        intent.putExtra(Intents.Encode.DATA, text);
        intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
        startActivity(intent);
    }
}
