package com.odoo.addons.partners.model;

import android.content.Context;

import com.odoo.base.res.ResPartner;
import com.odoo.base.res.ResPhysician;
import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.util.ODate;

/**
 * Created by daami on 11/08/14.
 */
public class ResPatients extends OModel{

    OColumn patient_id = new OColumn("Name", ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn sex = new OColumn("Sex", OVarchar.class);
    OColumn blood_type = new OColumn("Blood Type", OVarchar.class, 10);
    OColumn rh = new OColumn("RH", OVarchar.class, 5);
    OColumn general_info = new OColumn("General information", OText.class);
    OColumn primary_care_doctor = new OColumn("Primary care doctor", ResPhysician.class, OColumn.RelationType.ManyToOne);
    OColumn childbearing_age = new OColumn("Potential for childbearing", OBoolean.class);
    OColumn critical_info = new OColumn("Critical information", OText.class);
    OColumn ssn = new OColumn("Security Social Number", OVarchar.class, 256);
    OColumn dob = new OColumn("Date Of Birh", ODate.class);
    OColumn marital_status = new OColumn("Marital Status", OVarchar.class, 64);

    public ResPatients(Context context){
        super(context,"res.partner.patient");
    }
}
