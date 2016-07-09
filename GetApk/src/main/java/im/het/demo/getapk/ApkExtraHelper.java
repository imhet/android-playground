package im.het.demo.getapk;

import static im.het.demo.getapk.GetApkActivity.TAG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class ApkExtraHelper {

    public static List<AppInfo> getAllAppInfo(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> allPackages = packageManager.getInstalledPackages(0);
        List<AppInfo> appList = new ArrayList<>();

        for (int i = 0; i < allPackages.size(); i++) {
            PackageInfo packageInfo = allPackages.get(i);
                String name = packageInfo.applicationInfo.loadLabel(packageManager).toString();

            try {
                if (!isSystemApp(packageInfo)) {
                    AppInfo info = new AppInfo();
                    info.appName = name;
                    info.packageName = packageInfo.packageName;
                    appList.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        return appList;
    }

    public static void extraApkByPackageName(Context context, String packageName, String downloadDir) {
        try {
            String apkPath = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;

            extraApkToSDCard(apkPath, packageName, downloadDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void extraAllApk(Context context, String downloadDir) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> allPackages = packageManager.getInstalledPackages(0);
        for (int i = 0; i < allPackages.size(); i++) {
            PackageInfo packageInfo = allPackages.get(i);
            String path = packageInfo.applicationInfo.sourceDir;
            String name = packageInfo.applicationInfo.loadLabel(packageManager).toString();

            try {
                if (!isSystemApp(packageInfo)) {
                    Log.i(TAG, path + " " + name);
                    extraApkToSDCard(path, name, downloadDir);
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private static boolean isSystemApp(PackageInfo pInfo) {
        return (((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) && (
                (pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0));
    }

    private static void extraApkToSDCard(String apkPath, String apkName, String downloadApkDir) throws IOException {
        File apkDownloadDir = new File(downloadApkDir);
        if (!apkDownloadDir.exists()) {
            apkDownloadDir.mkdirs();
        }

        File in = new File(apkPath);
        File out = new File(apkDownloadDir, apkName + ".apk");
        if (!out.exists()) {
            out.createNewFile();
        }
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);

        int count;
        byte[] buffer = new byte[256 * 1024];
        while ((count = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, count);
        }

        fis.close();
        fos.flush();
        fos.close();
    }

}



