package com.termproject.user.googlemaptest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

//네트워크 예외처리

/**
 * Created by user on 2016-06-18.
 */

public class Notice2 extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.network_exception, container, false);
        return rootView;

    }
}
