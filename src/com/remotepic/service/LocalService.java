package com.remotepic.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.remotepic.MainActivity;
import com.remotepic.PhotoHandler;
import com.remotepic.R;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

public class LocalService extends Service {
	private AlarmManager am = null;
	private Camera camera;

	private NotificationManager mNM;

	private int NOTIFICATION = R.string.local_service_started;
	WakeLock wakeLock = null;

	private void acquireWakeLock() {
		if (null == wakeLock) {
			PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
			if (null != wakeLock) {
				wakeLock.acquire();
			}
		}
	}

	// 释放设备电源锁
	private void releaseWakeLock() {
		if (null != wakeLock) {
			wakeLock.release();
			wakeLock = null;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		acquireWakeLock();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
		init();
	}

	private void init() {
		am = (AlarmManager) getSystemService(ALARM_SERVICE);

		// 注册广播
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.vegetables_source.alarm");
		registerReceiver(alarmReceiver, filter);

		Intent intent = new Intent();
		intent.setAction("com.vegetables_source.alarm");
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi);// 马上开始，每1分钟触发一次
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("LocalService", "Received start id " + startId + ": " + intent);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		releaseWakeLock();
		Log.d("Destroy==>", "onDestroy() executed");
		mNM.cancel(NOTIFICATION);

		cancelAlertManager();

		if (camera != null) {
			camera.release();
			camera = null;
		}

		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		CharSequence text = getText(R.string.local_service_started);
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIndent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new Notification.Builder(this).setContentTitle("This is ContentTitle")// 设置下拉列表里的标题
				.setContentText(text).setContentIntent(contentIndent).setSmallIcon(R.drawable.ic_launcher)// 设置状态栏里面的图标（小图标）.setLargeIcon(BitmapFactory.decodeResource(res,
																											// R.drawable.i5))//下拉下拉列表里面的图标（大图标）
																											// 　　　　　　　.setTicker("this is bitch!")
																											// //设置状态栏的显示的信息
				.setWhen(System.currentTimeMillis())// 设置时间发生时间
				.setAutoCancel(true)// 设置可以清除
				.build();

		notificationManager.notify(NOTIFICATION, notification);
	}

	private void cancelAlertManager() {
		Intent intent = new Intent();
		intent.setAction("com.vegetables_source.alarm");
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
		am.cancel(pi);

		// 注销广播
		unregisterReceiver(alarmReceiver);
	}

	BroadcastReceiver alarmReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			SimpleDateFormat format = new SimpleDateFormat("HH", Locale.CHINA);
			String realTime = format.format(new java.util.Date());
			Log.i("realTime=", realTime);
			if (Integer.parseInt(realTime) > 12 && Integer.parseInt(realTime) < 21) {
				closeCamera();
				camera = openFacingBackCamera();
				if ("com.vegetables_source.alarm".equals(intent.getAction())) {

					if (camera != null) {
						SurfaceView dummy = new SurfaceView(getBaseContext());
						try {
							camera.setPreviewDisplay(dummy.getHolder());
						} catch (IOException e) {
							e.printStackTrace();
						}

						final Camera.Parameters parameters = camera.getParameters();
						parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
						camera.setParameters(parameters);
						camera.startPreview();
						// camera.takePicture(null, null, new
						// PhotoHandler(getApplicationContext()));
						camera.autoFocus(new AutoFocusCallback() {
							@Override
							public void onAutoFocus(boolean success, Camera camera) {
								// 从Camera捕获图片
								camera.takePicture(null, null, new PhotoHandler(getApplicationContext()));
								// 上传图片
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											upLoadByHttpClient4();
										} catch (ClientProtocolException e) {
											e.printStackTrace();
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								}).start();
							}
						});
					}

				}
			}
		}
	};

	// 开启摄像头
	private Camera openFacingBackCamera() {
		Camera cam = null;
		CameraInfo cameraInfo = new CameraInfo();
		;
		for (int camIdx = 0, cameraCount = Camera.getNumberOfCameras(); camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				try {
					cam = Camera.open(camIdx);
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}
		return cam;
	}

	// 关闭摄像头
	private void closeCamera() {
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		public LocalService getService() {
			return LocalService.this;
		}

	}

	private void upLoadByHttpClient4() throws ClientProtocolException, IOException {
		Log.i("LocalService", "upload start!!");
		HttpClient httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		HttpPost httppost = new HttpPost("http://zhbasketball.sinaapp.com/index.php/Upload/upload");
		Log.i("picurl", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/ServiceCamera/zhbb.jpg");
		File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/ServiceCamera/zhbb.jpg");
		MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
		FileBody fileBody = new FileBody(file);
		multipartEntity.addPart("filename", fileBody);
		httppost.setEntity(multipartEntity.build());
		HttpResponse response = httpclient.execute(httppost);
		HttpEntity resEntity = response.getEntity();
		if (resEntity != null) {
			Log.i("MainActivity uploadByHttpClient4", EntityUtils.toString(resEntity));
		}
		if (resEntity != null) {
			resEntity.consumeContent();
		}
		httpclient.getConnectionManager().shutdown();
	}
}
