package com.example.tenny.usbcommunication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Tenny on 2015/11/24.
 */
public class BoxAdapter extends ArrayAdapter<BoxItem> {
    public BoxAdapter(Context context, ArrayList<BoxItem> items) {
        super(context, R.layout.box_item, items);
    }

    static class ItemHolder {
        public TextView line, box, targetBox, remainBox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BoxItem i = getItem(position);
        ItemHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ItemHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.box_item, parent, false);
            viewHolder.box = (TextView) convertView.findViewById(R.id.box);
            viewHolder.line = (TextView) convertView.findViewById(R.id.line);
            viewHolder.targetBox = (TextView) convertView.findViewById(R.id.targetBox);
            viewHolder.remainBox = (TextView) convertView.findViewById(R.id.remainBox);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ItemHolder) convertView.getTag();
        }
        viewHolder.line.setText(i.line);
        viewHolder.box.setText(i.box);
        viewHolder.targetBox.setText(i.targetBox);
        int r = Integer.parseInt(i.targetBox) - Integer.parseInt(i.box);
        viewHolder.remainBox.setText(Integer.toString(r));
        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }
}
