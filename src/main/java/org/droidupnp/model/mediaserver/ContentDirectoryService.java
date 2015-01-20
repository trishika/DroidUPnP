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

import org.droidupnp.R;
import org.droidupnp.model.cling.localContent.AlbumContainer;
import org.droidupnp.model.cling.localContent.ArtistContainer;
import org.droidupnp.model.cling.localContent.AudioContainer;
import org.droidupnp.model.cling.localContent.CustomContainer;
import org.droidupnp.model.cling.localContent.ImageContainer;
import org.droidupnp.model.cling.localContent.VideoContainer;
import org.droidupnp.view.SettingsActivity;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

public class ContentDirectoryService extends AbstractContentDirectoryService
{
	private final static String TAG = "ContentDirectoryService";

	public final static char SEPARATOR = '$';

	// Type
	public final static int ROOT_ID   = 0;
	public final static int VIDEO_ID  = 1;
	public final static int AUDIO_ID  = 2;
	public final static int IMAGE_ID  = 3;

	// Test
	public final static String VIDEO_TXT  = "Videos";
	public final static String AUDIO_TXT  = "Music";
	public final static String IMAGE_TXT  = "Images";

	// Type subfolder
	public final static int ALL_ID    = 0;
	public final static int FOLDER_ID = 1;
	public final static int ARTIST_ID = 2;
	public final static int ALBUM_ID  = 3;

	// Prefix item
	public final static String VIDEO_PREFIX     = "v-";
	public final static String AUDIO_PREFIX     = "a-";
	public final static String IMAGE_PREFIX     = "i-";
	public final static String DIRECTORY_PREFIX = "d-";


	private static Context ctx;
	private static String baseURL;

	public ContentDirectoryService()
	{
		Log.v(TAG, "Call default constructor...");
	}

	public ContentDirectoryService(Context ctx, String baseURL)
	{
		this.ctx = ctx;
		this.baseURL = baseURL;
	}

	public void setContext(Context ctx)
	{
		this.ctx = ctx;
	}

	public void setBaseURL(String baseURL)
	{
		this.baseURL = baseURL;
	}

	@Override
	public BrowseResult browse(String objectID, BrowseFlag browseFlag,
			String filter, long firstResult, long maxResults,
			SortCriterion[] orderby) throws ContentDirectoryException
	{
		Log.d(TAG, "Will browse " + objectID);

		try
		{
			DIDLContent didl = new DIDLContent();
			TextUtils.StringSplitter ss = new TextUtils.SimpleStringSplitter(SEPARATOR);
			ss.setString(objectID);

			int type = -1;
			ArrayList<Integer> subtype = new ArrayList<Integer>();

			for (String s : ss)
			{
				int i = Integer.parseInt(s);
				if(type==-1)
				{
					type = i;
					if(type!=ROOT_ID && type!=VIDEO_ID && type!=AUDIO_ID && type!=IMAGE_ID)
						throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT, "Invalid type!");
				}
				else
				{
					subtype.add(i);
				}
			}

			Container container = null;

			Log.d(TAG, "Browsing type " + type);
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);

			Container rootContainer = new CustomContainer( "" + ROOT_ID, "" + ROOT_ID,
					ctx.getString(R.string.app_name), ctx.getString(R.string.app_name), baseURL);


			// Video
			Container videoContainer = null, allVideoContainer = null;
			if(sharedPref.getBoolean(SettingsActivity.CONTENTDIRECTORY_VIDEO, true))
			{
				videoContainer = new CustomContainer( "" + VIDEO_ID, "" + ROOT_ID,
						VIDEO_TXT, ctx.getString(R.string.app_name), baseURL);
				rootContainer.addContainer(videoContainer);
				rootContainer.setChildCount(rootContainer.getChildCount()+1);

				allVideoContainer = new VideoContainer( ""+ ALL_ID, "" + VIDEO_ID,
						"All", ctx.getString(R.string.app_name), baseURL, ctx);
				videoContainer.addContainer(allVideoContainer);
				videoContainer.setChildCount(videoContainer.getChildCount()+1);
			}

			// Audio
			Container audioContainer = null, artistAudioContainer = null, albumAudioContainer = null,
					allAudioContainer = null;
			if(sharedPref.getBoolean(SettingsActivity.CONTENTDIRECTORY_AUDIO, true))
			{
				audioContainer = new CustomContainer( "" + AUDIO_ID, "" + ROOT_ID,
						AUDIO_TXT, ctx.getString(R.string.app_name), baseURL);
				rootContainer.addContainer(audioContainer);
				rootContainer.setChildCount(rootContainer.getChildCount()+1);

				artistAudioContainer = new ArtistContainer( "" + ARTIST_ID, "" + AUDIO_ID,
						"Artist", ctx.getString(R.string.app_name), baseURL, ctx);
				audioContainer.addContainer(artistAudioContainer);
				audioContainer.setChildCount(audioContainer.getChildCount()+1);

				albumAudioContainer = new AlbumContainer( "" + ALBUM_ID, "" + AUDIO_ID,
						"Album", ctx.getString(R.string.app_name), baseURL, ctx, null);
				audioContainer.addContainer(albumAudioContainer);
				audioContainer.setChildCount(audioContainer.getChildCount()+1);

				allAudioContainer = new AudioContainer("" + ALL_ID, "" + AUDIO_ID,
						"All", ctx.getString(R.string.app_name), baseURL, ctx, null, null);
				audioContainer.addContainer(allAudioContainer);
				audioContainer.setChildCount(audioContainer.getChildCount()+1);
			}

			// Image
			Container imageContainer = null, allImageContainer = null;
			if(sharedPref.getBoolean(SettingsActivity.CONTENTDIRECTORY_IMAGE, true))
			{
				imageContainer = new CustomContainer( "" + IMAGE_ID, "" + ROOT_ID, IMAGE_TXT,
						ctx.getString(R.string.app_name), baseURL);
				rootContainer.addContainer(imageContainer);
				rootContainer.setChildCount(rootContainer.getChildCount()+1);

				allImageContainer = new ImageContainer( "" + ALL_ID, "" + IMAGE_ID, "All",
						ctx.getString(R.string.app_name), baseURL, ctx);
				imageContainer.addContainer(allImageContainer);
				imageContainer.setChildCount(imageContainer.getChildCount()+1);
			}

			if(subtype.size()==0)
			{
				if(type==ROOT_ID) container = rootContainer;
				if(type==AUDIO_ID) container = audioContainer;
				if(type==VIDEO_ID) container = videoContainer;
				if(type==IMAGE_ID) container = imageContainer;
			}
			else
			{
				if(type==VIDEO_ID)
				{
					if(subtype.get(0) == ALL_ID)
					{
						Log.d(TAG, "Listing all videos...");
						container = allVideoContainer;
					}
				}
				else if(type==AUDIO_ID)
				{
					if(subtype.size()==1)
					{
						if(subtype.get(0) == ARTIST_ID)
						{
							Log.d(TAG, "Listing all artists...");
							container = artistAudioContainer;
						}
						else if(subtype.get(0) == ALBUM_ID)
						{
							Log.d(TAG, "Listing album of all artists...");
							container = albumAudioContainer;
						}
						else if(subtype.get(0) == ALL_ID)
						{
							Log.d(TAG, "Listing all songs...");
							container = allAudioContainer;
						}
						// and others...
					}
					else if(subtype.size()==2 && subtype.get(0) == ARTIST_ID)
					{
						String artistId = "" + subtype.get(1);
						String parentId = "" + AUDIO_ID + SEPARATOR + subtype.get(0);
						Log.d(TAG, "Listing album of artist " + artistId);
						container = new AlbumContainer(artistId, parentId, "",
								ctx.getString(R.string.app_name), baseURL, ctx, artistId);
					}
					else if(subtype.size()==2 && subtype.get(0) == ALBUM_ID)
					{
						String albumId  = "" + subtype.get(1);
						String parentId = "" + AUDIO_ID + SEPARATOR + subtype.get(0);
						Log.d(TAG, "Listing song of album " + albumId);
						container = new AudioContainer(albumId, parentId, "",
								ctx.getString(R.string.app_name), baseURL, ctx, null, albumId);
					}
					else if(subtype.size()==3 && subtype.get(0) == ARTIST_ID)
					{
						String albumId  = "" + subtype.get(2);
						String parentId = "" + AUDIO_ID + SEPARATOR + subtype.get(0) + SEPARATOR + subtype.get(1);
						Log.d(TAG, "Listing song of album " + albumId + " for artist " + subtype.get(1));
						container = new AudioContainer(albumId, parentId, "",
								ctx.getString(R.string.app_name), baseURL, ctx, null, albumId);
					}
				}
				else if(type==IMAGE_ID)
				{
					if(subtype.get(0) == ALL_ID)
					{
						Log.d(TAG, "Listing all images...");
						container = allImageContainer;
					}
				}
			}

			if(container!=null)
			{
				Log.d(TAG, "List container...");

				// Get container first
				for(Container c : container.getContainers())
					didl.addContainer(c);

				Log.d(TAG, "List item...");

				// Then get item
				for(Item i : container.getItems())
					didl.addItem(i);

				Log.d(TAG, "Return result...");

				int count = container.getChildCount();
				Log.d(TAG, "Child count : " + count);
				String answer = "";
				try{
					answer = new DIDLParser().generate(didl);
				}
				catch (Exception ex)
				{
					throw new ContentDirectoryException(
					ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
				}
				Log.d(TAG, "answer : " + answer);

				return new BrowseResult(answer,count,count);
			}
		}
		catch (Exception ex)
		{
			throw new ContentDirectoryException(
				ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
		}

		Log.e(TAG, "No container for this ID !!!");
		throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
	}
}
