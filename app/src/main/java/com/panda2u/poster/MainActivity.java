package com.panda2u.poster;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.graphics.drawable.Drawable;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.widget.ShareDialog;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import ru.ok.android.sdk.Odnoklassniki;
import ru.ok.android.sdk.OkListener;
import ru.ok.android.sdk.util.OkAuthType;
import ru.ok.android.sdk.util.OkScope;

import static com.panda2u.poster.Services.scope;

public class MainActivity extends AppCompatActivity {
    private List<String> permissions = Arrays.asList("email", "user_photos", "user_friends");
    private AccessTokenTracker fbTracker;
    private ShareDialog shareDialog;
    private CallbackManager callbackManagerFB;

    private static Odnoklassniki odnoklassniki = App.odnoklassniki;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Services.the_activity = MainActivity.this;
        FacebookSdk.setAutoInitEnabled(true);
        setContentView(R.layout.activity_main);

        FloatingActionButton do_send_message = findViewById(R.id.button_message);
        do_send_message.setImageResource(R.drawable.ic_custom_send_message);
        do_send_message.setScaleType(ImageView.ScaleType.CENTER_CROP);

        callbackManagerFB = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        final Button vk_name_button = findViewById(R.id.button_name_VK);
        final Button vk_image_button = findViewById(R.id.button_image_VK);

        final LoginButton button_login_fb = findViewById(R.id.button_login_FB);
        button_login_fb.setPermissions(permissions);
        final Button fb_name_button = findViewById(R.id.button_name_FB);
        final Button fb_send = findViewById(R.id.button_image_FB);
        fb_name_button.setText(getResources().getString(R.string.text_not_auth));

        final Button ok_name_button = findViewById(R.id.button_name_OK);
        final Button ok_image_button = findViewById(R.id.button_image_OK);

        if (AccessToken.getCurrentAccessToken() != null) {
            AccessToken.setCurrentAccessToken(null);
        }

        /**
         * Show all logged out at app start
        */
        Show(Services.Socials.VK, Services.Socials.OUT);
        Show(Services.Socials.FB, Services.Socials.OUT);
        Show(Services.Socials.OK, Services.Socials.OUT);

        vk_name_button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                ToggleLogin(vk_name_button);
            }
        });

        vk_image_button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                PickImage(Services.Socials.VK);
            }
        });

        /**
         * Facebook login process
        */
        LoginManager.getInstance().registerCallback(callbackManagerFB,
            new FacebookCallback<LoginResult>() {
                @Override public void onSuccess(LoginResult loginResult) {
                    Show(Services.Socials.FB, Services.Socials.IN);
                    Services.SetUsername(Integer.valueOf(loginResult.getAccessToken().getUserId()), fb_name_button);
                    App.Toast(getResources().getString(R.string.authorized) + loginResult.getAccessToken().getUserId());
                }

                @Override public void onCancel() {
                    App.Toast("Login canceled");
                }

                @Override public void onError(FacebookException exception) {
                    App.Log("Facebook login process => FacebookException error:\n" + exception.getMessage());
                }
        });

        /**
         * Facebook logout listener
        */
        fbTracker = new AccessTokenTracker() {
            @Override protected void onCurrentAccessTokenChanged(AccessToken accessToken, AccessToken newAccessToken) {
                if (newAccessToken == null) {
                    App.Toast(getResources().getString(R.string.logged_out));
                    Show(Services.Socials.FB, Services.Socials.OUT);
                }
        }};
        fbTracker.startTracking();

        fb_name_button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                button_login_fb.performClick();
            }
        });

        fb_send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                PickImage(Services.Socials.FB);
            }
        });

        ok_name_button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                ToggleLogin(ok_name_button);
            }
        });

        ok_image_button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                PickImage(Services.Socials.OK);
            }
        });

    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManagerFB.onActivityResult(requestCode, resultCode, data);

        if (odnoklassniki.isActivityRequestOAuth(requestCode)) {
            odnoklassniki.onAuthActivityResult(requestCode, resultCode, data, getAuthListener());
        }
        odnoklassniki.onAuthActivityResult(requestCode, resultCode, data, getAuthListener());

        VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override public void onResult(VKAccessToken vk_auth_token) {
                Services.SetUsername(Integer.parseInt(vk_auth_token.userId), (Button)findViewById(R.id.button_name_VK));
                Show(Services.Socials.VK, Services.Socials.IN);
            }

            @Override public void onError(VKError e) {
                if (e.errorMessage != null) {
                    App.Log("VKSdk.onActivityResult => VKError error:\n" + e.errorMessage);
                } else {
                    App.Log("VKSdk.onActivityResult => VKError error");
                    App.Toast(getResources().getString(R.string.please_login));
                }
                Services.SetUsername(getResources().getInteger(R.integer.zero), (Button)findViewById(R.id.button_name_VK));
                Show(Services.Socials.VK, Services.Socials.OUT);
            }
        });

        /**
         * PickImage for VK succeed*/
        if (requestCode == getResources().getInteger(R.integer.requestVK) && resultCode == RESULT_OK) {
            GetBitmap(Services.Socials.VK, data);
            PostingMessageScene(Services.Socials.VK);
        }

        /**
         * PickImage for FB succeed*/
        if (requestCode == getResources().getInteger(R.integer.requestFB) && resultCode == RESULT_OK) {
            try {
                /**
                 * FB share dialog for image*/
                Bitmap image = GetBitmap(Services.Socials.FB, data);
                SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(image)
                    .build();
                SharePhotoContent content = new SharePhotoContent.Builder()
                    .addPhoto(photo)
                    .build();
                Toast.makeText(getApplicationContext(), "bitmapHeight:" + image.getHeight(), Toast.LENGTH_SHORT).show();
                if (shareDialog.canShow(content, ShareDialog.Mode.WEB)) {
                    shareDialog.show(content, ShareDialog.Mode.WEB);
                }
            } catch (Exception e) {
                App.Log("FB share dialog => Exception:\n" + e.getMessage());
                App.Toast("No share dialog available\n" + e.getMessage());
            }
        }

        /**
         * PickImage for OK succeed*/
        if (requestCode == getResources().getInteger(R.integer.requestOK) && resultCode == RESULT_OK) {
            try {
                GetBitmap(Services.Socials.OK, data);
                PostingMessageScene(Services.Socials.OK);
            } catch (Exception e) {
                App.Log("PickImage for OK => Exception:\n" + e.getMessage());
            }
        }
    }

    /**
     * Triggered at OK authorized
    */
    public OkListener getAuthListener() {
        return new OkListener() {
            @Override public void onSuccess(final JSONObject json) {
                try {
                    if (json.optString("session_secret_key") != ""
                        | json.optString("uid") != ""
                        | json.optString("access_token") != "") {
                        Show(Services.Socials.OK, Services.Socials.IN);
                    } else App.Toast("Something at AuthListener is empty");
                }
                catch (Exception e) {
                    App.Log("OkListener getAuthListener => Exception:\n" + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Couldn't get authorized now\n" + e, Toast.LENGTH_LONG).show();
                }
            }

            @Override public void onError(String e) {
                App.Log("OkListener getAuthListener => Error:\n" + e);
                App.Toast("Authorization error\n" + e);
            }
        };
    }

    /**
     * Toggles login / logout
     */
    protected void ToggleLogin(View social_button) {
        final Button button_name = (Button)social_button;
        String message = button_name.getId() == R.id.button_name_OK
            ? getResources().getString(R.string.logged_in_as) + button_name.getText()
            + getResources().getString(R.string.ask_logout) + getResources().getString(R.string.OK)
            : button_name.getId() == R.id.button_name_VK
            ? getResources().getString(R.string.logged_in_as) + button_name.getText()
            + getResources().getString(R.string.ask_logout) + getResources().getString(R.string.VK)
            : getResources().getString(R.string.logged_in_as) + button_name.getText()
            + getResources().getString(R.string.ask_logout) + getResources().getString(R.string.FB)
            ;

        switch (button_name.getId()) {
            case R.id.button_name_OK:
                if (App.odnoklassniki.getMAccessToken() != null) {
                    AlertDialog.Builder dialog_builder = new AlertDialog.Builder(MainActivity.this);
                    dialog_builder.setMessage(message);
                    dialog_builder
                        .setCancelable(true)
                        .setPositiveButton(getResources().getString(R.string.text_dialog_logout),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    App.Toast(button_name.getText().toString() + getResources().getString(R.string.logged_out));
                                    App.odnoklassniki.clearTokens();

                                    Show(Services.Socials.OK, Services.Socials.OUT);
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.cancel), null);
                    dialog_builder.create().show();
                } else {
                    App.Toast(getResources().getString(R.string.logging_in));
                    button_name.setText(getResources().getString(R.string.logging_in));
                    App.odnoklassniki.requestAuthorization(MainActivity.this,
                        getResources().getString(R.string.OK_REDIRECT_URI), OkAuthType.WEBVIEW_OAUTH,
                        OkScope.PHOTO_CONTENT, OkScope.VALUABLE_ACCESS, OkScope.LONG_ACCESS_TOKEN);
                }
                break;

            case R.id.button_name_VK:
                if (VKSdk.isLoggedIn()) {
                    AlertDialog.Builder dialog_builder = new AlertDialog.Builder(MainActivity.this);
                    dialog_builder.setMessage(message)
                        .setCancelable(true)
                        .setPositiveButton(getResources().getString(R.string.text_dialog_logout),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                App.Toast(
                                button_name.getText().toString()
                                        + getResources().getString(R.string.logged_out));

                                VKAccessToken.removeTokenAtKey(VKUIHelper.getApplicationContext(),
                                    "VK_SDK_ACCESS_TOKEN_PLEASE_DONT_TOUCH");
                                VKAccessToken.currentToken().accessToken = null;
                                VKSdk.logout();
                                Show(Services.Socials.VK, Services.Socials.OUT);
                                }
                            })
                        .setNegativeButton(getResources().getString(R.string.cancel), null);
                    dialog_builder.create().show();
                } else {
                    App.Toast(getResources().getString(R.string.logging_in));
                    button_name.setText(getResources().getString(R.string.logging_in));
                    VKSdk.login(MainActivity.this, scope);
                }
                break;
            default:
        }
    }

    /**
     *  Adjusts Message Posting scene
    */
    protected void PostingMessageScene(final Services.Socials social/*, final JSONObject map*/) {
        final EditText editText = (EditText) findViewById(R.id.edittext);
        FloatingActionButton do_send_message = findViewById(R.id.button_message);
        Button add_friend = findViewById(R.id.button_friend);
        RelativeLayout.LayoutParams cardgroup_params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        switch (social) {
            case OK:
                findViewById(R.id.sending_message_group).setVisibility(View.VISIBLE);
                findViewById(R.id.thumbnail_0).setVisibility(View.VISIBLE);
                findViewById(R.id.card_group_VK).setVisibility(View.INVISIBLE);

                cardgroup_params.removeRule(RelativeLayout.BELOW);
                cardgroup_params.topMargin = App.MakeDIP(getResources().getInteger(R.integer.twenty));
                cardgroup_params.setMarginStart(App.MakeDIP(getResources().getInteger(R.integer.twenty_negative)));
                cardgroup_params.setMarginEnd(App.MakeDIP(getResources().getInteger(R.integer.twenty_negative)));
                findViewById(R.id.card_group_OK).setLayoutParams(cardgroup_params);

            do_send_message.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Services.SendMessage(Services.Socials.OK, editText);
                    if(App.hide_posting_scene) {
                        PostingMessageScene(Services.Socials.OUT);
                        App.hide_posting_scene = false;
                    }
                }
            });

            add_friend.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {}
            });
                break;

            case VK:
                findViewById(R.id.sending_message_group).setVisibility(View.VISIBLE);
                findViewById(R.id.thumbnail_0).setVisibility(View.VISIBLE);
                findViewById(R.id.card_group_VK).setVisibility(View.VISIBLE);

                do_send_message.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Services.SendMessage(Services.Socials.VK, editText);
                    }
                });

                add_friend.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Services.SetFriends(findViewById(R.id.listview_friends));
                    }
                });
                break;

            /**
             * Hides Posting Scene
            */
            case OUT:
                findViewById(R.id.sending_message_group).setVisibility(View.INVISIBLE);
                findViewById(R.id.thumbnail_0).setVisibility(View.INVISIBLE);
                findViewById(R.id.card_group_VK).setVisibility(View.VISIBLE);
                cardgroup_params.addRule(RelativeLayout.BELOW, R.id.card_group_FB);
                cardgroup_params.topMargin = App.MakeDIP(getResources().getInteger(R.integer.twenty));
                cardgroup_params.setMarginStart(App.MakeDIP(getResources().getInteger(R.integer.twenty_negative)));
                cardgroup_params.setMarginEnd(App.MakeDIP(getResources().getInteger(R.integer.twenty_negative)));
                findViewById(R.id.card_group_OK).setLayoutParams(cardgroup_params);
                break;
            default: return;
        }
    }

    /**
     * Switches elements visibilities
    */
    protected void Show(Services.Socials social, Services.Socials state) {
        final TextView welcome = findViewById(R.id.textview_welcome);
        final String not_auth = getResources().getString(R.string.text_not_auth);
        final FloatingActionButton do_send_message = findViewById(R.id.button_message);
        final Drawable door_in = getResources().getDrawable(R.drawable.ic_custom_door_in);
        final Drawable door_out = getResources().getDrawable(R.drawable.ic_custom_door_out);
        final Drawable add_image = getResources().getDrawable(R.drawable.ic_custom_add_image);
        final RelativeLayout sending_message_group = findViewById(R.id.sending_message_group);

        final Button ok_name_button = findViewById(R.id.button_name_OK);
        final Button ok_image_button = findViewById(R.id.button_name_OK);
        final Drawable ok_logo = getResources().getDrawable(R.drawable.ic_custom_logo_ok);

        final Button fb_name_button = findViewById(R.id.button_name_FB);
        final Button fb_send = findViewById(R.id.button_image_FB);
        final Drawable fb_logo = getResources().getDrawable(R.drawable.com_facebook_button_icon);

        final Button vk_name_button = findViewById(R.id.button_name_VK);
        final Button vk_image_button = findViewById(R.id.button_image_VK);
        final Button vk_friend_button = findViewById(R.id.button_friend);
        final ListView friends = findViewById(R.id.listview_friends);
        final Drawable vk_logo = getResources().getDrawable(R.drawable.ic_ab_app);

        vk_logo.setBounds(App.MakeDIP(6), getResources().getInteger(R.integer.zero), vk_logo.getIntrinsicWidth() + App.MakeDIP(6), vk_logo.getIntrinsicHeight());
        fb_logo.setBounds(getResources().getInteger(R.integer.zero), getResources().getInteger(R.integer.zero), fb_logo.getIntrinsicWidth(), fb_logo.getIntrinsicHeight());
        ok_logo.setBounds(getResources().getInteger(R.integer.zero), getResources().getInteger(R.integer.zero), ok_logo.getIntrinsicWidth(), ok_logo.getIntrinsicHeight());
        door_out.setBounds(getResources().getInteger(R.integer.zero), getResources().getInteger(R.integer.zero), door_in.getIntrinsicWidth(), door_in.getIntrinsicHeight());
        door_in.setBounds(getResources().getInteger(R.integer.zero), getResources().getInteger(R.integer.zero), door_in.getIntrinsicWidth(), door_in.getIntrinsicHeight());
        add_image.setBounds(getResources().getInteger(R.integer.zero),getResources().getInteger(R.integer.zero), add_image.getIntrinsicWidth(), add_image.getIntrinsicHeight());
        vk_image_button.setCompoundDrawables(add_image,null, null,null);

        /* Facebook goes in */
        if(social == Services.Socials.FB && state == Services.Socials.IN) {
            welcome.setVisibility(View.INVISIBLE);
            findViewById(R.id.card_image_FB).setVisibility(View.VISIBLE);
        }

        /* Facebook goes out */
        if(social == Services.Socials.FB && state == Services.Socials.OUT) {
            fb_name_button.setCompoundDrawables(fb_logo, null, null, null);
            fb_name_button.setTextScaleX(1.0f);
            fb_name_button.setText(not_auth);
            findViewById(R.id.card_image_FB).setVisibility(View.GONE);
        }

        /* VK goes in */
        if(social == Services.Socials.VK && state == Services.Socials.IN) {
            welcome.setVisibility(View.INVISIBLE);
            vk_name_button.setCompoundDrawables( vk_logo, null, door_out, null );
            findViewById(R.id.card_image_VK).setVisibility(View.VISIBLE);
            do_send_message.setVisibility(View.VISIBLE);
            vk_friend_button.setVisibility(View.VISIBLE);
            friends.setVisibility(View.VISIBLE);
        }

        /* VK goes out */
        if(social == Services.Socials.VK && state == Services.Socials.OUT) {
            vk_name_button.setCompoundDrawables(vk_logo, null, door_in, null);
            vk_name_button.setTextScaleX(1.0f);
            vk_name_button.setText(not_auth);
            findViewById(R.id.card_image_VK).setVisibility(View.GONE);
            sending_message_group.setVisibility(View.GONE);
            findViewById(R.id.card_group_VK);
            findViewById(R.id.card_group_FB);
            findViewById(R.id.card_group_OK);
        }

        /* OK goes in */
        if(social == Services.Socials.OK && state == Services.Socials.IN) {
            Services.SetUsername(getResources().getInteger(R.integer.zero), (Button)findViewById(R.id.button_name_OK));
            findViewById(R.id.card_image_OK).setVisibility(View.VISIBLE);
            welcome.setVisibility(View.INVISIBLE);
        }

        /* OK goes out */
        if(social == Services.Socials.OK && state == Services.Socials.OUT) {
            ok_name_button.setText(getResources().getString(R.string.text_name_OK));
            findViewById(R.id.card_image_OK).setVisibility(View.GONE);
            ok_name_button.setCompoundDrawables(ok_logo,null,null,null);
        }

        if    ((social == Services.Socials.VK && state == Services.Socials.OUT
                    && fb_send.getVisibility() != View.VISIBLE
                    && ok_image_button.getVisibility() != View.VISIBLE)
            || (social == Services.Socials.FB && state == Services.Socials.OUT
                    && vk_image_button.getVisibility() != View.VISIBLE
                    && ok_image_button.getVisibility() != View.VISIBLE)
            || (social == Services.Socials.OK && state == Services.Socials.OUT
                    && vk_image_button.getVisibility() != View.VISIBLE
                    && fb_send.getVisibility() != View.VISIBLE)
        ) {
            PostingMessageScene(Services.Socials.OUT);
            findViewById(R.id.textview_welcome).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Gets Bitmap for upload and sets it as a preview to ImageView
    */
    protected Bitmap GetBitmap(Services.Socials to, Intent data) {
        /* Getting the path from the Uri */
        Uri selectedImageUri = data.getData();
        String path = "";

        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(selectedImageUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();

        if (path != null) {
            File file = new File(path);
            selectedImageUri = Uri.fromFile(file);
        }

        ImageView preView = findViewById(R.id.thumbnail_0);
        preView.setImageURI(selectedImageUri);
        Bitmap photo = null;

        if (to == Services.Socials.OK) {
            Toast.makeText(getApplicationContext(), "Got image from: " + selectedImageUri, Toast.LENGTH_SHORT).show();
            Services.UploadImageOK(data);
        } else {
            try {
                photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                if (to == Services.Socials.VK) {
                    Toast.makeText(getApplicationContext(), "Got bitmap from: " + selectedImageUri, Toast.LENGTH_SHORT).show();
                    Services.UploadImageVK(photo);
                }
            } catch (Exception e) {
                App.Log("GetBitmap => Exception:\n" + e.getMessage());
                Toast.makeText(getApplicationContext(), "Can't handle file\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        return photo;
    }

    /**
     * Starts file select dialog activity with request code
    */
    protected void PickImage(Services.Socials social) {
        Intent chose_file_intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        chose_file_intent.addCategory(Intent.CATEGORY_OPENABLE);
        chose_file_intent.setType("image/*");
        chose_file_intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION );
        if (social == Services.Socials.VK) {
            startActivityForResult(chose_file_intent, getResources().getInteger(R.integer.requestVK));
        }
        if (social == Services.Socials.FB) {
            startActivityForResult(chose_file_intent, getResources().getInteger(R.integer.requestFB));
        }
        if (social == Services.Socials.OK) {
            startActivityForResult(chose_file_intent, getResources().getInteger(R.integer.requestOK));
        }
    }
}
