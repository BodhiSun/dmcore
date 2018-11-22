package com.bodhi.downloadmanager_core;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bodhi.download_lib.DownLoadManagerUtil;
import com.bodhi.download_lib.DownloadCore;
import com.bodhi.download_lib.DownloadInfo;
import com.bodhi.download_lib.DownloadListener;

public class MainActivity extends AppCompatActivity {
//    public static String url="http://down.jser123.com/app-debug-v3.0.1_301_2_yyb_sign.apk";
    public static String url="http://down.jser123.com/bktt_release_v2.0.2_202_3__DEFAULT___sign.apk";
    private TextView tv_down_status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_down_status=findViewById(R.id.tv);

        DownloadCore.getInstance().init(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean b = getPackageManager().canRequestPackageInstalls();
            if (!b) {
                Uri packageURI = Uri.parse("package:" + getPackageName());
                //注意这个是8.0新API
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                startActivityForResult(intent, 10086);
            }
        }
    }

    public void testDownloadCore(View view) {
        DownloadInfo info = DownloadInfo.build(MainActivity.url).autoInstall().singleMode();
                info.listener(new DownloadListener() {
                    @Override
                    public void onStart() {
                        Log.i("test","------onStart-----");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_down_status.setText("开始");
                            }
                        });
                    }

                    @Override
                    public void onProgress(final int progress, long currentSize, long totalSize) {
                        Log.i("test","------onProgress---progress:"+progress);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_down_status.setText(progress+"%");

                            }
                        });

                    }

                    @Override
                    public void onComplete() {
                        Log.i("test","------onComplete-----");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_down_status.setText("下载完成");

                            }
                        });

                    }

                    @Override
                    public void onFail() {
                        Log.i("test","------onFail-----");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_down_status.setText("失败");

                            }
                        });

                    }

                    @Override
                    public void onInstalled() {
                        Log.i("test","------onInstalled-----");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"安装完成",Toast.LENGTH_SHORT).show();
                                tv_down_status.setText("安装完成");

                            }
                        });

                    }
                });
        DownloadCore.getInstance().start(info, new DownloadCore.DownloadDuplicateListener() {
            @Override
            public void onDuplicate() {
                Toast.makeText(MainActivity.this,"请勿重复下载",Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void testDownloadManagerUtil(View view) {
        DownLoadManagerUtil downLoadManagerUtil2 = new DownLoadManagerUtil(this);
        downLoadManagerUtil2.setOnProgressListener(new DownLoadManagerUtil.OnProgressListener() {
            @Override
            public void onProgress(float fraction) {
                Log.e("test","下载进度：" + (int)fraction+"%");

                //判断是否真的下载完成进行安装了，以及是否注册绑定过服务
                if(fraction== DownLoadManagerUtil.UNBIND_SERVICE){
                    Log.e("test","下载完成");
                }
            }
        });
//        downLoadManagerUtil2.downloadAPK(MainActivity.url,System.currentTimeMillis()+"测试安装包.apk");
        downLoadManagerUtil2.downloadAPK(MainActivity.url,"abc.apk");
    }
}
