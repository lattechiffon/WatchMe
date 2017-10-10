package com.lattechiffon.hanium;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * 개발자 정보를 보여주는 클래스입니다.
 *
 * @version 1.0
 * @author  Yongguk Go (lattechiffon@gmail.com)
 */
public class DeveloperActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        setContentView(R.layout.activity_developer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.activity_developer));
        }
    }

    @Override
    public void onBackPressed() {
        super .onBackPressed();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }
}