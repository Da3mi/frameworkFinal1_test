package com.odoo.addons.message;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.addons.message.providers.groups.MailGroupProvider;
import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OdooHelper;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.OUser;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.listview.OListAdapter;
import com.odoo.util.Base64Helper;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.drawer.DrawerListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.OETouchListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.OArguments;
import odoo.Odoo;


/**
 * Created by daami on 15/08/14.
 */
public class MailGroup extends BaseFragment implements OnPullListener {

    public static final String TAG = "com.odoo.addons.message.MailGroup";

    /**
     * OETouchListener
     */
    private OETouchListener mTouchAttacher;

    View mView = null;
    Boolean hasSynced = false;
    GridView mGroupGridView = null;
    List<Object> mGroupGridListItems = new ArrayList<Object>();
    OListAdapter mGroupGridViewAdapter = null;

    /**
     * Tag Colors
     */
    public static HashMap<String, Object> mMenuGroups = new HashMap<String, Object>();
    String mTagColors[] = new String[] { "#218559", "#192823", "#FF8800",
            "#CC0000", "#59A2BE", "#808080", "#9933CC", "#0099CC", "#669900",
            "#EBB035" };
    /**
     * Loaders
     */
    GroupsLoader mGroupsLoader = null;
    JoinUnfollowGroup mJoinUnFollowGroup = null;

    /**
     * Database Objects
     */
    MailFollowers mMailFollowerDB = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_message_groups_list,
                container, false);
        init();
        return mView;
    }

    public void init() {
        scope = new AppScope(getActivity());
        mMailFollowerDB = new MailFollowers(getActivity());
        initControls();
        mGroupsLoader = new GroupsLoader();
        mGroupsLoader.execute();
    }

    private void initControls() {
        mGroupGridView = (GridView) mView.findViewById(R.id.listGroups);
        mGroupGridViewAdapter = new OListAdapter(getActivity(),
                R.layout.fragment_message_groups_list_item, mGroupGridListItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View mView = convertView;
                if (mView == null)
                    mView = getActivity().getLayoutInflater().inflate(
                            getResource(), parent, false);
                generateView(position, mView);
                return mView;
            }
        };
        mGroupGridView.setAdapter(mGroupGridViewAdapter);
        mTouchAttacher = scope.main().getTouchAttacher();
        mTouchAttacher.setPullableView(mGroupGridView, this);
    }

    private void generateView(int position, View mView) {
        ODataRow row = (ODataRow) mGroupGridListItems.get(position);
        TextView txvName = (TextView) mView.findViewById(R.id.txvGroupName);
        TextView txvDesc = (TextView) mView
                .findViewById(R.id.txvGroupDescription);
        ImageView imgGroupPic = (ImageView) mView
                .findViewById(R.id.imgGroupPic);
        imgGroupPic.setImageBitmap(Base64Helper.getBitmapImage(getActivity(),
                row.getString("image_medium")));
        txvName.setText(row.getString("name"));
        txvDesc.setText(row.getString("description"));

        final int group_id = row.getInt("id");
        final Button btnJoin = (Button) mView.findViewById(R.id.btnJoinGroup);
        final Button btnUnJoin = (Button) mView
                .findViewById(R.id.btnUnJoinGroup);
        int total = mMailFollowerDB.count(
                "res_model = ? AND res_id = ? AND partner_id = ?",
                new String[]{db().getModelName(), group_id + "",
                        OUser.current(getActivity()).getPartner_id() + ""}
        );
        btnUnJoin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mJoinUnFollowGroup = new JoinUnfollowGroup(group_id, false);
                mJoinUnFollowGroup.execute();
                btnJoin.setVisibility(View.VISIBLE);
                btnUnJoin.setVisibility(View.GONE);
            }
        });
        btnJoin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mJoinUnFollowGroup = new JoinUnfollowGroup(group_id, true);
                mJoinUnFollowGroup.execute();
                btnJoin.setVisibility(View.GONE);
                btnUnJoin.setVisibility(View.VISIBLE);
            }
        });
        if (total > 0) {
            btnJoin.setVisibility(View.GONE);
            btnUnJoin.setVisibility(View.VISIBLE);

        } else {
            btnJoin.setVisibility(View.VISIBLE);
            btnUnJoin.setVisibility(View.GONE);
        }
    }

    @Override
    public Object databaseHelper(Context context) {
        return new MailGroupDB(context);
    }

    @Override
    public List<DrawerItem> drawerMenus(Context context) {

        MailGroupDB db = new MailGroupDB(context);
        mMailFollowerDB = new MailFollowers(context);

        List<DrawerItem> menu = new ArrayList<DrawerItem>();

        MailGroup group = new MailGroup();
        Message message = new Message();
        Bundle bundle = new Bundle();


            menu.add(new DrawerItem(TAG, "My Groups", true));

            // Join Group
            group.setArguments(bundle);
            message.setArguments(bundle);
            menu.add(new DrawerItem(TAG, "Join Group", 0,
                    R.drawable.ic_action_social_group, group));

            // Dynamic Groups
            List<ODataRow> groups = mMailFollowerDB.select(
                    "res_model = ? AND partner_id = ?",
                    new String[]{db.getModelName(),
                            OUser.current(context).getPartner_id() + ""}
            );
            int index = 0;
            MessageDB messageDB = new MessageDB(context);
            menu.add(new DrawerItem(TAG, "Physicians", 0,
                mTagColors[0], message));
        menu.add(new DrawerItem(TAG, "Managers", 0,
                mTagColors[2], message));
        menu.add(new DrawerItem(TAG, "secretary", 0,
                mTagColors[3], message));
            for (ODataRow row : groups) {
                if (mTagColors.length - 1 < index)
                    index = 0;
                ODataRow grp = db.select(row.getInt("res_id"));

                if (grp != null) {

                    bundle = new Bundle();
                    bundle.putInt("group_id", grp.getInt("id"));
                    message.setArguments(bundle);

                    int count = messageDB.count(
                            "to_read = ? AND model = ? AND res_id = ?",
                            new String[] { "true", db().getModelName(),
                                    row.getString("id") });


                    menu.add(new DrawerItem(TAG, grp.getString("name"), count,
                            mTagColors[index], message));
                    grp.put("tag_color", Color.parseColor(mTagColors[index]));
                    mMenuGroups.put("group_" + grp.getInt("id"), grp);
                    index++;
                }
            }
                return menu;
    }

    @Override
    public void onPullStarted(View arg0) {
        Log.d(TAG, "MailGroup->OETouchListener->onPullStarted()");
        scope.main().requestSync(MailGroupProvider.AUTHORITY);

    }

    class GroupsLoader extends AsyncTask<Void, Void, Void> {

        public GroupsLoader() {
            mView.findViewById(R.id.loadingProgress)
                    .setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            mGroupGridListItems.clear();
            mGroupGridListItems.addAll(db().select());
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            mGroupGridViewAdapter.notifiyDataChange(mGroupGridListItems);
            mGroupsLoader = null;
            checkStatus();
        }
    }

    private void checkStatus() {
        if (!db().isEmptyTable()) {
            mView.findViewById(R.id.groupSyncWaiter).setVisibility(View.GONE);
        } else {
            mView.findViewById(R.id.groupSyncWaiter)
                    .setVisibility(View.VISIBLE);
            TextView txvSyncDetail = (TextView) mView
                    .findViewById(R.id.txvMessageHeaderSubtitle);
            txvSyncDetail.setText("Your groups will appear shortly");
            if (!hasSynced) {
                hasSynced = true;
                scope.main().requestSync(MailGroupProvider.AUTHORITY);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
       /* getActivity().registerReceiver(syncFinishReceiver,
                new IntentFilter(SyncFinishReceiver.SYNC_FINISH));*/
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(syncFinishReceiver);
    }

    private SyncFinishReceiver syncFinishReceiver = new SyncFinishReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mTouchAttacher.setPullComplete();
            mGroupsLoader = new GroupsLoader();
            mGroupsLoader.execute();
            DrawerListener drawer = (DrawerListener) getActivity();
            drawer.refreshDrawer(TAG);
            drawer.refreshDrawer(Message.TAG);
        }

    };

    public class JoinUnfollowGroup extends AsyncTask<Void, Void, Boolean> {
        int mGroupId = 0;
        boolean mJoin = false;
        String mToast = "";
        OSyncHelper sync = null ;
        JSONObject result = new JSONObject();

        public JoinUnfollowGroup(int group_id, boolean join) {
            mGroupId = group_id;
            mJoin = join;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (mMailFollowerDB == null)
                    mMailFollowerDB = new MailFollowers(getActivity());
                int partner_id = OUser.current(getActivity()).getPartner_id();
                OdooHelper oe = db().getOEInstance();

                sync = mMailFollowerDB.getSyncHelper();
            // Odoo app = new App().getOdoo();

               /* if ( app == null) {
                    mToast = "No Connection";
                    return false;
                }*/

                OArguments arguments = new OArguments();
                arguments.add(new JSONArray().put(mGroupId));
                arguments.add(oe.Odooo().updateContext(new JSONObject()));
                if (mJoin) {
                   // sync.callMethod("action_follow",arguments,null);
                    oe.call_kw("action_follow",arguments,null);

                    mToast = "Group joined";
                    sync.syncWithServer();
                } else {
                   // sync.callMethod("action_unfollow",arguments,null);
                    oe.call_kw("action_unfollow",arguments,null);

                    mToast = "Unfollowed from group";
                    mMailFollowerDB.delete(
                            "res_id = ? AND partner_id = ? AND res_model = ? ",
                            new String[] { mGroupId + "", partner_id + "",
                                    db().getModelName() });
                    sync.syncWithServer();

                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Toast.makeText(getActivity(), mToast, Toast.LENGTH_LONG).show();
            DrawerListener drawer = (DrawerListener) getActivity();
            drawer.refreshDrawer(TAG);
            drawer.refreshDrawer(Message.TAG);
        }
    }
}

