/*
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.odoo.orm;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class OM2MRecord {
	OColumn mCol = null;
	int mId = 0;
	OModel mDatabase = null;


    /**
     * The Enum Many2Many ids Operation.
     */
    public enum Operation {

        /** Adds given ids to related many2many table. */
        ADD,
        /** Appends given ids to related many2many table. */
        APPEND,
        /** Removes given ids from related many2many table. */
        REMOVE,
        /**
         * Replace old ids with new one. (i.e, remove old one and insert new
         * one).
         */
        REPLACE
    }

    /** The operation. */
    Operation mOperation = null;

    /** The List<Integer> ids. */
    private List<Integer> mIds = new ArrayList<Integer>();

    public OM2MRecord(Operation operation, List<Integer> ids) {
        mIds.clear();
        mOperation = operation;
        mIds.addAll(ids);
    }

	public OM2MRecord(OModel model, OColumn col, int id) {
		mDatabase = model;
		mCol = col;
		mId = id;
	}

    public List<Integer> getIds() {
        return mIds;
    }

    public JSONArray getJSONIds() {
        JSONArray ids = new JSONArray();
        try {
            for (int id : mIds) {
                ids.put(id);
            }
        } catch (Exception e) {
        }
        return ids;
    }

    public void setIds(List<Integer> ids) {
        mIds.clear();
        mIds.addAll(ids);
    }
    public Operation getOperation() {
        return mOperation;
    }

    /**
     * Sets the operation.
     *
     * @param mOperation
     *            the new operation
     */
    public void setOperation(Operation mOperation) {
        this.mOperation = mOperation;
    }

    /**
     * Gets the ids.
     *
     * @return the ids
     */

	public List<ODataRow> browseEach() {
		OModel rel = mDatabase.createInstance(mCol.getType());
		return mDatabase.selectM2MRecords(mDatabase, rel, mId);
	}

	public ODataRow browseAt(int index) {
		List<ODataRow> list = browseEach();
		if (list.size() == 0) {
			return null;
		}
		return list.get(index);
	}

}
