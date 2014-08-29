package com.odoo.addons.message.services;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.odoo.addons.message.MailGroupDB;
import com.odoo.addons.message.MessageDB;
import com.odoo.addons.message.providers.message.MessageProvider;
import com.odoo.auth.OdooAccountManager;
import com.odoo.base.mail.MailFollowers;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OdooHelper;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.service.OService;

import org.json.JSONArray;



/**
 * Created by daami on 15/08/14.
 */
public class MailGroupSyncService extends OService {

    public static final String TAG = MailGroupSyncService.class.getSimpleName();
     //   private static SyncAdapterImpl sSyncAdapter = null;
        static int i = 0;
        Context mContext = null;

        public MailGroupSyncService() {
            mContext = this;
        }

       /* @Override
        public IBinder onBind(Intent intent) {
            IBinder ret = null;
            ret = getSyncAdapter().getSyncAdapterBinder();
            return ret;
        }*/

    /*    public SyncAdapterImpl getSyncAdapter() {

            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapterImpl(this);
            }
            return sSyncAdapter;
        }*/

    @Override
    public Service getService() {
        return this;
    }

    public void performSync(Context context, Account account, Bundle extras,
                                String authority, ContentProviderClient provider,
                                SyncResult syncResult) {
/*
            try {
                Intent intent = new Intent();
                intent.setAction(SyncFinishReceiver.SYNC_FINISH);

                MailGroupDB db = new MailGroupDB(context);
                db.setUser(OdooAccountManager.getAccountDetail(context,
                        account.name));
                OdooHelper oe = db.getOEInstance();
                OSyncHelper sync = null ;
                 sync = db.getSyncHelper().syncDataLimit(30);

                if (oe != null && sync.syncWithServer()) {
                    MailFollowers followers = new MailFollowers(context);
                    sync = followers.getSyncHelper().syncDataLimit(30);
                    ODomain domain = new ODomain();
                    domain.add("partner_id", "=", oe.getUser().getPartner_id());
                    domain.add("res_model", "=", db.getModelName());
                        if(sync.syncWithServer(domain,true))
                   {
                        // syncing group messages
                        JSONArray group_ids = new JSONArray();
                        for (ODataRow grp : followers.select(
                                "res_model = ? AND partner_id = ?", new String[] {
                                        db.getModelName(),
                                        oe.getUser().getPartner_id() + "" })) {
                            group_ids.put(grp.getInt("res_id"));
                        }
                        Bundle messageBundle = new Bundle();
                        messageBundle.putString("group_ids", group_ids.toString());
                        messageBundle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        messageBundle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                        ContentResolver.requestSync(account,
                                MessageProvider.AUTHORITY, messageBundle);

                    }
                }
                if (OdooAccountManager.currentUser(context).getAndroidName()
                        .equals(account.name))
                    context.sendBroadcast(intent);

            } catch (Exception e) {
                e.printStackTrace();
            }*/

            Log.v(TAG, "PartnersService:performSync()");
            try{
                OSyncHelper sync = null ;
                Intent intent= new Intent();
                intent.setAction(SyncFinishReceiver.SYNC_FINISH);
                MailGroupDB mailgrp =  new MailGroupDB(context);
                sync= mailgrp.getSyncHelper().syncDataLimit(60);
                if(sync.syncWithServer()){
                    context.sendBroadcast(intent);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }

        /*public class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
            private Context mContext;

            public SyncAdapterImpl(Context context) {
                super(context, true);
                mContext = context;
            }*/

           /* @Override
            public void onPerformSync(Account account, Bundle bundle, String str,
                                      ContentProviderClient providerClient, SyncResult syncResult) {
                Log.d(TAG, "Mail Group sync service started");
                try {
                    if (account != null) {
                        new MailGroupSyncService().performSync(mContext, account,
                                bundle, str, providerClient, syncResult);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }*/
        }


