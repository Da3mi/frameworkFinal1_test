package com.odoo.orm;

import android.util.Log;

import com.odoo.orm.types.OEManyToMany;
import com.odoo.orm.types.OEManyToOne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class OFieldsHelper {
	public static final String TAG = "com.odoo.orm.OEFieldsHelper";
	JSONObject mFields = new JSONObject();
	List<OValues> mValues = new ArrayList<OValues>();
	List<OColumn> mColumns = new ArrayList<OColumn>();
    OERelRecord mRelRecord = new OERelRecord();

	public OFieldsHelper(String[] fields) {
		addAll(fields);
	}

	public OFieldsHelper(List<OColumn> cols) {
		addAll(cols);
		mColumns.addAll(cols);
	}

	public void addAll(String[] fields) {
		try {
			for (int i = 0; i < fields.length; i++) {
				mFields.accumulate("fields", fields[i]);
			}
			if (fields.length == 1) {
				mFields.accumulate("fields", fields[0]);
			}
		} catch (Exception e) {
		}
	}

	public void addAll(List<OColumn> cols) {
		try {
			for (OColumn col : cols) {
				mFields.accumulate("fields", col.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    public void addAll(JSONArray records) {
        try {
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                OValues cValue = new OValues();
                for (OColumn col : mColumns) {

                        String key = col.getName();
                        Object value = false;
                        if (record.has(key)) {
                            value = record.get(key);
                        }
                        if (col.getmValueWatcher() != null) {
                            OValues values = col.getmValueWatcher().getValue(
                                    col, value);
                            cValue.setAll(values);
                        }
                     /*   if (col.getType() instanceof OEManyToOne) {
                            if (value instanceof JSONArray) {
                                JSONArray m2oRec = new JSONArray(
                                        value.toString());
                                value = m2oRec.get(0);
                                if ((Integer) value != 0) {
                                    OEManyToOne m2o = col
                                            .getType();
                                    OModel db = (OModel) m2o
                                            .getDBHelper();
                                    mRelRecord.add(db, value);
                                } else {
                                    value = false;
                                }
                            }
                        }*/
                    /*    if (col.getType() instanceof OEManyToMany) {
                            if (value instanceof JSONArray) {
                                JSONArray m2mRec = new JSONArray(
                                        value.toString());
                                List<Integer> ids = getIdsList(m2mRec);
                                OEM2MIds mIds = new OEM2MIds(Operation.REPLACE,
                                        ids);
                                value = mIds;
                                OEManyToMany m2m = (OEManyToMany) col.getType();
                                OModel db = (OModel) m2m.getDBHelper();
                                mRelRecord.add(db, ids);
                            }
                        }*/
                        cValue.put(key, value);

                }
                mValues.add(cValue);
            }
        } catch (Exception e) {
            Log.d(TAG, "OEFieldsHelper->addAll(JSONArray records)");
            e.printStackTrace();
        }
    }


	public JSONObject get() {
		return mFields;
	}

	public List<OValues> getValues() {
		return mValues;
	}


    public List<OERelationData> getRelationData() {
        return mRelRecord.getAll();
    }

    class OERelRecord {
        private HashMap<String, Object> _models = new HashMap<String, Object>();
        private HashMap<String, List<Object>> _model_ids = new HashMap<String, List<Object>>();

        @SuppressWarnings("unchecked")
        public void add(OModel db, Object ids) {
            if (!_models.containsKey(db.getModelName())) {
                _models.put(db.getModelName(), db);
            }
            List<Object> _ids = new ArrayList<Object>();
            if (ids instanceof List) {
                _ids = (List<Object>) ids;
            }
            if (ids instanceof Integer) {
                _ids.add(ids);
            }
            if (_model_ids.containsKey(db.getModelName())) {
                if (!_model_ids.containsValue(_ids))
                    _model_ids.get(db.getModelName()).addAll(_ids);
            } else {
                _model_ids.put(db.getModelName(), _ids);
            }
        }

        public List<OERelationData> getAll() {
            List<OERelationData> datas = new ArrayList<OFieldsHelper.OERelationData>();
            Set<String> keys = _models.keySet();
            for (String key : keys) {
                OModel db = (OModel) _models.get(key);
                datas.add(new OERelationData(db, _model_ids.get(key)));
            }
            return datas;
        }
    }

    public class OERelationData {
        OModel db;
        List<Object> ids;

        public OERelationData(OModel db, List<Object> ids) {
            super();
            this.db = db;
            this.ids = ids;
        }

        public OModel getDb() {
            return db;
        }

        public void setDb(OModel db) {
            this.db = db;
        }

        public List<Object> getIds() {
            return ids;
        }

        public void setIds(List<Object> ids) {
            this.ids = ids;
        }

    }

    public interface ValueWatcher {
        public OValues getValue(OColumn col, Object value);
    }
}
