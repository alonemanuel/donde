package com.example.donde.utils;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.donde.R;
import com.example.donde.activities.App;
import com.example.donde.archive.EventInfoFragment;
import com.example.donde.fragments.EventChatFragment;
import com.example.donde.fragments.EventMapFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    private String eventID;
    private int position;

    public ViewPagerAdapter(FragmentManager fm, String eventID, int position) {
        super(fm);

        this.eventID = eventID;
        this.position = position;

    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Bundle eventIDBundle = new Bundle();
        eventIDBundle.putString(App.getRes().getString(R.string.arg_event_id), eventID);
        eventIDBundle.putInt(App.getRes().getString(R.string.arg_position), this.position);
        switch (position) {

            case 0:
                EventInfoFragment infoFragment = new EventInfoFragment();
                infoFragment.setArguments(eventIDBundle);
                return infoFragment;
            case 1:
                EventMapFragment mapFragment = new EventMapFragment();
                mapFragment.setArguments(eventIDBundle);
                return mapFragment;
            case 2:
                EventChatFragment chatFragment = new EventChatFragment();
                chatFragment.setArguments(eventIDBundle);
                return chatFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}