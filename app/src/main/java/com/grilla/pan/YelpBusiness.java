package com.grilla.pan;

import android.location.Address;
import android.location.Geocoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bill on 5/26/15.
 */
public class YelpBusiness {
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
            for (int i = 1; i < cats.length(); i += 2) {
                this.categories.add(cats.getString(i));
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
}
