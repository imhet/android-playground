package im.het.demo.smsmanager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

public enum SmsHelper {

    INSTANCE;

    private static final String TAG = "smsmanager";
    private static final String ACTION_SMS_SEND = "action_send";
    private static final String ACTION_SMS_DELIVER = "action_delivered";
    private Uri URI_SMS = Uri.parse("content://sms");
    private static final int SEND_SMS_TIMEOUT = 5000;

    private SmsSendReceiver mSmsSendReceiver;
    private List<OnSmsSendListener> mOnSmsSendListener = new ArrayList<OnSmsSendListener>();
    private Handler mHandler;

    private String mReceivedAddress;
    private long mReceivedDate;
    private SmsReceiveObserver mSmsReceivedObserver;
    private List<OnSmsReceivedListener> mOnSmsReceivedListener = new ArrayList<OnSmsReceivedListener>();

    private Context mContext;

    private Runnable mSendSmsTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            applySmsSendListener(SmsManager.RESULT_ERROR_GENERIC_FAILURE, "send sms reject by user");
        }
    };

    /**
     * 发送短信
     *
     * @param context
     * @param number   号码
     * @param message  短信信息
     * @param listener 发送成功或失败的回调
     */
    public void send(Context context, String number, String message, OnSmsSendListener listener) {
        if (context == null) {
            throw new RuntimeException("context can not be null");
        }

        mContext = context.getApplicationContext();

        if (TextUtils.isEmpty(number) || TextUtils.isEmpty(message)) {
            throw new RuntimeException("number or message can not be empty");
        }

        if (listener != null) {
            mOnSmsSendListener.add(listener);
        }

        registerSendReceiver(context);

        try {
            PendingIntent sendIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_SMS_SEND), 0);
            PendingIntent deliverIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_SMS_DELIVER), 0);
            SmsManager.getDefault().sendTextMessage(number, null, message, sendIntent, deliverIntent);
        } catch (Exception e) {
            e.printStackTrace();
            applySmsSendListener(SmsManager.RESULT_ERROR_GENERIC_FAILURE, e.getMessage());
        }

        if (mHandler == null) {
            mHandler = new Handler();
        }

        // 在小米魅族等定制ROM的手机上拒绝发送短信权限没有回调或提示,这里启动超时检查,5秒内没有短信发送回调则认为用户主动拒绝了权限
        // 使用无法判断运行时权限 context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        mHandler.postDelayed(mSendSmsTimeoutRunnable, SEND_SMS_TIMEOUT);
    }

    private void registerSendReceiver(Context context) {
        if (mSmsSendReceiver == null) {
            mSmsSendReceiver = new SmsSendReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SMS_DELIVER);
            filter.addAction(ACTION_SMS_SEND);
            context.registerReceiver(mSmsSendReceiver, filter);
        }
    }

    private void unRegisterSendReceiver(Context context) {
        if (mSmsSendReceiver != null && context != null) {
            try {
                context.unregisterReceiver(mSmsSendReceiver);
                mSmsSendReceiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSmsSendResult(Intent intent, int resultCode) {
        String action = intent.getAction();

        Log.d(TAG, action + " , " + resultCode);

        String desc;
        switch (resultCode) {
            case Activity.RESULT_OK:
                desc = action + " success";
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
            default:
                desc = action + " failed";
                break;
        }

        applySmsSendListener(resultCode, desc);
    }

    private void applySmsSendListener(int code, String desc) {
        if (mOnSmsSendListener != null && mOnSmsSendListener.size() > 0) {
            for (OnSmsSendListener l : mOnSmsSendListener) {
                l.onSmsSend(code, desc);
            }

            mOnSmsSendListener.clear();
            unRegisterSendReceiver(mContext);
        }

        if (mHandler != null && mSendSmsTimeoutRunnable != null) {
            mHandler.removeCallbacks(mSendSmsTimeoutRunnable);
        }
    }

    /**
     * 监听指定号码和时间的短信并返回
     *
     * @param context
     * @param address  指定号码
     * @param date     时间
     * @param listener 接收短信的回调
     */
    public void receive(Context context, String address, long date, OnSmsReceivedListener listener) {
        if (context == null) {
            throw new RuntimeException("context can not be null");
        }

        mContext = context.getApplicationContext();
        mReceivedAddress = address;
        mReceivedDate = date;

        if (listener != null) {
            mOnSmsReceivedListener.add(listener);
        }

        registerSmsReceivedObserver(context);

        readSms(context);
    }

    private void registerSmsReceivedObserver(Context context) {
        mSmsReceivedObserver = new SmsReceiveObserver(null);
        context.getContentResolver().registerContentObserver(URI_SMS, true, mSmsReceivedObserver);
    }

    private void unRegisterSmsReceivedObserver(Context context) {
        if (context != null && mSmsReceivedObserver != null) {
            context.getContentResolver().unregisterContentObserver(mSmsReceivedObserver);
        }
    }

    private void readSms(Context context, String[] projection, String selection, String[] selectionArgs,
                         String sortOrder) {
        List<SmsData> result = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(URI_SMS, projection, selection, selectionArgs, sortOrder);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cursor != null) {
            int idIndex = cursor.getColumnIndex("_id");
            int addressIndex = cursor.getColumnIndex("address");
            int bodyIndex = cursor.getColumnIndex("body");
            int statusIndex = cursor.getColumnIndex("status");
            int typeIndex = cursor.getColumnIndex("type");
            int serviceCenterIndex = cursor.getColumnIndex("service_center");
            int dateIndex = cursor.getColumnIndex("date");

            while (cursor.moveToNext()) {
                SmsData data = new SmsData();

                data.id = cursor.getLong(idIndex);
                data.address = cursor.getString(addressIndex); // 发件人地址
                data.body = cursor.getString(bodyIndex); // 短信内容
                data.status = cursor.getInt(statusIndex); // 短信状态 -1 接收 , 0 complete , 64 pending , 128 failed
                data.type = cursor.getInt(typeIndex); // 短信类型1是接收到的，2是已发出
                data.serviceCenter = cursor.getString(serviceCenterIndex); // 短信服务中心号码编号
                data.date = cursor.getLong(dateIndex); // 日期，long型，如1346988516，可以对日期显示格式进行设置

                result.add(data);
            }
        }

        applySmsReceivedListener(result);
    }

    private void readSms(Context context) {
        String[] projection = null;
        String selection = "address = ? AND date < ?"; // null ;
        String[] selectionArgs = new String[] {mReceivedAddress, String.valueOf(mReceivedDate)}; // null ;
        String sortOrder = "date desc";

        readSms(context, projection, selection, selectionArgs, sortOrder);
    }

    private void applySmsReceivedListener(List<SmsData> smsList) {
        if (mOnSmsReceivedListener != null && mOnSmsReceivedListener.size() > 0) {
            for (OnSmsReceivedListener l : mOnSmsReceivedListener) {
                l.onSmsReceived(smsList);
            }

            mOnSmsReceivedListener.clear();

            unRegisterSmsReceivedObserver(mContext);
        }
    }

    private class SmsSendReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleSmsSendResult(intent, getResultCode());
        }
    }

    private class SmsReceiveObserver extends ContentObserver {

        public SmsReceiveObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            readSms(mContext);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    }

    class SmsData {
        public long id;
        public String address;
        public String body;
        public int status;
        public int type;
        public String serviceCenter;
        public long date;

        @Override
        public String toString() {
            return "SmsData{" +
                    "address='" + address + '\'' +
                    ", id=" + id +
                    ", body='" + body + '\'' +
                    ", status=" + status +
                    ", type=" + type +
                    ", serviceCenter='" + serviceCenter + '\'' +
                    ", date=" + date +
                    '}';
        }
    }

    interface OnSmsSendListener {
        void onSmsSend(int code, String desc);
    }

    interface OnSmsReceivedListener {
        void onSmsReceived(List<SmsData> smsList);
    }

}
