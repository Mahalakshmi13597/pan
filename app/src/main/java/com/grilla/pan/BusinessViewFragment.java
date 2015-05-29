package com.grilla.pan;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BusinessViewFragment extends Fragment {
    public static final String ARG_BUSINESS = "com.grilla.pan.BusinessViewFragment.business";
    private String TAG = "BusinessViewFragment";

    private String yelpURL = "http://www.yelp.com";
    private String yelpPhotosURL = "http://www.yelp.com/biz_photos/";

    private YelpBusiness business;

    public BusinessViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            business = getArguments().getParcelable(ARG_BUSINESS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_business_view, container, false);
        Context c = getActivity().getApplicationContext();

        // Basic elements of business
        TextView businessName = (TextView)rootView.findViewById(R.id.business_name);
        businessName.setText(business.name);

        TextView businessDistance = (TextView)rootView.findViewById(R.id.business_distance);
        businessDistance.setText(business.distance);

        ImageView businessStars = (ImageView)rootView.findViewById(R.id.business_image_rating);
        new DownloadImageTask(businessStars).execute(business.ratingImageURL);

        TextView businessRating = (TextView)rootView.findViewById(R.id.business_rating);
        businessRating.setText(business.rating);

        ImageView businessImage = (ImageView)rootView.findViewById(R.id.business_image);
        //new DownloadImageTask(businessImage).execute(business.imageURL);
        new YelpImageTask(businessImage).execute(business.id);

        TextView businessAddress = (TextView)rootView.findViewById(R.id.business_address);
        businessAddress.setText(business.address);

        return rootView;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    private class YelpImageTask extends AsyncTask<String, Void, String> {
        ImageView bmImage;

        public YelpImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected String doInBackground(String... urls) {
            String url = yelpPhotosURL + urls[0];
            String result = "";

            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(url);
                HttpResponse response = client.execute(request);

                InputStream in = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder str = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null)
                {
                    str.append(line);
                }
                in.close();
                result = str.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        protected void onPostExecute(String result) {
            //outputString(result);
            Document doc = Jsoup.parse(result);
            Element link = doc.select("a.biz-shim").first();

            String imgUrl = yelpURL + link.attr("href");
            Log.d(TAG, imgUrl);
            new YelpImageTask2(bmImage).execute(imgUrl);
        }
    }

    private class YelpImageTask2 extends AsyncTask<String, Void, String> {
        ImageView bmImage;

        public YelpImageTask2(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected String doInBackground(String... urls) {
            String url = urls[0];
            Log.d("URL", url);
            String result = "";

            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(url);
                HttpResponse response = client.execute(request);

                InputStream in = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder str = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null)
                {
                    str.append(line);
                }
                in.close();
                result = str.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        protected void onPostExecute(String result) {
            //outputString(result);
            Document doc = Jsoup.parse(result);
            Elements links = doc.select("meta");
            Element link = null;
            for (int i = 0; i < links.size(); i++) {
                if (links.get(i).hasAttr("property") && links.attr("property").equals("og:image")) {
                    link = links.get(i);
                    break;
                }
            }
            if (link == null)
                return;
            Log.d("TAG", link.toString());

            String imgUrl = yelpURL + link.attr("content");
            Log.d(TAG, imgUrl);
            new DownloadImageTask(bmImage).execute(imgUrl);
        }
    }

    private void outputString(String sb) {
        if (sb.length() > 4000) {
            Log.v(TAG, "sb.length = " + sb.length());
            int chunkCount = sb.length() / 4000;     // integer division
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= sb.length()) {
                    Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i));
                } else {
                    Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i, max));
                }
            }
        } else {
            Log.v(TAG, sb.toString());
        }
    }

}
