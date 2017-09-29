/*
 * Copyright (c) 2017 atfogo GmbH
 *
 * This software may be modified and distributed under the
 * terms of the MIT license. See the LICENSE file for details.
 */
package de.atfogo.elane.internal;

import java.util.Collections;
import java.util.Map;

import com.intershop.beehive.configuration.capi.common.Configuration;
import com.intershop.beehive.core.capi.request.Request;
import com.intershop.component.service.capi.chain.adapter.MultiOperationAdapter;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;
import com.intershop.component.service.capi.service.TrackingSFI;

import de.atfogo.elane.capi.Elane;
import de.atfogo.elane.capi.ElaneIntegration;

public class ElaneAdapter extends MultiOperationAdapter implements Elane, TrackingSFI
{
    public static final String ID = "ElaneAdapter";

    public static final String CONFIG_SITE_KEY = "elane.siteKey";

    public static final String CONFIG_INTEGRATION = "elane.integration";

    public ElaneAdapter(ServiceConfigurationBO serviceConfiguration, Class<?> serviceInterface)
    {
        super(serviceConfiguration, serviceInterface);

        if (!serviceInterface.isAssignableFrom(getClass())) {
            throw new IllegalArgumentException(getClass() + " does not implement " + serviceInterface);
        }
    }

    @Override
    public String getTemplate(String key)
    {
        switch(key != null ? key : "")
        {
            case "MainFrameHeader":
                return "inc/elane/MainFrameHeader.isml";

            case "MainFrameFooter":
                return "inc/elane/MainFrameFooter.isml";

            default:
                return null;
        }
    }

    @Override
    public String getTrackingEventHandlerId()
    {
        return ID;
    }

    @Override
    public String getSiteKey()
    {
        Configuration configuration = getConfiguration();
        String siteKey = configuration.getString(CONFIG_SITE_KEY);
        return siteKey;
    }

    @Override
    public ElaneIntegration getIntegration()
    {
        Configuration configuration = getConfiguration();
        String name = configuration.getString(CONFIG_INTEGRATION);
        return ElaneIntegration.byName(name);
    }

    @Override
    public Map<String, Object> getDataLayer()
    {
        Request request = Request.getCurrent();
        if (request == null)
        {
            return Collections.emptyMap();
        }

        Object dataLayerObject = request.getObject(ElaneRenderEventListener.RQ_ELANE_DATA_LAYER);
        if (!(dataLayerObject instanceof Map))
        {
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> dataLayer = (Map<String, Object>)dataLayerObject;
        return dataLayer;
    }
}
