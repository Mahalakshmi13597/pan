package com.grilla.pan;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public class YelpService {
    private static OAuthService yelpService;
    private static Token yelpToken;

    private static String YELP_CONSUMER_KEY = "i0HANNUji9wbUlItaPZTbg";
    private static String YELP_CONSUMER_SECRET = "hlooKcw-USOGcfEaPhI8CpVrmp8";
    private static String YELP_TOKEN = "6G6gS4eG8OnryQoYrq8Rcosdz2qCC6uT";
    private static String YELP_TOKEN_SECRET = "HuKYEiYLCx6UdfWpAYOKvBw_ulY";

    public static OAuthService getServiceInstance() {
        if (yelpService == null)
            yelpService = new ServiceBuilder().provider(YelpAPI.class).apiKey(YELP_CONSUMER_KEY)
                .apiSecret(YELP_CONSUMER_SECRET).build();

        return yelpService;
    }

    public static Token getTokenInstance() {
        if (yelpToken == null)
            yelpToken = new Token(YELP_TOKEN, YELP_TOKEN_SECRET);

        return yelpToken;
    }
}
