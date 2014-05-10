package dp.ws.time4popcorn;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

public class StorageHelper {

	private static final String TEMP_FOLDER_NAME = "time4popcorn";

	public final static long SIZE_KB = 1024L;
	public final static long SIZE_MB = SIZE_KB * SIZE_KB;
	public final static long SIZE_GB = SIZE_KB * SIZE_KB * SIZE_KB;

	public static void clearFolder(File parent) {
		if (parent.isDirectory()) {
			try {
				FileUtils.cleanDirectory(parent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static File getTempFolder(Context context) {
		String state = Environment.getExternalStorageState();
		String tempFolderPath = "";

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			tempFolderPath = Environment.getExternalStorageDirectory().getPath();
		} else {
			tempFolderPath = context.getApplicationInfo().dataDir;
		}
		tempFolderPath += "/" + TEMP_FOLDER_NAME;

		File tempFolder = new File(tempFolderPath);
		tempFolder.mkdirs();

		return tempFolder;
	}

	@SuppressWarnings("deprecation")
	public static long getAvailableSpaceInBytes(String path) {
		long availableSpace = -1L;
		try {
			StatFs stat = new StatFs(path);
			availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return availableSpace;
	}

	public static long getAvailableSpaceInKB(String path) {
		return getAvailableSpaceInBytes(path) / SIZE_KB;
	}

	public static long getAvailableSpaceInMB(String path) {
		return getAvailableSpaceInBytes(path) / SIZE_MB;
	}

	public static long getAvailableSpaceInGB(String path) {
		return getAvailableSpaceInBytes(path) / SIZE_GB;
	}
}