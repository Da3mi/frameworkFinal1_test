package com.odoo.addons.message;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.*;
import com.odoo.addons.message.providers.message.MessageProvider;
import com.odoo.auth.OdooAccountManager;
import com.odoo.base.ir.Attachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OEM2MIds;
import com.odoo.orm.OFieldsHelper;
import com.odoo.orm.OM2MRecord.Operation;
import com.odoo.orm.OM2MRecord;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.orm.OdooHelper;
import com.odoo.support.OUser;
import com.odoo.support.listview.OListAdapter;
import com.odoo.util.Base64Helper;
import com.odoo.util.ODate;
import com.odoo.util.controls.ExpandableHeightGridView;
import com.odoo.util.tags.TagsView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.OArguments;
import odoo.ODomain;
import odoo.Odoo;
import odoo.controls.MultiTagsTextView;

/**
 * Created by daami on 15/08/14.
 */


public class MessageComposeActivity extends Activity implements MultiTagsTextView.TokenListener,
        TextWatcher {

    public static final String TAG = "com.openerp.addons.message.MessageComposeActivity";
    public static final Integer PICKFILE_RESULT_CODE = 1;
    Context mContext = null;

    Boolean hasSynced = false;
    Boolean isReply = false;
    Boolean isQuickCompose = false;
    Integer mParentMessageId = 0;
    ODataRow mMessageRow = null;
    MailGroup mailGroup ;
    ODataRow mNoteObj = null;
    OdooHelper mOE = null;
    App mApp = null ;
    Odoo mOdoo = null ;

    HashMap<String, ODataRow> mSelectedPartners = new HashMap<String, ODataRow>();
    HashMap<String, ODataRow> mSelectedGroups = new HashMap<String, ODataRow>();

    /**
     * DBs
     */
    ResPartner mPartnerDB = null;
    MessageDB mMessageDB = null;
    MailGroupDB mMailGroupDB = null;
    PartnerLoader mPartnerLoader = null;
    GroupsLoader   mGroupsLoader = null;

    /**
     * Attachment
     */
    Attachment mAttachment = null;
    List<Object> mAttachments = new ArrayList<Object>();
    ExpandableHeightGridView mAttachmentGridView = null;
    OListAdapter mAttachmentAdapter = null;

    enum AttachmentType {
        IMAGE, FILE
    }

    /**
     * Controls & Adapters
     */
    TagsView mPartnerTagsView = null;
    TagsView mGroupeTagsView = null;
    OListAdapter mPartnerTagsAdapter = null;
    OListAdapter mGroupsTagsAdapter = null;
    List<Object> mTagsPartners = new ArrayList<Object>();
    List<Object> mTagsGroups = new ArrayList<Object>();
    List<Object> mTagsLocalPartners = new ArrayList<Object>();
    EditText edtSubject = null, edtBody = null, edtEmail = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_compose);
        mContext = this;
        getActionBar().setIcon(R.drawable.icon);
        if (OUser.current(mContext) == null) {
            // No Account
            Toast.makeText(mContext, "No Account Found", Toast.LENGTH_LONG)
                    .show();
            finish();
        } else {
            initDBs();
            initActionbar();
            handleIntent();
            init_Partner_Controls();
            init_Groups_Controls();
            checkForContact();
            checkForGroups();
        }
    }

    private void init_Partner_Controls() {
        if (!isReply) {
            mPartnerLoader = new PartnerLoader();

        }

        mPartnerTagsView = (TagsView) findViewById(R.id.receipients_view);
        mPartnerTagsView.addTextChangedListener(this);
        mPartnerTagsView.setCustomTagView(new TagsView.CustomTagViewListener() {

            @Override
            public View getViewForTags(LayoutInflater layoutInflater,
                                       Object object, ViewGroup tagsViewGroup) {
                ODataRow row = (ODataRow) object;
                View mView = layoutInflater.inflate(
                        R.layout.fragment_message_receipient_tag_layout, null);
                TextView txvSubject = (TextView) mView
                        .findViewById(R.id.txvTagSubject);
                txvSubject.setText(row.getString("name"));
                ImageView imgPic = (ImageView) mView
                        .findViewById(R.id.imgTagImage);
                if (!row.getString("image_small").equals("false")) {
                    imgPic.setImageBitmap(Base64Helper.getBitmapImage(
                            getApplicationContext(),
                            row.getString("image_small")));
                }
                return mView;
            }
        });


        mPartnerTagsAdapter = new OListAdapter(this,
                R.layout.tags_view_partner_item_layout, mTagsPartners) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View mView = convertView;
                if (mView == null) {
                    mView = getLayoutInflater().inflate(getResource(), parent,
                            false);
                }
                ODataRow row = (ODataRow) mTagsPartners.get(position);

                TextView txvSubject = (TextView) mView
                        .findViewById(R.id.txvSubject);
                TextView txvSubSubject = (TextView) mView
                        .findViewById(R.id.txvSubSubject);
                ImageView imgPic = (ImageView) mView
                        .findViewById(R.id.imgReceipientPic);
                txvSubject.setText(row.getString("name"));
                if (!row.getString("email").equals("false")) {
                    txvSubSubject.setText(row.getString("email"));
                } else {
                    txvSubSubject.setText("No email");
                }
                if (!row.getString("image_small").equals("false")) {
                    imgPic.setImageBitmap(Base64Helper.getBitmapImage(mContext,
                            row.getString("image_small")));
                } else {
                    imgPic.setImageResource(R.drawable.ic_action_user);
                }
                return mView;
            }
        };
        mPartnerTagsAdapter
                .setRowFilterTextListener(new OListAdapter.RowFilterTextListener() {

                    @Override
                    public String filterCompareWith(Object object) {
                        ODataRow row = (ODataRow) object;
                        return row.getString("name") + ";"
                                + row.getString("email");
                    }
                });
        mPartnerTagsView.setAdapter(mPartnerTagsAdapter);
        mPartnerTagsView.setPrefix("To: ");
        mPartnerTagsView.allowDuplicates(false);
        mPartnerTagsView.setTokenListener(this);

        // Attachment View
        mAttachmentGridView = (ExpandableHeightGridView) findViewById(R.id.lstAttachments);
        mAttachmentGridView.setExpanded(true);
        mAttachmentAdapter = new OListAdapter(this,
                R.layout.activity_message_compose_attachment_file_view_item,
                mAttachments) {
            @Override
            public View getView(final int position, View convertView,
                                ViewGroup parent) {
                ODataRow row = (ODataRow) mAttachments.get(position);
                View mView = convertView;
                if (mView == null)
                    mView = getLayoutInflater().inflate(getResource(), parent,
                            false);
                TextView txvFileName = (TextView) mView
                        .findViewById(R.id.txvFileName);
                txvFileName.setText(row.getString("name"));

                ImageView imgAttachmentImg = (ImageView) mView
                        .findViewById(R.id.imgAttachmentFile);
                if (!row.getString("file_type").contains("image")) {
                    imgAttachmentImg
                            .setImageResource(R.drawable.file_attachment);
                } else {
                    imgAttachmentImg.setImageURI(Uri.parse(row
                            .getString("file_uri")));
                }
                mView.findViewById(R.id.imgBtnRemoveAttachment)
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                mAttachments.remove(position);
                                mAttachmentAdapter
                                        .notifiyDataChange(mAttachments);
                            }
                        });
                return mView;
            }
        };
        mAttachmentGridView.setAdapter(mAttachmentAdapter);

        // Edittext
        edtEmail = (EditText) findViewById(R.id.edtMessageEmail);
        edtSubject = (EditText) findViewById(R.id.edtMessageSubject);
        edtBody = (EditText) findViewById(R.id.edtMessageBody);

    }


                    /* ***********trying to get groups tag*****/


    private void init_Groups_Controls() {
        if (!isReply) {
            mGroupsLoader = new GroupsLoader();

        }

        mGroupeTagsView = (TagsView) findViewById(R.id.receipients_view);
        mGroupeTagsView.addTextChangedListener(this);
        mGroupeTagsView.setCustomTagView(new TagsView.CustomTagViewListener() {


            @Override
            public View getViewForTags(LayoutInflater layoutInflater, Object object, ViewGroup tagsViewGroup) {

                ODataRow row = (ODataRow) object;
                View mView = layoutInflater.inflate(
                        R.layout.fragment_message_receipient_tag_layout, null);
                TextView txvSubject = (TextView) mView
                        .findViewById(R.id.txvTagSubject);
                txvSubject.setText(row.getString("name"));
                ImageView imgPic = (ImageView) mView
                        .findViewById(R.id.imgTagImage);
                if (!row.getString("image_small").equals("false")) {
                    imgPic.setImageBitmap(Base64Helper.getBitmapImage(
                            getApplicationContext(),
                            row.getString("image_small")));
                }
                return mView;
            }
        });

        mGroupsTagsAdapter = new OListAdapter(this,
                R.layout.tags_view_partner_item_layout, mTagsGroups) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View mView = convertView;
                if (mView == null) {
                    mView = getLayoutInflater().inflate(getResource(), parent,
                            false);
                }
                ODataRow row = (ODataRow) mTagsGroups.get(position);
                TextView txvSubject = (TextView) mView
                        .findViewById(R.id.txvSubject);
                TextView txvSubSubject = (TextView) mView
                        .findViewById(R.id.txvSubSubject);
                ImageView imgPic = (ImageView) mView
                        .findViewById(R.id.imgReceipientPic);
                txvSubject.setText(row.getString("name"));

                if (!row.getString("image_small").equals("false")) {
                    imgPic.setImageBitmap(Base64Helper.getBitmapImage(mContext,
                            row.getString("image_small")));
                } else {
                    imgPic.setImageResource(R.drawable.ic_action_user);
                }
                return mView;
            }
        };
        mGroupsTagsAdapter
                .setRowFilterTextListener(new OListAdapter.RowFilterTextListener() {

                    @Override
                    public String filterCompareWith(Object object) {
                        ODataRow row = (ODataRow) object;
                        return row.getString("name") + ";"
                                + row.getString("description");
                    }
                });
        mGroupeTagsView.setAdapter(mGroupsTagsAdapter);
        // mGroupeTagsView.setPrefix("To: ");
        mGroupeTagsView.allowDuplicates(false);
        mGroupeTagsView.setTokenListener(this);

        // Edittext

    }

    class GroupsLoader extends AsyncTask<Void, Void, Void> {

        public GroupsLoader() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            mTagsGroups.clear();
            mTagsGroups.addAll(mailGroup.db().select());
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mGroupsTagsAdapter.notifiyDataChange(mTagsGroups);

        }
    }


    private void initDBs() {
        mPartnerDB = new ResPartner(mContext);
        mMessageDB = new MessageDB(mContext);
        OValues oValues = new OValues();
        oValues.put("subject","Helloooooo");
        oValues.put("body","hello montassar ,happy to see you ");
        mMessageDB.create(oValues);
        mMailGroupDB = new MailGroupDB(mContext);
        OValues oValues1 = new OValues();
        oValues1.put("name","Physicians");
        oValues1.put("description","Good work");
        mMailGroupDB.create(oValues1);

       // mOE = mPartnerDB.getOEInstance();
       // mOdoo = mApp.getOdoo();
        mAttachment = new Attachment(mContext);
        mAttachments.clear();
    }
    private void checkForGroups() {
        Intent intent = getIntent();
        handleIntentFilter(intent);
        if(intent.getData() !=null){
            Cursor cursor = getContentResolver().query(intent.getData(),null,null,null,null);
            if(cursor !=null && cursor.moveToFirst()){
                int Groupe_id = cursor.getInt(cursor.getColumnIndex("data2"));
                ODataRow row = mMailGroupDB.select(Groupe_id);
                mSelectedGroups.put("Key_" + row.getString("id"),row);
                mGroupeTagsView.addObject(row);
                isQuickCompose = true;

            }
        }
    }
    private void checkForContact() {
        Intent intent = getIntent();
        handleIntentFilter(intent);
        if (intent.getData() != null) {
            Cursor cursor = getContentResolver().query(intent.getData(), null,
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int partner_id = cursor.getInt(cursor.getColumnIndex("data2"));
                ODataRow row = mPartnerDB.select(partner_id);
                mSelectedPartners.put("key_" + row.getString("id"), row);
                mPartnerTagsView.addObject(row);
                isQuickCompose = true;
            }
        }

        if (isReply) {
            mMessageRow = mMessageDB.select(mParentMessageId);
            List<ODataRow> partners = mMessageRow.getM2MRecord("partner_ids")
                    .browseEach();
            if (partners != null) {
                for (ODataRow partner : partners) {
                    mSelectedPartners.put("key_" + partner.getString("id"),
                            partner);
                    mPartnerTagsView.addObject(partner);
                }
            }
            edtSubject.setText("Re: " + mMessageRow.getString("subject"));
            edtBody.requestFocus();
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        String title = "Compose";
        if (intent.hasExtra("send_reply")) {
            isReply = true;
            mParentMessageId = intent.getExtras().getInt("message_id");
            title = "Reply";
        }
        setTitle(title);
    }

    private void initActionbar() {
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_message_compose_activty, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_message_compose_add_attachment_images:
                mAttachment
                        .requestAttachment(Attachment.Types.IMAGE_OR_CAPTURE_IMAGE);
                return true;
            case R.id.menu_message_compose_add_attachment_files:
                mAttachment.requestAttachment(Attachment.Types.FILE);
                return true;
            case R.id.menu_message_compose_send:
                SendMessage sendMessage = new SendMessage();
                edtSubject.setError(null);
                edtBody.setError(null);
                if (mSelectedPartners.size() == 0)
                {Toast.makeText(mContext, "sending message to Followers",
                        Toast.LENGTH_LONG).show();
                    sendMessage.execute();}
                else if (TextUtils.isEmpty(edtSubject.getText())) {
                    edtSubject.setError("Provide Message Subject !");
                } else if (TextUtils.isEmpty(edtBody.getText())) {
                    edtBody.setError("Provide Message Body !");
                } else {
                    Toast.makeText(this, "Sending message...", Toast.LENGTH_LONG)
                            .show();

                    sendMessage.execute();
                    if (isQuickCompose)
                        finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class SendMessage extends AsyncTask<Void, Void, Void> {
        OdooHelper mOE = null;
        OSyncHelper sync = null;
        boolean isConnection = true;
        boolean isempty = false ;
        String mToast = "";
        int newMessageId = 0;

        public SendMessage() {
            mOE = mMessageDB.getOEInstance();
            sync = mMessageDB.getSyncHelper();



        }

        @Override
        protected Void doInBackground(Void... params) {
            if (isConnection) {
                Object record_name = false, res_model = false;
                int res_id = 0;
                List<Integer> attachmentIds = new ArrayList<Integer>();
                if (mNoteObj == null) {
                    mAttachment.updateAttachments("mail.compose.message", 0,
                            mAttachments, false);
                    List<Long> lAttachmentIds = mAttachment.newAttachmentIds();
                    for (long id : lAttachmentIds)
                        attachmentIds.add(Integer.parseInt(id + ""));
                } /*else {
                    for (Object obj : mAttachments) {
                        ODataRow attachment = (ODataRow) obj;
                        attachmentIds.add(attachment.getInt("id"));
                    }
                    record_name = edtSubject.getText().toString();
                    res_model = "note.note";
                    res_id = mNoteObj.getInt("id");
                }*/
                try {

                    ODataRow user = new ResPartner(mContext).select(OUser
                            .current(mContext).getPartner_id());
                    OArguments args = new OArguments();

                    // Partners
                    JSONArray partners = new JSONArray();
                    List<Integer> partner_ids_list = new ArrayList<Integer>();
                    for (String key : mSelectedPartners.keySet()) {
                        partners.put(mSelectedPartners.get(key).getInt("id"));
                        partner_ids_list.add(mSelectedPartners.get(key).getInt(
                                "id"));
                    }
                    JSONArray partner_ids = new JSONArray();
                    if (partners.length() > 0) {
                        partner_ids.put(6);
                        partner_ids.put(false);
                        partner_ids.put(partners);
                    }

                    // attachment ids
                    JSONArray attachment_ids = new JSONArray();
                    if (attachmentIds.size() > 0) {
                        attachment_ids.put(6);
                        attachment_ids.put(false);
                        attachment_ids.put(new JSONArray(attachmentIds
                                .toString()));
                    }
                    if (!isReply) {
                        mToast = "Message sent.";
                        JSONObject arguments = new JSONObject();
                        arguments.put("composition_mode", "comment");
                        arguments.put("model", res_model);
                        arguments.put("parent_id", false);
                        String email_from = user.getString("name") + " <"
                                + user.getString("email") + ">";
                        arguments.put("email_from", email_from);
                        arguments.put("subject", edtSubject.getText()
                                .toString());
                        arguments.put("body", edtBody.getText().toString());
                        arguments.put("post", true);
                        arguments.put("notify", false);
                        arguments.put("same_thread", true);
                        arguments.put("use_active_domain", false);
                        arguments.put("reply_to", false);
                        arguments.put("res_id", res_id);
                        arguments.put("record_name", record_name);

                        if (partner_ids.length() > 0)
                            arguments.put("partner_ids", new JSONArray("["
                                    + partner_ids.toString() + "]"));
                        else
                            arguments.put("partner_ids", new JSONArray());

                        if (attachment_ids.length() > 0)
                            arguments.put("attachment_ids", new JSONArray("["
                                    + attachment_ids.toString() + "]"));
                        else
                            arguments.put("attachment_ids", new JSONArray());
                        arguments.put("template_id", false);

                        JSONObject kwargs = new JSONObject();
                        kwargs.put("context",
                                mOE.Odooo().updateContext(new JSONObject()));

                        args.add(arguments);
                        String model = "mail.compose.message";

                        // Creating compose message
                        int id = (Integer) sync.callMethod("create",args,null,kwargs);

                        // Resetting kwargs
                        args = new OArguments();
                        args.add(new JSONArray().put(id));
                        args.add(mOE.Odooo().updateContext(new JSONObject()));

                        // Sending mail
                       sync.callMethod("send_mail", args,null, null);
                        syncMessage();
                    } else {
                        mToast = "Message reply sent.";
                        String model = "mail.thread";
                        String method = "message_post";
                        args = new OArguments();
                        args.add(false);

                        JSONObject context = new JSONObject();
                        res_id = mMessageRow.getInt("res_id");
                        res_model = mMessageRow.getString("model");
                        context.put("default_model",
                                (res_model.equals("false") ? false : res_model));
                        context.put("default_res_id", (res_id == 0) ? false
                                : res_id);
                        context.put("default_parent_id", mParentMessageId);
                        context.put("mail_post_autofollow", true);
                        context.put("mail_post_autofollow_partner_ids",
                                new JSONArray());

                        JSONObject kwargs = new JSONObject();
                        kwargs.put("context", context);
                        kwargs.put("subject", edtSubject.getText().toString());
                        kwargs.put("body", edtBody.getText().toString());
                        kwargs.put("parent_id", mParentMessageId);
                        kwargs.put("attachment_ids", new JSONArray(
                                attachmentIds));
                        if (partner_ids.length() > 0)
                            kwargs.put("partner_ids", new JSONArray("["
                                    + partner_ids.toString() + "]"));
                        else
                            kwargs.put("partner_ids", new JSONArray());
                        newMessageId = (Integer) sync.callMethod( method,
                                args, null, kwargs);
                        // Creating local entry
                        OValues values = new OValues();

                        OEM2MIds partnerIds = new OEM2MIds(OEM2MIds.Operation.ADD,
                                partner_ids_list);
                        values.put("id", newMessageId);
                        values.put("partner_ids", partnerIds);
                        values.put("subject", edtSubject.getText().toString());
                        values.put("type", "comment");
                        values.put("body", edtBody.getText().toString());
                        values.put("email_from", false);
                        values.put("parent_id", mParentMessageId);
                        values.put("record_name", false);
                        values.put("to_read", false);
                        values.put("author_id", user.getInt("id"));
                        values.put("model", res_model);
                        values.put("res_id", res_id);
                        values.put("date", ODate.getDate());
                        values.put("has_voted", false);
                        values.put("vote_nb", 0);
                        values.put("starred", false);
                        OEM2MIds attachment_Ids = new OEM2MIds(OEM2MIds.Operation.ADD,
                                attachmentIds);
                        values.put("attachment_ids", attachment_Ids);
                        newMessageId = (int) mMessageDB.create(values);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isConnection) {
                Toast.makeText(mContext, "No Connection", Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(mContext, mToast, Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.putExtra("new_message_id", newMessageId);
                setResult(RESULT_OK, intent);
                finish();
            }
        }

    }

    public void syncMessage() {
        Bundle bundle = new Bundle();
        Account account =
                OdooAccountManager.getAccount(
                getApplicationContext(), OUser
                        .current(getApplicationContext()).getAndroidName());
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        if (bundle != null) {
            settingsBundle.putAll(bundle);
        }
        ContentResolver.requestSync(account, MessageProvider.AUTHORITY,
                settingsBundle);
    }

    // TODO : load local partners ???
    class LocalPartnerLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mTagsLocalPartners.clear();
            ResPartner partners = new ResPartner(mContext);
            if (!partners.isEmptyTable())
                mTagsLocalPartners.addAll(partners.select());
            return null;
        }
    }

    /**
     * TO DO : load mail Groups
     */


    class PartnerLoader extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(final String...searchFor) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    String filter = searchFor[0];
                    mTagsPartners.clear();
                    // Loading records from server
                    if (mOE != null) {
                        try {
                            OFieldsHelper fields = new OFieldsHelper(
                                    mPartnerDB.getDatabaseServerColumns());
                            ODomain domain = new ODomain();
                            domain.add("|");
                            domain.add("name", "=ilike", filter + "%");
                            domain.add("email", "=ilike", filter + "%");
                            JSONObject result = mOE.Odooo().search_read(
                                    mPartnerDB.getModelName(), fields.get(),
                                    domain.get());
                            for (int i = 0; i < result.getJSONArray("records")
                                    .length(); i++) {
                                JSONObject rec = result.getJSONArray("records")
                                        .getJSONObject(i);

                                ODataRow row = new ODataRow();
                                row.put("id", rec.getInt("id"));
                                row.put("name", rec.getString("name"));
                                row.put("email", rec.getString("email"));
                                row.put("image_small",
                                        rec.getString("image_small"));

                                mTagsPartners.add(row);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mPartnerTagsAdapter.notifiyDataChange(mTagsPartners);
        }

    }

    @Override
    public void onTokenAdded(Object token, View view) {
        ODataRow row = (ODataRow) token;
        mSelectedPartners.put("key_" + row.getString("id"), row);
    }

    @Override
    public void onTokenSelected(Object token, View view) {

    }

    @Override
    public void onTokenRemoved(Object token) {
        ODataRow row = (ODataRow) token;
        if (!isReply)
            mSelectedPartners.remove("key_" + row.getString("id"));
        else
            mPartnerTagsView.addObject(token);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            ODataRow attachment = mAttachment.handleResult(requestCode, data);
            mAttachments.add(attachment);
            mAttachmentAdapter.notifiyDataChange(mAttachments);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Handle message intent filter for attachments
     *
     * @param intent
     */
    private void handleIntentFilter(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        // Single attachment
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            ODataRow single = mAttachment.handleResult(intent);
            single.put("file_type", type);
            mAttachments.add(single);
            mAttachmentAdapter.notifiyDataChange(mAttachments);
        }

        // Multiple Attachments
        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            List<ODataRow> multiple = mAttachment.handleMultipleResult(intent);
            mAttachments.addAll(multiple);
            mAttachmentAdapter.notifiyDataChange(mAttachments);
        }
    /*    // note.note send as mail
        if (intent.getAction() != null
                && intent.getAction().equals(
                NoteDetail.ACTION_FORWARD_NOTE_AS_MAIL)) {
            edtSubject = (EditText) findViewById(R.id.edtMessageSubject);
            edtBody = (EditText) findViewById(R.id.edtMessageBody);

            NoteDB note = new NoteDB(mContext);
            int note_id = intent.getExtras().getInt("note_id");
            List<ODataRow> attachment = mAttachment.select(
                    note.getModelName(), note_id);
            mNoteObj = note.select(note_id);
            edtSubject.setText("I've shared note with you.");
            edtBody.setText(HTMLHelper.stringToHtml(mNoteObj.getString("name")));
            if (attachment.size() > 0) {
                mAttachments.addAll(attachment);
                mAttachmentAdapter.notifiyDataChange(mAttachments);
            }
        }
*/
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            edtBody.setText(intent.getExtras().getString(Intent.EXTRA_TEXT));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPartnerLoader != null)
            mPartnerLoader.cancel(true);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().length() >= 7 && s.toString().length() <= 9
                && before == 0) {
            String filter = s.toString().replace("To: ", "");
            if (mPartnerLoader != null)
                mPartnerLoader.cancel(true);
            mPartnerLoader = new PartnerLoader();
            mPartnerLoader.execute(new String[] { filter });
        } else {
            if (mPartnerLoader != null)
                mPartnerLoader.cancel(true);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}


