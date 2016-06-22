package im.het.demo.smsmanager;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "smsmanager";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);

        findViewById(R.id.sendSmsBySystem).setOnClickListener(this);
        findViewById(R.id.sendSms).setOnClickListener(this);
        findViewById(R.id.receiveSms).setOnClickListener(this);
        findViewById(R.id.deleteSms).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendSmsBySystem:
                openSendSmsActivity();
                break;
            case R.id.sendSms:
                sendSms();
                break;
            case R.id.receiveSms:
                receiveSms();
                break;
            case R.id.deleteSms:
                deleteSms();
                break;
            default:
                break;
        }
    }

    /**
     * 调起系统的发短信界面,无需权限
     */
    void openSendSmsActivity() {
        Uri uri = Uri.parse("smsto:10086");
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", "test");
        startActivity(it);
    }

    /**
     * 发送短信
     * <p/>
     * 需要权限 android.permission.SEND_SMS
     */
    void sendSms() {
        // 发送测试短信给 10086
        String content = "hello test" + (new Date()).toString();
        String number = "10086";

        SmsHelper.INSTANCE.send(this, number, content, new SmsHelper.OnSmsSendListener() {
            @Override
            public void onSmsSend(int code, String desc) {
                Log.i(TAG, "code : " + code + " , desc : " + desc);
            }
        });
    }

    /**
     * 监听短信,指定联系人和时间
     * <p/>
     * 需要权限 android.permission.RECEIVE_SMS 和 android.permission.READ_SMS
     */
    void receiveSms() {

        // 监听 10086 短信
        String address = "10086";
        long date = System.currentTimeMillis();

        SmsHelper.INSTANCE.receive(this, address, date, new SmsHelper.OnSmsReceivedListener() {
            @Override
            public void onSmsReceived(List<SmsHelper.SmsData> smsList) {
                if (smsList != null) {
                    Log.i(TAG, smsList.size() + " ");
                    for (SmsHelper.SmsData data : smsList) {
                        Log.i(TAG, data.toString());
                    }
                }
            }
        });
    }

    /**
     * 根据条件删除短信
     * <p/>
     * 需要权限 android.permission.WRITE_SMS
     */
    void deleteSms() {
        Uri uriSms = Uri.parse("content://sms");

        // 删除发送给 10086 的短信
        String whereCause = "address like ? AND type = ?";
        String smsNumber = "10086" + "%";
        String smsType = "2";
        String[] selectionArgs = new String[] {smsNumber, smsType};

        int result = getContentResolver().delete(uriSms, whereCause, selectionArgs);

        Log.i(TAG, whereCause + " , result = " + result);
    }

}
