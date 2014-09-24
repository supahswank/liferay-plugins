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

package com.liferay.knowledgebase.admin.importer;

import com.liferay.knowledgebase.KBArticleImportException;
import com.liferay.knowledgebase.admin.importer.util.KBArticleMarkdownConverter;
import com.liferay.knowledgebase.model.KBArticle;
import com.liferay.knowledgebase.model.KBArticleConstants;
import com.liferay.knowledgebase.service.KBArticleLocalServiceUtil;
import com.liferay.knowledgebase.util.PortletPropsValues;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.zip.ZipReader;
import com.liferay.portal.kernel.zip.ZipReaderFactoryUtil;
import com.liferay.portal.service.ServiceContext;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author James Hinkey
 * @author Sergio González
 */
public class KBArticleImporter {

	public void processZipFile(
			long userId, long groupId, InputStream inputStream,
			ServiceContext serviceContext)
		throws KBArticleImportException {

		if (inputStream == null) {
			throw new KBArticleImportException("Input stream is null");
		}

		try {
			ZipReader zipReader = ZipReaderFactoryUtil.getZipReader(
				inputStream);

			Map<String, String> metadata = getMetadata(zipReader);

			processKBArticleFiles(
				userId, groupId, zipReader, metadata, serviceContext);
		}
		catch (IOException ioe) {
			throw new KBArticleImportException(ioe);
		}
	}

	protected KBArticle addKBArticleMarkdown(
			long userId, long groupId, long parentResourcePrimaryKey,
			String markdown, String fileEntryName, ZipReader zipReader,
			Map<String, String> metadata, ServiceContext serviceContext)
		throws KBArticleImportException {

		if (Validator.isNull(markdown)) {
			throw new KBArticleImportException(
				"Markdown is null for file entry " + fileEntryName);
		}

		KBArticleMarkdownConverter kbArticleMarkdownConverter =
			new KBArticleMarkdownConverter(markdown, fileEntryName, metadata);

		String urlTitle = kbArticleMarkdownConverter.getUrlTitle();

		KBArticle kbArticle =
			KBArticleLocalServiceUtil.fetchKBArticleByUrlTitle(
				groupId, urlTitle);

		try {
			if (kbArticle == null) {
				kbArticle = KBArticleLocalServiceUtil.addKBArticle(
					userId, parentResourcePrimaryKey,
					kbArticleMarkdownConverter.getTitle(), urlTitle, markdown,
					null, kbArticleMarkdownConverter.getSourceURL(), null, null,
					serviceContext);
			}
		}
		catch (Exception e) {
			StringBundler sb = new StringBundler(4);

			sb.append("Unable to add basic KB article for file entry ");
			sb.append(fileEntryName);
			sb.append(": ");
			sb.append(e.getLocalizedMessage());

			throw new KBArticleImportException(sb.toString(), e);
		}

		try {
			String html =
				kbArticleMarkdownConverter.processAttachmentsReferences(
					userId, kbArticle, zipReader,
					new HashMap<String, FileEntry>());

			return KBArticleLocalServiceUtil.updateKBArticle(
				userId, kbArticle.getResourcePrimKey(),
				kbArticleMarkdownConverter.getTitle(), html,
				kbArticle.getDescription(),
				kbArticleMarkdownConverter.getSourceURL(), null, null, null,
				serviceContext);
		}
		catch (Exception e) {
			StringBundler sb = new StringBundler(4);

			sb.append("Unable to update KB article for file entry ");
			sb.append(fileEntryName);
			sb.append(": ");
			sb.append(e.getLocalizedMessage());

			throw new KBArticleImportException(sb.toString(), e);
		}
	}

	protected Map<String, List<String>> getFolderNameFileEntryNamesMap(
		ZipReader zipReader) {

		Map<String, List<String>> folderNameFileEntryNamesMap =
			new TreeMap<String, List<String>>();

		for (String zipEntry : zipReader.getEntries()) {
			String extension = FileUtil.getExtension(zipEntry);

			if (!ArrayUtil.contains(
					PortletPropsValues.MARKDOWN_IMPORTER_ARTICLE_EXTENSIONS,
					StringPool.PERIOD.concat(extension))) {

				continue;
			}

			String folderName = zipEntry.substring(
				0, zipEntry.lastIndexOf(StringPool.SLASH));

			List<String> fileEntryNames = folderNameFileEntryNamesMap.get(
				folderName);

			if (fileEntryNames == null) {
				fileEntryNames = new ArrayList<String>();
			}

			fileEntryNames.add(zipEntry);

			folderNameFileEntryNamesMap.put(folderName, fileEntryNames);
		}

		return folderNameFileEntryNamesMap;
	}

	protected Map<String, String> getMetadata(ZipReader zipReader)
		throws KBArticleImportException {

		InputStream inputStream = null;

		try {
			inputStream = zipReader.getEntryAsInputStream(".METADATA");

			if (inputStream == null) {
				return Collections.emptyMap();
			}

			Properties properties = new Properties();

			properties.load(inputStream);

			Map<String, String> metadata = new HashMap<String, String>(
				properties.size());

			for (Object key : properties.keySet()) {
				Object value = properties.get(key);

				if (value != null) {
					metadata.put(key.toString(), value.toString());
				}
			}

			return metadata;
		}
		catch (IOException ioe) {
			throw new KBArticleImportException(ioe);
		}
		finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	protected void processKBArticleFiles(
			long userId, long groupId, ZipReader zipReader,
			Map<String, String> metadata, ServiceContext serviceContext)
		throws KBArticleImportException {

		Map<String, List<String>> folderNameFileEntryNamesMap =
			getFolderNameFileEntryNamesMap(zipReader);

		Set<String> folderNames = folderNameFileEntryNamesMap.keySet();

		for (String folderName : folderNames) {
			List<String> fileEntryNames = folderNameFileEntryNamesMap.get(
				folderName);

			String sectionIntroFileEntryName = null;

			List<String> sectionFileEntryNames = new ArrayList<String>();

			for (String fileEntryName : fileEntryNames) {
				if (fileEntryName.endsWith(
						PortletPropsValues.MARKDOWN_IMPORTER_ARTICLE_INTRO)) {

					sectionIntroFileEntryName = fileEntryName;
				}
				else {
					sectionFileEntryNames.add(fileEntryName);
				}
			}

			long parentResourcePrimaryKey =
				KBArticleConstants.DEFAULT_PARENT_RESOURCE_PRIM_KEY;

			long sectionResourcePrimaryKey = parentResourcePrimaryKey;

			if (Validator.isNotNull(sectionIntroFileEntryName)) {
				KBArticle sectionIntroKBArticle = addKBArticleMarkdown(
					userId, groupId, parentResourcePrimaryKey,
					zipReader.getEntryAsString(sectionIntroFileEntryName),
					sectionIntroFileEntryName, zipReader, metadata,
					serviceContext);

				sectionResourcePrimaryKey =
					sectionIntroKBArticle.getResourcePrimKey();
			}

			for (String sectionFileEntryName : sectionFileEntryNames) {
				String sectionMarkdown = zipReader.getEntryAsString(
					sectionFileEntryName);

				if (Validator.isNull(sectionMarkdown)) {
					if (_log.isWarnEnabled()) {
						_log.warn(
							"Missing Markdown in file entry " +
								sectionFileEntryName);
					}
				}

				addKBArticleMarkdown(
					userId, groupId, sectionResourcePrimaryKey, sectionMarkdown,
					sectionFileEntryName, zipReader, metadata, serviceContext);
			}
		}
	}

	private static Log _log = LogFactoryUtil.getLog(KBArticleImporter.class);

}