package com.dabbawala.dabbawala.Remote;

import retrofit2.Call;
import retrofit2.http.Url;
import retrofit2.http.GET;

public interface IGoogleAPI {
    //GET
   Call<String> getPath(@Url String url);
}
