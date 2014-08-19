package com.odoo.addons.message;

import android.content.Context;

import com.odoo.base.ir.IrAttachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

import org.json.JSONArray;

import odoo.controls.OField;

/**
 * Created by daami on 18/08/14.
 */
public class MessageDB extends OModel {
    /**
     * Instantiates a new o model.
     *
     * @param context    the context
     *
     */
    public MessageDB(Context context) {
        super(context,"mail.message");
    }

    OColumn partner_ids = new OColumn("Partners", ResPartner.class, OColumn.RelationType.ManyToMany);
    OColumn subject = new OColumn("Subject", OText.class);
    OColumn type = new OColumn("Type", OText.class);
    OColumn body = new OColumn("Body", OVarchar.class);
    OColumn email_from = new OColumn("From",OText.class).setDefault(false);

    OColumn parent_id = new OColumn("Parent", OInteger.class);
    OColumn record_name = new OColumn("Record Title",OText.class);
    OColumn to_read = new OColumn("To Read", OVarchar.class);




        public OValues getValue(OColumn col, Object value) {
            OValues values = new OValues();
            try {
                if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    if (array.getInt(0) == 0) {
                        values.put(col.getName(), false);
                        values.put("email_from", array.getString(1));
                    } else {
                        values.put("email_from", false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return values;
        }

    OColumn author_id = new OColumn("Author",ResPartner.class, OColumn.RelationType.ManyToMany);
    /*columns.add(new OEColumn("author_id", "author", OEFields
            .manyToOne(new ResPartnerDB(mContext)), mValueWatcher));*/
    OColumn model = new OColumn("Model",OVarchar.class);
    OColumn res_id = new OColumn("Resource Reference", OText.class);
    OColumn date = new OColumn("Date", OVarchar.class);
    OColumn hase_vote = new OColumn("Has Voted",OVarchar.class);
    OColumn vote_nb = new OColumn("vote_numbers",OInteger.class);
    OColumn starred = new OColumn("Starred", OVarchar.class);
    OColumn Attachements = new OColumn("Attachments", IrAttachment.class, OColumn.RelationType.ManyToOne);

}
