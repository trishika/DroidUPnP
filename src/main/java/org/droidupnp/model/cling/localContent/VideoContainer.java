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

package org.droidupnp.model.cling.localContent;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import org.droidupnp.model.mediaserver.ContentDirectoryService;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.seamless.util.MimeType;

import java.util.List;

public class VideoContainer extends DynamicContainer
{
	private static final String TAG = "VideoContainer";

	public VideoContainer(String id, String parentID, String title, String creator, String baseURL, Context ctx)
	{
		super(id, parentID, title, creator, baseURL, ctx, null, null);
		uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	public Integer getChildCount()
	{
		String[] columns = { MediaStore.Video.Media._ID };
		Cursor cursor = ctx.getContentResolver().query(uri, columns, where, whereVal, orderBy);
		if(cursor == null)
			return 0;
		return cursor.getCount();
	}

	@Override
	public List<Container> getContainers()
	{
		String[] columns = {
			MediaStore.Video.Media._ID,
			MediaStore.Video.Media.TITLE,
			MediaStore.Video.Media.DATA,
			MediaStore.Video.Media.ARTIST,
			MediaStore.Video.Media.MIME_TYPE,
			MediaStore.Video.Media.SIZE,
			MediaStore.Video.Media.DURATION,
			MediaStore.Images.Media.HEIGHT,
			MediaStore.Images.Media.WIDTH,
		};

		Cursor cursor = ctx.getContentResolver().query(uri, columns, where, whereVal, orderBy);
		if(cursor!=null)
		{
			if (cursor.moveToFirst())
			{
				do
				{
					String id = ContentDirectoryService.VIDEO_PREFIX + cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
					String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
					String creator = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST));
					String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
					String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
					long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
					long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
					long height = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
					long width = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));

					String extension = "";
					int dot = filePath.lastIndexOf('.');
					if (dot >= 0)
						extension = filePath.substring(dot).toLowerCase();

					Res res = new Res(new MimeType(mimeType.substring(0, mimeType.indexOf('/')),
						mimeType.substring(mimeType.indexOf('/') + 1)), size, "http://" + baseURL + "/" + id + extension);
					res.setDuration(duration / (1000 * 60 * 60) + ":"
						+ (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
						+ (duration % (1000 * 60)) / 1000);
					res.setResolution((int) width, (int) height);

					addItem(new VideoItem(id, parentID, title, creator, res));

					Log.v(TAG, "Added video item " + title + " from " + filePath);

				} while (cursor.moveToNext());
			}
			cursor.close();
		}

		return containers;
	}
}
