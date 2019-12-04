package com.panda2u.poster;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.methods.VKApiFriends;
import com.vk.sdk.api.methods.VKApiPhotos;
import com.vk.sdk.api.methods.VKApiUsers;
import com.vk.sdk.api.methods.VKApiWall;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.api.model.VKPhotoArray;
import com.vk.sdk.api.photo.VKImageParameters;
import com.vk.sdk.api.photo.VKUploadImage;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.ok.android.sdk.OkListener;
import ru.ok.android.sdk.OkRequestMode;

public class Services extends Fragment {
    protected enum Socials {IN, OUT, VK, FB, OK;}
    protected static Activity the_activity;
    static String typed_text;

    protected static String additional_message_text = "";
    protected static int clickCounter;
    protected static VKParameters params_wall_post = new VKParameters();
    protected static VKParameters params_friends_get = new VKParameters();
    protected static VKApiWall wall_instance = new VKApiWall();
    protected static VKApiPhotos photos_instance = new VKApiPhotos();
    protected static int vk_user_id;
    protected static String[] scope = new String[] {VKScope.FRIENDS, VKScope.WALL, VKScope.PHOTOS};
    public static JSONObject CommitImageMapOK = new JSONObject();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VKSdk.logout();

        App.odnoklassniki.clearTokens();
        App.Toast("Services => onCreate: " + System.currentTimeMillis());
        App.Log("Services => onCreate: " + System.currentTimeMillis());
    }

    @Override public void onStop() {
        super.onStop();
        VKSdk.logout();
        App.odnoklassniki.clearTokens();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Corrects user name representation regarding its length. SetUsername() method helper
     */
    protected static void NamePlacer(Button name_button, String firstName, String lastName) {
        String displayed_name = firstName.concat(" ").concat(lastName);
        App.Toast("Welcome, " + displayed_name);

        if (displayed_name.length() > App.GetContext().getResources().getInteger(R.integer.twenty)) {
            name_button.setTextScaleX(.75f);
            if (displayed_name.length() >= 24) {
                name_button.setTextScaleX(firstName.length() > App.GetContext().getResources().getInteger(R.integer.twenty) ? .75f : 1.0f);
                displayed_name = firstName.substring(App.GetContext().getResources().getInteger(R.integer.zero), App.GetContext().getResources().getInteger(R.integer.twenty)).concat("...");
            }
        }
        name_button.setText(displayed_name);
    }

    /**
     *  Dispenses current user name on button
     */
    protected static void SetUsername(int user_id, final Button social_button) {
        String fields = "first_name, last_name";
        switch (social_button.getId()) {
            case R.id.button_name_VK:
                if (user_id == 0) { return; }
                vk_user_id = user_id;
                VKApiUsers users = new VKApiUsers();
                VKRequest get_username_request = users.get(VKParameters.from(VKApiConst.USER_ID));
                get_username_request.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override public void onComplete(VKResponse get_username_response) {
                    super.onComplete(get_username_response);
                    try {
                        JSONArray jsonArray = get_username_response.json.getJSONArray(App.GetContext().getResources().getString(R.string.response));
                        String firstName = jsonArray.getJSONObject(0).getString(App.GetContext().getResources().getString(R.string.firstname));
                        String lastName = jsonArray.getJSONObject(0).getString(App.GetContext().getResources().getString(R.string.lastname));
                        NamePlacer(social_button, firstName, lastName);
                    } catch (Exception e) {
                        App.Log("SetUsername(VK) => Exception:\n" + e.getMessage());
                        App.Toast("self id VKrequest Exception\n" + e.getMessage());
                    }
                    }
                });
                break;

            case R.id.button_name_FB:
                final List<String> firstNameLastName = Arrays.asList("", "");
                Bundle paramsFB = new Bundle();
                paramsFB.putString(GraphRequest.FIELDS_PARAM, fields);
                new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    AccessToken.getCurrentAccessToken().getUserId(),
                    paramsFB,
                    HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                        try {
                            firstNameLastName.set(App.GetContext().getResources().getInteger(R.integer.zero), response.getJSONObject().getString(App.GetContext().getResources().getString(R.string.firstname)));
                            firstNameLastName.set(1, response.getJSONObject().getString(App.GetContext().getResources().getString(R.string.lastname)));
                            NamePlacer(social_button,
                                firstNameLastName.get(App.GetContext().getResources().getInteger(R.integer.zero)),
                                firstNameLastName.get(1));
                        } catch (JSONException e) {
                            //App.Log("SetUsername(FB) => Exception:\n" + e.getMessage());
                            App.Toast(e.getLocalizedMessage());
                        }
                        }
                    }
                ).executeAsync();
                break;

            case R.id.button_name_OK:
                try {
                    Map<String, String> paramsOK = new HashMap<>();
                    paramsOK.put("fields", fields);
                    Set<OkRequestMode> mode = OkRequestMode.getDEFAULT();
                    mode.add(OkRequestMode.NO_PLATFORM_REPORTING);

                    App.odnoklassniki.requestAsync("users.getCurrentUser", paramsOK, mode, new OkListener() {
                        @Override public void onSuccess(@NotNull JSONObject json) {
                            String first_name = json.optString(App.GetContext().getResources().getString(R.string.firstname));
                            String last_name = json.optString(App.GetContext().getResources().getString(R.string.lastname));
                            NamePlacer(social_button, first_name, last_name);
                        }

                        @Override public void onError(@org.jetbrains.annotations.Nullable String e) {
                            App.Log("SetUsername(OK) => Error:\n" + e);
                            String error = "Could not get current user info\n" + e;
                            App.Toast(error);
                        }
                    });

                } catch (Exception e) {
                    App.Log("SetUsername(OK) => Exception:\n" + e);
                    App.Toast("SetUsername(OK) => e" + e.getMessage());
                }
                break;
        }
    }


    /**
     * Confirms photo upload action.
     */
    private static void CommitImageUploadOK(final JSONObject map) {
        String data_comment = map.optString(App.GetContext().getString(R.string.comment));
        Map<String, String> params = new HashMap<>();
        params.put(App.GetContext().getString(R.string.photo_id), map.optString(App.GetContext().getString(R.string.photo_id)));
        params.put(App.GetContext().getString(R.string.comment), data_comment);
        params.put(App.GetContext().getString(R.string.token), map.optString(App.GetContext().getString(R.string.token)));

        Set<OkRequestMode> mode = OkRequestMode.getDEFAULT();
        mode.add(OkRequestMode.NO_PLATFORM_REPORTING);

        /**
         * params must have photo_id and token keys, could have others
         */
        App.odnoklassniki.requestAsync("photosV2.commit", params, mode, new OkListener() {
            @Override public void onSuccess(@NotNull final JSONObject json) {
                App.Toast("Message sent");
            }

            @Override public void onError(@org.jetbrains.annotations.Nullable final String e) {
                App.Toast("Message not sent!\n" + e);
                App.Log("CommitImageUploadOK photosV2.commit => Error:\n" + e);
            }
        });
    }

    /**
     * Do uploads image on server
     */
    private static void ImageUploaderOK(JSONObject map, Intent data) {
        final String CACHE_DIR = App.GetAppDataDir()
                + App.GetContext().getResources().getString(R.string.cache);
        final Uri uri = data.getData();
        String last_part = uri.getPath().contains(".")
                ? uri.getLastPathSegment().substring(uri.getLastPathSegment().indexOf(':') + 1)
                : uri.getLastPathSegment().replace(":", "").concat(".jpg");
        final String FILE_EXTENSION = last_part.substring(last_part.indexOf('.'));
        final String TEMP_FILE_NAME = last_part.substring(App.GetContext().getResources().getInteger(R.integer.zero), last_part.indexOf('.'));

        try {
            String url = map.optString(App.GetContext().getResources().getString(R.string.upload_url));
            final String photo_id = map.optString(App.GetContext().getResources().getString(R.string.photo_id));
            final File TEMP_FILE = new File(CACHE_DIR + TEMP_FILE_NAME + FILE_EXTENSION);
            InputStream in = App.GetContext().getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(TEMP_FILE);
            byte[] buf = new byte[1024]; //which buffer size is best?
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "pic1", TEMP_FILE.getName(),
                            RequestBody.create(TEMP_FILE, MediaType.parse("image/jpeg")))
                    .build();

            final OkHttpClient client = new OkHttpClient.Builder(new OkHttpClient())
                    .retryOnConnectionFailure(false)
                    .build();

            final Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            final Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    App.Toast("onFailure" + e.getMessage());
                }

                @Override public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        JSONObject response_json = new JSONObject(response.body().string());
                        if (response.isSuccessful()) {
                            CommitImageMapOK.put(App.GetContext().getResources().getString(R.string.token),
                                    response_json.optJSONObject(App.GetContext().getResources().getString(R.string.photos))
                                            .optJSONObject(photo_id).optString(App.GetContext().getResources().getString(R.string.token)));
                            CommitImageMapOK.put(App.GetContext().getResources().getString(R.string.photo_id),
                                    response_json.optJSONObject(App.GetContext().getResources()
                                            .getString(R.string.photos)).names().get(0).toString());
                            CommitImageMapOK.put(App.GetContext().getResources().getString(R.string.filename),
                                    TEMP_FILE_NAME + FILE_EXTENSION);
                            CommitImageMapOK.put(App.GetContext().getResources().getString(R.string.filesize),
                                    TEMP_FILE.length());
                            CommitImageMapOK.put(App.GetContext().getResources().getString(R.string.comment),
                                    "");
                        } else {
                        App.Log("response is unhappy");
                        App.Toast("response is unhappy");
                        }
                    } catch (JSONException e) {
                        App.Toast("jsn excptn" + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            App.Log("ImageUploaderOK => Exception:\n" + e.getMessage());
        }
    }

    /**
     * Gets upload url and photo id from picked image data
     */
    public static void UploadImageOK(final Intent data) {
        Map<String, String> params = new HashMap<>();
        Set<OkRequestMode> mode = OkRequestMode.getDEFAULT();
        mode.add(OkRequestMode.NO_PLATFORM_REPORTING);
        try {
            App.odnoklassniki.requestAsync("photosV2.getUploadUrl", params, mode, new OkListener() {
                @Override public void onSuccess(@NotNull JSONObject json) {
                    JSONObject upload_params = new JSONObject();
                    String photo_id = "";
                    String upload_url = json.optString(App.GetContext().getResources().getString(R.string.upload_url));
                    try {
                        photo_id = json.getJSONArray("photo_ids").get(0).toString();
                        upload_params
                                .put(App.GetContext().getResources().getString(R.string.upload_url), upload_url)
                                .put(App.GetContext().getResources().getString(R.string.photo_id), photo_id);
                        ImageUploaderOK(upload_params, data);
                    } catch (JSONException e) {
                        App.Toast("UploadImageOK photosV2.getUploadUrl JSONException:\n" + e.getMessage());
                        App.Log("UploadImageOK photosV2.getUploadUrl => JSONException:\n" + e.getMessage());
                    }
                }

                @Override public void onError(@org.jetbrains.annotations.Nullable String s) {
                    App.Log("UploadImageOK photosV2.getUploadUrl => server error:\n" + s);
                    App.Toast("Unhappy OK server response\n" + s);
                }
            });

        } catch (Exception e) {
            App.Log("UploadImageOK failed => Exception:\n" + e.getMessage());
            App.Toast("Failed upload image to OK\n" + e);
        }
    }

    /**
     * Uploads image on VK server and attaches it to Message as a parameter
     */
    protected static void ImageUploaderVK(Bitmap photo) {
        VKRequest request = VKApi.uploadWallPhotoRequest(
            new VKUploadImage(photo,
            VKImageParameters.jpgImage(1.0f)),
            App.GetContext().getResources().getInteger(R.integer.zero),
            App.GetContext().getResources().getInteger(R.integer.zero));

        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override public void onComplete(VKResponse response) {
                VKApiPhoto photoModel = ((VKPhotoArray) response.parsedModel).get(0);
                String parsed_photo = "photo" + photoModel.owner_id + "_" + photoModel.id;
                params_wall_post.put("attachments", parsed_photo);
                App.Toast("photo attached");
            }

            @Override public void onError(VKError e) {
                App.Log("ImageUploaderVK => VKError:\n" + e.errorMessage);
                App.Toast(e.errorMessage);
            }
        });
    }

    /**
     * Gets upload URL for image
     */
    public static void UploadImageVK(final Bitmap photo) {
        VKRequest get_wall_to_upload_request = photos_instance.getWallUploadServer();
        get_wall_to_upload_request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    if (response.json.getJSONObject(App.GetContext().getResources().getString(R.string.response)).getString(App.GetContext().getResources().getString(R.string.upload_url)) != "") {
                        ImageUploaderVK(photo);
                    }
                } catch (Exception e) {
                    App.Log("UploadImageVK => Exception:\n" + e.getMessage());
                    App.Toast("error getting upload url\n" + e.getMessage());
                }
            }

            @Override public void onError(VKError e) {
                App.Log("UploadImageVK => VKError:\n" + e.errorMessage);
            }
        });
    }

    /** Adds friends one by one up to list while n < count,
     * then sends a message to the wall and resets the list
     */
    protected static void SetFriends(View view) {
        final ListView list_to_show = (ListView) view;
        if (App.friend_clicks < 5) {
            App.friend_clicks += 1;
            VKApiFriends friends = new VKApiFriends();
            App.Toast("vk_user_id " + vk_user_id + "\nApp.clicks " + App.friend_clicks);
            params_friends_get.put(VKApiConst.USER_ID, vk_user_id);
            params_friends_get.put(VKApiConst.COUNT, 1);
            params_friends_get.put("order", "random");
            /* Note: Can't get response to "wall.post" without giving VKApiConst.FIELDS some arg */
            params_friends_get.put(VKApiConst.FIELDS, "online");

            VKRequest friends_request = friends.get(params_friends_get);
            friends_request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override public void onComplete(VKResponse response) {

                try {
                    JSONObject jsonObject = response.json.getJSONObject(App.GetContext().getResources().getString(R.string.response));
                    JSONArray friends = jsonObject.getJSONArray("items");

                    if (App.friends_VKlist != null) {
                        /* add one to list */
                        App.Toast("add one to VKlist");
                        App.friends_VKlist.add(new VKApiUser(friends.getJSONObject(0)));
                        additional_message_text += App.friends_VKlist.get(App.friends_VKlist.getCount() - 1).fields.getString(App.GetContext().getResources().getString(R.string._id)) + ", ";
                    }

                    if (App.friends_VKlist == null) {
                        App.Toast("new VKList");
                        App.friends_VKlist = new VKList<VKApiUser>(friends, VKApiUser.class);
                        additional_message_text += "\nFriends ids:\n" + App.friends_VKlist.get(/*clickCounter - 1*/0).fields.getString(App.GetContext().getResources().getString(R.string._id)) + ", ";
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        App.GetContext(),
                        android.R.layout.simple_expandable_list_item_1,
                        App.friends_VKlist);
                    list_to_show.setAdapter(arrayAdapter);

                } catch (JSONException e) {
                    App.Log("SetFriends => JSONException:\n" + e.getMessage());
                    App.Toast("Setting friends JSONException " + e.getMessage());
                }
                super.onComplete(response);
                }
            });

        } else {
            App.friend_clicks = 0;
            additional_message_text =
                    additional_message_text.substring(App.GetContext().getResources().getInteger(R.integer.zero), additional_message_text.length() - 2);
        }
    }

    /**
     * Sends testlog_text message to users wall. Used (int)0 to set wall_owner_id = vk_authorized_user_id
     */
    public static void SendMessage(Socials socials, EditText edit_text) {
        typed_text = edit_text.getText().toString();

        String signed_message = typed_text;
        switch (socials) {
            case VK:
                /* Note: negative int required for community wall id */
                params_wall_post.put("owner_id", vk_user_id);
                signed_message += additional_message_text;

                signed_message += clickCounter != 0 && additional_message_text.length() > App.GetContext().getResources().getInteger(R.integer.zero)
                        ? additional_message_text.substring(
                        App.GetContext().getResources().getInteger(R.integer.zero), additional_message_text.length() - 2)
                        : additional_message_text;
                signed_message += signed_message.length() > App.GetContext().getResources().getInteger(R.integer.zero)
                        ? App.GetContext().getResources().getString(R.string.newline)
                        + App.GetContext().getResources().getString(R.string.underline)
                        : "";
                signed_message += App.GetContext().getResources().getString(R.string.newline)
                        .concat(App.GetContext().getResources().getString(R.string.sent))
                        .concat(App.GetContext().getResources().getString(R.string.app_name));
                params_wall_post.put("message", signed_message);
                VKRequest wallpost_request = wall_instance.post(params_wall_post);
                wallpost_request.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override public void onComplete(VKResponse response) {
                        super.onComplete(response);
                        App.Toast("Message sent");
                    }

                    @Override public void onError(VKError e) {
                        App.Log("SendMessage(VK) => VKError:\n" + e.errorMessage);
                        App.Toast("Message NOT sent!\n" + e.errorMessage);
                    }
                });
                break;

            case OK:
                try {
                    signed_message += typed_text.length() > App.GetContext().getResources().getInteger(R.integer.zero)
                            ? App.GetContext().getResources().getString(R.string.newline)
                            + App.GetContext().getResources().getString(R.string.underline)
                            : "";
                    signed_message += CommitImageMapOK.optString(App.GetContext().getResources().getString(R.string.filename)) != ""
                            ? App.GetContext().getResources().getString(R.string.newline) + CommitImageMapOK.optString(App.GetContext().getResources().getString(R.string.filename))
                            + App.GetContext().getResources().getString(R.string.newline) + "file size before upload, bytes: "
                            + CommitImageMapOK.optString(App.GetContext().getResources().getString(R.string.filesize)) : ""
                    ;
                    signed_message += App.GetContext().getResources().getString(R.string.newline)
                            + App.GetContext().getResources().getString(R.string.sent) + App.GetContext().getResources().getString(R.string.app_name);

                    CommitImageMapOK.putOpt(App.GetContext().getResources().getString(R.string.comment), signed_message);

                } catch (JSONException je) {
                    App.Log("SendMessage(OK) => JSONException:\n" + je.getMessage());
                }
                CommitImageUploadOK(CommitImageMapOK);
                break;
            default: break;
        }

        /**
         * Resets Message related fields
         */
        App.hide_posting_scene = true;
        additional_message_text = "";
        clickCounter = App.GetContext().getResources().getInteger(R.integer.zero);
        params_wall_post.remove("attachments");
        App.friends_VKlist = null;
    }
}

