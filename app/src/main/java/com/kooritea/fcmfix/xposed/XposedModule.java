package com.kooritea.fcmfix.xposed;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class XposedModule {

    protected XC_LoadPackage.LoadPackageParam loadPackageParam;

    protected XposedModule(final XC_LoadPackage.LoadPackageParam loadPackageParam){
        this.loadPackageParam = loadPackageParam;
    }

    protected abstract void onCanReadConfig() throws Exception;
    private boolean isRegisterUnlockBroadcastReceive = false;

    protected void printLog(String text){
        Intent log = new Intent("com.kooritea.fcmfix.log");
        log.putExtra("text",text);
        try{
            AndroidAppHelper.currentApplication().getApplicationContext().sendBroadcast(log);
        }catch (Exception e){
            XposedBridge.log("[fcmfix] "+ text);
        }
    }

    /**
     * 多次调用也仅调用一次onCanReadConfig
     * @param context
     */
    protected void checkUserDeviceUnlock(Context context){
        if(!isRegisterUnlockBroadcastReceive){
            if (context.getSystemService(UserManager.class).isUserUnlocked()) {
                try {
                    this.onCanReadConfig();
                } catch (Exception e) {
                    printLog("读取配置文件初始化失败: " + e.getMessage());
                }
            } else {
                isRegisterUnlockBroadcastReceive = true;
                AndroidAppHelper.currentApplication().getApplicationContext().registerReceiver(unlockBroadcastReadConfigReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
                AndroidAppHelper.currentApplication().getApplicationContext().registerReceiver(new BroadcastReceiver() {
                    public void onReceive(Context context1, Intent intent) {
                        String action = intent.getAction();
                        if (Intent.ACTION_USER_UNLOCKED.equals(action)) {
                            printLog("Send broadcast GCM_RECONNECT due to screen on");
                            AndroidAppHelper.currentApplication().getApplicationContext().sendBroadcast(new Intent("com.google.android.intent.action.GCM_RECONNECT"));
                        }
                    }
                }, new IntentFilter(Intent.ACTION_SCREEN_ON));
            }
        }

    }

    private BroadcastReceiver unlockBroadcastReadConfigReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_USER_UNLOCKED.equals(action)) {
                printLog("User Device Unlock Broadcast");
                try {
                    onCanReadConfig();
                    AndroidAppHelper.currentApplication().getApplicationContext().unregisterReceiver(unlockBroadcastReadConfigReceiver);
                    isRegisterUnlockBroadcastReceive = false;
                } catch (Exception e) {
                    printLog("读取配置文件初始化失败: " + e.getMessage());
                }
            }
        }
    };

}
