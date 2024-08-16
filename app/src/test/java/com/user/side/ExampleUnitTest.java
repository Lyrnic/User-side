package com.user.side;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws JSONException {
        JSONObject jsonBody = new JSONObject();
        JSONObject message = new JSONObject();
        JSONObject data = new JSONObject();

        data.put("device_state",true);

        message.put("token", "tokentokentokentoken");
        message.put("data",data);

        jsonBody.put("message",message);

        System.out.println(jsonBody);
    }
}