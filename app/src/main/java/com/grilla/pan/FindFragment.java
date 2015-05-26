package com.grilla.pan;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

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

/**
 * Created by bill on 5/25/15.
 */
public class FindFragment extends Fragment {

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
    ArrayList<String> searchResults;
    ArrayAdapter<String> searchResultsAdapter;

    // Required empty constructor
    public FindFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        YELP_CONSUMER_KEY = getString(R.string.yelp_consumer_key);
        YELP_CONSUMER_SECRET = getString(R.string.yelp_consumer_secret);
        YELP_TOKEN = getString(R.string.yelp_token);
        YELP_TOKEN_SECRET = getString(R.string.yelp_token_secret);
        YELP_API_PATH = getString(R.string.yelp_api_path);
        YELP_SEARCH_PATH = getString(R.string.yelp_search_path);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_find, container, false);
        Context c = getActivity().getApplicationContext();

        // Create and initialize yelp OAuth 1.0a service
        yelpService = new ServiceBuilder().provider(YelpAPI.class).apiKey(YELP_CONSUMER_KEY)
                .apiSecret(YELP_CONSUMER_SECRET).build();
        yelpAccessToken = new Token(YELP_TOKEN, YELP_TOKEN_SECRET);

        // setup list of results
        final ListView searchResultsList = (ListView)rootView.findViewById(R.id.search_results);
        searchResults = new ArrayList<>();
        searchResultsAdapter = new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, searchResults);
        searchResultsList.setAdapter(searchResultsAdapter);
        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String clicked = searchResults.get(position);
            }
        });

        // search button
        Button findButton = (Button)rootView.findViewById(R.id.button_find);
        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Need to empty old array! (adds on elements to end)
                searchResults.clear();

                // get search terms
                EditText location = (EditText)rootView.findViewById(R.id.search_location);
                EditText term = (EditText)rootView.findViewById(R.id.search_term);

                searchYelp(location.getText().toString(), term.getText().toString());
            }
        });

        return rootView;
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
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // update the list
            searchResultsAdapter.notifyDataSetChanged();
        }
    }
}
