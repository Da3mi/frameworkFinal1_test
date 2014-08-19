package com.odoo.base.res;

import android.content.Context;

import com.odoo.base.res.ResPartner;
import com.odoo.base.res.ResPhysician;
import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OReal;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.util.ODate;

/**
 * Created by daami on 11/08/14.
 */
public class ResAppointment extends OModel {

    OColumn patient_id = new OColumn("Patient first name", ResPartner.class,
            OColumn.RelationType.ManyToOne).setRelatedColumn("first_name");
    OColumn Appointment_date = new OColumn("Date", ODateTime.class);
    OColumn duration = new OColumn("Duration", OReal.class);
    OColumn doctor = new OColumn("physician", ResPhysician.class,
            OColumn.RelationType.ManyToOne).setRelatedColumn("name");
    OColumn ssn = new OColumn("Securiy social number", OVarchar.class, 100);
    OColumn comments = new OColumn("Comments", OText.class);
    OColumn appointment_type = new OColumn("Type", OVarchar.class, 50);
    OColumn urgency = new OColumn("Urgency Level", OVarchar.class, 50);
    // OColumn speciality = new OColumn("Speciality", Specialty.class, OColumn.RelationType.ManyToOne);
    OColumn state = new OColumn("State", OVarchar.class, 50);
    // OColumn history_ids = new OColumn("History", AppointementHistory.class, OColumn.RelationType.OneToMany)


    public ResAppointment(Context context) {
        super(context, "res.appointment");
    }

}
