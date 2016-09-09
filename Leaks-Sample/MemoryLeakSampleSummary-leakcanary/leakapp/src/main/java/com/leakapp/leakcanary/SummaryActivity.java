package com.leakapp.leakcanary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.leakapp.leakcanary.leaks.HandlerLeakActivity;

public class SummaryActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_handler:
                toActivity(HandlerLeakActivity.class);
                break;
            case R.id.btn_inner_class:
                //toActivity(InnerClassLeakActivity.class);
                break;
            case R.id.btn_anonymous:
                //toActivity(AnonymousClassLeakActivity.class);
                break;
            case R.id.btn_singleton_context:
                //toActivity(SingletonContextLeakActivity.class);
                break;
            case R.id.btn_static_drawable:
                //toActivity(StaticDrawableActivity.class);
                break;

        }
    }

    private void toActivity(Class cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }
}
