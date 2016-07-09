package im.het.demo.getapk;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GetApkActivity extends Activity {

    public static final String TAG = "GetApkActivity";

    private String APK_DOWNLOAD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
            "ApkBackup" + File.separator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.act_main);

        List<AppInfo> appList = ApkExtraHelper.getAllAppInfo(getApplicationContext());

        ListView appListView = (ListView) findViewById(R.id.appList);
        AppListAdapter appListAdapter = new AppListAdapter(appList);
        appListView.setAdapter(appListAdapter);

        appListAdapter.notifyDataSetChanged();
    }

    static class AppHolder {
        public TextView appName;
        public TextView packageName;
        public Button appAction;
    }

    class AppListAdapter extends BaseAdapter {

        private List<AppInfo> mAppList;

        AppListAdapter(List<AppInfo> appList) {
            mAppList = appList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppHolder holder = new AppHolder();
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.lv_item_app, null);
                holder.appName = (TextView) convertView.findViewById(R.id.item_title);
                holder.packageName = (TextView) convertView.findViewById(R.id.item_info);
                holder.appAction = (Button) convertView.findViewById(R.id.item_action);
                convertView.setTag(holder);
            }

            final AppInfo appInfo = mAppList.get(position);
            holder = (AppHolder) convertView.getTag();
            holder.appName.setText(appInfo.appName);
            holder.packageName.setText(appInfo.packageName);
            holder.appAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ApkExtraHelper
                            .extraApkByPackageName(getApplicationContext(), appInfo.packageName, APK_DOWNLOAD_DIR);

                    Toast.makeText(GetApkActivity.this , appInfo.appName + " has download to " + APK_DOWNLOAD_DIR , Toast
                            .LENGTH_LONG).show(); ;
                }
            });

            return convertView;
        }

        @Override
        public int getCount() {
            return mAppList == null ? 0 : mAppList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

    }

}
