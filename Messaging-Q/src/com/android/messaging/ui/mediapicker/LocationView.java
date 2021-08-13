package com.android.messaging.ui.mediapicker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.app.Fragment;

import com.android.messaging.R;
import com.juphoon.helper.mms.ui.RcsBaiduLocationActivity;

public class LocationView extends Fragment {

    private View parentView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        parentView = inflater.inflate(R.layout.act_location_fragment, container, false);
        getActivity().startActivity(new Intent(getActivity(), RcsBaiduLocationActivity.class));

        return parentView;
    }
}
