package com.grilla.pan;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bill on 5/25/15.
 */
public class FindFragment extends Fragment {
    private String ARG_BUSINESS_NAMES = "com.grilla.pan.FindFragment.ARG_BUSINESS_NAMES";
    private String ARG_BUSINESSES = "com.grilla.pan.FindFragment.ARG_BUSINESSES";
    static String TAG = "FindFragment";

    // Yelp api information
    String YELP_CONSUMER_KEY;
    String YELP_CONSUMER_SECRET;
    String YELP_TOKEN;
    String YELP_TOKEN_SECRET;
    String YELP_API_PATH;
    String YELP_SEARCH_PATH;

    // OAuth
    OAuthService yelpService;
    Token yelpAccessToken;

    // Interface elements
    View rootView;
    ArrayList<String> searchResults;
    ArrayAdapter<String> searchResultsAdapter;
    ListView searchResultsList;

    ArrayList<YelpBusiness> yelpBusinesses;

    // Required empty constructor
    public FindFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        YELP_CONSUMER_KEY = getString(R.string.yelp_consumer_key);
        YELP_CONSUMER_SECRET = getString(R.string.yelp_consumer_secret);
        YELP_TOKEN = getString(R.string.yelp_token);
        YELP_TOKEN_SECRET = getString(R.string.yelp_token_secret);
        YELP_API_PATH = getString(R.string.yelp_api_path);
        YELP_SEARCH_PATH = getString(R.string.yelp_search_path);

        //if (searchResults == null) searchResults = new ArrayList<>();
        //if (yelpBusinesses == null) yelpBusinesses = new ArrayList<>();

        if (savedInstanceState != null) {
            Log.d(TAG, "Howdy");
            yelpBusinesses = savedInstanceState.getParcelableArrayList(ARG_BUSINESSES);
            if (!yelpBusinesses.isEmpty()) Log.d(TAG, yelpBusinesses.get(0).name);
            else Log.d(TAG, "Businesses empty");
            searchResults = savedInstanceState.getStringArrayList(ARG_BUSINESS_NAMES);
            if (!searchResults.isEmpty()) Log.d(TAG, searchResults.get(0));
            else Log.d(TAG, "Search results empty");
        } else {
            Log.d(TAG, "No howdy :(");
            yelpBusinesses = new ArrayList<>();
            searchResults = new ArrayList<>();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_find, container, false);
        Context c = getActivity().getApplicationContext();

        // Create and initialize yelp OAuth 1.0a service
        yelpService = YelpService.getServiceInstance();
        yelpAccessToken = YelpService.getTokenInstance();

        // setup list of results
        searchResultsList = (ListView)rootView.findViewById(R.id.search_results);
        searchResultsAdapter = new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, searchResults);
        searchResultsList.setAdapter(searchResultsAdapter);
        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                YelpBusiness selected = yelpBusinesses.get(position);
                String clicked = selected.id + ": " + selected.name + ", " + selected.address + "; (" + selected.lat + ", " + selected.lng + ")";
                Log.d("CLICKED", clicked);

                SearchFragment fragment = new SearchFragment();
                FragmentTransaction ft = getFragmentManager().beginTransaction();

                Bundle args = new Bundle();
                args.putParcelable(SearchFragment.ARG_BUSINESS, selected);
                fragment.setArguments(args);

                ft.replace(R.id.container, fragment);
                ft.addToBackStack(null);

                ft.commit();
            }
        });

        // search button
        Button findButton = (Button)rootView.findViewById(R.id.button_find);
        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Need to reset everything
                searchResults.clear();
                yelpBusinesses.clear();
                searchResultsList.setSelectionAfterHeaderView();
                hideKeyboard();

                // get search terms
                EditText location = (EditText)rootView.findViewById(R.id.search_location);
                EditText term = (EditText)rootView.findViewById(R.id.search_term);

                searchYelp(location.getText().toString(), term.getText().toString());
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");

        
       /* if (savedInstanceState != null) {
            Log.d(TAG, "Howdy");
            yelpBusinesses = savedInstanceState.getParcelableArrayList(ARG_BUSINESSES);
            if (!yelpBusinesses.isEmpty()) Log.d(TAG, yelpBusinesses.get(0).name);
            else Log.d(TAG, "Businesses empty");
            searchResults = savedInstanceState.getStringArrayList(ARG_BUSINESS_NAMES);
            if (!searchResults.isEmpty()) Log.d(TAG, searchResults.get(0));
            else Log.d(TAG, "Search results empty");
        }*/
    }

    @Override
    public void onSaveInstanceState(Bundle args) {
        super.onSaveInstanceState(args);
        Log.d(TAG, "onSaveInstanceState");

        args.putStringArrayList(ARG_BUSINESS_NAMES, searchResults);
        args.putParcelableArrayList(ARG_BUSINESSES, yelpBusinesses);
    }

    private void searchYelp(String location, String search_term) {
        new SendYelpRequest().execute(YELP_API_PATH + YELP_SEARCH_PATH, "location", location, "term", search_term);
    }

    private class SendYelpRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            OAuthRequest request = new OAuthRequest(Verb.GET, params[0]);

            // Key, Value pairs input in params
            for (int i = 1; i < params.length; i+=2) {
                request.addQuerystringParameter(params[i], params[i+1]);
            }

            yelpService.signRequest(yelpAccessToken, request);

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
                    searchResults.add(j.getString("name"));
                    JSONObject location = j.getJSONObject("location");

                    YelpBusiness b = new YelpBusiness(j.getString("id"),
                                                        j.getString("name"),
                                                        j.getString("categories"),
                                                        location.getString("address"));
                    yelpBusinesses.add(b);
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

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
