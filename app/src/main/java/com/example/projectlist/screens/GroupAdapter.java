package com.example.projectlist.screens;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.projectlist.R;
import com.example.projectlist.model.Group;

import java.util.List;

public class GroupAdapter extends ArrayAdapter<Group> {
    int selectedItemPosition = 0;
    public GroupAdapter(Context context, List<Group> arr) {
        super(context, R.layout.listview_names_item, arr);
    }

    public String getGroupId(int pos) {
        return getItem(pos).uid;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Group group = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_names_item, null);
        }

        ((TextView) convertView.findViewById(R.id.text_names_item)).setText(group.group);

        if (position == selectedItemPosition) {
            convertView.setBackgroundResource(R.drawable.selected_background);
        } else {
            convertView.setBackgroundResource(R.drawable.default_background);
        }

        return convertView;
    }

    public void setSelectedItem(int position) {
        selectedItemPosition = position;
        notifyDataSetChanged();
    }
}
