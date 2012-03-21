/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core.internal.preferences;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.aptana.core.CorePlugin;
import com.aptana.core.ICorePreferenceConstants;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.EclipseUtil;

/**
 * @author Max Stepanov
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer
{

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences()
	{
		IEclipsePreferences prefs = EclipseUtil.defaultScope().getNode(CorePlugin.PLUGIN_ID);
		prefs.putBoolean(ICorePreferenceConstants.PREF_SHOW_SYSTEM_JOBS, ICorePreferenceConstants.DEFAULT_DEBUG_MODE);
		prefs.put(ICorePreferenceConstants.PREF_DEBUG_LEVEL, IdeLog.StatusLevel.ERROR.toString());
		prefs.put(
				ICorePreferenceConstants.PREF_WEB_FILES,
				"*.js;*.htm;*.html;*.xhtm;*.xhtml;*.css;*.xml;*.xsl;*.xslt;*.fla;*.gif;*.jpg;*.jpeg;*.php;*.asp;*.jsp;*.png;*.as;*.sdoc;*.swf;*.shtml;*.txt;*.aspx;*.asmx;"); //$NON-NLS-1$
		try
		{
			prefs.flush();
		}
		catch (BackingStoreException e)
		{
			IdeLog.logError(CorePlugin.getDefault(), e);
		}

		// Migrate auto-refresh pref to Eclipse's
		prefs = EclipseUtil.instanceScope().getNode(CorePlugin.PLUGIN_ID);
		boolean migrated = prefs.getBoolean("migrated_auto_refresh", false); //$NON-NLS-1$
		if (!migrated)
		{
			// by default, turn on auto refresh
			boolean autoRefresh = ICorePreferenceConstants.DEFAULT_AUTO_REFRESH_PROJECTS;
			// Check if user set a value explicitly
			prefs = EclipseUtil.instanceScope().getNode(CorePlugin.PLUGIN_ID);
			String oldValue = prefs.get(ICorePreferenceConstants.PREF_AUTO_REFRESH_PROJECTS, null);
			if (oldValue != null)
			{
				// there's an explicit value set, make sure to use that.
				autoRefresh = Boolean.valueOf(oldValue);
				prefs.remove(ICorePreferenceConstants.PREF_AUTO_REFRESH_PROJECTS);
				try
				{
					prefs.flush();
				}
				catch (BackingStoreException e)
				{
					IdeLog.logError(CorePlugin.getDefault(), e);
				}
			}
			ResourcesPlugin.getPlugin().getPluginPreferences().setValue(ResourcesPlugin.PREF_AUTO_REFRESH, autoRefresh);
		}
	}

}
