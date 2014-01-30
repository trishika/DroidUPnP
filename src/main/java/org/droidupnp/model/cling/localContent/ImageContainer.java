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
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.seamless.util.MimeType;

import java.util.List;

public class ImageContainer extends DynamicContainer
{
	private static final String TAG = "ImageContainer";

	public ImageContainer(String id, String parentID, String title, String creator, String baseURL, Context ctx)
	{
		super(id, parentID, title, creator, baseURL, ctx, null, null);
		uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	public Integer getChildCount()
	{
		String[] columns = { MediaStore.Images.Media._ID };
		return ctx.getContentResolver().query(uri, columns, where, whereVal, orderBy).getCount();
	}

	@Override
	public List<Container> getContainers()
	{
		String[] columns = {
			MediaStore.Images.Media._ID,
			MediaStore.Images.Media.TITLE,
			MediaStore.Images.Media.DATA,
			MediaStore.Images.Media.MIME_TYPE,
			MediaStore.Images.Media.SIZE,
			MediaStore.Images.Media.HEIGHT,
			MediaStore.Images.Media.WIDTH,
		};

		Cursor cursor = ctx.getContentResolver().query(uri, columns, where, whereVal, orderBy);
		if (cursor.moveToFirst())
		{
			do
			{
				String id = ContentDirectoryService.IMAGE_PREFIX + cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
				String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE));
				String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
				String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
				long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
				long height = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
				long width = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));

				String extension = "";
				int dot = filePath.lastIndexOf('.');
				if (dot >= 0)
					extension = filePath.substring(dot).toLowerCase();

				Res res = new Res(new MimeType(mimeType.substring(0, mimeType.indexOf('/')),
						mimeType.substring(mimeType.indexOf('/') + 1)), size, "http://" + baseURL + "/" + id + extension);
				res.setResolution((int)width, (int)height);

				addItem(new ImageItem(id, parentID, title, "", res));

				Log.v(TAG, "Added image item " + title + " from " + filePath);

			} while (cursor.moveToNext());
		}
		cursor.close();

		return containers;
	}

}
