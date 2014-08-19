package com.odoo.addons.message.providers.groups;

import com.odoo.support.provider.OContentProvider;

/**
 * Created by daami on 15/08/14.
 */
public class MailGroupProvider extends OContentProvider {

    /** The contenturi. */
    public static String CONTENTURI = "com.odoo.addons.message.providers.groups.MailGroupProvider";

    /** The authority. */
    public static String AUTHORITY = "com.odoo.addons.message.providers.groups";

    @Override
    public String authority() {
        return AUTHORITY;
    }

    @Override
    public String contentUri() {
        return CONTENTURI;
    }
}
