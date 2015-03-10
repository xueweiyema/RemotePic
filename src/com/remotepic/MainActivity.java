package com.remotepic;

import java.io.File;
import java.io.IOException;

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

import com.remotepic.handler.MainActivityHanlder;
import com.remotepic.service.LocalService;

import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {
	private Intent serviceIntent;
	private LocalService myService = null;

	private String str = "Hello";

	private boolean mIsBound = false;
	private MainActivityHanlder handler = new MainActivityHanlder(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		System.out.println("MainActivity.onCreate the key is " + (savedInstanceState == null ? "空" : savedInstanceState.getString("key")));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("MainActivity.onSaveInstanceState()");
		str = "Hello World";
		outState.putString("key", str);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		System.out.println("MainActivity.onSaveInstanceState the key is " + savedInstanceState == null ? "空" : savedInstanceState.getString("key"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return str;
	}

	private void startMyService() {
		serviceIntent = new Intent(MainActivity.this, LocalService.class);
		startService(serviceIntent);
	}

	public void click(View v) {
		int key = v.getId();
		switch (key) {
		
		case R.id.button1:
			startMyService();
			doBindService();
			break;
			
		case R.id.button2:
			Intent stopIntent = new Intent(this, LocalService.class);
			stopService(stopIntent);
			doUnbindService();
			break;
			
		case R.id.button3:
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						upLoadByHttpClient4();
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).start();
			break;
			
		case R.id.button4:
			this.finish();
			break;
		default:
			break;
		}

	}

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(serviceConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	@Override
	protected void onStart() {
		super.onStart();
		// bindService(serviceIntent, serviceConnection,
		// Context.BIND_AUTO_CREATE);
	}

	ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			myService = ((LocalService.LocalBinder) service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			myService = null;
		}

	};

	/**
	 * upLoadByAsyncHttpClient:由HttpClient4上传
	 * 
	 * @return void
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws
	 * @since CodingExample　Ver 1.1
	 */
	private void upLoadByHttpClient4() throws ClientProtocolException, IOException {
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
		Message msg = new Message();
		msg.what = MainActivityHanlder.TOAST;

		handler.sendMessage(msg);
	}
}
