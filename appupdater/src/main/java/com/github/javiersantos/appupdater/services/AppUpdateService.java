package com.github.javiersantos.appupdater.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.github.javiersantos.appupdater.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by junhoe on 16/03/2018.
 */

public class AppUpdateService extends Service {

    public static final String ACTION_START = "AppUpdater.AppUpdateService.action.start";
    public static final String ACTION_RESTART = "AppUpdater.AppUpdateService.action.restart";
    private static final String TAG = "AppUpdateService";
    private static final String DOWNLOAD_NOTIFICATION_CHANNEL_ID = "App Download Notification";
    private static final String INSTALL_NOTIFICATION_CHANNEL_ID = "App Install Notification";
    private static final String FAILURE_NOTIFICATION_CHANNEL_ID = "App Download Failure Notification";
    private static final String PARAM_VERSION = "version";
    private static final String FILE_PATH_PREFIX = "file://";
    private static final int NOTIFICATION_DOWNLOAD_ID = 0;
    private static final int NOTIFICATION_INSTALL_ID = 1;
    private static final int NOTIFICATION_FAILURE_ID = 2;
    private static final int BUFFER_SIZE = 2048;
    public static String INTENT_EXTRA_APP_ID = "appId";
    public static String INTENT_EXTRA_FILE_URL = "fileURL";
    public static String INTENT_EXTRA_ICON_RES_ID = "iconResId";
    private NotificationManager notificationManager;
    private OkHttpClient client;
    private String fileUrl;
    private int iconResId = R.drawable.ic_stat_name;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Objects.equals(intent.getAction(), ACTION_START)) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel downloadChannel = new NotificationChannel(DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                        "App Download Channel",
                        NotificationManager.IMPORTANCE_DEFAULT);

                NotificationChannel installChannel = new NotificationChannel(INSTALL_NOTIFICATION_CHANNEL_ID,
                        "App Install Channel",
                        NotificationManager.IMPORTANCE_DEFAULT);

                NotificationChannel failureChannel = new NotificationChannel(FAILURE_NOTIFICATION_CHANNEL_ID,
                        "App Download Failure Channel",
                        NotificationManager.IMPORTANCE_DEFAULT);

                notificationManager.createNotificationChannel(downloadChannel);
                notificationManager.createNotificationChannel(installChannel);
                notificationManager.createNotificationChannel(failureChannel);
            }

            client = new OkHttpClient();
            fileUrl = intent.getStringExtra(INTENT_EXTRA_FILE_URL);
            iconResId = intent.getIntExtra(INTENT_EXTRA_ICON_RES_ID, R.drawable.ic_stat_name);

            final String appId = intent.getStringExtra(INTENT_EXTRA_APP_ID);

            final Handler mainHandler = new Handler(getMainLooper());
            Request request = new Request.Builder().url(fileUrl).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), R.string.appupdater_download_failure_description_toast, Toast.LENGTH_SHORT).show();
                            NotificationCompat.Builder failureNotiBuilder = getFailureNotificationBuilder();
                            notificationManager.notify(TAG, NOTIFICATION_FAILURE_ID, failureNotiBuilder.build());
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), R.string.appupdater_download_description_start_toast, Toast.LENGTH_SHORT).show();
                        }
                    });
                    NotificationCompat.Builder downloadNotificationBuilder = getDownloadNotificationBuilder();
                    notificationManager.notify(TAG, NOTIFICATION_DOWNLOAD_ID, downloadNotificationBuilder.build());
                    File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    String appName = response.request().url().queryParameter(PARAM_VERSION) + ".apk";
                    String filePath = String.format("%s/%s", directory.getAbsolutePath(), appName);
                    File fileToBeDownloaded = new File(filePath);
                    try {
                        downloadFile(response, fileToBeDownloaded, downloadNotificationBuilder);
                        notificationManager.cancel(TAG, NOTIFICATION_DOWNLOAD_ID);
                        NotificationCompat.Builder installNotificationBuilder = getInstallNotificationBuilder(appId, fileToBeDownloaded);
                        notificationManager.notify(TAG, NOTIFICATION_INSTALL_ID, installNotificationBuilder.build());
                    } catch (IOException e) {
                        Log.i(TAG, "Exception : " + e.getMessage());
                        notificationManager.cancel(TAG, NOTIFICATION_DOWNLOAD_ID);
                        NotificationCompat.Builder failureNotificationBuilder = getFailureNotificationBuilder();
                        notificationManager.notify(TAG, NOTIFICATION_FAILURE_ID, failureNotificationBuilder.build());
                    }
                }
            });
        }
        return START_NOT_STICKY;
    }

    private NotificationCompat.Builder getDownloadNotificationBuilder() {
        return new NotificationCompat.Builder(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getApplicationContext().getResources().getString(R.string.appupdater_download_notification_title))
                .setContentText(getApplicationContext().getResources().getString(R.string.appupdater_download_notification_content))
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setProgress(100, 0, true);
    }

    private NotificationCompat.Builder getInstallNotificationBuilder(String appId, File file) {
        Context context = getApplicationContext();
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri data = FileProvider.getUriForFile(context,  appId + ".provider", file);
            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setData(data)
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        PendingIntent pending = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, INSTALL_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setAutoCancel(true)
                .setContentTitle(context.getResources().getString(R.string.appupdater_install_notification_title))
                .setContentText(context.getResources().getString(R.string.appupdater_install_notification_content))
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setContentIntent(pending);
    }

    private NotificationCompat.Builder getFailureNotificationBuilder() {
        Context context = getApplicationContext();
        Intent intent = new Intent(this, AppUpdateService.class).setAction(ACTION_START)
                .putExtra(INTENT_EXTRA_FILE_URL, fileUrl)
                .putExtra(INTENT_EXTRA_ICON_RES_ID, iconResId);

        PendingIntent pending = PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        return new NotificationCompat.Builder(this, FAILURE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setAutoCancel(true)
                .setContentTitle(context.getResources().getString(R.string.appupdater_download_failure_notification_title))
                .setContentText(context.getResources().getString(R.string.appupdater_download_failure_notification_content))
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setContentIntent(pending);
    }

    private void downloadFile(Response response, File fileToBeDownloaded, NotificationCompat.Builder builder) throws IOException {
        fileToBeDownloaded.createNewFile();
        ResponseBody body = response.body();
        InputStream is = body.byteStream();
        OutputStream os = new FileOutputStream(fileToBeDownloaded);
        long contentLength = body.contentLength();
        long totalLength = 0;
        byte[] data = new byte[BUFFER_SIZE];
        int count;
        long prevTimeStamp = System.currentTimeMillis();
        while ((count = is.read(data)) != -1) {
            totalLength += count;
            os.write(data, 0, count);
            long currTimeStamp = System.currentTimeMillis();
            if (currTimeStamp >= prevTimeStamp + 200) {
                prevTimeStamp = currTimeStamp;
                int currProgress = (int) (100 * totalLength / contentLength);
                builder.setProgress(100, currProgress, false);
                notificationManager.notify(TAG, NOTIFICATION_DOWNLOAD_ID, builder.build());
            }
        }

        os.flush();
        os.close();
        is.close();
    }

}
