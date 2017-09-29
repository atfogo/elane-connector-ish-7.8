/*
 * Copyright (c) 2017 atfogo GmbH
 *
 * This software may be modified and distributed under the
 * terms of the MIT license. See the LICENSE file for details.
 */
 package de.atfogo.elane.internal;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.intershop.beehive.core.capi.configuration.ConfigurationMgr;
import com.intershop.beehive.core.capi.localization.LocaleInformation;
import com.intershop.beehive.core.capi.log.Logger;
import com.intershop.beehive.core.capi.pipeline.PipelineDictionary;
import com.intershop.beehive.core.capi.request.Request;
import com.intershop.beehive.core.internal.objectpath.ObjectPathMgr;
import com.intershop.beehive.core.internal.objectpath.ObjectPathMgrImpl;
import com.intershop.beehive.core.internal.pipeline.PipelineDictionaryImpl;
import com.intershop.component.catalog.capi.CatalogCategoryBO;
import com.intershop.component.catalog.capi.CatalogCategoryPath;

public class ElaneDataLayerBuilder
{
    public static final String CONFIG_DATA_LAYER = "elane.dataLayer[{0}]";

    public static final String DELIMITER_OBJECT_PATH = "#";

    public static final String DELIMITER_STRING = "'";

    private Request request;

    private String pageletEntryPointId;

    private PipelineDictionary dictionary;

    private String configuration;

    private ObjectPathMgr objectPathMgr;

    private LocaleInformation locale;

    private Map<String, Object> dataLayer;

    public ElaneDataLayerBuilder(Request request, String pageletEntryPointId, PipelineDictionary dictionary)
    {
        this.request = request;
        this.pageletEntryPointId = pageletEntryPointId;
        this.dictionary = dictionary;
    }

    public Map<String, Object> build()
    {
        if (initObjectPathMgr() == null)
        {
            return Collections.emptyMap();
        }

        if (initConfiguration() == null)
        {
            return Collections.emptyMap();
        }

        dataLayer = new HashMap<>();
        dataLayer.put("Locale", request.getLocale());

        evaluateConfiguration();

        return dataLayer;
    }

    protected String initConfiguration()
    {
        String key = MessageFormat.format(CONFIG_DATA_LAYER, pageletEntryPointId);
        configuration = ConfigurationMgr.getString(key, null);

        if (configuration == null)
        {
            String basePageletEntryPointId = getBasePageletEntryPointId(pageletEntryPointId);
            if (basePageletEntryPointId != null)
            {
                key = MessageFormat.format(CONFIG_DATA_LAYER, basePageletEntryPointId);
                configuration = ConfigurationMgr.getString(key, null);
            }
        }

        if (configuration == null)
        {
            Logger.info(this, "Elane data-layer is not configured for {}", pageletEntryPointId);
        }

        return configuration;
    }

    protected String getBasePageletEntryPointId(String pageletEntryPointId)
    {
        int i = pageletEntryPointId.indexOf('-');
        if (i < 0)
        {
            return null;
        }
        return pageletEntryPointId.substring(0, i);
    }

    protected ObjectPathMgr initObjectPathMgr()
    {
        objectPathMgr = ObjectPathMgrImpl.getInstance();
        return objectPathMgr;
    }

    protected void evaluateConfiguration()
    {
        String[] keyExpressionPairs = configuration.split(";");
        for (int i = 0; i < keyExpressionPairs.length; ++i)
        {
            String keyExpressionPair = keyExpressionPairs[i];
            String[] keyExpression = keyExpressionPair.split("=");
            if (keyExpression.length == 2)
            {
                String key = keyExpression[0].trim();
                String expression = keyExpression[1].trim();
                Object value = evaluateExpression(expression);
                switch(key)
                {
                    case "PageSortKey":
                        dataLayer.put(key, getPageSortKey(value));
                        break;

                    case "CategoryPath":
                        dataLayer.put(key, getCategoryPath(value));
                        break;

                    case "CategoryName":
                        dataLayer.put(key, getCategoryName(value));
                        break;

                    default:
                        dataLayer.put(key, value);
                        break;
                }
            }
        }
    }

    protected Object evaluateExpression(String expression)
    {
        if (expression.startsWith(DELIMITER_OBJECT_PATH) && expression.endsWith(DELIMITER_OBJECT_PATH))
        {
            String path = trim(expression, DELIMITER_OBJECT_PATH, DELIMITER_OBJECT_PATH);
            Object value = objectPathMgr.lookupObject(dictionary, PipelineDictionaryImpl.pipelineStrategy, path);
            return value;
        }

        if (expression.startsWith(DELIMITER_STRING) && expression.endsWith(DELIMITER_STRING))
        {
            String value = trim(expression, DELIMITER_STRING, DELIMITER_STRING);
            return value;
        }

        try
        {
            Double value = Double.parseDouble(expression);
            return value;
        }
        catch (NumberFormatException e)
        {
        }

        return expression;
    }

    protected String trim(String st, String prefix, String postfix)
    {
        int beginIndex = prefix.length();
        int endIndex = st.length() - postfix.length();
        return st.substring(beginIndex, endIndex);
    }

    protected Object getPageSortKey(Object value)
    {
        Object result = value;

        if (value instanceof CatalogCategoryBO)
        {
            CatalogCategoryBO categoryBO = (CatalogCategoryBO)value;
            CatalogCategoryBO parent = categoryBO.getParentCatalogCategoryBO();
            if (parent != null)
            {
                Collection<CatalogCategoryBO> siblings = parent.getAccessibleSubCatalogCategoryBOs();
                int i = 0;
                for (CatalogCategoryBO sibling : siblings)
                {
                    ++i;
                    if (categoryBO.getID().equals(sibling.getID()))
                    {
                        result = new Integer(i);
                        break;
                    }
                }
            }
        }

        return result;
    }

    protected Object getCategoryPath(Object value)
    {
        Object result = value;

        if (value instanceof CatalogCategoryBO)
        {
            CatalogCategoryBO categoryBO = (CatalogCategoryBO)value;
            CatalogCategoryPath path = categoryBO.getCatalogCategoryPath();
            if (path != null)
            {
                LinkedList<String> categoryPath = new LinkedList<>();
                for (CatalogCategoryBO pathEntry : path.values())
                {
                    categoryPath.addFirst(pathEntry.getName());
                }
                result = categoryPath;
            }
        }

        return result;
    }

    protected Object getCategoryName(Object value)
    {
        Object result = value;

        if (value instanceof CatalogCategoryBO)
        {
            CatalogCategoryBO categoryBO = (CatalogCategoryBO)value;
            String categoryName = categoryBO.getDisplayName(locale);
            if (categoryName == null || categoryName.length() < 1)
            {
                categoryName = categoryBO.getName();
            }
            result = categoryName;
        }

        return result;
    }
}
