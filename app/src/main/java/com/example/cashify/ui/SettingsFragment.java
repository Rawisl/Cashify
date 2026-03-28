package com.example.cashify.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.cashify.CategoryManagement.CategoryManagement;
import com.example.cashify.R;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }
    //Hàm nạp giao diện .Xml lên màn hình
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        //Tìm cái nút categories bằng id đã đặt cho n
        LinearLayout btnCategories = view.findViewById(R.id.btn_categories);

        btnCategories.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Intent(màn hình Activity hiện tại, màn hình Activity  muốn đến)
                //Do đây là 1 fragment, không có quyền, nên phải gọi hàm getActivity() để lấy Activity chứa nó

                Intent intent = new Intent(getActivity(), CategoryManagement.class);
                startActivity(intent);
            }
        });
        return view;
    }
}