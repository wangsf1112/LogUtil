package com.wsf.logutil.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class LogUtil {
    private static final String TAG = "LogUtil";
    private static boolean debuggable = true; //是否输出Log信息
    private static boolean saveToLocal = false; //是否保存到本地文件
    private static boolean encrypted = false; //是否加密
    private static final int saveLevel = Log.DEBUG; //Write file level
    private static final SparseArray<String> logMap = new SparseArray<>();
    private static String DIR_LOGS;
    private static File currFile;
    private static Context appContext;
    private static final long FILE_MAX_SIZE = 1024 * 1024; //1MB
    private static final long REMAIN_MAX_NUM = 50; //最大保留文件个数
    private static final String LOG_DATA_PATTERN = "MM-dd HH-mm-ss.SSS";
    private static final String FILE_DATA_PATTERN = "yyyy-MM-dd-HH-mm-ss";

    static {
        logMap.put(Log.VERBOSE, " V/");
        logMap.put(Log.DEBUG, " D/");
        logMap.put(Log.INFO, " I/");
        logMap.put(Log.WARN, " W/");
        logMap.put(Log.ERROR, " E/");
    }

    /**
     * 初始化（存储路径、清理过多的历史文件）
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        File rootFile = context.getExternalFilesDir("Logs");
        if (rootFile != null) {
            DIR_LOGS = rootFile.getAbsolutePath();
        }
        clearLogs();
        if (rootFile != null && !rootFile.exists()) {
            rootFile.mkdirs();
        }
    }

    /**
     * 设置是否输出Log信息
     */
    public static void setDebuggable(boolean debuggable) {
        LogUtil.debuggable = debuggable;
    }

    public static boolean isDebuggable() {
        return debuggable;
    }

    /**
     * 设置是否保存Log信息到本地文件
     */
    public static void setSaveToLocal(boolean saveToLocal) {
        LogUtil.saveToLocal = saveToLocal;
    }

    public static boolean isSaveToLocal() {
        return saveToLocal;
    }

    public static boolean isEncrypted() {
        return encrypted;
    }

    public static void setEncrypted(boolean isEncrypted) {
        LogUtil.encrypted = isEncrypted;
    }

    public static void v(String tag, String msg) {
        trace(Log.VERBOSE, tag, msg);
    }

    public static void d(String tag, String msg) {
        trace(Log.DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        trace(Log.INFO, tag, msg);
    }

    public static void w(String tag, String msg) {
        trace(Log.WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        trace(Log.ERROR, tag, msg);
    }

    private static void trace(int type, String tag, String msg) {
        if (debuggable) {
            switch (type) {
                case Log.VERBOSE:
                    Log.v(tag, msg);
                    break;
                case Log.DEBUG:
                    Log.d(tag, msg);
                    break;
                case Log.INFO:
                    Log.i(tag, msg);
                    break;
                case Log.WARN:
                    Log.w(tag, msg);
                    break;
                case Log.ERROR:
                    Log.e(tag, msg);
                    break;
            }
        }

        if (saveToLocal && type >= saveLevel) {
            writeLog(type, tag, msg);
        }
    }

    private static void writeLog(int type, String tag, String msg) {
        if (DIR_LOGS == null) {
            Log.e(TAG, "writeLog DIR_LOGS is null");
            return;
        }

        msg = getDataFormat(LOG_DATA_PATTERN)
                + logMap.get(type) + tag + ": "
                + msg + "\n";

        if (currFile == null || currFile.length() > FILE_MAX_SIZE) {
            currFile = new File(DIR_LOGS, getDataFormat(FILE_DATA_PATTERN) + ".log");
            msg = getFileHead(appContext) + msg;
        }

        if (encrypted) {
            msg = encode(msg);
        }
        write(currFile, msg);
    }

    private static void write(File file, String msg) {
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file, true);
                    fos.write(msg.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.flush();
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }
        }.execute();
    }

    private static void clearLogs() {
        if (DIR_LOGS == null) {
            Log.e(TAG, "clearLogs DIR_LOGS is null");
            return;
        }
        new Thread(() -> {
            File rootFile = new File(DIR_LOGS);
            if (rootFile.exists()) {
                File[] files = rootFile.listFiles((dir, name) -> name.endsWith(".log"));
                if (files == null || files.length <= REMAIN_MAX_NUM) {
                    return;
                }
                List<File> fileList = Arrays.asList(files);
                Collections.sort(fileList, (file1, file2) -> {
                    //按时间排序
                    return Long.compare(file1.lastModified(), file2.lastModified());
                });

                int totalSize = fileList.size();
                int count = 0;
                for (File file : fileList) { //删除时间较早，且超过最大保留个数的文件
                    file.delete();
                    count++;
                    if (totalSize - count <= REMAIN_MAX_NUM) {
                        break;
                    }
                }
            }
        }).start();
    }

    private static String getDataFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        return format.format(new Date());
    }

    //字符串加密（Base64）
    private static String encode(String content) {
        return Base64.encodeToString(content.getBytes(), Base64.NO_WRAP);
    }

    private static String getFileHead(Context context) {
        String versionName = "N/A";
        String versionCode = "N/A";
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            versionCode = String.valueOf(pi.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "\n******************************" +
                "\nManufacturer : " + Build.MANUFACTURER + // 设备厂商
                "\nModel        : " + Build.MODEL + // 设备型号
                "\nVersion      : " + Build.VERSION.RELEASE + // 系统版本
                "\nSDK          : " + Build.VERSION.SDK_INT + // SDK版本
                "\nVersionName  : " + versionName +
                "\nVersionCode  : " + versionCode +
                "\n******************************\n";
    }
}
