package com.odoo.base.res;

import android.content.Context;

import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OVarchar;

/**
 * Created by daami on 11/08/14.
 */
public class ResPhysician extends OModel {

    OColumn name = new OColumn("Name", OVarchar.class, 64);
    OColumn spec = new OColumn("Speciality", OVarchar.class);
    OColumn ssn = new OColumn("Security social number",OVarchar.class);

    public ResPhysician(Context context) {
        super(context, "res.physician");
    }
}
