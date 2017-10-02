/*
 * Copyright (c) 2017 atfogo GmbH
 * 
 * This software may be modified and distributed under the 
 * terms of the MIT license. See the LICENSE file for details.
 */
package de.atfogo.elane.capi;

import java.util.Map;

public interface Elane
{
    public String getSiteKey();

    public ElaneIntegration getIntegration();

    public Map<String, Object> getDataLayer();
}
