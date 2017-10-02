/*
 * Copyright (c) 2017 atfogo GmbH
 *
 * This software may be modified and distributed under the
 * terms of the MIT license. See the LICENSE file for details.
 */
package de.atfogo.elane.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.intershop.beehive.app.capi.AppContext;
import com.intershop.beehive.core.capi.app.AppContextUtil;
import com.intershop.beehive.core.capi.pipeline.PipelineDictionary;
import com.intershop.beehive.core.capi.request.Request;
import com.intershop.component.application.capi.ApplicationBO;
import com.intershop.component.pmc.capi.pagelet.PageletEntryPoint;
import com.intershop.component.pmc.capi.rendering.AbstractRenderEventListener;
import com.intershop.component.pmc.capi.rendering.RenderEntity;
import com.intershop.component.pmc.capi.rendering.RenderEvent;
import com.intershop.component.pmc.capi.rendering.RenderProcessState;
import com.intershop.component.service.capi.assignment.ServiceExecutable;
import com.intershop.component.service.capi.service.ServiceConfigurationBORepository;

import de.atfogo.elane.capi.Elane;
import de.atfogo.elane.capi.ElaneIntegration;

public class ElaneRenderEventListener extends AbstractRenderEventListener<PageletEntryPoint>
{
    public static final String RQ_ELANE_DATA_LAYER = "ElaneDataLayer";

    @Override
    public Class<PageletEntryPoint> getRenderedClass()
    {
        return PageletEntryPoint.class;
    }

    @Override
    public void notifyRendering(RenderEvent<PageletEntryPoint> renderEvent)
    {
        if (renderEvent == null || !RenderProcessState.PRE_RENDERING.equals(renderEvent.getRenderProcessState()))
        {
            return;
        }

        Request request = Request.getCurrent();
        if (request == null)
        {
            return;
        }

        if (request.getObject(RQ_ELANE_DATA_LAYER) != null)
        {
            return;
        }

        RenderEntity<PageletEntryPoint> renderEntity = renderEvent.getRenderEntity();
        if (renderEntity == null)
        {
            return;
        }

        PageletEntryPoint pageletEntryPoint = renderEntity.getRenderObject();
        if (pageletEntryPoint == null)
        {
            return;
        }

        String id = pageletEntryPoint.getId();
        if (id == null)
        {
            return;
        }

        PipelineDictionary dictionary = request.getPipelineDictionary();
        if (dictionary == null)
        {
            return;
        }

        Elane elane = getElane();
        if (elane == null || !ElaneIntegration.AUTOMATIC.equals(elane.getIntegration()))
        {
            return;
        }

        ElaneDataLayerBuilder builder = new ElaneDataLayerBuilder(request, id, dictionary);
        Map<String, Object> dataLayer = builder.build();
        request.putObject(RQ_ELANE_DATA_LAYER, dataLayer);
    }

    protected Elane getElane()
    {
        AppContext appContext = AppContextUtil.getCurrentAppContext();
        if (appContext == null)
        {
            return null;
        }

        ApplicationBO applicationBO = appContext.getVariable(ApplicationBO.CURRENT);
        if (applicationBO == null)
        {
            return null;
        }

        ServiceExecutable services = applicationBO.getRepository(ServiceConfigurationBORepository.EXTENSION_ID);
        if (services == null)
        {
            return null;
        }

        Collection<Elane> adapters = services.getServiceAdapters(Elane.class);
        if (adapters == null)
        {
            return null;
        }

        Iterator<Elane> i = adapters.iterator();
        if (!i.hasNext())
        {
            return null;
        }

        Elane elane = i.next();
        return elane;
    }
}
