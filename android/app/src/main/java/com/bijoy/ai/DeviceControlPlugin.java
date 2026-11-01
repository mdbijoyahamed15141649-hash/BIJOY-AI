package com.bijoy.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.media.AudioManager;
import android.hardware.camera2.CameraManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.util.ArrayList;

@CapacitorPlugin(
    name = "DeviceControl",
    permissions = {
        @Permission(alias = "camera", strings = {Manifest.permission.CAMERA}),
        @Permission(alias = "microphone", strings = {Manifest.permission.RECORD_AUDIO})
    }
)
public class DeviceControlPlugin extends Plugin {

    @PluginMethod
    public void startVoice(PluginCall call) {
        if (getPermissionState("microphone") != PermissionState.GRANTED) {
            requestPermissionForAlias("microphone", call, "voicePermissionCallback");
            return;
        }
        launchVoice(call);
    }

    @PermissionCallback
    private void voicePermissionCallback(PluginCall call) {
        if (getPermissionState("microphone") == PermissionState.GRANTED) {
            launchVoice(call);
        } else {
            call.reject("মাইক্রোফোন অনুমতি প্রয়োজন");
        }
    }

    private void launchVoice(PluginCall call) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "স্যার, আপনার নির্দেশ বলুন");
        startActivityForResult(call, intent, "voiceResult");
    }

    @ActivityCallback
    private void voiceResult(PluginCall call, ActivityResult result) {
        if (call == null) return;

        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            ArrayList<String> matches = result.getData().getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (matches != null && !matches.isEmpty()) {
                JSObject data = new JSObject();
                data.put("text", matches.get(0));
                call.resolve(data);
                return;
            }
        }
        call.reject("কথা শনাক্ত করা যায়নি");
    }

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
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            getActivity().startActivity(intent);
            call.resolve();
        } else {
            call.reject("ক্যামেরা পাওয়া যায়নি");
        }
    }

    @PluginMethod
    public void launchApp(PluginCall call) {
        String packageName = call.getString("packageName", "");
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
        int percent = Math.max(0, Math.min(100, call.getInt("percent", 50)));
        AudioManager audio = (AudioManager)
                getContext().getSystemService(Context.AUDIO_SERVICE);
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                Math.round(max * percent / 100f), 0);
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
            call.reject("ক্যামেরা অনুমতি প্রয়োজন");
        }
    }

    private void changeTorch(PluginCall call) {
        try {
            boolean enabled = call.getBoolean("enabled", false);
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
            call.reject("ফ্ল্যাশলাইট পাওয়া যায়নি");
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }
}
