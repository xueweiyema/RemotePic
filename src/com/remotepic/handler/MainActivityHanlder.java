package com.remotepic.handler;

import com.remotepic.MainActivity;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class MainActivityHanlder extends Handler {
	public static final int TOAST = 0;
	private MainActivity context;

	public MainActivityHanlder(MainActivity context) {
		this.context = context;
	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		switch (msg.what) {
		case TOAST:
			Toast.makeText(context, "上传完毕", Toast.LENGTH_SHORT).show();
			break;

		default:
			break;
		}
	}
}
