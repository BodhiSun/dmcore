package com.bodhi.download_lib;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;

import com.bodhi.http.HttpCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author : Sun
 * @version : 1.0
 * create time : 2018/11/21 17:23
 * desc :DownloadManager下载任务，用timer轮询监听下载进度,实现下载自动安装，并带有下载状态和安装状态的回调功能,安装apk采用fileprovider方式
 */
public class DownloadCore {

    public static String INTENT_DOWNLOAD = "intent_download";
    public static String INTENT_INSTALL = "intent_install";
    public static String DOWNLOAD_KEY = "download_key";


    private static DownloadCore downloadCore;

    public static DownloadCore getInstance() {
        if (downloadCore == null) {
            downloadCore = new DownloadCore();
        }
        return downloadCore;
    }

    private DownloadCore() {
    }

    ;

    private Context appContext;

    private HashMap<String, DownloadInfo> keyMap = new HashMap<>();

    public void init(Context context) {
        this.appContext = context;

        //初始化网络请求框架
        HttpCore.getInstance().init(context);
    }

    public void start(DownloadInfo downloadInfo, DownloadDuplicateListener duplicateListener) {
        boolean duplicate = false;
        if (downloadInfo.isSingleMode()) {
            if (isDownloading(downloadInfo)) {
                duplicate = true;
            }
        }

        if (!duplicate) {
            String key = "" + keyMap.size();
            keyMap.put(key, downloadInfo);
            appContext.startService(new Intent(appContext, DownloadService.class).setAction(INTENT_DOWNLOAD).putExtra(DOWNLOAD_KEY, key));
        } else {
            if (duplicateListener != null) {
                duplicateListener.onDuplicate();
            }
        }
    }


    public void install(final DownloadInfo info) {
        String key = getKey(info);
        if (!TextUtils.isEmpty(key)) {
            appContext.startService(new Intent(appContext, DownloadService.class).setAction(INTENT_INSTALL).putExtra(DOWNLOAD_KEY, key));
        }
    }

    public boolean isDownloading(DownloadInfo info) {
        Set<String> keys = keyMap.keySet();
        for (String key : keys) {
            DownloadInfo temp = keyMap.get(key);
            if (!temp.isDownloadComplete() && temp.getApkUrl().equals(info.getApkUrl())) {
                return true;
            }
        }

        return false;
    }

    public DownloadInfo getDownloadInfo(String key) {
        return keyMap.get(key);
    }

    public String getPackageName() {
        return appContext.getPackageName();
    }

    public String getKey(DownloadInfo info) {
        Set<String> keys = keyMap.keySet();
        for (String key : keys) {
            if (info.equals(keyMap.get(key))) {
                return key;
            }
        }
        return "";
    }

    public void updateProgress(final DownloadInfo item) {
        DownloadManager manager = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(item.getDownloadID());
        final Cursor cursor = manager.query(query);
        long currentSize = 0, totalSize = -1;
        int status =-1;
        try {
            if(cursor.moveToFirst()){
                currentSize =cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                totalSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            }
            int progress = (int)(currentSize*100/totalSize);

            item.onProgress(progress,currentSize,totalSize);
            if(status==DownloadManager.STATUS_SUCCESSFUL){
                item.downloadComplete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(cursor!=null){
                cursor.close();
            }
        }

    }


    public void installComplete(String pkgName) {
        List<DownloadInfo> infoList = new ArrayList<>(keyMap.values());
        for (DownloadInfo info : infoList) {
            if (info.getPackageName().equals(pkgName)) {
                info.installComplete();
            }
        }
    }

    public interface DownloadDuplicateListener {
        void onDuplicate();
    }
}
