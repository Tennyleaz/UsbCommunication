package com.example.tenny.usbcommunication;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TabHost;

public class MyTabHost extends TabHost {

    public MyTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyTabHost(Context context) {
        super(context);
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode) {
        // leave it empty here. It looks that when you use hard keyboard,
        // this method will be called and the focus will be token.
    }
}
