package com.example.tenny.usbcommunication;

/**
 * Created by Tenny on 2015/11/24.
 */
public class ValueItem {
    public String Weight, MaxWeight, MinWeight,
            Radius, MaxRadius, MinRadius,
            Resist, MaxResist, MinResist;
    public String name, time;

    public ValueItem (String name, String maxWeight, String weight, String minWeight, String maxRadius, String radius, String minRadius, String maxResist, String resist, String minResist, String time) {
        super();
        this.Weight = weight;
        this.MaxWeight = maxWeight;
        this.MinWeight = minWeight;
        this.Radius = radius;
        this.MaxRadius = maxRadius;
        this.MinRadius = minRadius;
        this.Resist = resist;
        this.MaxResist = maxResist;
        this.MinResist = minResist;
        this.name = name;
        this.time = time;
    }
}