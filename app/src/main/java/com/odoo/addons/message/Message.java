package com.odoo.addons.message;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener ;

import com.odoo.R;
import com.odoo.addons.message.providers.message.MessageProvider;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.orm.OdooHelper;
import com.odoo.receivers.DataSetChangeReceiver;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.FragmentListener;
import com.odoo.support.listview.OListAdapter;
import com.odoo.util.ODate;
import com.odoo.util.drawer.DrawerItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import com.odoo.util.drawer.DrawerListener;
import com.openerp.OESwipeListener.SwipeCallbacks;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;

import org.json.JSONArray;
import org.json.JSONObject;

import odoo.OArguments;
import odoo.controls.OList;

/**
 * Created by daami on 15/08/14.
 */
public class Message extends BaseFragment implements OnPullListener, OnItemLongClickListener,
        OnItemClickListener,SwipeCallbacks, OnScrollListener, OnTouchListener,OList.OnListBottomReachedListener{


    public static final String TAG = "com.odoo.addons.message.Message";

    /**
     * On bottom reached.
     *
     * @param record_limit  the record_limit
     * @param record_offset
     */
    @Override
    public void onBottomReached(Integer record_limit, Integer record_offset) {

    }

    /**
     * Show loader.
     *
     * @return the boolean
     */
    @Override
    public Boolean showLoader() {
        return null;
    }

    private enum MType {
        INBOX, TOME, TODO, ARCHIVE, GROUP
    }

    Integer mRecentSwiped = -1;
    Integer mGroupId = null;
    Integer mSelectedItemPosition = -1;
    Integer selectedCounter = 0;
    MType mType = MType.INBOX;
    String mCurrentType = "inbox";
    View mView = null;
    SearchView mSearchView = null;
    OETouchListener mTouchAttacher;
    ActionMode mActionMode;
    @SuppressLint("UseSparseArrays")
    HashMap<Integer, Boolean> mMultiSelectedRows = new HashMap<Integer, Boolean>();
    OListAdapter mListViewAdapter = null;
    ListView mListView = null;
    List<Object> mMessageObjects = new ArrayList<Object>();
    Integer tag_color_count = 0;
    Boolean isSynced = false;

    /**
     * Background data operations
     */
    MessagesLoader mMessageLoader = null;
    StarredOperation mStarredOperation = null;
    ReadUnreadOperation mReadUnreadOperation = null;

    HashMap<String, Integer> message_row_indexes = new HashMap<String, Integer>();
    HashMap<String, Integer> message_model_colors = new HashMap<String, Integer>();

    int[] background_resources = new int[] {
            R.drawable.message_listview_bg_toread_selector,
            R.drawable.message_listview_bg_tonotread_selector };

    int[] starred_drawables = new int[] { R.drawable.ic_action_starred,
            R.drawable.ic_action_unstarred };

    String tag_colors[] = new String[] { "#A4C400", "#00ABA9", "#1BA1E2",
            "#AA00FF", "#D80073", "#A20025", "#FA6800", "#6D8764", "#76608A",
            "#EBB035" };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mSelectedItemPosition = savedInstanceState.getInt(
                    "mSelectedItemPosition", -1);
        }
        mView = inflater.inflate(R.layout.fragment_message, container, false);
        scope = new AppScope(getActivity());
        init();
        return mView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_fragment_message, menu);
        mSearchView = (SearchView) menu.findItem(R.id.menu_message_search)
                .getActionView();
        // Hiding compose menu for group messages.
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("group_id")) {
            MenuItem compose = menu.findItem(R.id.menu_message_compose);
            compose.setVisible(true);
        }

    }

    private void init() {
        Log.d(TAG, "Message->init()");
        mListView = (ListView) mView.findViewById(R.id.lstMessages);
        mListViewAdapter = new OListAdapter(getActivity(),
                R.layout.fragment_message_listview_items, mMessageObjects) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View mView = convertView;
                if (mView == null)
                    mView = getActivity().getLayoutInflater().inflate(
                            getResource(), parent, false);
                mView = handleRowView(mView, position);
                return mView;
            }
        };
        mListView.setAdapter(mListViewAdapter);
        initData();
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setMultiChoiceModeListener(mMessageViewMultiChoiceListener);
        mTouchAttacher = scope.main().getTouchAttacher();
        mTouchAttacher.setPullableView(mListView, this);
        mTouchAttacher.setSwipeableView(mListView, this);
        mListView.setOnScrollListener(this);
        mView.setOnTouchListener(this);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_message_compose:
                getActivity().startActivity(
                        new Intent(getActivity(), MessageComposeActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initData() {
        Log.d(TAG, "Message->initData()");
        if (mSelectedItemPosition > -1) {
            return;
        }
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (bundle.containsKey("type")) {
                mCurrentType = bundle.getString("type");
                String title = "Archive";
                if (mCurrentType.equals("inbox")) {
                    mMessageLoader = new MessagesLoader(MType.INBOX);
                    mMessageLoader.execute((Void) null);
                    title = "Inbox";
                } else if (mCurrentType.equals("to-me")) {
                    title = "To-Me";
                    mMessageLoader = new MessagesLoader(MType.TOME);
                    mMessageLoader.execute((Void) null);
                } else if (mCurrentType.equals("to-do")) {
                    title = "To-DO";
                    mMessageLoader = new MessagesLoader(MType.TODO);
                    mMessageLoader.execute((Void) null);
                } else if (mCurrentType.equals("archive")) {
                    mMessageLoader = new MessagesLoader(MType.ARCHIVE);
                    mMessageLoader.execute((Void) null);

                }
                scope.main().setTitle(title);
            } else {
                if (bundle.containsKey("group_id")) {
                    mGroupId = bundle.getInt("group_id");
                    mMessageLoader = new MessagesLoader(MType.GROUP);
                    mMessageLoader.execute((Void) null);
                } else {

                    scope.main().setTitle("Inbox");
                    mMessageLoader = new MessagesLoader(MType.INBOX);
                    mMessageLoader.execute((Void) null);
                }

            }
        }
    }

AbsListView.MultiChoiceModeListener mMessageViewMultiChoiceListener = new AbsListView.MultiChoiceModeListener() {

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        mMultiSelectedRows.put(position, checked);
        if (checked) {
            selectedCounter++;
        } else {
            selectedCounter--;
        }
        if (selectedCounter != 0) {
            mode.setTitle(selectedCounter + "");
        }
    }

    @SuppressLint("UseSparseArrays")
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_message_mark_unread_selected:
                mReadUnreadOperation = new ReadUnreadOperation(true);
                mReadUnreadOperation.execute();
                mode.finish();
                return true;
            case R.id.menu_message_mark_read_selected:
                mReadUnreadOperation = new ReadUnreadOperation(false);
                mReadUnreadOperation.execute();
                mode.finish();
                return true;
            case R.id.menu_message_more_move_to_archive_selected:
                mReadUnreadOperation = new ReadUnreadOperation(false);
                mReadUnreadOperation.execute();
                mode.finish();
                return true;
            case R.id.menu_message_more_add_star_selected:
                mStarredOperation = new StarredOperation(true);
                mStarredOperation.execute();
                mode.finish();
                return true;
            case R.id.menu_message_more_remove_star_selected:
                mStarredOperation = new StarredOperation(false);
                mStarredOperation.execute();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private int getStatusMessage(MType type) {
        switch (type) {
            case INBOX:
                return R.string.message_inbox_all_read;
            case TOME:
                return R.string.message_tome_all_read;
            case TODO:
                return R.string.message_todo_all_read;
            case GROUP:
                return R.string.message_no_group_message;
            default:
                break;
        }
        return 0;
    }




    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_fragment_message_context, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        selectedCounter = 0;
        mListView.clearChoices();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }
};
    // Handling each row view
    private View handleRowView(View mView, final int position) {
        final ODataRow row = (ODataRow) mMessageObjects.get(position);
        boolean to_read = row.getBoolean("to_read");
        mView.setBackgroundResource((to_read) ? background_resources[1]
                : background_resources[0]);

        TextView txvSubject, txvBody, txvFrom, txvDate, txvTag, txvchilds;
        final ImageView imgStarred = (ImageView) mView
                .findViewById(R.id.imgMessageStarred);

        final boolean starred = row.getBoolean("starred");
        imgStarred.setImageResource((starred) ? starred_drawables[0]
                : starred_drawables[1]);
        imgStarred.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Handling Starred click event
                mMultiSelectedRows.put(position, true);
                mStarredOperation = new StarredOperation((starred) ? false
                        : true);
                mStarredOperation.execute();
            }
        });

        txvSubject = (TextView) mView.findViewById(R.id.txvMessageSubject);
        txvBody = (TextView) mView.findViewById(R.id.txvMessageBody);
        txvFrom = (TextView) mView.findViewById(R.id.txvMessageFrom);
        txvDate = (TextView) mView.findViewById(R.id.txvMessageDate);
        txvTag = (TextView) mView.findViewById(R.id.txvMessageTag);
        txvchilds = (TextView) mView.findViewById(R.id.txvChilds);

        if (!to_read) {
            txvSubject.setTextColor(Color.BLACK);
            txvFrom.setTextColor(Color.BLACK);
        } else {
            txvSubject.setTextColor(Color.parseColor("#414141"));
            txvFrom.setTextColor(Color.parseColor("#414141"));
        }
        String subject = row.getString("subject");
        if (subject.equals("false")) {
            subject = row.getString("type");
        }
        if (!row.getString("record_name").equals("false"))
            subject = row.getString("record_name");
        txvSubject.setText(subject);
        if (row.getInt("childs") > 0) {
            txvchilds.setVisibility(View.VISIBLE);
            txvchilds.setText(row.getString("childs") + " reply");
        } else
            txvchilds.setVisibility(View.GONE);

      //  txvBody.setText(HTMLHelper.htmlToString(row.getString("body")));
        String date = row.getString("date");
        txvDate.setText(ODate.getDate(getActivity(), date, TimeZone
                .getDefault().getID()));

        String from = row.getString("email_from");
        if (from.equals("false")) {
            ODataRow author_id = row.getM2ORecord("author_id").browse();
            if (author_id != null)
                from = row.getM2ORecord("author_id").browse().getString("name");
        }
        txvFrom.setText(from);

        String model_name = row.getString("model");
        if (model_name.equals("false")) {
            model_name = row.getString("type");
        } else {
            String[] model_parts = TextUtils.split(model_name, "\\.");
            @SuppressWarnings({ "unchecked", "rawtypes" })
            HashSet unique_parts = new HashSet(Arrays.asList(model_parts));
            model_name = TextUtils.join(" ",
                    unique_parts.toArray());
        }
        int tag_color = 0;
        if (message_model_colors.containsKey(model_name)) {
            tag_color = message_model_colors.get(model_name);
        } else {
            tag_color = Color.parseColor(tag_colors[tag_color_count]);
            message_model_colors.put(model_name, tag_color);
            tag_color_count++;
            if (tag_color_count > tag_colors.length) {
                tag_color_count = 0;
            }
        }
        if (row.getString("model").equals("mail.group")) {
            String res_id = row.getString("res_id");
            if (MailGroup.mMenuGroups.containsKey("group_" + res_id)) {
                ODataRow grp = (ODataRow) MailGroup.mMenuGroups.get("group_"
                        + res_id);
                model_name = grp.getString("name");
                tag_color = grp.getInt("tag_color");
            }
        }
        txvTag.setBackgroundColor(tag_color);
        txvTag.setText(model_name);
        return mView;
    }
    @Override
    public Object databaseHelper(Context context) {
        return new MessageDB(context);
    }

    @Override
    public List<DrawerItem> drawerMenus(Context context) {
        List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
       // MessageDB db = new MessageDB(context);

            drawerItems.add(new DrawerItem(TAG, "Messages", true));
            drawerItems
                    .add(new DrawerItem(TAG, "Inbox", count(MType.INBOX,
                            context), R.drawable.ic_action_inbox,
                            getFragment("inbox")
                    ));
            drawerItems.add(new DrawerItem(TAG, "To: me", count(MType.TOME,
                    context), R.drawable.ic_action_user, getFragment("to-me")));
            drawerItems.add(new DrawerItem(TAG, "To-do", count(MType.TODO,
                    context), R.drawable.ic_action_todo, getFragment("to-do")));
            drawerItems.add(new DrawerItem(TAG, "Archives", 0,
                    R.drawable.ic_action_archive, getFragment("archive")));

        return drawerItems;    }


    private BaseFragment getFragment(String value) {
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("type", value);
        message.setArguments(bundle);
        return message;

    }

    private int count(MType type, Context context) {
        int count = 0;
        MessageDB db = new MessageDB(context);
        String where = null;
        String whereArgs[] = null;
        HashMap<String, Object> obj = getWhere(type);
        where = (String) obj.get("where");
        whereArgs = (String[]) obj.get("whereArgs");
        count = db.count(where, whereArgs);
        return count;
    }
    public HashMap<String, Object> getWhere(MType type) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        String where = null;
        String[] whereArgs = null;
        switch (type) {
            case INBOX:
                where = "to_read = ? AND starred = ?";
                whereArgs = new String[] { "true", "false" };
                break;
            case TOME:
                where = "res_id = ? AND to_read = ?";
                whereArgs = new String[] { "0", "true" };
                break;
            case TODO:
                where = "to_read = ? AND starred = ?";
                whereArgs = new String[] { "true", "true" };
                break;
            case GROUP:
                where = "res_id = ? AND model = ?";
                whereArgs = new String[] { mGroupId + "", "mail.group" };
                break;
            default:
                where = null;
                whereArgs = null;
                break;
        }
        map.put("where", where);
        map.put("whereArgs", whereArgs);
        return map;
    }

    public class MessagesLoader extends AsyncTask<Void, Void, Boolean> {

        MType messageType = null;

        public MessagesLoader(MType type) {
            messageType = type;
            mView.findViewById(R.id.loadingProgress)
                    .setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            scope.main().runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    mMessageObjects.clear();
                    HashMap<String, Object> map = getWhere(messageType);
                    String where = (String) map.get("where");
                    String whereArgs[] = (String[]) map.get("whereArgs");
                    mType = messageType;
                    List<ODataRow> result = db().select(where, whereArgs,
                            null, null, "date DESC");
                    HashMap<String, ODataRow> parent_list_details = new HashMap<String, ODataRow>();
                    if (result.size() > 0) {
                        int i = 0;
                        for (ODataRow row : result) {
                            boolean isParent = true;
                            String key = row.getString("parent_id");
                            if (key.equals("false")) {
                                key = row.getString("id");
                            } else {
                                isParent = false;
                            }

                            int childs = db().count("parent_id = ? ",
                                    new String[] { key });
                            if (!parent_list_details.containsKey(key)) {
                                // Fetching row parent message
                                ODataRow newRow = null;

                                if (isParent) {
                                    newRow = row;
                                } else {
                                    newRow = db().select(Integer.parseInt(key));
                                }

                                newRow.put("childs", childs);
                                parent_list_details.put(key, null);
                                message_row_indexes.put(key, i);
                                i++;
                                mMessageObjects.add(newRow);

                            }
                        }
                    }
                }
            });
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            if (mSearchView != null)
                mSearchView
                        .setOnQueryTextListener(getQueryListener(mListViewAdapter));
            mMessageLoader = null;
            checkMessageStatus();
        }

    }

    private void checkMessageStatus() {

        // Fetching parent ids from Child row with order by date desc
        if (mMessageObjects.size() == 0) {
            if (db().isEmptyTable() && !isSynced) {
                isSynced = true;

                if (mView.findViewById(R.id.waitingForSyncToStart) != null) {
                    mView.findViewById(R.id.waitingForSyncToStart)
                            .setVisibility(View.VISIBLE);
                }
                try {
                    Thread.sleep(2000);
                    if (mGroupId != null) {
                        Bundle group_bundle = new Bundle();
                        JSONArray ids = new JSONArray();
                        ids.put(mGroupId);
                        group_bundle.putString("group_ids", ids.toString());
                        scope.main().requestSync(MessageProvider.AUTHORITY,
                                group_bundle);
                    } else {
                        scope.main().requestSync(MessageProvider.AUTHORITY);
                    }
                } catch (Exception e) {
                }
            } else {

                mView.findViewById(R.id.waitingForSyncToStart).setVisibility(
                        View.GONE);
                TextView txvMsg = (TextView) mView
                        .findViewById(R.id.txvMessageAllReadMessage);
                txvMsg.setVisibility(View.VISIBLE);
                int string = getStatusMessage(mType);
                if (string != 0)
                    txvMsg.setText(string);
            }
        }
        mListViewAdapter.notifiyDataChange(mMessageObjects);
    }



    private int getStatusMessage(MType type) {
        switch (type) {
            case INBOX:
                return R.string.message_inbox_all_read;
            case TOME:
                return R.string.message_tome_all_read;
            case TODO:
                return R.string.message_todo_all_read;
            case GROUP:
                return R.string.message_no_group_message;
            default:
                break;
        }
        return 0;
    }
    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        mSelectedItemPosition = position;
        view.setSelected(true);
        ODataRow row = (ODataRow) mMessageObjects.get(position);
        MessageDetail detail = new MessageDetail();
        Bundle bundle = new Bundle();
        bundle.putInt("message_id", row.getInt("id"));
        bundle.putInt("position", position);
        detail.setArguments(bundle);
        FragmentListener listener = (FragmentListener) getActivity();
        listener.startDetailFragment(detail);
    }

    /**
     * Callback method to be invoked when an item in this view has been
     * clicked and held.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need to access
     * the data associated with the selected item.
     *
     * @param parent   The AbsListView where the click happened
     * @param view     The view within the AbsListView that was clicked
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     * @return true if the callback consumed the long click, false otherwise
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    @Override
    public void onPullStarted(View view) {
        scope.main().requestSync(MessageProvider.AUTHORITY);

    }

    @Override
    public void onResume() {
        super.onResume();
        scope.context().registerReceiver(messageSyncFinish,
                new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
        scope.context().registerReceiver(datasetChangeReceiver,
                new IntentFilter(DataSetChangeReceiver.DATA_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecentSwiped > -1) {
            makeArchive(mRecentSwiped);
        }
        scope.context().unregisterReceiver(messageSyncFinish);
        scope.context().unregisterReceiver(datasetChangeReceiver);
        Bundle outState = new Bundle();
        outState.putInt("mSelectedItemPosition", mSelectedItemPosition);
        onSaveInstanceState(outState);
    }

    private SyncFinishReceiver messageSyncFinish = new SyncFinishReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTouchAttacher.setPullComplete();
            scope.main().refreshDrawer(TAG);
            mListViewAdapter.clear();
            mMessageObjects.clear();
            mListViewAdapter.notifiyDataChange(mMessageObjects);
            new MessagesLoader(mType).execute();

        }
    };


    private DataSetChangeReceiver datasetChangeReceiver = new DataSetChangeReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                mView.findViewById(R.id.waitingForSyncToStart).setVisibility(
                        View.GONE);

                String id = intent.getExtras().getString("id");
                String model = intent.getExtras().getString("model");
                if (model.equals("mail.message")) {
                    ODataRow row = db().select(Integer.parseInt(id));
                    if (!row.getString("parent_id").equals("false")) {
                        row = db().select(row.getInt("parent_id"));
                    }
                    row.put("childs", 0);
                    int parent_id = row.getInt("id");
                    if (message_row_indexes.containsKey(parent_id + "")
                            && mMessageObjects.size() > 0) {
                        mMessageObjects.remove(Integer
                                .parseInt(message_row_indexes.get(
                                        parent_id + "").toString()));
                    }
                    mMessageObjects.add(0, row);
                    message_row_indexes.put(parent_id + "", parent_id);
                    mListViewAdapter.notifiyDataChange(mMessageObjects);
                }

            } catch (Exception e) {
            }

        }
    };


    private class MarkAsArchive extends AsyncTask<Void, Void, Void> {

        ODataRow mRow = null;
        OSyncHelper mOE = null;
        boolean mToRead = false;
        boolean isConnection = true;
        Context mContext = null;

        public MarkAsArchive(ODataRow row) {
            mRow = row;
            mOE = db().getSyncHelper();
            if (mOE == null)
                isConnection = false;
            mToRead = false;
            mContext = getActivity();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!isConnection)
                return null;
            String default_model = "false";
            JSONArray ids = new JSONArray();
            int parent_id = 0, res_id = 0;
            if (mRow.getString("parent_id").equals("false")) {
                parent_id = mRow.getInt("id");
                res_id = mRow.getInt("res_id");
                default_model = mRow.getString("model");
            } else {
                parent_id = mRow.getInt("parent_id");
            }
            ids.put(parent_id);
            for (ODataRow child : db().select("parent_id = ? ",
                    new String[] { parent_id + "" })) {
                ids.put(child.getInt("id"));
            }
            if (toggleReadUnread(mOE, ids, default_model, res_id, parent_id,
                    mToRead)) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            DrawerListener drawer = (DrawerListener) mContext;
            drawer.refreshDrawer(TAG);
            drawer.refreshDrawer(MailGroup.TAG);
        }

    }

    /*Method for Make Message as Read,Unread and Archive */
    private boolean toggleReadUnread(OSyncHelper oe, JSONArray ids,
                                     String default_model, int res_id, int parent_id, boolean to_read) {
        boolean flag = false;

        JSONObject newContext = new JSONObject();
        OArguments args = new OArguments();
        try {
            if (default_model.equals("false")) {
                newContext.put("default_model", false);
            } else {
                newContext.put("default_model", default_model);
            }
            newContext.put("default_res_id", res_id);
            newContext.put("default_parent_id", parent_id);

            // Param 1 : message_ids list
            args.add(ids);

            // Param 2 : to_read - boolean value
            args.add((to_read) ? false : true);

            // Param 3 : create_missing - If table does not contain any value
            // for
            // this row than create new one
            args.add(true);

            // Param 4 : context
            args.add(newContext);

            // Creating Local Database Requirement Values
            OValues values = new OValues();
            String value = (to_read) ? "true" : "false";
            values.put("starred", false);
            values.put("to_read", value);
            int result = (Integer) oe.callMethod("set_message_read", args, null);
            if (result > 0) {
                for (int i = 0; i < ids.length(); i++) {
                    int id = ids.getInt(i);
                    db().update(values, id);
                }
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * Making message read or unread or Archive
     */
    public class ReadUnreadOperation extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog mProgressDialog = null;
        boolean mToRead = false;
        boolean isConnection = true;
        OSyncHelper mOE = null;

        public ReadUnreadOperation(boolean toRead) {
            mOE = db().getSyncHelper().syncDataLimit(30);
            if (mOE == null)
                isConnection = false;
            mToRead = toRead;
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Working...");
            if (isConnection) {
                mProgressDialog.show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!isConnection)
                return false;

            boolean flag = false;
            for (int position : mMultiSelectedRows.keySet()) {
                if (mMultiSelectedRows.get(position)) {

                    ODataRow row = (ODataRow) mMessageObjects.get(position);

                    String default_model = "false";
                    JSONArray ids = new JSONArray();
                    int parent_id = 0, res_id = 0;
                    if (row.getString("parent_id").equals("false")) {
                        parent_id = row.getInt("id");
                        res_id = row.getInt("res_id");
                        default_model = row.getString("model");
                    } else {
                        parent_id = row.getInt("parent_id");
                    }
                    ids.put(parent_id);
                    for (ODataRow child : db().select("parent_id = ? ",
                            new String[] { parent_id + "" })) {
                        ids.put(child.getInt("id"));
                    }
                    if (toggleReadUnread(mOE, ids, default_model, res_id,
                            parent_id, mToRead)) {
                        flag = true;
                    }

                }
            }
            return flag;
        }




        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                ArrayList<Integer> keys = new ArrayList<Integer>(
                        mMultiSelectedRows.keySet());
                for (int position = keys.size() - 1; position >= 0; position--) {
                    if (!mToRead && !mType.equals(MType.ARCHIVE)) {
                        mMessageObjects.remove(position);
                    }
                }
                mListViewAdapter.notifiyDataChange(mMessageObjects);
                if (mMessageObjects.size() == 0) {
                    TextView txvMsg = (TextView) mView
                            .findViewById(R.id.txvMessageAllReadMessage);
                    txvMsg.setVisibility(View.VISIBLE);
                    txvMsg.setText(getStatusMessage(mType));
                }
                DrawerListener drawer = (DrawerListener) getActivity();
                drawer.refreshDrawer(TAG);
                drawer.refreshDrawer(MailGroup.TAG);

            } else {
                Toast.makeText(getActivity(), "No connection",
                        Toast.LENGTH_LONG).show();
            }
            mMultiSelectedRows.clear();
            mProgressDialog.dismiss();
        }

    }



    /**
     * Marking each row starred/unstarred in background
     */
    public class StarredOperation extends AsyncTask<Void, Void, Boolean> {

        boolean mStarred = false;
        ProgressDialog mProgressDialog = null;
        boolean isConnection = true;
        OSyncHelper mOE = null;

        public StarredOperation(boolean starred) {
            mStarred = starred;
            mOE = db().getSyncHelper();
            if (mOE == null)
                isConnection = false;
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Working...");
            if (isConnection) {
                mProgressDialog.show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!isConnection) {
                return false;
            }
            JSONArray mIds = new JSONArray();
            for (int position : mMultiSelectedRows.keySet()) {
                if (mMultiSelectedRows.get(position)) {
                    ODataRow row = (ODataRow) mMessageObjects.get(position);
                    mIds.put(row.getInt("id"));
                }
            }
            OArguments args = new OArguments();
            // Param 1 : message_ids list
            args.add(mIds);

            // Param 2 : starred - boolean value
            args.add(mStarred);

            // Param 3 : create_missing - If table does not contain any value
            // for
            // this row than create new one
            args.add(true);

            // Creating Local Database Requirement Values
            OValues values = new OValues();
            String value = (mStarred) ? "true" : "false";
            values.put("starred", value);

            boolean response = (Boolean) mOE.callMethod("set_message_starred",
                    args, null);
            response = (!mStarred && !response) ? true : response;
            if (response) {
                try {
                    for (int i = 0; i < mIds.length(); i++)
                        db().update(values, mIds.getInt(i));
                } catch (Exception e) {
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                for (int position : mMultiSelectedRows.keySet()) {
                    ODataRow row = (ODataRow) mMessageObjects.get(position);
                    row.put("starred", mStarred);
                }
                mListViewAdapter.notifiyDataChange(mMessageObjects);
                DrawerListener drawer = (DrawerListener) getActivity();
                drawer.refreshDrawer(TAG);
                drawer.refreshDrawer(MailGroup.TAG);
            } else {
                Toast.makeText(getActivity(), "No connection",
                        Toast.LENGTH_LONG).show();
            }
            mMultiSelectedRows.clear();
            mProgressDialog.dismiss();
        }

    }
    /**
     * Callback method to be invoked while the list view or grid view is being scrolled. If the
     * view is being scrolled, this method will be called before the next frame of the scroll is
     * rendered. In particular, it will be called before any calls to
     * {@link ##Adapter#getView(int, android.view.View, android.view.ViewGroup)}.
     *
     * @param view        The view whose scroll state is being reported
     * @param scrollState The current scroll state. One of {@link #SCROLL_STATE_IDLE},
     *                    {@link #SCROLL_STATE_TOUCH_SCROLL} or {@link #SCROLL_STATE_IDLE}.
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mRecentSwiped > -1) {
            makeArchive(mRecentSwiped);
        }
    }

    /**
     * Callback method to be invoked when the list or grid has been scrolled. This will be
     * called after the scroll has completed
     *
     * @param view             The view whose scroll state is being reported
     * @param firstVisibleItem the index of the first visible cell (ignore if
     *                         visibleItemCount == 0)
     * @param visibleItemCount the number of visible cells
     * @param totalItemCount   the number of items in the list adaptor
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {

    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mRecentSwiped > -1) {
            makeArchive(mRecentSwiped);
        }
        return false;
    }

    @Override
    public boolean canSwipe(int position) {
        if (mRecentSwiped != position
                && (mType != MType.ARCHIVE && mType != MType.GROUP))
            return true;
        return false;
    }

    @Override
    public void onSwipe(View view, int[] ids) {
        for (int id : ids) {
            int current_pos = id;
            if (mRecentSwiped > -1) {
                int archive_pos = mRecentSwiped;
                if (mRecentSwiped < current_pos && mRecentSwiped != -1) {
                    current_pos = id - 1;
                } else {
                    if (mRecentSwiped == mMessageObjects.size()) {
                        archive_pos = mRecentSwiped - 1;
                    }
                }
                makeArchive(archive_pos);
                mRecentSwiped = -1;
            }
            mRecentSwiped = current_pos;
            toggleSwipeView(getViewFromListView(current_pos), true);
        }
    }

    private void makeArchive(int position) {
        ODataRow row = (ODataRow) mMessageObjects.get(position);
        mMessageObjects.remove(position);
        mListViewAdapter.notifiyDataChange(mMessageObjects);
        toggleSwipeView(getViewFromListView(position), false);
        mRecentSwiped = -1;
        checkMessageStatus();
        MarkAsArchive archive = new MarkAsArchive(row);
        archive.execute();
    }

    private View getViewFromListView(int position) {
        final int firstListItemPosition = mListView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition
                + mListView.getChildCount() - 1;

        if (position < firstListItemPosition || position > lastListItemPosition) {
            return mListView.getAdapter().getView(position, null, mListView);
        } else {
            final int childIndex = position - firstListItemPosition;
            return mListView.getChildAt(childIndex);
        }
    }

    private void toggleSwipeView(final View child_view, boolean visible) {
        if (visible) {
            child_view.findViewById(R.id.messageArchiveView).setVisibility(
                    View.VISIBLE);
            child_view.findViewById(R.id.undoArchived).setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            child_view.findViewById(R.id.messageArchiveView)
                                    .setVisibility(View.GONE);
                            mRecentSwiped = -1;
                        }
                    });
        } else {
            child_view.findViewById(R.id.messageArchiveView).setVisibility(
                    View.GONE);
        }
    }
}
