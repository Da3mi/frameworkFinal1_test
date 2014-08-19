package com.odoo.addons.partners.model;

import android.content.Context;

import com.odoo.addons.partners.model.ResMedicament;
import com.odoo.base.res.ResPartner;
import com.odoo.base.res.ResPhysician;
import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

/**
 * Created by daami on 11/08/14.
 */
public class ResPrescription extends OModel{

    OColumn patient_id = new OColumn("Patient", ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn pregnancy_warning = new OColumn("Pregnancy warning", OBoolean.class);
    OColumn notes = new OColumn("Prescription Notes", OText.class);
    OColumn pharmacy = new OColumn("Pharmacy",OVarchar.class);
    OColumn prescription_date = new OColumn("Prescription Date", ODateTime.class);
    OColumn prescription_warning_ack = new OColumn("prescription verified",OBoolean.class);
    OColumn physician_id = new OColumn("Prescribing Doctor", ResPhysician.class,
            OColumn.RelationType.ManyToOne).setRelatedColumn("name");
    OColumn name = new OColumn("Prescription ID", OVarchar.class);

    //  OColumn template = new OColumn("Medication ")
    OColumn medicament_id = new OColumn("Medicament", ResMedicament.class,
            OColumn.RelationType.ManyToOne);
    OColumn quantity = new OColumn("Units", OInteger.class);

    public ResPrescription(Context context) {
        super(context, "res.partner.prescription");
    }
}
