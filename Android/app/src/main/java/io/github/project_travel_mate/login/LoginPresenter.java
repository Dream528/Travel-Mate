package io.github.project_travel_mate.login;

import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static utils.Constants.API_LINK_V2;
import static utils.Constants.STATUS_CODE_CREATED;
import static utils.Constants.STATUS_CODE_OK;

/**
 * Created by el on 5/4/17.
 */

class LoginPresenter {
    private LoginView view;

    public void bind(LoginView view) {
        this.view = view;
    }

    public void unbind() {
        view = null;
    }

    public void signUp() {
        view.openSignUp();
    }


    /**
     * Calls Signup API
     *
     * @param name      user's name
     * @param email     user's email id
     * @param pass      password user entered
     * @param mhandler  handler
     */
    public void ok_signUp(final String name, final String email, String pass, final Handler mhandler) {

        view.showLoadingDialog();

        String uri = API_LINK_V2 + "sign-up";

        //Set up client
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("email", email)
                .addFormDataPart("password", pass)
                .addFormDataPart("firstname", name)
                .addFormDataPart("lastname", name)
                .build();

        //Execute request
        final Request request = new Request.Builder()
                .url(uri)
                .post(requestBody)
                .build();

        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                view.showError();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                final String res = Objects.requireNonNull(response.body()).string();
                final int responseCode = response.code();
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String successfulMessage = "\"Successfully registered\"";
                            if (responseCode == STATUS_CODE_CREATED &&  res.equals(successfulMessage)) {
                                //if successful redirect to login
                                view.openLogin();
                                view.setLoginEmail(email);
                                view.showMessage("signup succeeded! please login");
                            } else {
                                // show error message
                                view.showMessage(res);
                            }
                            view.dismissLoadingDialog();
                        } catch (Exception e) {
                            e.printStackTrace();
                            view.showError();
                        }
                    }
                });
            }
        });
    }

    public void login() {
        view.openLogin();
    }

    /**
     * Calls Login API and checks for validity of credentials
     * If yes, transfer to MainActivity
     *
     * @param email     user's email id
     * @param pass      password user entered
     */
    public void ok_login(final String email, String pass, final Handler mhandler) {

        view.showLoadingDialog();

        String uri = API_LINK_V2 + "sign-in";

        //Set up client
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", email)
                .addFormDataPart("password", pass)
                .build();

        //Execute request
        Request request = new Request.Builder()
                .url(uri)
                .post(requestBody)
                .build();
        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                view.showError();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String res = Objects.requireNonNull(response.body()).string();
                final int responseCode = response.code();
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (responseCode == STATUS_CODE_OK) {
                                JSONObject responeJsonObject = new JSONObject(res);
                                String token = responeJsonObject.getString("token");
                                view.rememberUserInfo(token, email);
                                view.startMainActivity();
                                view.dismissLoadingDialog();
                            } else {
                                view.showError();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    }
                );
            }
        });
    }

}
