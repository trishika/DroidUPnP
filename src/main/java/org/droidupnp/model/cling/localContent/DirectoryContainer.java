package org.droidupnp.model.cling.localContent;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import org.apache.tika.Tika;
import org.droidupnp.R;
import org.droidupnp.model.mediaserver.ContentDirectoryService;
import org.droidupnp.view.SettingsActivity;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.seamless.util.MimeType;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by stephan on 16.12.17.
 */

public class DirectoryContainer extends Container {

    private static final String TAG = "DirectoryContainer";
    protected Context ctx;
    protected StorageManager storageManager;
    protected File path;
    protected String baseURL;
    protected static Tika tika;

    static {
        tika = new Tika();
    }

    public DirectoryContainer(String parentID, String title, String creator, String baseURL, Context ctx,
                              File path) {

        this.setClazz(new DIDLObject.Class("object.container"));

        setId(ContentDirectoryService.DIRECTORY_PREFIX + path.getAbsolutePath());
        setParentID(parentID);
        setTitle(title);
        setCreator(creator);
        setRestricted(true);
        setSearchable(true);
        setWriteStatus(WriteStatus.NOT_WRITABLE);

        this.baseURL = baseURL;
        this.ctx = ctx;
        this.path = path;
    }

    public static String appendSlash(String path) {
        if (path.endsWith("/")) return path;
        else return path + "/";
    }

    public static boolean startsWithPath(File first, File second) {
        return appendSlash(first.getAbsolutePath()).startsWith(appendSlash(second.getAbsolutePath()));
    }

    public static List<Pair<String, File>> getRoots(Context ctx) {
        List<Pair<String, File>> roots = new ArrayList<>();
        StorageManager storageManager = ctx.getSystemService(StorageManager.class);
        List<StorageVolume> volumes = storageManager.getStorageVolumes();
        for (StorageVolume volume : volumes) {
            String description = volume.getDescription(ctx);
            File path;
            try {
                path = (File) volume.getClass().getMethod("getPathFile").invoke(volume);
            } catch (Exception e) {
                Log.e(TAG, "Could not get path of volume " + description + ": " + e);
                continue;
            }
            Log.d(TAG, "Found volume " + description + " (" + path + ") in state " + volume.getState());
            if (Environment.MEDIA_MOUNTED.equals(volume.getState()) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(
                    volume.getState())) {
                roots.add(new Pair<>(description, path));
            }
        }
        return roots;
    }

    public static boolean isValidSubpath(File path, List<Pair<String, File>> volumes) {
        for (Pair<String, File> volume : volumes)
        {
            if (startsWithPath(path, volume.second))
            {
                return true;
            }
        }
        return false;
    }

    public static String getExtension(File file) {
        String path = file.getAbsolutePath();
        int filePos = path.lastIndexOf("/");
        if (filePos >= 0) path = path.substring(filePos + 1);
        int dotPos = path.lastIndexOf(".");
        if (dotPos >= 0) return path.substring(dotPos + 1);
        else return null;
    }

    public static String getMimeType(File file, Context ctx) {
        String type = null;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (sharedPref.getBoolean(SettingsActivity.CONTENTDIRECTORY_PROBEMIMETYPE, true))
        {
            try {
                type = tika.detect(file);
                Log.v(TAG, "Type of " + file + " from content: " + type);
            } catch (IOException e) {
                Log.e(TAG, "Could not detect mime type of " + file + ": " + e);
            }
        }
        if (type == null)
        {
            String extension = getExtension(file);
            if (extension != null)
            {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        return type;
    }

    public static boolean isAudioFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    public static boolean isVideoFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public static boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    @Override
    public Integer getChildCount() {
        int count = 0;
        if (path.isDirectory()) {
            for (File file : path.listFiles()) {
                if (file.isDirectory()) {
                    count++;
                } else {
                    String mimeType = getMimeType(file, ctx);
                    if (isAudioFile(mimeType) || isImageFile(mimeType) || isVideoFile(mimeType)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public List<Container> getContainers() {
        if (path.isDirectory()) {
            File files[] = path.listFiles();
            Arrays.sort(files);
            for (File file : files) {
                if (file.isDirectory()) {
                    Log.d(TAG, "Found directory " + file);
                    containers.add(new DirectoryContainer(id, file.getName(), ctx.getString(R.string.app_name), baseURL, ctx, file));
                } else {
                    String fileid = ContentDirectoryService.FILE_PREFIX + file.getAbsolutePath();
                    try {
                        fileid = URLEncoder.encode(fileid, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.w(TAG, "Could not encode fileid " + fileid + ": " + e);
                    }
                    String mimeType = getMimeType(file, ctx);
                    Log.d(TAG, "Found file " + file + " with mime type " + mimeType);
                    if (mimeType == null) mimeType = "application/octet-stream";
                    Res res = new Res(new MimeType(mimeType.substring(0, mimeType.indexOf('/')),
                            mimeType.substring(mimeType.indexOf('/') + 1)), file.length(), "http://" + baseURL + "/"
                            + fileid);
                    if (isAudioFile(mimeType)) {
                        items.add(new MusicTrack(fileid, id, file.getName(), "", "", "", res));
                    } else if (isImageFile(mimeType)) {
                        items.add(new ImageItem(fileid, id, file.getName(), "", res));
                    } else if (isVideoFile(mimeType)) {
                        items.add(new VideoItem(fileid, id, file.getName(), "", res));
                    }
                }
            }
        }
        return containers;
    }
}
