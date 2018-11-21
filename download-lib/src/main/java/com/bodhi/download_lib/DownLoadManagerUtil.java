package com.bodhi.download_lib;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author : Sun
 * @version :
 * create time : 2018/7/13 15:47
 * desc :DownloadManager下载任务，用ContentObserver监听下载进度,实现下载并自动安装功能
 */
public class DownLoadManagerUtil {
    public static final int HANDLE_DOWNLOAD = 0x001;
    public static final float UNBIND_SERVICE = 2.0F;
    private Context mContext;
    private DownloadManager downloadManager;
    private ScheduledExecutorService scheduledExecutorService;
    private OnProgressListener onProgressListener;

    private DownloadChangeObserver downloadChangeObserver;
    private long downloadId;
    private DownloadBroadcast downloadBroadcast;

    public Handler downLoadHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(onProgressListener!=null && HANDLE_DOWNLOAD==msg.what){
                //被除数可以为0，除数必须大于0
                if(msg.arg1>=0 && msg.arg2>0){
                    onProgressListener.onProgress((msg.arg1*100/msg.arg2));
                }
            }
        }
    };

    private Runnable progressRunnable =new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };
    private String name;

    public DownLoadManagerUtil(Context mContext) {
        this.mContext = mContext;
    }

    //下载apk
    public void downloadAPK(String url, String name) {
        this.name=name;
        downloadManager = (DownloadManager)mContext.getSystemService(Context.DOWNLOAD_SERVICE);

        downloadChangeObserver = new DownloadChangeObserver();

        //在执行下载前注册内容监听者
        registerContentObserver();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        /**设置用于下载时的网络状态*/
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

        /**设置通知栏是否可见*/
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        /**设置漫游状态下是否可以下载*/
        request.setAllowedOverRoaming(false);

        /**如果我们希望下载的文件可以被系统的Downloads应用扫描到并管理，
         我们需要调用Request对象的setVisibleInDownloadsUi方法，传递参数true.*/
        request.setVisibleInDownloadsUi(true);

        /**设置文件保存路径*/
        request.setDestinationInExternalFilesDir(mContext.getApplicationContext(), Environment.DIRECTORY_DOWNLOADS,name);

        /**将下载请求放入队列， return下载任务的ID*/
        downloadId = downloadManager.enqueue(request);

        //执行下载任务时注册广播监听下载成功状态
        registerBroadcast();
    }

    /**
     * 发送Handler消息更新进度和状态
     */
    private void updateProgress() {
        int[] bytesAndStatus = getBytesAndStatus(downloadId);
        downLoadHandler.sendMessage(downLoadHandler.obtainMessage(HANDLE_DOWNLOAD, bytesAndStatus[0], bytesAndStatus[1], bytesAndStatus[2]));
    }


    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     *
     * @param downloadId
     * @return
     */
    private int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{
                -1, -1, 0
        };
        DownloadManager.Query query =new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor=downloadManager.query(query);
            if(cursor!=null && cursor.moveToFirst()){
                //已经下载文件大小
                bytesAndStatus[0]=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //下载文件的总大小
                bytesAndStatus[1]=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                //下载状态
                bytesAndStatus[2]=cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bytesAndStatus;
    }

    /**
     * 注册广播
     */
    private void registerBroadcast(){
        /**注册service 广播 1.任务完成时 2.进行中的任务被点击*/
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        mContext.registerReceiver(downloadBroadcast =new DownloadBroadcast(),intentFilter);
    }

    /**
     * 注销广播
     */
    private void unregisterBroadcast() {
        if (downloadBroadcast != null) {
            mContext.unregisterReceiver(downloadBroadcast);
            downloadBroadcast = null;
        }
    }

    /**
     * 注册ContentObserver
     */
    private void registerContentObserver() {
        /** observer download change **/
        if (downloadChangeObserver != null) {
            mContext.getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), false, downloadChangeObserver);
        }
    }

    /**
     * 注销ContentObserver
     */
    private void unregisterContentObserver() {
        if (downloadChangeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(downloadChangeObserver);
        }
    }

    /**
     * 关闭定时器，线程等操作
     */
    private void close(){
        if(scheduledExecutorService!=null && !scheduledExecutorService.isShutdown()){
            scheduledExecutorService.shutdown();
        }

        if(downLoadHandler!=null){
            downLoadHandler.removeCallbacksAndMessages(null);
        }
    }

    private void installApk(Uri apkUri) {
        Log.i("test","apkUri:"+apkUri);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String typeStr="application/vnd.android.package-archive";
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{
            intent.setDataAndType(apkUri,typeStr);
            mContext.startActivity(intent);
        }catch (Exception e){
            File apk = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/" + name);
            if(apk!=null){
                String apkPath = apk.getAbsolutePath();
                Log.i("test","apkPath:"+apkPath);
                apkUri = Uri.parse("file://" + apkPath);
                Log.i("test","apkUri2:"+apkUri);
            }

            intent.setDataAndType(apkUri,typeStr);
            mContext.startActivity(intent);
        }
    }

    /**
     * 对外开发的方法
     *
     * @param onProgressListener
     */
    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }


    /**
     * 监听DownLoadManager下载进度
     *
     */
    public class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver(){
            super(downLoadHandler);
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         *
         * @param selfChange 此值意义不大, 一般情况下该回调值false
         */
        @Override
        public void onChange(boolean selfChange) {
            scheduledExecutorService.scheduleAtFixedRate(progressRunnable,0,2, TimeUnit.SECONDS);
        }
    }

    /**
     * 接受下载完成广播
     */
    private class DownloadBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            switch (intent.getAction()){
                case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                        if(downloadId == downId && downId!=-1&&downloadManager!=null){
                            Uri uriForDownloadedFile = downloadManager.getUriForDownloadedFile(downloadId);

                            close();

                            if(uriForDownloadedFile!=null){
//                                Log.e("test","广播监听下载完成，APK存储路径为 ："+uriForDownloadedFile.getPath());
//                                SPUtil.put(SPUtil.SP_DOWNLOAD_PATH,uriForDownloadedFile.getPath());

                                installApk(uriForDownloadedFile);

                                unregisterBroadcast();
                                unregisterContentObserver();
                            }

                            if(onProgressListener!=null){
                                onProgressListener.onProgress(UNBIND_SERVICE);
                            }
                        }
                    break;
                case DownloadManager.ACTION_NOTIFICATION_CLICKED:

                    break;
                default:
                    break;
            }
        }
    }


    public interface OnProgressListener {
        /**
         * 下载进度
         *
         * @param fraction 已下载/总大小
         */
        void onProgress(float fraction);
    }
}
