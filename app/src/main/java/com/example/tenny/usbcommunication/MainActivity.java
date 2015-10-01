package com.example.tenny.usbcommunication;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {
    static final String TAG = "0305-" + MainActivity.class.getSimpleName();
    static final String SERVERIP = "140.113.167.14";
    static final int SERVERPORT = 9000; //8000= echo server, 9000=real server

    final int SYS_MSG = 0x11;
    final int SENSOR_MSG = 0x12;
    final int TIMEOUT = 300;

    private Button btnTurnOn;
    private Button btnTurnOff;
    private ListView lvMessageBox;
    private TextViewAdapter messageAdapter;
    private ArrayList<TextView> messageList;
    private AsyncTask<Void, Integer, String> listeningTask;
    private TextView Pname, Pcode, Iname, Icode, connectState, message, serialText, severState;
    private ScrollForeverTextView msg;
    private EditText scannerInput;
    private ImageView imageStatus;
    private boolean connected;

    private static ProgressDialog pd;
    private String str1, productSerial, itemCode;
    private AsyncTask task = null;
    private UsbManager manager;
    private UsbDevice device;
    private UsbInterface dataInterface;
    private UsbEndpoint endpointOut;
    private UsbEndpoint endpointIn;
    private UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvMessageBox = (ListView) findViewById(R.id.lv_message_box);
        messageList = new ArrayList<TextView>();
        messageAdapter = new TextViewAdapter();
        lvMessageBox.setAdapter(messageAdapter);
        Pname = (TextView) findViewById(R.id.tv2);
        Pcode = (TextView) findViewById(R.id.tv4);
        Iname = (TextView) findViewById(R.id.tv6);
        Icode = (TextView) findViewById(R.id.tv10);
        message = (TextView) findViewById(R.id.textView);
        msg = (ScrollForeverTextView) findViewById(R.id.msg);
        connectState = (TextView) findViewById(R.id.connected);
        imageStatus = (ImageView) findViewById(R.id.imageView);
        scannerInput = (EditText) findViewById(R.id.scannerIn);
        serialText = (TextView) findViewById(R.id.tv9);
        severState = (TextView) findViewById(R.id.tv11);
        connected = false;

        if(!isNetworkConnected()){  //close when not connected
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("警告");
            dialog.setMessage("無網路連線,\n程式即將關閉");
            dialog.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    });
            dialog.show();
            Log.e("Mylog", "no network");
        }
        else {
            pd = ProgressDialog.show(MainActivity.this, "連線中", "Please wait...");  // 開啟一個新線程，在新線程裡執行耗時的方法
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InitServer();  // 耗時的方法
                    InitOkHandler.sendEmptyMessage(0);  // 執行耗時的方法之後發送消給handler
                }
            }).start();
            new Thread(new Runnable() {
                @Override
                public void run() {  // 需要背景作的事
                    try {
                        for (int i = 0; i < 10; i++) {
                            Thread.sleep(1000);
                        }
                        if(connected)
                            return;
                        else {
                            Log.e("Mylog", "1000ms timeout");
                            ServerDownHandler.sendEmptyMessage(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        TextView.OnEditorActionListener scannerTextListener = new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    //example_confirm();//match this behavior to your 'Send' (or Confirm) button
                    productSerial = exampleView.getText().toString();
                    exampleView.setText("");
                    Log.d("Mylog", "Scanner enter captured: " + productSerial);
                    serialText.setText(productSerial);
                    if( productSerial.equals(itemCode) ) {
                        imageStatus.setImageResource(R.drawable.green_circle);
                        sendData("0");
                    }
                    else {
                        imageStatus.setImageResource(R.drawable.red_cross);
                        sendData("1");
                        View view = imageStatus.getRootView();
                        view.setBackgroundColor(getResources().getColor(R.color.red));
                    }
                }
                return true;
            }
        };
        scannerInput.setOnEditorActionListener(scannerTextListener);
        //scannerInput.setInputType(InputType.);
        //scannerInput.setSelected(true);
        task = new UpdateTask().execute();
    }

    private void InitServer() {
        SocketHandler.closeSocket();
        SocketHandler.initSocket(SERVERIP, SERVERPORT);
        String init = "CONNECT\tFF_1<END>";
        SocketHandler.writeToSocket(init);
        str1 = SocketHandler.getOutput();
        Log.d("Mylog", str1);
    }

    private boolean isNetworkConnected(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private Handler InitOkHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息後就會執行此方法
            updateUI();
            pd.dismiss();// 關閉ProgressDialog
        }
    };

    private Handler ServerDownHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息後就會執行此方法
            pd.dismiss();// 關閉ProgressDialog
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("警告");
            dialog.setMessage("伺服器無回應,\n程式即將關閉");
            dialog.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    });
            dialog.show();
        }
    };

    private void updateUI() {
        if(str1.contains("CONNECT_OK")) {
            //message.setText("伺服器辨識成功");
            severState.setText("伺服器辨識成功");
            severState.setTextColor(Color.GREEN);
            connected = true;
        }
        else
            message.setText(str1);
        msg.setText("1234567890wwwwwwwwwwwwwwwwwwwwww1234567890...1234567890wwwwwwwwwwwwwwwwwwwwww1234567890..." +
                "1234567890wwwwwwwwwwwwwwwwwwwwww1234567890...1234567890wwwwwwwwwwwwwwwwwwwwww1234567890...");
        //msg.setSelected(true);
        scannerInput.requestFocus();
        tryGetUsbPermission();
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                        if(null != usbDevice){
                            afterGetUsbPermission(usbDevice);
                        }
                    }
                    else {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
                        Toast.makeText(context, String.valueOf("Permission denied for device" + usbDevice), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };

    private void tryGetUsbPermission(){
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbPermissionActionReceiver, filter);
        //here do emulation to ask all connected usb device for permission
        for (final UsbDevice usbDevice : manager.getDeviceList().values()) {
            //add some conditional check if necessary
            //if(isWeCaredUsbDevice(usbDevice)){
            if(usbDevice.getVendorId() == 9025) {
                if (manager.hasPermission(usbDevice)) {
                    //if has already got permission, just goto connect it
                    //that means: user has choose yes for your previously popup window asking for grant perssion for this usb device
                    //and also choose option: not ask again
                    afterGetUsbPermission(usbDevice);
                } else {
                    //this line will let android popup window, ask user whether to allow this app to have permission to operate this usb device
                    manager.requestPermission(usbDevice, mPermissionIntent);
                }
                connectState.setText("Connected");
                connectState.setTextColor(Color.GREEN);
                return;
            }
        }

        Log.e(TAG, "after for loop. no device found.");
        //if(!isUsbFound) {
            connectState.setText("Not connected");
            connectState.setTextColor(Color.RED);
        //}
    }

    private void afterGetUsbPermission(UsbDevice usbDevice) {
        //manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Log.d(TAG, "afterGetUsbPermission.");
        try {
            setupUsb();
            readData();
        } catch (IOException e) {
            Log.e(TAG, "Can not find USB device.");
        }

        btnTurnOn = (Button) findViewById(R.id.btn_turn_on);
        btnTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("1");
            }
        });
        btnTurnOff = (Button) findViewById(R.id.btn_turn_off);
        btnTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("0");
                View view = v.getRootView();
                view.setBackgroundColor(getResources().getColor(R.color.background));
            }
        });
    }

    /*@Override
    protected void onResume() {
        super.onResume();
        //tryGetUsbPermission();
    }*/
    /*@Override
    protected void onPause() {
        super.onPause();
        listeningTask.cancel(true);
    }*/

    private void setupUsb() throws IOException {

        for (UsbDevice d : manager.getDeviceList().values()) {
            String s = "Found device: " + d.getDeviceName();
            Log.d(TAG, s);
            updateMessage(s, SYS_MSG);
            if(d.getVendorId() == 9025) {
                device = d;
                Log.d(TAG, "Arduino Uno found!");
                break;
            }
        }
        if (null == device) {
            throw new IOException();
        }

        dataInterface = device.getInterface(1);
        for (int j=0; j<dataInterface.getEndpointCount(); j++) {
            UsbEndpoint ep = dataInterface.getEndpoint(j);
            if (ep.getDirection() == UsbConstants.USB_DIR_OUT && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                Log.d(TAG, "Found usb dir out endpoint.:" + j);
                Log.d("Mylog", "Found usb dir out endpoint.:" + j);
                endpointOut = dataInterface.getEndpoint(j);
            }
            if (ep.getDirection() == UsbConstants.USB_DIR_IN && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                Log.d(TAG, "Found usb dir in endpoint.:" + j);
                endpointIn = dataInterface.getEndpoint(j);
            }
        }
        while(connection == null) {
            connection = manager.openDevice(device);
        }
        connection.claimInterface(dataInterface, true);
    }

    private void sendData(final String data) {
        final byte[] bytes = data.getBytes();
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connection.bulkTransfer(endpointOut, bytes, bytes.length, TIMEOUT);
                Time time = new Time("Asia/Tokyo");
                time.setToNow();
                String msg = time.year + "/" + (time.month + 1) + "/" + time.monthDay + " " + time.hour + ":" + time.minute + ":" + time.second;
                if (data.equals("1")) {
                    msg = "Turn on light at " + msg;
                } else {
                    msg = "Turn off light at " + msg;
                }
                updateMessage(msg, SYS_MSG);
            }
        });
    }

    private void readData() {
        listeningTask = new AsyncTask<Void, Integer, String>() {
            private boolean isCancelled = false;
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                while (true) {
                    if (isCancelled) return null;
                    byte[] mReadBuffer = new byte[16];
                    int numBytesRead = connection.bulkTransfer(endpointIn, mReadBuffer, mReadBuffer.length, 300);
                    if (numBytesRead > 0) {
                        for (int i=0; i<numBytesRead; i++) {
                            //Log.d(TAG, "value = " + (mReadBuffer[i] & 0xff));
                            if ((mReadBuffer[i] & 0xff) == 13) {
                                return msg;
                            }
                            msg += (char)(mReadBuffer[i] & 0xff);
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(String msg) {
                if (null != msg) {
                    String s = "Got light sensor value = " + msg;
                    Log.d(TAG, s);
                    updateMessage(s, SENSOR_MSG);
                    //SystemClock.sleep(1000);
                    readData();
                }
            }

            @Override
            protected void onCancelled() {
                isCancelled = true;
            }
        };
        listeningTask.execute();
    }

    private void updateMessage(String msg, int type) {
        TextView v = new TextView(this);
        v.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.WRAP_CONTENT, ListView.LayoutParams.WRAP_CONTENT));
        switch (type) {
            case SENSOR_MSG:
                v.setTextColor(getResources().getColor(R.color.orange));
                break;
            case SYS_MSG:
                v.setTextColor(getResources().getColor(R.color.blue));
                break;
        }
        v.setText(msg);
        messageList.add(v);
        messageAdapter.notifyDataSetChanged();
    }

    public class TextViewAdapter extends BaseAdapter {

        public int getCount() {
            return messageList.size();
        }

        public long getItemId(int pos) {
            return 0;
        }

        public View getItem(int pos) {
            return null;
        }

        public View getView(int pos, View v, ViewGroup parent) {
            return messageList.get(pos);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            lvMessageBox.invalidate();
            lvMessageBox.setSelection(getCount() - 1);
        }
    }

    private class UpdateTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... v) {
            //Log.d("Mylog", "UpdateTask listening0...");
            while(!isCancelled() && connected){
                Log.d("Mylog", "UpdateTask listening...");
                String result;
                result = SocketHandler.getOutput();
                publishProgress(result);
                Log.d("Mylog", "result=" + result);
            }
            return null;
        }
        protected void onProgressUpdate(String... values) {
            String result = values[0];
            String[] lines = result.split("<END>");
            for(String s: lines) {
                if(s != null && s.contains("MSG")) {
                    s = s.replaceAll("MSG\t", "");
                    s = s.replaceAll("<N>", "\n");
                    s = s.replaceAll("<END>", "");
                    msg.setText(s);
                }
                else if(s != null && s.contains("LIST")) {
                    s = s.replaceAll("LIST\t", "");
                    s = s.replaceAll("<N>", "\n");
                    s = s.replaceAll("<END>", "");
                    //String line = values[0];
                    String[] items = s.split("\t");
                    if(items.length >= 4) {
                        Pcode.setText(items[0]);
                        Pname.setText(items[1]);
                        Icode.setText(items[2]);
                        itemCode = items[2];
                        Iname.setText(items[3]);
                    }
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d("Mylog", "back is pressed");
            unregisterReceiver(mUsbPermissionActionReceiver);
            Log.d("Mylog", "back is pressed 2");
            task.cancel(true);
            Log.d("Mylog", "back is pressed 3");
            SocketHandler.closeSocket();
            Log.d("Mylog", "back is pressed 4");
            finish();
        }
        /*if(keyCode == KeyEvent.KEYCODE_ENTER) {
            Log.d("Mylog", "Enter is pressed");
        }*/
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onStop(){
        //listeningTask.cancel(true);
        task.cancel(true);
        SocketHandler.closeSocket();
        finish();
        super.onStop();
    }
}

