package com.odoo.addons.partners;

import java.util.List;

import odoo.controls.OForm;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.R;
import com.odoo.base.res.ResAppointment;
import com.odoo.addons.partners.model.ResMedicament;
import com.odoo.base.res.ResPartner;
import com.odoo.addons.partners.model.ResPatients;
import com.odoo.addons.partners.model.ResPrescription;
import com.odoo.base.res.ResPhysician;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.addons.partners.Partners.Type;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;

public class PartnersDetail extends BaseFragment {

	View mView = null;
	OForm mForm,mForm1,mForm2,mForm3 = null;
    private Type mType = null ;
	private Integer mId = null;
    Context context ;
	ODataRow mRecord = new ODataRow();
	Boolean mEditMode = true;
	Menu mMenu = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		initargs();
		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.partners_detail, container, false);
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		init();
	}

	private void init() {

        ResPartner resPartner = new ResPartner(getActivity());
        switch(mType){

            case Companies:

                OControls.setVisible(mView,R.id.partnerDetail);
                OControls.setVisible(mView,R.id.appointmentDetail);
                mForm = (OForm) mView.findViewById(R.id.partnerDetail);
                mForm3 = (OForm) mView.findViewById(R.id.appointmentDetail);
                ResAppointment resAppointment= new ResAppointment(getActivity());
                ResPhysician resPhysician = new ResPhysician(getActivity());
                OValues oValues = new OValues();
                oValues.put("name","ABC");
                resPhysician.create(oValues);

                if (mId != null) {
                    mRecord = resPartner.select(mId);
                    mForm.initForm(mRecord);

                } else {
                    mForm.setModel(resPartner);
                }

                if (mId != null) {
                    mRecord = resAppointment.select(mId);
                    mForm3.initForm(mRecord);

                } else {
                    mForm3.setModel(resAppointment);
                }

                mForm3.setEditable(mEditMode);
                break;

            case Customers:
                OControls.setVisible(mView,R.id.partnerDetail);
                mForm = (OForm) mView.findViewById(R.id.partnerDetail);
                if (mId != null) {
                    mRecord = resPartner.select(mId);
                    mForm.initForm(mRecord);

                } else {
                    mForm.setModel(resPartner);
                }
                OControls.setVisible(mView,R.id.patientDetail);
                mForm1 = (OForm) mView.findViewById(R.id.patientDetail);
               //ResPartner resPartner = new ResPartner(getActivity());
                ResPatients resPatient= new ResPatients(getActivity());
                if (mId != null) {
                    mRecord = resPatient.select(mId);
                    mForm1.initForm(mRecord);

                } else {
                    mForm1.setModel(resPatient);
                }
                mForm1.setEditable(mEditMode);
                break;
            case Suppliers:

                OControls.setVisible(mView,R.id.partnerDetail);
                mForm = (OForm) mView.findViewById(R.id.partnerDetail);

                if (mId != null) {
                    mRecord = resPartner.select(mId);
                    mForm.initForm(mRecord);

                } else {
                    mForm.setModel(resPartner);
                }
                OControls.setVisible(mView,R.id.PrescriptionDetail);
                mForm2 = (OForm) mView.findViewById(R.id.PrescriptionDetail);
                // ResPartner resPartner = new ResPartner(getActivity());
                ResPrescription resPrescription = new ResPrescription(getActivity());
                ResMedicament resMedicament = new ResMedicament(getActivity());
                resMedicament.add_default();
                if (mId != null) {
                    mRecord = resPrescription.select(mId);
                    mForm2.initForm(mRecord);

                } else {
                    mForm2.setModel(resPrescription);
                }
                mForm2.setEditable(mEditMode);
                break;

        }


        mForm.setEditable(mEditMode);

	}

	private void initargs() {
		Bundle arg = getArguments();
        mType = Partners.Type.valueOf(arg.getString("type"));
        String mode = arg.getString("Menu");
		if (arg != null && arg.containsKey(OColumn.ROW_ID)) {
			mId = arg.getInt(OColumn.ROW_ID);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_partners_detail, menu);
		mMenu = menu;
	    updateMenu(mEditMode);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

        ResPrescription resPrescription = new ResPrescription(getActivity());
        ResAppointment resAppointment = new ResAppointment(getActivity());
        ResPatients resPatients = new ResPatients(getActivity());
        Bundle arg = getArguments();
        mType = Partners.Type.valueOf(arg.getString("type"));
		switch (item.getItemId()) {

		case R.id.menu_partner_detail_edit:
			mEditMode = !mEditMode;
			updateMenu(mEditMode);
            mForm.setEditable(mEditMode);
            if(mType== Type.Companies) {
                mForm3.setEditable(mEditMode);
            }
            else if (mType==Type.Customers){
                mForm1.setEditable(mEditMode);
            }
            else {
                mForm2.setEditable(mEditMode);
            }



			break;
		case R.id.menu_partner_detail_save:
			mEditMode = false;
            OValues values = mForm.getFormValues();


            if (values != null) {
                updateMenu(mEditMode);
                if (mId != null) {
                    db().update(values, mId);
                } else {
                    db().create(values);

                }
            }
            getActivity().getSupportFragmentManager().popBackStack();

            if(mType== Type.Companies) {

                OValues values1 = mForm3.getFormValues();
                if (values1 != null) {
                    updateMenu(mEditMode);
                    if (mId != null) {
                        resAppointment.update(values1, mId);
                    } else {
                        resAppointment.create(values1);
                    }
                }


                getActivity().getSupportFragmentManager().popBackStack();
            }
            else if (mType==Type.Customers){
                OValues values2 = mForm1.getFormValues();

                if (values2 != null) {
                    updateMenu(mEditMode);
                    if (mId != null) {
                       resPatients.update(values2, mId);
                    } else {
                       resPatients.create(values2);
                    }
                }
                getActivity().getSupportFragmentManager().popBackStack();
            }
            else if (mType==Type.Suppliers){
                OValues values3 = mForm2.getFormValues();

                if (values3 != null) {
                    updateMenu(mEditMode);
                    if (mId != null) {
                        resPrescription.update(values3, mId);
                    } else {
                       resPrescription.create(values3);
                    }
                }
                getActivity().getSupportFragmentManager().popBackStack();
            }
			break;
		case R.id.menu_partner_detail_delete:
			db().delete(mId);

			getActivity().getSupportFragmentManager().popBackStack();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateMenu(Boolean editMode) {
		mMenu.findItem(R.id.menu_partner_detail_edit).setVisible(!editMode);
		mMenu.findItem(R.id.menu_partner_detail_save).setVisible(editMode);
	}

	@Override
	public Object databaseHelper(Context context) {
		return new ResPartner(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

}
