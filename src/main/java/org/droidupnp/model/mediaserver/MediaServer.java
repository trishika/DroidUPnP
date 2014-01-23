/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 *
 * This file is part of DroidUPNP.
 *
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.droidupnp.model.mediaserver;

import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.droidupnp.R;
import org.droidupnp.view.SettingsActivity;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationError;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

public class MediaServer extends fi.iki.elonen.SimpleWebServer
{
	private final static String TAG = "MediaServer";

	private UDN udn = null;
	private LocalDevice localDevice = null;
	private LocalService localService = null;
	private Context ctx = null;

	private final static int port = 8192;
	private static InetAddress localAddress;

	public MediaServer(InetAddress localAddress, Context ctx) throws ValidationException
	{
		super(null, port, null, true);

		Log.e(TAG, "Creating media server !");

		localService = new AnnotationLocalServiceBinder()
				.read(ContentDirectoryService.class);

		localService.setManager(new DefaultServiceManager<ContentDirectoryService>(
				localService, ContentDirectoryService.class));

		udn = UDN.valueOf(new UUID(0,10).toString());
		this.localAddress = localAddress;
		this.ctx = ctx;
		createLocalDevice();

		ContentDirectoryService contentDirectoryService = (ContentDirectoryService)localService.getManager().getImplementation();
		contentDirectoryService.setContext(ctx);
		contentDirectoryService.setBaseURL(getAddress());
	}

	public void restart()
	{
		Log.d(TAG, "Restart mediaServer");
//		try {
//			stop();
//			createLocalDevice();
//			start();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	public void createLocalDevice() throws ValidationException
	{
		String version = "";
		try {
			version = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Application version name not found");
		}

		DeviceDetails details = new DeviceDetails(
			SettingsActivity.getSettingContentDirectoryName(ctx),
			new ManufacturerDetails(ctx.getString(R.string.app_name), ctx.getString(R.string.app_url)),
			new ModelDetails(ctx.getString(R.string.app_name), ctx.getString(R.string.app_url)),
			ctx.getString(R.string.app_name), version);

		List<ValidationError> l = details.validate();
		for( ValidationError v : l )
		{
			Log.e(TAG, "Validation pb for property "+ v.getPropertyName());
			Log.e(TAG, "Error is " + v.getMessage());
		}


		DeviceType type = new UDADeviceType("MediaServer", 1);

		localDevice = new LocalDevice(new DeviceIdentity(udn), type, details, localService);
	}


	public LocalDevice getDevice() {
		return localDevice;
	}

	public String getAddress() {
		return localAddress.getHostAddress() + ":" + port;
	}

	public class InvalidIdentificatorException extends java.lang.Exception
	{
		public InvalidIdentificatorException(){super();}
		public InvalidIdentificatorException(String message){super(message);}
	}

	class ServerObject
	{
		ServerObject(String path, String mime)
		{
			this.path = path;
			this.mime = mime;
		}
		public String path;
		public String mime;
	}

	private ServerObject getFileServerObject(String id) throws InvalidIdentificatorException
	{
		try
		{
			// Remove extension
			int dot = id.lastIndexOf('.');
			if (dot >= 0)
				id = id.substring(0,dot);

			// Try to get media id
			int mediaId = Integer.parseInt(id.substring(3));
			Log.v(TAG, "media of id is " + mediaId);

			MediaStore.MediaColumns mediaColumns = null;
			Uri uri = null;

			if(id.startsWith("/"+ContentDirectoryService.AUDIO_PREFIX))
			{
				Log.v(TAG, "Ask for audio");
				uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				mediaColumns = new MediaStore.Audio.Media();
			}
			else if(id.startsWith("/"+ContentDirectoryService.VIDEO_PREFIX))
			{
				Log.v(TAG, "Ask for video");
				uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				mediaColumns = new MediaStore.Video.Media();
			}
			else if(id.startsWith("/"+ContentDirectoryService.IMAGE_PREFIX))
			{
				Log.v(TAG, "Ask for image");
				uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				mediaColumns = new MediaStore.Images.Media();
			}

			if(uri!=null && mediaColumns!=null)
			{
				String[] columns = new String[]{mediaColumns.DATA, mediaColumns.MIME_TYPE};
				String where = mediaColumns._ID + "=?";
				String[] whereVal = {"" + mediaId};

				String path = null;
				String mime = null;
				Cursor cursor = ctx.getContentResolver().query(uri, columns, where, whereVal, null);

				if(cursor.moveToFirst())
				{
					path = cursor.getString(cursor.getColumnIndexOrThrow(mediaColumns.DATA));
					mime = cursor.getString(cursor.getColumnIndexOrThrow(mediaColumns.MIME_TYPE));
				}
				cursor.close();

				if(path!=null)
					return new ServerObject(path, mime);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error while parsing " + id);
			Log.e(TAG, "exception", e);
		}

		throw new InvalidIdentificatorException(id + " was not found in media database");
	}

	@Override
	public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms,
	                      Map<String, String> files)
	{
		Response res = null;

		Log.e(TAG, "Serve uri : " + uri);

		for(Map.Entry<String, String> entry : header.entrySet())
			Log.d(TAG, "Header : key=" + entry.getKey() + " value=" + entry.getValue());

		for(Map.Entry<String, String> entry : parms.entrySet())
			Log.d(TAG, "Params : key=" + entry.getKey() + " value=" + entry.getValue());

		for(Map.Entry<String, String> entry : files.entrySet())
			Log.d(TAG, "Files : key=" + entry.getKey() + " value=" + entry.getValue());

		try
		{
			try
			{
				ServerObject obj = getFileServerObject(uri);

				Log.i(TAG, "Will serve " + obj.path);
				res = serveFile(new File(obj.path), obj.mime, header);
			}
			catch(InvalidIdentificatorException e)
			{
				return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404, file not found.");
			}

			if( res != null )
			{
				String version = "1.0";
				try {
					version = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
				} catch (PackageManager.NameNotFoundException e) {
					Log.e(TAG, "Application version name not found");
				}

				// Some DLNA header option
				res.addHeader("realTimeInfo.dlna.org", "DLNA.ORG_TLAG=*");
				res.addHeader("contentFeatures.dlna.org", "");
				res.addHeader("transferMode.dlna.org", "Streaming");
				res.addHeader("Server", "DLNADOC/1.50 UPnP/1.0 Cling/2.0 DroidUPnP/"+version +" Android/" + Build.VERSION.RELEASE);
			}

			return res;
		}
		catch(Exception e)
		{
			Log.e(TAG, "Unexpected error while serving file");
			Log.e(TAG, "exception", e);
		}

		return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "INTERNAL ERROR: unexpected error.");
	}
}
