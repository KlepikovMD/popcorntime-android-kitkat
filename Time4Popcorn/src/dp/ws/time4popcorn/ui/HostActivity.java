package dp.ws.time4popcorn.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;
import dp.ws.popcorntime.R;
import dp.ws.time4popcorn.StorageHelper;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.mozilla.universalchardet.UniversalDetector;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.vlc.Util;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.gui.CompatErrorActivity;
import org.videolan.vlc.gui.video.VideoPlayerActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HostActivity extends Activity {
	private final String JS_INTERFACE_NAME = "android_t4p";
	private final int MIN_SPLASH_TIME = 2000;

	public WebView mWebView;
	private ImageView mSplashImage;

	private String videoPath;
	private String fileName;
	private String subtitlePath;
	private String subtitleEncoding;

	private File tempFolder;

	private DownloadTask torrentDownloadTask = null;
	private SubtitleTask subtitleTask = null;

	private boolean mInitialLoad = true;
	private long mInitialLoadStart = 0;
	private boolean doubleBackToExitPressedOnce = false;
	
	private boolean isSubtitleComplete = false;
	private boolean isTorrentComplete = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		tempFolder = ((VLCApplication) VLCApplication.getAppContext()).tempFolder;

		if (!LibVlcUtil.hasCompatibleCPU(this)) {
			Intent i = new Intent(this, CompatErrorActivity.class);
			startActivity(i);
			finish();
			super.onCreate(savedInstanceState);
			return;
		}

		try {
			// Start LibVLC
			Util.getLibVlcInstance();
		} catch (LibVlcException e) {
			e.printStackTrace();
			Intent i = new Intent(this, CompatErrorActivity.class);
			i.putExtra("runtimeError", true);
			i.putExtra("message", "LibVLC failed to initialize (LibVlcException)");
			startActivity(i);
			finish();
			super.onCreate(savedInstanceState);
			return;
		}

		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_hostui);

		mSplashImage = (ImageView) findViewById(R.id.imgSplash);

		mWebView = (WebView) findViewById(R.id.webView);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setDomStorageEnabled(true);

		mWebView.addJavascriptInterface(new WebInterface(this), JS_INTERFACE_NAME);

		mWebView.requestFocus(View.FOCUS_DOWN);

		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				if (mInitialLoad) {
					// remove splash

					long now = new Date().getTime();
					long leftToWait = MIN_SPLASH_TIME - (now - mInitialLoadStart);

					if (leftToWait < 0) {
						clearSplash();
					} else {
						new Handler().postDelayed(new Runnable() {

							@Override
							public void run() {
								clearSplash();
							}
						}, leftToWait);
					}

				}
			}
		});

		String guiUrl = "http://mobile_wv.time4popcorn.eu";

		if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
			guiUrl = "http://mobile_ch.time4popcorn.eu";
		}

		mInitialLoadStart = new Date().getTime();
		mWebView.loadUrl(guiUrl);
	}

	private void clearSplash() {
		mWebView.setVisibility(View.VISIBLE);
		mSplashImage.setVisibility(View.GONE);
		mInitialLoad = false;
	}

	@Override
	public void onBackPressed() {
		if (torrentDownloadTask != null) {
			torrentDownloadTask.cancel(true);
			torrentDownloadTask = null;
		}
		if (subtitleTask != null) {
			subtitleTask.cancel(true);
			subtitleTask = null;
		}
		mWebView.loadUrl("javascript:ui.events.nativeBack()");
	}

	public void clearTmpFolder() {
		StorageHelper.clearFolder(((VLCApplication) VLCApplication.getAppContext()).tempFolder);
		Toast.makeText(this, "Cache folder was cleared", Toast.LENGTH_SHORT).show();		
	}
	
	public void jsOnBackPressed() {
		if (doubleBackToExitPressedOnce) {
			finish();
			System.exit(0);
		}

		this.doubleBackToExitPressedOnce = true;
		Toast.makeText(this, "Click BACK again to exit", Toast.LENGTH_SHORT).show();

		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				doubleBackToExitPressedOnce = false;
			}
		}, 2000);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		dropTorrent();
	}

	public void dropTorrent() {
		new DropTask().execute();
	}

	public boolean isReady() {
		if (isSubtitleComplete && isTorrentComplete) {
			return true;
		}

		return false;
	}
		
	public String downloadTorrent(String torrentUrl, String fileName, String subtitlesUrl) {

		Log.d("tag", "torrentUrl: " + torrentUrl);
		Log.d("tag", "fileName: " + fileName);
		Log.d("tag", "subtitlesUrl: " + subtitlesUrl);

		videoPath = "";
		this.fileName = fileName;

		if (subtitlesUrl == null || "".equals(subtitlesUrl)) {
			isSubtitleComplete = true;
			subtitlePath = "";
		} else {
			isSubtitleComplete = false;
			subtitleTask = new SubtitleTask();
			subtitleTask.execute(subtitlesUrl);
		}

		isTorrentComplete = false;
		torrentDownloadTask = new DownloadTask();
		torrentDownloadTask.execute(torrentUrl, fileName);

		return Integer.toString(torrentUrl.hashCode());
	}

	public String playVideo() {
		if (!"".equals(videoPath)) {
			Intent i = new Intent(this, VideoPlayerActivity.class);
			i.setAction(Intent.ACTION_VIEW);
			i.setData(Uri.parse("file://" + videoPath));

			if (!"".equals(subtitlePath)) {
				i.putExtra(VideoPlayerActivity.SUBTITLE_EXTARA_KEY, subtitlePath);
				i.putExtra(VideoPlayerActivity.SUBTITLE_ENCODING_EXTARA_KEY, subtitleEncoding);
			}
			
			if (!"".equals(fileName)) {
				i.putExtra(VideoPlayerActivity.FILENAME_EXTARA_KEY, fileName);
			}
			
			startActivity(i);
		}
		return videoPath;
	}

	public void urlRequest(String url, String callbackMethod) {
		// Log.d("tag", url + " / " + callbackMethod);
		new RequestTask().execute(url, callbackMethod);
	}

	private class RequestTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String callback = "";
			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost(params[0]);
				HttpResponse httpResponse = httpClient.execute(httpPost);

				BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
				String line = "";
				StringBuffer sb = new StringBuffer();
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				reader.close();

				callback = "javascript:" + params[1] + "(" + sb.toString() + ")";
			} catch (Exception e) {
				Log.e("tag", e.getMessage());
				e.printStackTrace();
			}

			return callback;
		}

		@Override
		protected void onPostExecute(String result) {
			// Log.d("tag", result);
			if (!"".equals(result)) {
				mWebView.loadUrl(result);
			}
		}
	}

	private class SubtitleTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String subtitleUrl = params[0];

			if (subtitleUrl == null || "".equals(subtitleUrl)) {
				isSubtitleComplete = true;
				return null;
			}
			while (!isSubtitleComplete) {
				try {
					URLConnection connection = new URL(subtitleUrl).openConnection();
					connection.connect();

					ZipInputStream zis = new ZipInputStream(new BufferedInputStream(connection.getInputStream()));

					ZipEntry zi = zis.getNextEntry();
					subtitlePath = "";
					while (zi != null) {
						String[] part = zi.getName().split("\\.");
						if (part.length == 0) {
							zi = zis.getNextEntry();
							continue;
						}

						String extension = part[part.length - 1];
						if ("srt".equals(extension)) {
							subtitlePath = tempFolder.getPath() + "/subtitle_temp.srt"; // zi.getName();

							UniversalDetector detector = new UniversalDetector(null);

							byte[] buffer = new byte[1024];
							int count = 0;

							ByteArrayOutputStream baos = new ByteArrayOutputStream();

							while ((count = zis.read(buffer)) > 0) {
								if (!detector.isDone()) {
									detector.handleData(buffer, 0, count);
								}
								baos.write(buffer, 0, count);
							}
							detector.dataEnd();

							subtitleEncoding = detector.getDetectedCharset();
							if (subtitleEncoding == null || "".equals(subtitleEncoding)) {
								subtitleEncoding = "UTF-8";
							}
//							 Log.d("tag", "Subtitle enc: " + subtitleEncoding);

							detector.reset();
							
							String data = new String(baos.toByteArray(), Charset.forName(subtitleEncoding));
							FileUtils.write(new File(subtitlePath), data, Charset.forName("UTF-8"));
//														
							break;
						}
						zi = zis.getNextEntry();
					}
					zis.closeEntry();
					zis.close();
					isSubtitleComplete = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (isReady()) {
				playVideo();
				mWebView.loadUrl("javascript:ui.loading_wrapper.hide()");
			}
		}
	}

	private class DownloadTask extends AsyncTask<String, Void, Integer> {
		private final int COMPLETE = 101;

		@Override
		protected Integer doInBackground(String... params) {
			while (!isTorrentComplete) {
				try {

					File torrentTempFile = new File(tempFolder.getPath() + "/metadata_temp.torrent");

					URLConnection connection = new URL(params[0]).openConnection();
					connection.connect();

					InputStream is = connection.getInputStream();
					OutputStream os = new FileOutputStream(torrentTempFile);

					byte[] data = new byte[2048];
					int read;
					while ((read = is.read(data)) != -1) {
						os.write(data, 0, read);
					}
					os.flush();

					is.close();
					os.close();

					videoPath = torrentTempFile.getPath();
					isTorrentComplete = true;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			return COMPLETE;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (isReady()) {
				mWebView.loadUrl("javascript:ui.loading_wrapper.hide()");
				playVideo();
			}
		}
	}

	private class DropTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (torrentDownloadTask != null) {
					if (AsyncTask.Status.FINISHED != torrentDownloadTask.getStatus()) {
						torrentDownloadTask.cancel(true);
					}
					torrentDownloadTask = null;
				}
				if (subtitleTask != null) {
					if (AsyncTask.Status.FINISHED != subtitleTask.getStatus()) {
						subtitleTask.cancel(true);
					}
					subtitleTask = null;
				}
				StorageHelper.clearFolder(tempFolder);
			} catch (Exception ex) {

			}
			return null;
		}
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// MenuInflater inflater = getMenuInflater();
	// inflater.inflate(R.menu.torrent_select, menu);
	// return true;
	// }
	//
	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// Intent intent;
	// switch (item.getItemId()) {
	// case R.id.torrent_settings_menu:
	// intent = new Intent(this, SetPreferenceActivity.class);
	// startActivity(intent);
	// return true;
	// default:
	// return super.onOptionsItemSelected(item);
	// }
	// }

}
