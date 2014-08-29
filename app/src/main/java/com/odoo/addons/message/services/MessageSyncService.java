package com.odoo.addons.message.services;

import android.accounts.Account;
import android.app.ActivityManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import com.odoo.App;
import com.odoo.MainActivity;
import com.odoo.R;
import com.odoo.addons.message.MessageDB;
import com.odoo.addons.message.widgets.MessageWidget;
import com.odoo.auth.OdooAccountManager;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.orm.OdooHelper;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.OUser;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.service.OService;
import com.odoo.util.ODate;
import com.odoo.util.OENotificationHelper;
import com.odoo.util.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by daami on 15/08/14.
 */
public class MessageSyncService extends OService {
    /** The Constant TAG. */
    public static final String TAG = MessageSyncService.class.getSimpleName();

    /** The s sync adapter. */
   // private static SyncAdapterImpl sSyncAdapter = null;

    /** The i. */
    static int i = 0;

    /** The context. */
    Context mContext = null;
     /**
     * Instantiates a new message sync service.
     */
    public MessageSyncService() {
        super();
        mContext = this;
      
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onBind(android.content.Intent)
     */
    /*@Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }
*/
    /**
     * Gets the sync adapter.
     *
     * @return the sync adapter
     */
   /* public SyncAdapterImpl getSyncAdapter() {
        if (sSyncAdapter == null) {
            sSyncAdapter = new SyncAdapterImpl(this);
        }
        return sSyncAdapter;
    }*/

    @Override
    public Service getService() {
        return this;
    }

    /**
     * Perform sync.
     *
     * @param context
     *            the context
     * @param account
     *            the account
     * @param extras
     *            the extras
     * @param authority
     *            the authority
     * @param provider
     *            the provider
     * @param syncResult
     *            the sync result
     */
    public void performSync(Context context, Account account, Bundle extras,
                            String authority, ContentProviderClient provider,
                            SyncResult syncResult) {



        Log.v(TAG, "PartnersService:performSync()");
        try{
            OSyncHelper sync = null;
            Intent intent= new Intent();
            intent.setAction(SyncFinishReceiver.SYNC_FINISH);
            MessageDB message =  new MessageDB(context);
            sync= message.getSyncHelper().syncDataLimit(30);
            if(sync.syncWithServer()){
                context.sendBroadcast(intent);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        /*Intent intent = new Intent();
        Intent updateWidgetIntent = new Intent();
        OSyncHelper sync = null ;
        updateWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateWidgetIntent.putExtra(MessageWidget.ACTION_MESSAGE_WIDGET_UPDATE,
                true);
        intent.setAction(SyncFinishReceiver.SYNC_FINISH);
        OUser user = OdooAccountManager.getAccountDetail(context,
                account.name);
        try {

            MessageDB msgDb = new MessageDB(context);
            msgDb.setUser(user);
            sync = msgDb.getSyncHelper().syncDataLimit(30);
            OdooHelper oe = msgDb.getOEInstance();
            if (oe == null) {
                return;
            }
            int user_id = user.getUser_id();

            // Updating User Context for OE-JSON-RPC
            JSONObject newContext = new JSONObject();
            newContext.put("default_model", "res.users");
            newContext.put("default_res_id", user_id);

            OArguments arguments = new OArguments();
            // Param 1 : ids
            arguments.addNull();
            // Param 2 : domain
            ODomain domain = new ODomain();

            // Data limit.
            PreferenceManager mPref = new PreferenceManager(context);
            int data_limit = mPref.getInt("sync_data_limit", 60);
            domain.add("create_date", ">=", ODate.getDateBefore(data_limit));

            if (!extras.containsKey("group_ids")) {
                // Last id
                JSONArray msgIds = new JSONArray();
                for (ODataRow row : msgDb.select()) {
                    msgIds.put(row.getInt("id"));
                }
                domain.add("id", "not in", msgIds);

                domain.add("|");
                // Argument for check partner_ids.user_id is current user
                domain.add("partner_ids.user_ids", "in",
                        new JSONArray().put(user_id));

                domain.add("|");
                // Argument for check notification_ids.partner_ids.user_id
                // is
                // current user
                domain.add("notification_ids.partner_id.user_ids", "in",
                        new JSONArray().put(user_id));

                // Argument for check author id is current user
                domain.add("author_id.user_ids", "in",
                        new JSONArray().put(user_id));

            } else {
                JSONArray group_ids = new JSONArray(
                        extras.getString("group_ids"));

                // Argument for group model check
                domain.add("model", "=", "mail.group");

                // Argument for group model res id
                domain.add("res_id", "in", group_ids);
            }

            arguments.add(domain.getArray());
            // Param 3 : message_unload_ids
            arguments.add(new JSONArray());
            // Param 4 : thread_level
            arguments.add(true);
            // Param 5 : context
            arguments.add(oe.Odooo().updateContext(newContext));
            // Param 6 : parent_id
            arguments.addNull();
            // Param 7 : limit
            arguments.add(50);
            List<Integer> ids = msgDb.ids();
            if (oe.syncWithMethod("message_read", arguments)) {
                List<Integer> affected_ids = oe.getAffectedIds();
                int affected_rows = msgDb.select("to_read = ?",
                        new String[] { "true" }).size();
                boolean notification = true;
                ActivityManager am = (ActivityManager) context
                        .getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> taskInfo = am
                        .getRunningTasks(1);
                ComponentName componentInfo = taskInfo.get(0).topActivity;
                if (componentInfo.getPackageName().equalsIgnoreCase(
                        "com.odoo")) {
                    notification = false;
                }
                if (notification && affected_rows > 0) {
                    OENotificationHelper mNotification = new OENotificationHelper();
                    Intent mainActiivty = new Intent(context,
                            MainActivity.class);
                    mNotification.setResultIntent(mainActiivty, context);
                    mNotification.showNotification(context, affected_rows
                                    + " new messages", affected_rows
                                    + " new message received (OpneERP)", authority,
                            R.drawable.icon);
                }
                intent.putIntegerArrayListExtra("new_ids",
                        (ArrayList<Integer>) affected_ids);
            }
            List<Integer> updated_ids = updateOldMessages(msgDb, oe, user, ids);
            intent.putIntegerArrayListExtra("updated_ids",
                    (ArrayList<Integer>) updated_ids);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (user.getAndroidName().equals(account.name) && sync.syncWithServer()) {
            context.sendBroadcast(intent);
            context.sendBroadcast(updateWidgetIntent);

        }*/
    }

   /* private List<Integer> updateOldMessages(MessageDB db, OdooHelper oe,
                                            OUser user, List<Integer> ids) {
        Log.d(TAG, "MessageSyncServide->updateOldMessages()");
        List<Integer> updated_ids = new ArrayList<Integer>();
        try {
            JSONArray ids_array = new JSONArray();
            for (int id : ids)
                ids_array.put(id);
            JSONObject fields = new JSONObject();

            fields.accumulate("fields", "read");
            fields.accumulate("fields", "starred");
            fields.accumulate("fields", "partner_id");
            fields.accumulate("fields", "message_id");

            ODomain domain = new ODomain();
            domain.add("message_id", "in", ids_array);
            domain.add("partner_id", "=", user.getPartner_id());
            JSONObject result = mOdoo.search_read("mail.notification",
                    fields, domain.get());
            for (int j = 0; j < result.getJSONArray("records").length(); j++) {
                JSONObject objRes = result.getJSONArray("records")
                        .getJSONObject(j);
                int message_id = objRes.getJSONArray("message_id").getInt(0);
                boolean read = objRes.getBoolean("read");
                boolean starred = objRes.getBoolean("starred");
                OValues values = new OValues();
                values.put("starred", starred);
                values.put("to_read", !read);
                db.update(values, message_id);
                updated_ids.add(message_id);
            }
            updateMessageVotes(db, oe, user, ids_array);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updated_ids;
    }

    private void updateMessageVotes(MessageDB db, OdooHelper oe, OUser user,
                                    JSONArray ids_array) {
        Log.d(TAG, "MessageSyncServide->updateMessageVotes()");
        try {
            JSONObject vote_fields = new JSONObject();
            vote_fields.accumulate("fields", "vote_user_ids");

            ODomain domain = new ODomain();
            domain.add("id", "in", ids_array);
            JSONObject vote_detail = oe.Odooo().search_read("mail.message",
                    vote_fields, domain.get(), 0, 0, null, null);
            for (int j = 0; j < vote_detail.getJSONArray("records").length(); j++) {
                JSONObject obj_vote = vote_detail.getJSONArray("records")
                        .getJSONObject(j);
                JSONArray voted_user_ids = obj_vote
                        .getJSONArray("vote_user_ids");
                OValues values = new OValues();
                for (int i = 0; i < voted_user_ids.length(); i++) {
                    if (voted_user_ids.getInt(i) == user.getUser_id()) {
                        values.put("has_voted", true);
                        break;
                    } else {
                        values.put("has_voted", false);
                    }
                }
                int total_votes = voted_user_ids.length();
                int message_id = obj_vote.getInt("id");
                values.put("vote_nb", total_votes);
                db.update(values, message_id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    *//**
     * The Class SyncAdapterImpl.
     *//*
    public class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

        *//** The m context. *//*
        private Context mContext;

        *//**
         * Instantiates a new sync adapter impl.
         *
         * @param context
         *            the context
         *//*
        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle bundle, String str,
                                  ContentProviderClient providerClient, SyncResult syncResult) {
            if (account != null) {
                new MessageSyncService().performSync(mContext, account, bundle,
                        str, providerClient, syncResult);
            }

        }

    }*/
}
