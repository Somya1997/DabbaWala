package com.dabbawala.dabbawala.Common;

import com.dabbawala.dabbawala.Remote.IGoogleAPI;
import com.dabbawala.dabbawala.Remote.RetrofitClient;

public class Common {
    public static final String baseURL="https://maps.googleapis.com";
    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
