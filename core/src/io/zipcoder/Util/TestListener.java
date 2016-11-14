package io.zipcoder.Util;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;

/**
 * Created by christophernobles on 11/14/16.
 */
public class TestListener extends HttpRequestBuilder implements Net.HttpResponseListener {
    @Override
    public void handleHttpResponse(Net.HttpResponse httpResponse) {

    }

    @Override
    public void failed(Throwable t) {

    }

    @Override
    public void cancelled() {

    }
}
