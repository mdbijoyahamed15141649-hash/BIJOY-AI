package com.bijoy.ai;

import android.Manifest;
import android.content.Intent;
import android.content.Context;
import android.media.AudioManager;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraAccessException;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.getcapacitor.*;
import com.getcapacitor.annotation.*;

@CapacitorPlugin(
    name = "DeviceControl",
    permissions = {
        @Permission(alias = "camera", strings = {Manifest.permission.CAMERA})
    }
)
public class DeviceControlPlugin extends Plugin {

    @PluginMethod
    public void openSettings(PluginCall call) {
        String type = call.getString("type", "settings");
        Intent intent;

        switch (type) {
            case "wifi":
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                break;
            case "bluetooth":
                intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                break;
            default:
                intent = new Intent(Settings.ACTION_SETTINGS);
        }

        getActivity().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void openCamera(PluginCall call) {
        try {
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            getActivity().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("ক্যামেরা খোলা যায়নি");
        }
    }

    @PluginMethod
    public void launchApp(PluginCall call) {
        String packageName = call.getString("packageName", "");

        if (packageName.isEmpty()) {
            call.reject("অ্যাপের package name দিন");
            return;
        }

        Intent intent = getContext().getPackageManager()
            .getLaunchIntentForPackage(packageName);

        if (intent == null) {
            call.reject("অ্যাপটি পাওয়া যায়নি");
            return;
        }

        getActivity().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void setVolume(PluginCall call) {
        int percent = call.getInt("percent", 50);
        percent = Math.max(0, Math.min(100, percent));

        AudioManager audio = (AudioManager)
            getContext().getSystemService(Context.AUDIO_SERVICE);

        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            Math.round(max * percent / 100f),
            0
        );

        call.resolve();
    }

    @PluginMethod
    public void setTorch(PluginCall call) {
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "cameraPermissionCallback");
            return;
        }
        changeTorch(call);
    }

    @PermissionCallback
    private void cameraPermissionCallback(PluginCall call) {
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            changeTorch(call);
        } else {
            call.reject("ফ্ল্যাশলাইটের জন্য ক্যামেরা অনুমতি প্রয়োজন");
        }
    }

    private void changeTorch(PluginCall call) {
        boolean enabled = call.getBoolean("enabled", false);

        try {
            CameraManager manager = (CameraManager)
                getContext().getSystemService(Context.CAMERA_SERVICE);

            for (String id : manager.getCameraIdList()) {
                Boolean available = manager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);

                if (Boolean.TRUE.equals(available)) {
                    manager.setTorchMode(id, enabled);
                    call.resolve();
                    return;
                }
            }

            call.reject("এই ফোনে ফ্ল্যাশলাইট পাওয়া যায়নি");
        } catch (Exception e) {
            call.reject("ফ্ল্যাশলাইট চালানো যায়নি: " + e.getMessage());
        }
    }
}
