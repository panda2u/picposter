package com.panda2u.poster;
import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;

import com.vk.sdk.VKSdk;
import com.vk.sdk.api.model.VKList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import ru.ok.android.sdk.Odnoklassniki;

public class App extends android.app.Application {
    private static float density;
    private static Context context;
    public static boolean hide_posting_scene = false;

    public static VKList friends_VKlist = null;
    public static int friend_clicks = 0;
    public static StringBuilder testlog_text = new StringBuilder("");
    public static int testlog_counter = 0;

    public static Odnoklassniki odnoklassniki;

    @Override public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        VKSdk.initialize(context);
        VKSdk.logout();

        odnoklassniki = Odnoklassniki.createInstance(
                context,
                context.getResources().getString(R.string.OK_APP_ID),
                context.getResources().getString(R.string.OK_PUBLIC_KEY));

        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public void onTerminate() {
        VKSdk.logout();
        super.onTerminate();
    }

    public static String GetAppDataDir() {
        return context.getApplicationInfo().dataDir;
    }

    public static Context GetContext() {
        return context;
    }

    public static void TestLogWrite(EditText edittext_view) {
        Log(String.format("TagFriend: %s", testlog_counter +1));
        Toast(String.format("TagFriend: %s", testlog_counter +1));
        if (testlog_counter < 5) { testlog_counter++; }
        else {
            File xFile = new File(App.GetAppDataDir() + context.getResources().getString(R.string.cache)+ "log.txt");
            if (xFile.exists()) {
                if (testlog_text.toString() != "") { Toast(testlog_text.toString()); }
                try {
                    BufferedReader br = new BufferedReader(new FileReader(xFile));
                    String line;

                    while ((line = br.readLine()) != null) {
                        testlog_text.append(line);
                        testlog_text.append('\n');
                    }
                    br.close();
                    xFile.deleteOnExit();
                } catch (IOException e) {
                    Toast("IO: " + e.getMessage());
                }
            }
            edittext_view.setText(testlog_text.toString());
            testlog_text = new StringBuilder();

            testlog_counter = 0;
            new File(App.GetAppDataDir() + context.getResources().getString(R.string.cache)+ "log.txt").delete();
        }
    }

    public static void TestLogShow(EditText edittext_view) {
        File xFile = new File(App.GetAppDataDir() + context.getResources().getString(R.string.cache)+ "log.txt");
        if (xFile.exists()) {
            if (testlog_text.toString() != "") { Toast(testlog_text.toString()); }
            try {
                BufferedReader br = new BufferedReader(new FileReader(xFile));
                String line;

                while ((line = br.readLine()) != null) {
                    testlog_text.append(line);
                    testlog_text.append('\n');
                }
                br.close();
                xFile.deleteOnExit();
            } catch (IOException e) {
                Toast.makeText(context, "IO: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        edittext_view.setText(testlog_text.toString());
        testlog_text = new StringBuilder();

        testlog_counter = 0;
        xFile.delete();
    }

    /** Returns number of pixels (which are required by Android methods)
     * for dip sizes representation regarding device display properties
     * dip: desired number of density-independent-pixels
     */
    public static int MakeDIP(int dip) {
        return Math.round(density) * dip;
    }

    /**
     * Log writing
     */
    public static void Log(String text) {
        File logFile = new File(App.GetAppDataDir() + context.getResources().getString(R.string.cache)
                + context.getResources().getString(R.string.logfile_name));
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) { e.printStackTrace(); }
        }

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Toasting
     */
    protected static void Toast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
