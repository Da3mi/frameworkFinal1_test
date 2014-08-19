package com.odoo.addons.message;

import android.content.Context;

import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OBlob;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

/**
 * Created by daami on 15/08/14.
 */
public class MailGroupDB extends OModel {
    /**
     * Instantiates a new o model.
     *
     * @param context    the context
     *
     */
    public MailGroupDB(Context context) {
        super(context, "mail.group");
    }

    OColumn name = new OColumn("Name", OVarchar.class);
    OColumn description = new OColumn("Description",OText.class);
    OColumn image_medium = new OColumn("medium Image", OBlob.class);
}
