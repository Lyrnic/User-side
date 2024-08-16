package com.user.side.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.user.side.fragments.PermissionFragment;

import java.util.ArrayList;

public class PermissionsAdapter extends FragmentStateAdapter {
    ArrayList<String> permissions;

    public PermissionsAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, ArrayList<String> permissions) {
        super(fragmentManager, lifecycle);
        this.permissions = permissions;
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return PermissionFragment.getPermissionFragment(permissions.get(position));
    }

    @Override
    public int getItemCount() {
        return permissions.size();
    }
}
