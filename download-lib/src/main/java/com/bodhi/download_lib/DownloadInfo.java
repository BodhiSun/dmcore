package com.bodhi.download_lib;

import com.bodhi.http.HttpCore;
import com.bodhi.http.exception.URLNullException;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author : Sun
 * @version : 1.0
 * create time : 2018/11/21 16:56
 * desc :
 */
public class DownloadInfo {

    public static DownloadInfo build(String apkUrl){
        DownloadInfo info = new DownloadInfo(apkUrl,"");
        return info;
    }

    public static DownloadInfo build(String apkUrl,String name){
        DownloadInfo info = new DownloadInfo(apkUrl,name);
        return info;
    }

    private DownloadInfo(){
    }

    public DownloadInfo(String apkUrl,String name){
        this.apkUrl=apkUrl;
        this.name=name;
    }

    private String apkUrl;
    private String name;
    private List<String> startReports = new ArrayList<>();
    private List<String> completeReports = new ArrayList<>();
    private List<String> installReports = new ArrayList<>();
    private boolean autoInstall = false;
    private boolean singleMode = false;

    private long downloadID;
    private boolean isDownloadComplete = false;
    private String  packageName;

    private DownloadListener downloadListener;

    private Timer timer;
    private TimerTask timerTask;

    public DownloadInfo startReport(String... reports){
        for (String report : reports) {
            startReports.add(report);
        }

        return this;
    }

    public DownloadInfo completeReport(String... reports){
        for (String report : reports) {
            completeReports.add(report);
        }

        return this;
    }

    public DownloadInfo installReport(String... reports){
        for (String report : reports) {
            installReports.add(report);
        }

        return this;
    }

    public DownloadInfo autoInstall(){
        this.autoInstall = true;
        return this;
    }

    public DownloadInfo singleMode(){
        singleMode = true;
        return this;
    }

    public DownloadInfo listener(DownloadListener listener){
        this.downloadListener = listener;
        return this;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public String getPackageName() {
        return packageName == null ? "" : packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getApkUrl() {
        return apkUrl == null ? "" : apkUrl;
    }

    public long getDownloadID() {
        return downloadID;
    }

    public boolean isDownloadComplete() {
        return isDownloadComplete;
    }

    public boolean isSingleMode() {
        return singleMode;
    }

    public boolean isAutoInstall() {
        return autoInstall;
    }

    public void downloadStart(long downloadID){
        this.downloadID=downloadID;

        if (downloadListener!=null) {
            downloadListener.onStart();
        }

        for (String installReport : startReports) {
            //上报开始下载
            try {
                HttpCore.getInstance().get(installReport);
            } catch (URLNullException e) {
                e.printStackTrace();
            }
        }

        startTimer();
    }

    public void onProgress(int progress, long currentSize, long totalSize){
        if (downloadListener!=null) {
            downloadListener.onProgress(progress,currentSize,totalSize);
        }
    }

    public void fail(){
        if (downloadListener!=null) {
            downloadListener.onFail();
        }

        cancelTimer();
    }

    public void downloadComplete(){
        isDownloadComplete = true;

        if (downloadListener!=null) {
            downloadListener.onComplete();
        }

        for (String installReport : completeReports) {
            //上报下载完成
            try {
                HttpCore.getInstance().get(installReport);
            } catch (URLNullException e) {
                e.printStackTrace();
            }
        }

        cancelTimer();

        if (autoInstall) {
            DownloadCore.getInstance().install(this);
        }
    }

    public void installStart(){

    }

    public void installComplete(){
        if (downloadListener!=null) {
            downloadListener.onInstalled();
        }

        for (String installReport : installReports) {
            //上报安装完成
            try {
                HttpCore.getInstance().get(installReport);
            } catch (URLNullException e) {
                e.printStackTrace();
            }
        }
    }

    private DownloadInfo self(){
        return this;
    }

    public void startTimer(){
        cancelTimer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                DownloadCore.getInstance().updateProgress(self());
            }
        };

        timer = new Timer();
        timer.schedule(timerTask,1000,1000);
    }

    public void cancelTimer(){
        if (timerTask!=null) {
            timerTask.cancel();
        }
        timerTask=null;

        if (timer!=null) {
            timer.cancel();
        }
        timer=null;

        System.gc();
    }

}
