package com.odoo.addons.message.providers.message;

import com.odoo.support.provider.OContentProvider;

/**
 * Created by daami on 15/08/14.
 */
public class MessageProvider extends OContentProvider {

    /**
     * The contenturi.
     */
    public static String CONTENTURI = "com.odoo.addons.message.providers.message.MessageProvider";

    /**
     * The authority.
     */
    public static String AUTHORITY = "com.odoo.addons.message.providers.message";

    @Override
    public String authority() {
        return AUTHORITY;
    }

    @Override
    public String contentUri() {
        return CONTENTURI;
    }
}
