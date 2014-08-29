package com.odoo.orm.types;

import com.odoo.orm.OModel;

/**
 * Created by daami on 19/08/14.
 */
public class OEManyToMany {
    OModel mDb = null;

    public OEManyToMany(OModel db) {
        mDb = db;
    }

    public OModel getDBHelper() {
        return mDb;
    }
}
