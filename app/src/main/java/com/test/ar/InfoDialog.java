package com.test.ar;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.widget.TextView;

public class InfoDialog extends Dialog {
    private Activity activity;
    private TextView textInfo;
    private String text;

    InfoDialog(Activity act, String text) {
        super(act);

        this.activity = act;
        this.text = text;

        // Remove cancelable
        this.setCancelable(false);

        // Remove background overlay
        this.getWindow().setDimAmount(0);

        // Bottom position
        this.getWindow().setGravity(Gravity.BOTTOM);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_info);

        textInfo = (TextView) findViewById(R.id.textInfo);
        textInfo.setText("Text for: " + text);
    }
}