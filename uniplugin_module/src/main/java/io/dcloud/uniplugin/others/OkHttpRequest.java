package io.dcloud.uniplugin.others;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import java.io.IOException;

public class OkHttpRequest {
    public  static String url = "";
    public  static String accessToken  = "";
    public  static String id= "";
    public static void sendRequest() {
        // 1. Set up the logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 2. Build the client and attach the interceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        //accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyQ29udGV4dCI6IntcInVzZXJuYW1lXCI6XCIxMzI1MDgyMDg0NlwiLFwibmlja05hbWVcIjpcIjgyMDg0NlFZXCIsXCJmYWNlXCI6XCJodHRwczovL3Rlc3QuaW9ldmlzYS5jb20vcGljcy9wcm9maWxlLnBuZ1wiLFwiaWRcIjpcIjE5OTcxMTE5NjUzODU5NTgyODlcIixcImxvbmdUZXJtXCI6dHJ1ZSxcInJvbGVcIjpcIk1FTUJFUlwifSIsInN1YiI6IjEzMjUwODIwODQ2IiwiZXhwIjoxOTQzNTEyMDQyfQ.8aHzNHRocp5SOZXgOAZv2b5o9qmQe9ekn5szYjp0v3k";
       // url = "https://buyer-ceshi.shanxunsw.com/buyer/hashrate/package/task/ad/watch";

        // 3. Prepare the request payload
        String jsonTemplate = "{\"adId\": \"%s\",\"unitId\": \"12\",\"channel\": \"test\"}";

// 3. Format the string to inject the id
        String jsonPayload = String.format(jsonTemplate, id);
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonPayload, mediaType);

        // 4. Define variables
//        String url = "https://example.com";
//        String accessToken = "YOUR_ACCESS_TOKEN_HERE";

        // 5. Build the request with Authorization header
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("accessToken", accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        // 3. Use enqueue() instead of execute() to run asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Runs on background thread if the network fails
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Runs on background thread when the server responds
                try (Response res = response) {
                    if (res.isSuccessful() && res.body() != null) {
                        String responseData = res.body().string();
                        System.out.println("Success response: " + responseData);

                        // WARNING: If you need to update Android UI elements (TextView, Toast, etc.) here,
                        // you MUST wrap them inside: runOnUiThread(() -> { ... });
                    } else {
                        System.out.println("Server returned error code: " + res.code());
                    }
                }
            }
        });
    }
}
