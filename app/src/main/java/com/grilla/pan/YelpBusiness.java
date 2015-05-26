package com.grilla.pan;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class YelpBusiness implements Parcelable {
    public String id;
    public String name;
    public ArrayList<String> categories;
    public String address;
    public double lat = 90, lng = 200; // initial value outside of possible ranges

    public YelpBusiness(String id, String name, String categories, String address) {
        this.id = id;
        this.name = name;

        this.categories = new ArrayList<>();
        try {
            JSONArray cats = new JSONArray(categories);
            for (int i = 0; i < cats.length(); i += 1) {
                String cat = cats.getString(i);
                int mid = cat.indexOf("\",\"");
                int end = cat.indexOf("\"", mid+3);
                String theCat = cat.substring(mid+3, end);
                this.categories.add(theCat);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.address = address;
    }

    public boolean latLngSet() {
        return lat != 90 && lng != 200;
    }

    public void setLatLng(double lat, double lng) { this.lat = lat; this.lng = lng; }

    public String toString() {
        String first = id + ": " + name;
        String cats = "[";
        for (String s : categories) {
            cats += s + ", ";
        }
        cats += "]";
        return first + " " + cats + " (" + lat + ", " + lng + ")";
    }

    protected YelpBusiness(Parcel in) {
        id = in.readString();
        name = in.readString();
        if (in.readByte() == 0x01) {
            categories = new ArrayList<String>();
            in.readList(categories, String.class.getClassLoader());
        } else {
            categories = null;
        }
        address = in.readString();
        lat = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        if (categories == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(categories);
        }
        dest.writeString(address);
        dest.writeDouble(lat);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<YelpBusiness> CREATOR = new Parcelable.Creator<YelpBusiness>() {
        @Override
        public YelpBusiness createFromParcel(Parcel in) {
            return new YelpBusiness(in);
        }

        @Override
        public YelpBusiness[] newArray(int size) {
            return new YelpBusiness[size];
        }
    };
}