<%--
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
--%>

<%@ include file="/init.jsp" %>

<aui:form name="fm">
	<aui:input label="message" name="message" type="textarea" />

	<aui:input label="url" name="url" />

	<aui:button disabled="<%= !PushNotificationsPermission.contains(permissionChecker, ActionKeys.SEND_NOTIFICATION) %>" type="submit" value="send" />
</aui:form>

<aui:script use="aui-base">
	var form = A.one('#<portlet:namespace />fm');

	form.on(
		'submit',
		function(event) {
			event.halt();

			var message = form.one('textarea[name="<portlet:namespace />message"]').val();
			var type = 'text';
			var url = form.one('input[name="<portlet:namespace />url"]').val().trim();

			if (url.length !== 0) {
				if (<portlet:namespace />isImage(url)) {
					type = 'image';
				}
				else {
					type = 'link';
				}
			}

			Liferay.Service(
				'/push-notifications-portlet.pushnotificationsdevice/send-push-notification',
				{
					payload: A.JSON.stringify(
						{
							message: message,
							type: type,
							url: url
						}
					)
				}
			);
		}
	);

	function <portlet:namespace />isImage(url) {
		var regex = /(.*\.(?:gif|jpeg|jpg|png))/i;

		if (regex.test(url)) {
			return true;
		}
		else {
			return false;
		}
	}
</aui:script>