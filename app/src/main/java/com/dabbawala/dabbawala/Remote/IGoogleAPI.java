package com.dabbawala.dabbawala.Remote;

import retrofit2.Call;
import retrofit2.http.Url;

public interface IGoogleAPI {
    //GET
   Call<String> getPath(@Url String url);
}
