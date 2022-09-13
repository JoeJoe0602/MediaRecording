package comp5216.sydney.edu.au.mediarecording.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

public class Utils {


    /***
     * 是否是wifi
     * @param context
     * @return
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * 对网络连接状态进行判断
     * @return true, 可用； false， 不可用
     */

    public static boolean isOpenNetwork(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connManager.getActiveNetworkInfo() != null) {
            return connManager.getActiveNetworkInfo().isAvailable();
        }
        return false;

    }

    /**
     * 存 文件上传状态的
     * @param activity
     * @param documentId
     * @param sessionUri
     */
    public static void saveSharedPreferences(Activity activity,String documentId,Uri sessionUri){
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(documentId, sessionUri.toString());
        editor.apply();
    }

    /**
     * 读文件上传状态
     * @param activity
     * @param documentId
     * @return
     */
    public static Uri readSharedPreferences(Activity activity, String documentId){
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        try {
            return Uri.parse(sharedPref.getString(documentId,""));
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除已完成的状态
     * @param activity
     * @param documentId
     */
    public static void deleteSharedPreferences(Activity activity, String documentId){
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(documentId);
        editor.commit();
    }

}
