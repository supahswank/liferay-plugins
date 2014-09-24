/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.google.mail.groups.util;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.util.portlet.PortletProps;

/**
 * @author Matthew Kong
 */
public class PortletPropsValues {

	public static final String EMAIL_PERMISSION = GetterUtil.getString(
		PortletProps.get(PortletPropsKeys.EMAIL_PERMISSION));

	public static final String EMAIL_PREFIX = GetterUtil.getString(
		PortletProps.get(PortletPropsKeys.EMAIL_PREFIX));

	public static final String GOOGLE_API_SERVICE_ACCOUNT_ID =
		GetterUtil.getString(
			PortletProps.get(PortletPropsKeys.GOOGLE_API_SERVICE_ACCOUNT_ID));

	public static final String GOOGLE_API_SERVICE_ACCOUNT_PRIVATE_KEY_P12_FILE =
		GetterUtil.getString(
			PortletProps.get(
				PortletPropsKeys.
					GOOGLE_API_SERVICE_ACCOUNT_PRIVATE_KEY_P12_FILE));

	public static final String GOOGLE_API_SERVICE_ACCOUNT_USER =
		GetterUtil.getString(
			PortletProps.get(PortletPropsKeys.GOOGLE_API_SERVICE_ACCOUNT_USER));

	public static final boolean SYNC_ON_STARTUP = GetterUtil.getBoolean(
		PortletProps.get(PortletPropsKeys.SYNC_ON_STARTUP));

}