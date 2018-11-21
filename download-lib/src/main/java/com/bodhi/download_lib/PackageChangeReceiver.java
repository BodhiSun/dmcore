package com.bodhi.download_lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author : Sun
 * @version : 1.0
 * create time : 2018/11/21 17:21
 * desc :
 */
public class PackageChangeReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_PACKAGE_ADDED)||action.equals(Intent.ACTION_PACKAGE_REPLACED)){
            String pkgName = intent.getData().getSchemeSpecificPart();
            DownloadCore.getInstance().installComplete(pkgName);
        }
    }
}
