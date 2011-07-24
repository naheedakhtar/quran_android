package com.quran.labs.androidquran.common;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.service.QuranDataService;

public abstract class InternetActivity extends BaseQuranActivity {
	
	protected ProgressDialog pDialog = null;
	protected QuranDataService downloadService;
	protected AsyncTask<?, ?, ?> currentTask = null;
	protected boolean starting = true;
	protected ServiceConnection serviceConnection;
	
	public boolean isInternetOn() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null && cm.getActiveNetworkInfo() != null) 
			return cm.getActiveNetworkInfo().isConnectedOrConnecting();
		return false;
	}
	
	protected void connect() {
		if (isInternetOn())
        	onConnectionSuccess();
        else
        	onConnectionFailed();
	}
	
	protected void onConnectionSuccess() {
		
	}
	
	protected void onConnectionFailed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Unable to connect to server, make sure that your Internet connection is active. Retry ?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.dismiss();
		        	   connect();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.dismiss();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	protected void startDownloadService(Intent intent) {
    	starting = true;    	
    	initServiceConnection();
    	if (!QuranDataService.isRunning)
    		startService(intent);
    	
    	bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    	showProgressDialog();
    }
	
	protected void downloadTranslation(String url, String fileName) {
		Intent intent = new Intent(this, QuranDataService.class);
    	intent.putExtra(QuranDataService.DWONLOAD_TYPE_KEY, QuranDataService.DOWNLOAD_TRANSLATION);
    	intent.putExtra(QuranDataService.URL_KEY, url);
    	intent.putExtra(QuranDataService.FILE_NAME_KEY, fileName);
    	startDownloadService(intent);
	}
	
	protected void downloadQuranImages() {
		Intent intent = new Intent(this, QuranDataService.class);
    	intent.putExtra(QuranDataService.DWONLOAD_TYPE_KEY, QuranDataService.DOWNLOAD_QURAN_IMAGES);
    	startDownloadService(intent);
	}
	
	protected void downloadSura(int readerId, int sura) {
		Intent intent = new Intent(this, QuranDataService.class);
		intent.putExtra(QuranDataService.DWONLOAD_TYPE_KEY, QuranDataService.DOWNLOAD_SURA_AUDIO);
		intent.putExtra(QuranDataService.SOURA_KEY, sura);
		intent.putExtra(QuranDataService.READER_KEY, readerId);
		startDownloadService(intent);
	}
	
	private void initServiceConnection() {
	    serviceConnection = new ServiceConnection() {
	    	public void onServiceConnected(ComponentName name, IBinder service){
	    		downloadService = ((QuranDataService.QuranDownloadBinder)service).getService();
	    		starting = false;
	    	}
	
	    	public void onServiceDisconnected(ComponentName className) {
	    		downloadService = null;
	    	}
	    };
	}
	
    class ProgressBarUpdateTask extends AsyncTask<Void, Integer, Void> {	
		@Override
		protected Void doInBackground(Void... params) {
    		while (starting || QuranDataService.isRunning){
    			try {
    				Thread.sleep(1000);
    				if ((serviceConnection != null) && (downloadService != null)){
    					int progress = downloadService.getProgress();
    					publishProgress(progress);
    				}
    			}
    			catch (InterruptedException ie){}
    		}
    		
    		return null;
    	}
    	
		@Override
    	public void onProgressUpdate(Integer...integers){
    		int progress = integers[0];
    		if (progress == -1){
    			pDialog.setTitle(R.string.extracting_title);
				pDialog.setMessage(getString(R.string.extracting_message));
				pDialog.setProgress(0);
    		}
    		else pDialog.setProgress(progress);
    	}
    	
    	@Override
    	public void onPostExecute(Void val){
    		pDialog.dismiss();
			pDialog = null;
			currentTask = null;
			onFinishDownload();
    	}
    }
    
    protected void onFinishDownload() {
    	
    }
    
	private void showProgressDialog(){
    	pDialog = new ProgressDialog(this);
    	pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    	pDialog.setTitle(R.string.downloading_title);
    	pDialog.setCancelable(false);
    	pDialog.setMessage(getString(R.string.downloading_message));
    	pDialog.show();
    	
    	currentTask = new ProgressBarUpdateTask().execute();
    }

}
