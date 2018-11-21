package com.bodhi.download_lib;

/**
 * @author : Sun
 * @version : 1.0
 * create time : 2018/11/21 17:19
 * desc :
 */
public interface DownloadListener {
    void onStart();
    void onProgress(int progress,long currentSize,long totalSize);
    void onComplete();
    void onFail();
    void onInstalled();
}
