/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.sWebsites;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;

import mobi.intuitit.android.content.LauncherIntent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SonetWidget extends AppWidgetProvider {
	private static final String TAG = "SonetWidget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action.equals(ACTION_REFRESH)) {
			int[] appWidgetIds;
			if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) appWidgetIds = new int[]{intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)};
			else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			else appWidgetIds = new int[]{AppWidgetManager.INVALID_APPWIDGET_ID};
			context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
		} else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) onDeleted(context, new int[]{appWidgetId});
			else super.onReceive(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
			if (intent.getExtras().getInt(LauncherIntent.Extra.EXTRA_API_VERSION, 1) >= 2) {
				int widget = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				Sonet.sWidgetsContext.put(widget, context);
				context.startService(new Intent(context, SonetScrollableBuilder.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget));
			}
		} else if (Sonet.ACTION_BUILD_SCROLL.equals(action)) {
			Log.v(TAG,"BUILD_SCROLL");
			int widget = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			Sonet.sWidgetsContext.put(widget, context);
			context.startService(new Intent(context, SonetScrollableBuilder.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget));
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_FINISH)) {
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_VIEW_CLICK)) {
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Error.ERROR_SCROLL_CURSOR)) {
			Log.d(TAG, intent.getStringExtra(LauncherIntent.Extra.EXTRA_ERROR_MESSAGE) + "");
		} else super.onReceive(context, intent);
	}

	@Override
	public final void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getService(context, 0, new Intent(context, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
			context.getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)});
			context.getContentResolver().delete(Accounts.CONTENT_URI, Accounts.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)});
			context.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)});
		}
	}

	private void onClick(Context context, Intent intent) {
		boolean hasbuttons = false;
		String appWidgetId = Integer.toString(intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
		String id = intent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS);
		Uri uri = Widgets.CONTENT_URI;
		Cursor c = context.getContentResolver().query(uri, new String[]{Widgets._ID, Widgets.HASBUTTONS}, Widgets.WIDGET + "=?", new String[]{appWidgetId}, null);
		if (c.moveToFirst()) hasbuttons = c.getInt(c.getColumnIndex(Widgets.HASBUTTONS)) == 1;
		c.close();
		if (hasbuttons) {
			String link = null;
			Cursor item = context.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.LINK}, Statuses._ID + "=?", new String[]{id}, null);
			if (item.moveToFirst()) {
				int service = item.getInt(item.getColumnIndex(Statuses.SERVICE));
				link = item.getString(item.getColumnIndex(Statuses.LINK));
				if (link == null) link = sWebsites[service];
			}
			item.close();
			if (link != null) context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		} else context.startActivity(new Intent(context, StatusDialog.class).setData(Uri.withAppendedPath(Statuses_styles.CONTENT_URI, id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

}