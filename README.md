# dmcore
The download manager util which contains two mode of system  download manager and custom download installation.（分别用系统自带的Download Manager 和自己实现下载安装两种方式的便捷下载器）

#### Usage:
##### 基于系统DownloadManager实现的下载器：
```java
DownLoadManagerUtil downLoadManagerUtil = new DownLoadManagerUtil(context);
downLoadManagerUtil.setOnProgressListener(new DownLoadManagerUtil.OnProgressListener() {
    @Override
    public void onProgress(float fraction) {
        Log.e("dm","下载进度：" + (int)fraction+"%");

        //判断是否真的下载完成进行安装了，以及是否注册绑定过服务
        if(fraction== DownLoadManagerUtil.UNBIND_SERVICE){
            Log.e("dm","下载完成");
        }
    }
});
```

##### 自己封装实现的下载器：
```java
DownloadCore.getInstance().init(context);
DownloadInfo info = DownloadInfo.build(apkUrl).autoInstall().singleMode();
info.listener(new DownloadListener() {
    @Override
    public void onStart() {
        Log.i("dm","------onStart-----");
    }

    @Override
    public void onProgress(final int progress, long currentSize, long totalSize) {
        Log.i("dm","------onProgress---progress:"+progress+"%");
    }

    @Override
    public void onComplete() {
        Log.i("dm","------onComplete-----");
    }

    @Override
    public void onFail() {
        Log.i("dm","------onFail-----");
    }

    @Override
    public void onInstalled() {
        Log.i("dm","------onInstalled-----");
    }
});

DownloadCore.getInstance().start(info, new DownloadCore.DownloadDuplicateListener() {
    @Override
    public void onDuplicate() {
        Toast.makeText(MainActivity.this,"请勿重复下载",Toast.LENGTH_SHORT).show();
    }
});
```
