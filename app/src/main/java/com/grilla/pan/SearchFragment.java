package com.grilla.pan;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {
    public static final String ARG_BUSINESS = "com.grilla.pan.SearchFragment.business";

    private RequestLocationUpdatesListener mCallback;

    private YelpBusiness business;
    private Location location;
    private ArrayList<YelpBusiness> yelpBusinesses;

    private OAuthService yelpService;
    private Token yelpToken;

    private String YELP_API_PATH;
    private String YELP_SEARCH_PATH;

    private ArrayAdapter<String> searchResultsAdapter;
    private ArrayList<String> searchResults;

    public SearchFragment() {
        // Required empty public constructor
    }

    public interface RequestLocationUpdatesListener {
        Location onRequestLocation() throws Exception;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        YELP_API_PATH = getString(R.string.yelp_api_path);
        YELP_SEARCH_PATH = getString(R.string.yelp_search_path);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Load data given to it
        if (savedInstanceState == null) savedInstanceState = getArguments();
        if (savedInstanceState != null) business = savedInstanceState.getParcelable(ARG_BUSINESS);

        Log.d("TEST", business.toString());

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        Context c = getActivity().getApplicationContext();

        TextView businessText = (TextView)rootView.findViewById(R.id.business_text);
        businessText.setText("Selected " + business.name);

        Button searchButton = (Button)rootView.findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });

        yelpService = YelpService.getServiceInstance();
        yelpToken = YelpService.getTokenInstance();
        yelpBusinesses = new ArrayList<>();

        ListView searchResultsList = (ListView)rootView.findViewById(R.id.search_results);
        searchResults = new ArrayList<>();
        searchResultsAdapter = new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, searchResults);
        searchResultsList.setAdapter(searchResultsAdapter);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (RequestLocationUpdatesListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement RequestLocationUpdatesListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void doSearch() {
        try {
            location = mCallback.onRequestLocation();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.d("LOC", location.getLatitude() + ", " + location.getLongitude());

        String categories = "";
        for (int i = 0; i < business.categories.size(); i++) {
            categories += business.categories.get(i);
            if (i != business.categories.size()-1)
                categories += ",";
        }

        String loc = location.getLatitude() + "," + location.getLongitude();
        new SendYelpRequest().execute(YELP_API_PATH + YELP_SEARCH_PATH, "ll", loc, "category_filter",categories);
    }

    private class SendYelpRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            OAuthRequest request = new OAuthRequest(Verb.GET, params[0]);

            // Key, Value pairs input in params
            for (int i = 1; i < params.length; i+=2) {
                request.addQuerystringParameter(params[i], params[i+1]);
            }

            yelpService.signRequest(yelpToken, request);

            Response response = request.send();
            return response.getBody();
        }

        @Override
        protected void onPostExecute(String response) {
            // Load business data from JSON
            try {
                JSONObject jo = new JSONObject(response);
                JSONArray businesses = jo.getJSONArray("businesses");

                for (int i = 0; i < businesses.length(); i++) {
                    JSONObject j = businesses.getJSONObject(i);
                    JSONObject location = j.getJSONObject("location");

                    YelpBusiness b = new YelpBusiness(j.getString("id"),
                            j.getString("name"),
                            j.getString("categories"),
                            location.getString("address"));
                    yelpBusinesses.add(b);
                    searchResults.add(b.name);
                    new GetGeocodeTask().execute(b);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // update the list
            searchResultsAdapter.notifyDataSetChanged();
        }
    }

    // asynchronous task to get latitude and longitude from address
    private class GetGeocodeTask extends AsyncTask<YelpBusiness, Void, LatLng> {
        private YelpBusiness b;

        @Override
        protected LatLng doInBackground(YelpBusiness... params) {
            b = params[0];

            Geocoder coder = new Geocoder(getActivity());
            List<Address> address;
            LatLng p1;

            try {
                address = coder.getFromLocationName(b.address, 5);
                if (address == null) {
                    return null;
                }
                Address location = address.get(0);
                location.getLatitude();
                location.getLongitude();

                p1 = new LatLng(location.getLatitude(), location.getLongitude() );

            } catch (Exception ex) {
                return null;
            }

            return p1;
        }

        @Override
        protected void onPostExecute(LatLng result) {
            if (result != null) b.setLatLng(result.latitude, result.longitude);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
