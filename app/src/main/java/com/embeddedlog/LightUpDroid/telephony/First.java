package com.embeddedlog.LightUpDroid.telephony;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.embeddedlog.LightUpDroid.AlarmActivity;
import com.embeddedlog.LightUpDroid.DeskClock;
import com.embeddedlog.LightUpDroid.MainActivity;
import com.embeddedlog.LightUpDroid.R;

/**
 * Created by k54hu on 12/6/2016.
 */

public class First extends AppCompatActivity {


    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.first);




// Giao Vien
        btn = (Button)findViewById(R.id.gv);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(First.this, AlarmActivity.class);
                startActivity(intent);
            }
        });
// Hoc sinh
        btn = (Button)findViewById(R.id.gc);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(First.this, MainActivity.class);
                startActivity(intent);
            }
        });
//Nhac nhơ
        btn = (Button)findViewById(R.id.hs);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(First.this, DeskClock.class);
                startActivity(intent);
            }
        });

    }

}
