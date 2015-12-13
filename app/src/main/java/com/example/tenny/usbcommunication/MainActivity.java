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
import android.support.v4.content.res.ResourcesCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {
    static final String TAG = "0305-" + MainActivity.class.getSimpleName();
    static final String SERVERIP = "192.168.1.250";//"140.113.167.14";
    static final int SERVERPORT = 9000; //8000= echo server, 9000=real server
    static final int SEEK_DEST = 95;
    static final int MAX_LINE = 9;
    static final String BOARD_ID = "FF_1";
    static final boolean debug = false;

    //final int SYS_MSG = 0x11;
    //final int SENSOR_MSG = 0x12;
    final int TIMEOUT = 300;

    private Button btnTurnOn;
    private Button btnTurnOff;
    private ListView lvMessageBox, valueListView, boxListView;
    private TextViewAdapter messageAdapter;
    private ArrayList<TextView> messageList;
    private ValueAdapter valueAdapter;
    private ArrayList<ValueItem> valueArray;
    private BoxAdapter boxAdapter;
    private ArrayList<BoxItem> boxArray;
    private ArrayList<String> nextBrandArray;
    private ArrayAdapter<String> nextBrandAdapter;
    private AsyncTask<Void, Integer, String> listeningTask;
    private TextView Pname, Pcode, Iname, Icode, connectState, message, serialText, severState, countTV, swapTitle, swapMsg, workerID;
    private ScrollForeverTextView msg;
    private EditText scannerInput;
    private ImageView imageStatus;
    private boolean connected, need_to_send, swapWorking, swapEnd, bc_msg_reply, bc_msgWorking, notOnstop=false, hasProduct;
    private int count;
    private static ProgressDialog pd;
    private String str1, productSerial, itemCode, snedString, returnWorkerID;
    private AsyncTask task = null;
    private UsbManager manager;
    private UsbDevice device;
    private UsbInterface dataInterface;
    private UsbEndpoint endpointOut;
    private UsbEndpoint endpointIn;
    private UsbDeviceConnection connection;
    private int connectionTimeoutCount;
    private SeekBar mySeekBar;
    private Button n1, n2, n3, n4, n5, n6, n7,n8, n9, n0, btn_enter, btn_delete;
    private MyTabHost tabHost;
    private AlertDialog dialog;
    static boolean active = false;
    private Spinner brandSelector;
    private int returnBrandName=0;
    private RelativeLayout layout1;

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
        workerID = (TextView) findViewById(R.id.workerID);
        connected = false;
        need_to_send = false;
        countTV = (TextView) findViewById(R.id.count);
        count = 0;
        returnWorkerID = "";
        //connectionTimeoutCount = 0;
        swapMsg = (TextView) findViewById(R.id.swap_msg);
        swapMsg.setVisibility(View.INVISIBLE);
        swapTitle = (TextView) findViewById(R.id.swapTitle);
        mySeekBar = (SeekBar) findViewById(R.id.myseek);
        mySeekBar.setEnabled(false);
        mySeekBar.setVisibility(View.GONE);
        swapWorking = false;
        swapEnd = false;
        bc_msg_reply = false;
        bc_msgWorking = false;
        valueListView = (ListView) findViewById(R.id.valueListView);
        boxListView = (ListView) findViewById(R.id.boxListView);
        valueArray = new ArrayList<ValueItem>();
        boxArray = new ArrayList<BoxItem>();
        valueAdapter = new ValueAdapter(MainActivity.this, valueArray);
        boxAdapter = new BoxAdapter(MainActivity.this, boxArray);
        valueListView.setAdapter(valueAdapter);
        boxListView.setAdapter(boxAdapter);
        //create 9 box lines
        for (int i=1; i<=MAX_LINE; i++) {
            BoxItem b = new BoxItem(String.valueOf(i), "0", "0");
            boxArray.add(b);
            ValueItem v = new ValueItem("(無)", "0", "0", "0", "0", "0", "0", "0", "0", "0", "");
            valueArray.add(v);
        }
        boxAdapter.notifyDataSetChanged();
        valueAdapter.notifyDataSetChanged();

        tabHost = (MyTabHost)findViewById(R.id.tabHost);
        tabHost.setup();
        tabHost.clearFocus();
        TabHost.TabSpec spec=tabHost.newTabSpec("tab1");
        spec.setContent(R.id.tab1layout);
        spec.setIndicator("品 管");
        tabHost.addTab(spec);
        spec=tabHost.newTabSpec("tab2");
        spec.setIndicator("數 量");
        spec.setContent(R.id.tab2layout);
        tabHost.addTab(spec);
        tabHost.setCurrentTab(0);
        TabWidget tabWidget = (TabWidget)tabHost.findViewById(android.R.id.tabs);
        View tabView = tabWidget.getChildTabViewAt(0);
        TextView tab = (TextView)tabView.findViewById(android.R.id.title);
        tab.setTextSize(24);
        tabView = tabWidget.getChildTabViewAt(1);
        tab = (TextView)tabView.findViewById(android.R.id.title);
        tab.setTextSize(24);

        brandSelector = (Spinner) findViewById(R.id.brandSelecter);
        nextBrandArray = new ArrayList<String>();
        nextBrandArray.add("(無)");
        nextBrandAdapter = new ArrayAdapter<String>(MainActivity.this,  android.R.layout.simple_spinner_dropdown_item, nextBrandArray);
        brandSelector.setAdapter(nextBrandAdapter);
        brandSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                returnBrandName = pos;
            }

            public void onNothingSelected(AdapterView<?> parent) {
                returnBrandName = 0;
            }
        });

        n0 = (Button) findViewById(R.id.int0);
        n1 = (Button) findViewById(R.id.int1);
        n2 = (Button) findViewById(R.id.int2);
        n3 = (Button) findViewById(R.id.int3);
        n4 = (Button) findViewById(R.id.int4);
        n5 = (Button) findViewById(R.id.int5);
        n6 = (Button) findViewById(R.id.int6);
        n7 = (Button) findViewById(R.id.int7);
        n8 = (Button) findViewById(R.id.int8);
        n9 = (Button) findViewById(R.id.int9);
        n0.setOnClickListener(numberListener);
        n1.setOnClickListener(numberListener);
        n2.setOnClickListener(numberListener);
        n3.setOnClickListener(numberListener);
        n4.setOnClickListener(numberListener);
        n5.setOnClickListener(numberListener);
        n6.setOnClickListener(numberListener);
        n7.setOnClickListener(numberListener);
        n8.setOnClickListener(numberListener);
        n9.setOnClickListener(numberListener);
        btn_enter = (Button) findViewById(R.id.btn_enter);
        btn_enter.setOnClickListener(enterListener);
        btn_enter.setEnabled(false);
        btn_delete = (Button) findViewById(R.id.btn_del);
        btn_delete.setOnClickListener(deleteListener);
        layout1 = (RelativeLayout) findViewById(R.id.layout1);

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
                        if(!connected) {
                            Log.e("Mylog", "1000ms timeout");
                            ServerDownHandler.sendEmptyMessage(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    while (scannerInput != null && !scannerInput.hasFocus())
                        scannerInput.requestFocus();
                }
            });
        }

        TextView.OnEditorActionListener scannerTextListener = new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    //example_confirm();//match this behavior to your 'Send' (or Confirm) button
                    if(!hasProduct) return true;
                    productSerial = exampleView.getText().toString();
                    exampleView.setText("");
                    Log.d("Mylog", "Scanner enter captured: " + productSerial);
                    serialText.setText(productSerial);
                    if( productSerial.equals(itemCode) ) {
                        Log.d("Mylog", "1 itemCode=" + itemCode);
                        imageStatus.setImageResource(R.drawable.green_circle);
                        sendData("0");
                        count++;
                        snedString = "UPDATE\tLIST\t" + productSerial + "\t10<END>";
                        need_to_send = true;

                        layout1.setBackgroundColor(getResources().getColor(R.color.background));
                    }
                    else {
                        Log.d("Mylog", "2 itemCode=" + itemCode);
                        imageStatus.setImageResource(R.drawable.red_cross);
                        sendData("1");
                        layout1.setBackgroundColor(getResources().getColor(R.color.yellow));
                    }
                }
                return true;
            }
        };
        scannerInput.setOnEditorActionListener(scannerTextListener);
        //scannerInput.setInputType(InputType.);
        //scannerInput.setSelected(true);
        scannerInput.setFocusableInTouchMode(true);
        scannerInput.requestFocus();
        scannerInput.setOnTouchListener(foucsHandler);
        Log.d("Mylog", "Before new task");
        task = new UpdateTask().execute();
    }

    View.OnTouchListener foucsHandler = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View arg0, MotionEvent event) {
            arg0.requestFocusFromTouch();
            return false;
        }
    };

    private void InitServer() {
        SocketHandler.closeSocket();
        SocketHandler.initSocket(SERVERIP, SERVERPORT);
        String init = "CONNECT\t" + BOARD_ID + "<END>";
        SocketHandler.writeToSocket(init);
        str1 = SocketHandler.getOutput();
        //Log.d("Mylog", str1);
        if(str1!=null && str1.contains("CONNECT_EXIST")) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
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
            Log.e("Mylog", "ServerDownHandler: connect failed!");
            if(dialog!=null && dialog.isShowing()) return;
            if(active) {
                dialog = new AlertDialog.Builder(MainActivity.this).create();
                dialog.setTitle("警告");
                dialog.setMessage("伺服器無回應，\n5秒後自動重新連線，若問題持續請洽系統管理員");
                dialog.setButton("關閉程式",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                                Log.d("mylog", "to finish task...");
                            }
                        });
                dialog.show();
            }
            notOnstop = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("mylog", "wait 5000ms");
                    try { Thread.sleep(5000); }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("mylog", "InterruptedException e=" + e);
                    }
                    if(dialog!=null && dialog.isShowing())
                        dialog.cancel();
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }).start();
        }
    };

    private void updateUI() {
        if(str1!=null && str1.contains("CONNECT_OK")) {
            //message.setText("伺服器辨識成功");
            severState.setText("伺服器辨識成功");
            severState.setTextColor(Color.GREEN);
            connected = true;
        }
        else {
            message.setText(str1);
        }
        msg.setText("無廣播資料");
        msg.setSelected(true);
        msg.requestFocus();
        mySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {  //結束拖動時觸發
                if (seekBar.getProgress() > SEEK_DEST) {
                    swapTitle.setText("目前無換牌指令");
                    swapTitle.setTextColor(getResources().getColor(R.color.dark_gray));
                    swapMsg.setText("");
                    swapMsg.setVisibility(View.INVISIBLE);
                    seekBar.setProgress(5);
                    seekBar.setEnabled(false);
                    swapEnd = true;
                    swapWorking = false;
                    brandSelector.setSelection(0);
                    //bname = "";
                    //brandName.setText(bname);
                    mySeekBar.setVisibility(View.GONE);
                    mySeekBar.setEnabled(false);
                    btn_enter.setEnabled(false);
                    workerID.setText("");
                    if (task != null) {
                        Log.d("Mylog", "task is: " + task.getStatus());
                        task.cancel(true);
                    }
                    task = new UpdateTask().execute();
                    Log.d("Mylog", "swap end.");
                } else {
                    seekBar.setThumb(ResourcesCompat.getDrawable(getResources(), R.drawable.slider, null));
                    seekBar.setProgress(5);  //go back to zero
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {  /* 開始拖動時觸發*/ }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {  //進度改變時觸發  只要在拖動，就會重複觸發
                if (progress > SEEK_DEST)
                    seekBar.setThumb(ResourcesCompat.getDrawable(getResources(), R.drawable.slider_ok, null));
                else
                    seekBar.setThumb(ResourcesCompat.getDrawable(getResources(), R.drawable.slider, null));
            }
        });

        scannerInput.requestFocus();
        tryGetUsbPermission();
    }

    private View.OnClickListener deleteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(returnWorkerID!=null && returnWorkerID.length()>0) {
                returnWorkerID = returnWorkerID.substring(0, returnWorkerID.length() - 1);
                workerID.setText(returnWorkerID);
            }
        }
    };

    private View.OnClickListener enterListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("Mylog", "enter pressed, ID=" + returnWorkerID);
            if(nextBrandArray.size()>1 && returnBrandName!=1) {
                dialog = new AlertDialog.Builder(MainActivity.this).create();
                dialog.setTitle("警告");
                dialog.setMessage("未確認下個品牌");
                dialog.setButton("重試一次",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                            }
                        });
                dialog.show();
                return;
            }
            mySeekBar.setVisibility(View.VISIBLE);
            mySeekBar.setEnabled(true);
            swapTitle.setText("向右滑動切換品牌");
            swapTitle.setTextColor(getResources().getColor(R.color.black));
        }
    };

    private View.OnClickListener numberListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(returnWorkerID.length() < 7) {
                Button b = (Button) v;
                returnWorkerID += b.getText();
                workerID.setText(returnWorkerID);
            }
        }
    };

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
                layout1.setBackgroundColor(getResources().getColor(R.color.yellow));
            }
        });
        btnTurnOff = (Button) findViewById(R.id.btn_turn_off);
        btnTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("0");
                layout1.setBackgroundColor(getResources().getColor(R.color.background));
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
            //updateMessage(s, SYS_MSG);
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
                if(!debug)
                    connection.bulkTransfer(endpointOut, bytes, bytes.length, TIMEOUT);
                /*Time time = new Time("Asia/Tokyo");
                time.setToNow();
                String msg = time.year + "/" + (time.month + 1) + "/" + time.monthDay + " " + time.hour + ":" + time.minute + ":" + time.second;
                if (data.equals("1")) {
                    msg = "Turn on light at " + msg;
                } else {
                    msg = "Turn off light at " + msg;
                }
                updateMessage(msg, SYS_MSG);*/
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
                    //updateMessage(s, SENSOR_MSG);
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

    /*private void updateMessage(String msg, int type) {
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
    }*/

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
            Log.d("Mylog", "UpdateTask start.");
            while(!isCancelled()) {
                if(!connected) continue;
                //if(connectionTimeoutCount >= 11) break;
                if(need_to_send && snedString!=null) {
                    Log.d("Mylog", "sned:"+snedString);
                    SocketHandler.writeToSocket(snedString);
                    need_to_send = false;
                }
                //Log.d("Mylog", "swapEnd=" + swapEnd + ", swapWorking=" + swapWorking + " bc_msg_reply=" +bc_msg_reply);
                if(swapEnd) {
                    Log.d("Mylog", "prepare to send SWAP OK, ID=" +  returnWorkerID);
                    String s = "SWAP_OK\t" + returnWorkerID +"<END>";
                    SocketHandler.writeToSocket(s);
                    swapWorking = false;
                    swapEnd = false;
                    returnWorkerID = "";
                    Log.d("Mylog", "swapWorking -> false");
                    continue;
                }
                if(swapWorking) {
                    Log.e("Mylog", "break!");
                    break;
                }
                if(bc_msg_reply) {
                    Log.d("Mylog", "to send BC_MSG_OK<END>");
                    String s = "BC_MSG_OK<END>";
                    SocketHandler.writeToSocket(s);
                    bc_msg_reply = false;
                    bc_msgWorking = false;
                    Log.d("Mylog", "BC_MSG_OK");
                }
                if(bc_msgWorking)
                    continue;

                Log.d("Mylog", "UpdateTask listening...");
                String result;
                result = SocketHandler.getOutput();
                publishProgress(result);
                Log.d("Mylog", "result=" + result);

                /*if (result == null || result.isEmpty() || result.equals(""))
                    connectionTimeoutCount++;
                else
                    connectionTimeoutCount = 0;*/
            }
            return null;
        }
        protected void onProgressUpdate(String... values) {
            /*if(connectionTimeoutCount >= 10) {
                Log.e("Mylog", "connect failed!");
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("警告");
                dialog.setMessage("伺服器無回應，程式即將關閉\n請嘗試重新連線或洽系統管理員");
                dialog.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                            }
                        });
                dialog.show();
                return;
            }*/
            String result = values[0];
            String[] lines = result.split("<END>");
            for(String s: lines) {
                Log.d("Mylog", "s="+s);
                if(s!=null && s.contains("SWAP_MSG\t")) {
                    s = s.replaceAll("SWAP_MSG\t", "");
                    swapMsg.setVisibility(View.VISIBLE);
                    swapMsg.setText(s);
                    if(s.contains("此工號不存在")) {
                        if(task!=null)
                            task.cancel(true);
                        swapWorking = true;
                        Log.d("mylog", "此工號不存在");
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("警告");
                        dialog.setMessage("此工號不存在！");
                        dialog.setPositiveButton("重試一次",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialoginterface, int i) {
                                        mySeekBar.setVisibility(View.GONE);
                                        btn_enter.setEnabled(true);
                                        swapMsg.setText("請輸入品牌與員工ID");
                                        Log.d("Mylog", "此工號不存在::ok is pressed, task is: " + task.getStatus());
                                    }
                                });
                        dialog.show();
                    }
                } else if(s!=null && s.contains("BC_MSG")) {  //廣播
                    bc_msgWorking = true;
                    Log.d("mylog", "inside BC_MSG");
                    s = s.replaceAll("BC_MSG\t", "");
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("廣播");
                    dialog.setMessage(s);
                    dialog.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    //send BC_MSG_OK<END>
                                    bc_msg_reply = true;
                                }
                            });
                    dialog.setCancelable(false);
                    dialog.show();
                }else if(s != null && s.contains("MSG\t")) {
                    s = s.replaceAll("MSG\t", "");
                    s = s.replaceAll("<N>", "\n");
                    s = s.replaceAll("<END>", "");
                    msg.setText(s);
                }
                else if(s!=null && s.contains("UPDATE_LIST")) {
                    s = s.replaceAll("UPDATE_LIST\t", "");
                    String[] items = s.split("\t");
                    if(items.length >= 2) {
                        if(items[0].equals(itemCode)) {
                            if ((int) Double.parseDouble(items[1]) != count) {
                                Log.e("Mylog", "product " + productSerial + " count:" + items[1] + " != " + count);
                                countTV.setText(items[1]);
                            }
                            countTV.setText(items[1]);
                        }
                    }
                }
                else if(s != null && s.contains("LIST\t")) {
                    s = s.replaceAll("LIST\t", "");
                    s = s.replaceAll("<N>", "\n");
                    s = s.replaceAll("<END>", "");
                    //String line = values[0];
                    String[] items = s.split("\t");
                    if(items.length >= 4) {
                        hasProduct = true;
                        Pcode.setText(items[0]);
                        Pname.setText(items[1]);
                        Icode.setText(items[2]);
                        itemCode = items[2];
                        Iname.setText(items[3]);
                        countTV.setText("0");
                    }
                } else if(s!=null && s.contains("LIST_EMPTY")) {
                    Log.d("Mylog", "clear!");
                    hasProduct = false;
                    Pcode.setText("");
                    Pname.setText("");
                    Icode.setText("");
                    itemCode = "";
                    Iname.setText("");
                    countTV.setText("0");
                } else if(s!=null && s.contains("SWAP")) {
                    swapWorking = true;
                    Log.d("Mylog", "swap!!");
                    String[] items = s.split("\t");
                    nextBrandArray.clear();
                    if(items.length >= 1) {  //have next brand
                        nextBrandArray.add("(請選擇)");
                        nextBrandArray.add(items[1]);
                        nextBrandAdapter.notifyDataSetChanged();
                    } else {
                        nextBrandArray.add("(無)");
                        nextBrandAdapter.notifyDataSetChanged();
                    }
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("警告");
                    dialog.setMessage("已下達換牌指令！");
                    dialog.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    swapWorking = true;
                                    btn_enter.setEnabled(true);
                                    Log.d("Mylog", "OK pressed");
                                }
                            });
                    swapMsg.setText("請輸入品牌與員工ID");
                    Log.d("Mylog", "prepare to show dialog...");
                    dialog.show();
                }else if(s!=null && s.contains("UPDATE_BOX\t")) { //UPDATE_BOX \t 線號 \t 現在箱數 \t 目標箱數
                    s = s.replaceAll("UPDATE_BOX\t", "");
                    s = s.replaceAll("<N>", "\n");
                    s = s.replaceAll("<END>", "");
                    String[] items = s.split("\n");
                    for(String i: items) {
                        Log.d("Mylog","line i=" + i);
                        String[] single_item = i.split("\t");
                        if(single_item.length >= 3) {
                            int lineNumber = Integer.parseInt(single_item[0]) - 1;
                            BoxItem b = new BoxItem(single_item[0], single_item[1], single_item[2]);
                            boxArray.set(lineNumber, b);
                        }
                    }
                    boxAdapter.notifyDataSetChanged();
                } else if(s!=null && s.contains("UPDATE_VALUE\t")) {  //時間\t線號\t品牌名稱\t重量max\t重量value\t重量min\t圓周max\t圓周value\t圓周min\t透氣率max\t透氣率value\t透氣率min
                    s = s.replaceAll("UPDATE_VALUE\t", "");
                    s = s.replaceAll("<N>", "\n");
                    s = s.replaceAll("<END>", "");
                    String[] items = s.split("\n");
                    for(String i: items) {
                        Log.d("Mylog","line i=" + i);
                        String[] single_item = i.split("\t");
                        if(single_item.length >= 12) {
                            int lineNumber = Integer.parseInt(single_item[1]) - 1;
                            String name = "生產線" + single_item[1] + " " + single_item[2];
                            String time = "最後更新: " + single_item[0];
                            ValueItem v = new ValueItem(name, single_item[3], single_item[4], single_item[5], single_item[6], single_item[7], single_item[8], single_item[9], single_item[10], single_item[11], time);
                            valueArray.set(lineNumber, v);
                        }
                    }
                    valueAdapter.notifyDataSetChanged();
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

