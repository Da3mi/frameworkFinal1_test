package com.odoo.orm;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.ODomain;
import odoo.Odoo;
import odoo.OdooAccountExpireException;
import odoo.OdooInstance;
import odoo.OdooVersion;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.odoo.App;
import com.odoo.support.OUser;
import com.odoo.util.ODate;
import com.odoo.util.PreferenceManager;

public class OdooHelper {

	public static final String TAG = OdooHelper.class.getSimpleName();

    OUser mUser = null ;
	Context mContext = null;
	Boolean mForceConnect = false;
	Odoo mOdoo = null;
	App mApp = null;
    OModel mDatabase = null;
    List<Integer> mResultIds = new ArrayList<Integer>();
    List<ODataRow> mRemovedRecordss = new ArrayList<ODataRow>();

    PreferenceManager mPref = null;
    int mAffectedRows = 0;

	public OdooHelper(Context context) {
		mContext = context;
		mApp = (App) context.getApplicationContext();
        mOdoo = mApp.getOdoo() ;
        mUser = OUser.current(context);
        if (mUser != null) {
            mUser = login(mUser.getUsername(), mUser.getPassword(),
                    mUser.getDatabase(), mUser.getHost());
        }
	}

	public OdooHelper(Context context, Boolean forceConnect) {
		mContext = context;
		mForceConnect = forceConnect;
		mApp = (App) context.getApplicationContext();
        mUser = OUser.current(context);
        if (mOdoo == null && mUser != null)
            mUser = login(mUser.getUsername(), mUser.getPassword(),
                    mUser.getDatabase(), mUser.getHost());
	}

   public Object call_kw(String method, OArguments arguments) {
        return call_kw(method, arguments, new JSONObject());
    }

    public Object call_kw(String method, OArguments arguments,
                          JSONObject context) {
        return call_kw(null, method, arguments, context, null);
    }

    public Object call_kw(String method, OArguments arguments,
                          JSONObject context, JSONObject kwargs) {

        return call_kw(null, method, arguments, context, kwargs);
    }

    public Object call_kw(String model, String method, OArguments arguments,
                          JSONObject context, JSONObject kwargs) {
        Log.d(TAG, "OEHelper->call_kw()");
        JSONObject result = null;
        if (model == null) {

            model = mDatabase.getModelName();
        }
        try {
            if (context != null) {
                arguments.add(mOdoo.updateContext(context));
            }
            if (kwargs == null)
                result = mOdoo.call_kw(model, method, arguments.getArray());
            else
                result = mOdoo.call_kw(model, method, arguments.getArray(),
                        kwargs);
            return result.get("result");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
/*
    public boolean syncWithMethod(String method, OArguments args) {
        return syncWithMethod(method, args, false);
    }*/



    public int getAffectedRows() {
        return mAffectedRows;
    }
/*
    public boolean syncWithMethod(String method, OArguments args,
                                  boolean removeLocalIfNotExists) {
        Log.d(TAG, "OEHelper->syncWithMethod()");
        Log.d(TAG, "Model: " + mDatabase.getModelName());
        Log.d(TAG, "User: " + mUser.getAndroidName());
        Log.d(TAG, "Method: " + method);
        boolean synced = false;
        OFieldsHelper fields = new OFieldsHelper(
                mDatabase.getColumns());
        try {
            JSONObject result = mOdoo.call_kw(mDatabase.getModelName(),
                    method, args.getArray());
            if (result.getJSONArray("result").length() > 0)
                mAffectedRows = result.getJSONArray("result").length();
            synced = handleResultArray(fields, result.getJSONArray("result"),
                    false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return synced;
    }*/


    public OUser login(String username, String password, String database,
			String serverURL) {
		Log.d(TAG, "OHelper->login()");
		OUser userObj = null;
		try {
			mOdoo = new Odoo(serverURL, mForceConnect);
			JSONObject response = mOdoo.authenticate(username, password,
					database);
			int userId = 0;
			if (response.get("uid") instanceof Integer) {
				mApp.setOdooInstance(mOdoo);
				userId = response.getInt("uid");
				ODomain domain = new ODomain();
				domain.add("id", "=", userId);
				userObj = getUserDetail(domain, database, username, password,
						serverURL, userId, mForceConnect, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return userObj;
	}

    private boolean handleResultArray(OFieldsHelper fields, JSONArray results,
                                      boolean removeLocalIfNotExists) {
        boolean flag = false;
        try {
            fields.addAll(results);
            // Handling many2many and many2one records
            List<OFieldsHelper.OERelationData> rel_models = fields.getRelationData();
            for (OFieldsHelper.OERelationData rel : rel_models) {
                OdooHelper oe = rel.getDb().getOEInstance();
                //oe.syncWithServer(false, null, rel.getIds(), false, 0, false);
            }
            List<Integer> result_ids = mDatabase.createORReplace(
                    fields.getValues());
            mResultIds.addAll(result_ids);
            mRemovedRecordss.addAll(mDatabase.getRemovedRecords());
            if (result_ids.size() > 0) {
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public List<ODataRow> getRemovedRecords() {
        return mRemovedRecordss;
    }

  /*  public List<Long> getAffectedIds() {
        List<Long> ids = new ArrayList<Long>();
        for (Long id : mResultIds) {
            ids.add(Long.parseLong(id.toString()));
        }
        return ids;
    }*/

	public OUser instance_login(OdooInstance instance, String username,
			String password) throws OdooAccountExpireException {
		Log.d(TAG, "OHelper->instance_login()");
		OUser userObj = null;
		try {
			mOdoo = mApp.getOdoo();
			String odooServer = mOdoo.getServerURL();
			String odooDB = mOdoo.getDatabaseName();
			JSONObject response = mOdoo.oauth_authenticate(instance, username,
					password);
			int userId = 0;
			if (response.get("uid") instanceof Integer) {
				mApp.setOdooInstance(mOdoo);
				userId = response.getInt("uid");
				ODomain domain = new ODomain();
				domain.add("id", "=", userId);
				userObj = getUserDetail(domain, odooDB, username, password,
						odooServer, userId, false, instance);
			}
		} catch (OdooAccountExpireException e) {
			throw new OdooAccountExpireException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return userObj;
	}

	public OUser getUserDetail(ODomain domain, String database,
			String username, String password, String url, int userId,
			boolean mForceConnect, OdooInstance instance)
			throws OdooAccountExpireException {
		OUser userObj = null;
		try {
			OFieldsHelper fields = new OFieldsHelper(new String[] { "name",
					"partner_id", "tz", "image", "company_id" });
			JSONObject res = mOdoo
					.search_read("res.users", fields.get(), domain.get())
					.getJSONArray("records").getJSONObject(0);
			userObj = new OUser();
			userObj.setAvatar(res.getString("image"));
			userObj.setName(res.getString("name"));
			userObj.setDatabase(database);
			userObj.setHost(url);
			userObj.setIsactive(true);
			userObj.setAndroidName(generateOdooName(username,
					(instance != null) ? instance.getDatabaseName() : database));
			userObj.setPartner_id(res.getJSONArray("partner_id").getInt(0));
			userObj.setTimezone(res.getString("tz"));
			userObj.setUser_id(userId);
			userObj.setUsername(username);
			userObj.setPassword(password);
			userObj.setAllowSelfSignedSSL(mForceConnect);
			String company_id = new JSONArray(res.getString("company_id"))
					.getString(0);
			userObj.setCompany_id(company_id);
			userObj.setOAuthLogin(false);
			if (instance != null) {
				userObj.setOAuthLogin(true);
				userObj.setInstanceDatabase(instance.getDatabaseName());
				userObj.setInstanceUrl(instance.getInstanceUrl());
				userObj.setClientId(instance.getClientId());
			}
			OdooVersion odooVersion = mApp.getOdooVersion();
			userObj.setVersion_number(odooVersion.getVersion_number());
			userObj.setVersion_serie(odooVersion.getServer_serie());

		} catch (Exception e) {
			e.printStackTrace();
		}
		return userObj;
	}

    public Odoo Odooo() {
        return mOdoo;
    }

    public List<Integer> getAffectedIds() {
        List<Integer> ids = new ArrayList<Integer>();
        for (Integer id : mResultIds) {
            ids.add(Integer.parseInt(id.toString()));
        }
        return ids;
    }

    public OUser getUser() {
        return mUser;
    }

    public List<OdooInstance> getUserInstances(OUser user) {
		List<OdooInstance> list = new ArrayList<OdooInstance>();
		mOdoo = mApp.getOdoo();
		try {
			OArguments args = new OArguments();
			for (OdooInstance instance : mOdoo.get_instances(args.get())) {
				if (instance.isSaas()) {
					list.add(instance);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	private String generateOdooName(String username, String database) {
		return username + "[" + database + "]";
	}
}
