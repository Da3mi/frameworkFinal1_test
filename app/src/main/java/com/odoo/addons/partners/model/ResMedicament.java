package com.odoo.addons.partners.model;

import android.content.Context;

import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

/**
 * Created by daami on 11/08/14.
 */
public class ResMedicament extends OModel {


    OColumn name = new OColumn("Name", OVarchar.class);
    OColumn category = new OColumn("Category",OVarchar.class);
    OColumn active_component = new OColumn("Active Component",OVarchar.class);
    OColumn indications = new OColumn("Indication", OText.class);


    public ResMedicament(Context context){ super(context, "res.partner.medicament");
    }

    public long add_default() {
        OValues values = new OValues();

        values.put("name", "doliprain");
        values.put("category", "xxxxx");
        values.put("active_component", "xxxxx");
        values.put("indications", "xxxxx");
        values.put("therapeutic_action", "xxxxx");

        long new_id = this.create(values);
        return new_id ;
    }

}
