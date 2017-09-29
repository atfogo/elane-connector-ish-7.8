/*
 * Copyright (c) 2017 atfogo GmbH
 * 
 * This software may be modified and distributed under the 
 * terms of the MIT license. See the LICENSE file for details.
 */
package de.atfogo.elane.capi;

public enum ElaneIntegration
{
    AUTOMATIC,

    MANUAL;

    public static ElaneIntegration byName(String name)
    {
        for (ElaneIntegration i : values())
        {
            if (i.name().equals(name))
            {
                return i;
            }
        }

        return null;
    }
}
