package com.example.tenny.usbcommunication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Tenny on 2015/11/24.
 */
public class ValueAdapter extends ArrayAdapter<ValueItem> {
    public ValueAdapter(Context context, ArrayList<ValueItem> items) {
        super(context, R.layout.value_item, items);
    }

    static class ItemHolder {
        public TextView weight, maxWeight, minWeight, radius, maxRadius, minRadius, resist, maxResist, minResist, brandName, time;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ValueItem i = getItem(position);
        ItemHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ItemHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.value_item, parent, false);
            viewHolder.weight = (TextView) convertView.findViewById(R.id.weight);
            viewHolder.maxWeight = (TextView) convertView.findViewById(R.id.maxWeight);
            viewHolder.minWeight = (TextView) convertView.findViewById(R.id.minWeight);
            viewHolder.radius = (TextView) convertView.findViewById(R.id.radius);
            viewHolder.maxRadius = (TextView) convertView.findViewById(R.id.maxRadius);
            viewHolder.minRadius = (TextView) convertView.findViewById(R.id.minRadius);
            viewHolder.resist = (TextView) convertView.findViewById(R.id.resist);
            viewHolder.maxResist = (TextView) convertView.findViewById(R.id.maxResist);
            viewHolder.minResist = (TextView) convertView.findViewById(R.id.minResist);
            viewHolder.brandName = (TextView) convertView.findViewById(R.id.brandName);
            viewHolder.time = (TextView) convertView.findViewById(R.id.time);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ItemHolder) convertView.getTag();
        }
        viewHolder.weight.setText(i.Weight);
        viewHolder.maxWeight.setText(i.MaxWeight);
        viewHolder.minWeight.setText(i.MinWeight);
        viewHolder.radius.setText(i.Radius);
        viewHolder.maxRadius.setText(i.MaxRadius);
        viewHolder.minRadius.setText(i.MinRadius);
        viewHolder.resist.setText(i.Resist);
        viewHolder.maxResist.setText(i.MaxResist);
        viewHolder.minResist.setText(i.MinResist);
        viewHolder.brandName.setText(i.name);
        viewHolder.time.setText(i.time);
        changeColor(viewHolder.weight, i.MaxWeight, i.MinWeight, i.Weight);
        changeColor(viewHolder.radius, i.MaxRadius, i.MinRadius, i.Radius);
        changeColor(viewHolder.resist, i.MaxResist, i.MinResist, i.Resist);
        return convertView;
    }

    private void changeColor(TextView v, String max, String min, String value) {
        if (Double.parseDouble(value) > Double.parseDouble(max)) {
            v.setTextColor(Color.parseColor("#f44336")); //red
        } else if (Double.parseDouble(value) < Double.parseDouble(min)) {
            v.setTextColor(Color.parseColor("#412dff")); //blue
        } else {
            v.setTextColor(Color.parseColor("#4caf50")); //green
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }
}