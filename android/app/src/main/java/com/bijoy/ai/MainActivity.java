package com.bijoy.ai;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        registerPlugin(DeviceControlPlugin.class);
        registerPlugin(BackgroundVoicePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
