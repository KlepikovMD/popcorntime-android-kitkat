package dp.ws.time4popcorn.ui;

import org.videolan.vlc.VLCApplication;

import dp.ws.time4popcorn.StorageHelper;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class WebInterface {
	private HostActivity mContext;

	public WebInterface(HostActivity context) {
		this.mContext = context;
	}
	
	@JavascriptInterface
	public void preloadPeers(String TORRENT_URL) {
		Log.d("tag", "preloadPeers: " + TORRENT_URL);
	}

	@JavascriptInterface
	public String getApplicationRootPath() {
		return mContext.getApplicationInfo().dataDir;
	}

	@JavascriptInterface
	public String getTempFolderPath() {
		return ((VLCApplication) VLCApplication.getAppContext()).tempFolder.getPath();
	}
	
	@JavascriptInterface
	public void clearTmpFolder() {
		mContext.clearTmpFolder();
	}

	@JavascriptInterface
	public void clearTempFolder() {
		StorageHelper.clearFolder(((VLCApplication) VLCApplication.getAppContext()).tempFolder);
	}

	@JavascriptInterface
	public void winClose() {
		mContext.finish();
	}

	@JavascriptInterface
	public void onBackPressed() {
		mContext.jsOnBackPressed();
	}

	@JavascriptInterface
	public String downloadTorrent(String torrentUrl, String fileName, String subtitlesUrl) {
		percentage = 0;
		return mContext.downloadTorrent(torrentUrl, fileName, subtitlesUrl);
	}

	@JavascriptInterface
	public void dropTorrent() {
		mContext.dropTorrent();
	}

	private int percentage = 0;
	
	@JavascriptInterface
	public String getTorrentInfo() {

		double speed = 0;
		String peers = "\"0\"";
		String eta = "\"\"";
		int initPcnt = 0;
		Log.d("tag", "getTorrentInfo...");
		if (mContext.isReady()) {
			percentage = 100;
		}

		String infoJson = "{" 
				+ "\"percentage\":" + percentage + "," 
				+ "\"speed\":" + String.format("%.2f", speed) + ","
				+ "\"peers\":" + peers + ","
				+ "\"eta\":" + eta + "," 
				+ "\"initpercentage\":" + initPcnt 
				+ "}";

		// Log.d("tag", infoJson);

		return infoJson;
	}

	@JavascriptInterface
	public String playVideo() {
		return mContext.playVideo();
	}

	@JavascriptInterface
	public void urlRequest(String url, String callbackMethod) {
		mContext.urlRequest(url, callbackMethod);
	}

	@JavascriptInterface
	public void log(String msg) {
		Log.d("tag", "Log js: " + msg);
	}
}