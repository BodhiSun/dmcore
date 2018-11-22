package com.bodhi.download_lib;

import android.app.DownloadManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;

/**
 * @author : Sun
 * @version : 1.0
 * create time : 2018/11/21 20:12
 * desc :
 */
public class DownloadService extends Service {
    /**
     * 下载
     */
    public static final int WHAT_DOWNLOAD = 0x0000_0101;
    /**
     * 安装
     */
    private static final int WHAT_INSTALL = 0X0000_0102;

    private DownloadManager downloadManager;

    private Handler downloadHandler;
    private PackageChangeReceiver packageChangeReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        registerPackageReceiver();
    }

    private void registerPackageReceiver() {
        //8.0注册安装广播
        packageChangeReceiver = new PackageChangeReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addDataScheme("package");
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);

        registerReceiver(packageChangeReceiver,intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(DownloadCore.INTENT_DOWNLOAD)) {
                getHandler().sendMessage(getHandler().obtainMessage(WHAT_DOWNLOAD,intent.getStringExtra(DownloadCore.DOWNLOAD_KEY)));
            } else if (action.equals(DownloadCore.INTENT_INSTALL)) {
                getHandler().sendMessage(getHandler().obtainMessage(WHAT_INSTALL,intent.getStringExtra(DownloadCore.DOWNLOAD_KEY)));
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void startDownload(DownloadInfo info){
        if (info==null) {
            return;
        }

        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(info.getApkUrl()));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalFilesDir(getApplicationContext(), Environment.DIRECTORY_DOWNLOADS,(info.getName().equals("")?"暂无名称"+"":info.getName())+".apk");
        request.setVisibleInDownloadsUi(true);
        //将下载请求放入队列， return下载任务的ID
        long id = downloadManager.enqueue(request);
        info.downloadStart(id);

    }

    @Override
    public void onDestroy() {
        if(downloadHandler!=null){
            downloadHandler.removeCallbacksAndMessages(null);
            downloadHandler=null;
        }
        unregisterReceiver(packageChangeReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Handler getHandler() {
        if (downloadHandler != null) {
            return downloadHandler;
        }
        return new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case WHAT_DOWNLOAD:
                        startDownload(DownloadCore.getInstance().getDownloadInfo((String)msg.obj));
                        break;
                    case WHAT_INSTALL:
//                        installApk(DownloadCore.getInstance().getDownloadInfo((String)msg.obj));
                        installApkWithFileProvider(DownloadCore.getInstance().getDownloadInfo((String)msg.obj));
                        break;
                }
            }
        };

    }

    private void installApk(DownloadInfo info) {
        infoPackageName(info);

        Uri apkUri = downloadManager.getUriForDownloadedFile(info.getDownloadID());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String typeStr="application/vnd.android.package-archive";
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try{
            intent.setDataAndType(apkUri,typeStr);
            startActivity(intent);
            info.installStart();
        }catch (Exception e){

            File apk = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/" + ((info.getName().equals("")?"暂无名称"+"":info.getName())+".apk"));
            if(apk!=null){
                String apkPath = apk.getAbsolutePath();
                apkUri = Uri.parse("file://" + apkPath);
            }

            intent.setDataAndType(apkUri,typeStr);
            startActivity(intent);
            info.installStart();
        }
    }

    private void infoPackageName(DownloadInfo info) {
        Uri fileUri = downloadManager.getUriForDownloadedFile(info.getDownloadID());
        File apkFile = getRealFile(fileUri);

        PackageInfo pi = getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
        if (pi != null) {
            info.setPackageName(pi.packageName);
        }
    }

//------------------------------------------------------华丽的分割线-----------------------------------------------------------------

    private void installApkWithFileProvider(DownloadInfo info) {

        Uri fileUri = downloadManager.getUriForDownloadedFile(info.getDownloadID());

        File apkFile = getRealFile(fileUri);

        PackageInfo pi = getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
        if (pi != null) {
            info.setPackageName(pi.packageName);
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory("android.intent.category.DEFAULT");
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(this, DownloadCore.getInstance().getPackageName() + ".fileprovider", apkFile);
            intent.setDataAndType(uri, getContentResolver().getType(uri));
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), getIntentType(apkFile));
        }
        try {
            startActivity(intent);
            info.installStart();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "安装出现未知问题", Toast.LENGTH_SHORT).show();
        }
    }

    private File getRealFile(Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String path = null;
        if (scheme == null || ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = getApplicationContext().getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        path = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return null == path ? null : new File(path);
    }

    private String getIntentType(File file) {
        String suffix = file.getName();
        String name = suffix.substring(suffix.lastIndexOf(".") + 1, suffix.length()).toLowerCase();
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(name);
    }

}
