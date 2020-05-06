package seeker.luckyecgdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.clj.fastble.data.BleScanState;
import com.clj.fastble.utils.HexUtil;

//import static com.feel.comm.CommTx.SET_WIFI_CONFIG;
import com.feel.comm.CommRx;
import com.feel.comm.CommTx;
import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;
//import com.feel.ble.comm.ObserverManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final String BleDeviceName = "FeelKit";
    private static final String serviceUUID_str = "00000001-0000-1000-8000-00805F9B34FB";
    private static final String notifyUUID_str = "00000003-0000-1000-8000-00805F9B34FB";
    private static final String writeUUID_str = "00000002-0000-1000-8000-00805F9B34FB";
    private static String feelname;
    private static SharedPreferences SP_IP;
    private ConnectionInfo mInfo;
    private OkSocketOptions mOkOptions;
    private IConnectionManager mOKManager;
    private String ip_addr;
    SharedPreferences.OnSharedPreferenceChangeListener mIPListener;

    BleDevice bleconnbleDevice;
    CommRx bleCommRx;
    CommTx bleCommTx;

    private LogAdapter mSendLogAdapter = new LogAdapter();
    private LogAdapter mReceLogAdapter = new LogAdapter();

    private SocketActionAdapter adapter = new SocketActionAdapter() {

        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
           // mManager.send(new HandShakeBean());
          //  mConnect.setText("DisConnect");
          //  mIPET.setEnabled(false);
          //  mPortET.setEnabled(false);
        }

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            if (e != null) {
                logSend("异常断开(Disconnected with exception):" + e.getMessage());
            } else {
                logSend("正常断开(Disconnect Manually)");
            }
         //   mConnect.setText("Connect");
          //  mIPET.setEnabled(true);
          //  mPortET.setEnabled(true);
        }

        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            logSend("连接失败(Connecting Failed)");
    //        mConnect.setText("Connect");
      //      mIPET.setEnabled(true);
       //     mPortET.setEnabled(true);
        }

        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {
            String str = new String(data.getBodyBytes(), Charset.forName("utf-8"));
            logRece(str);
        }

        @Override
        public void onSocketWriteResponse(ConnectionInfo info, String action, ISendable data) {
            String str = new String(data.parse(), Charset.forName("utf-8"));
            logSend(str);
        }

        @Override
        public void onPulseSend(ConnectionInfo info, IPulseSendable data) {
            String str = new String(data.parse(), Charset.forName("utf-8"));
            logSend(str);
        }
    };

    private void logSend(final String log) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            LogBean logBean = new LogBean(System.currentTimeMillis(), log);
            mSendLogAdapter.getDataList().add(0, logBean);
            mSendLogAdapter.notifyDataSetChanged();
        } else {
            final String threadName = Thread.currentThread().getName();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    logSend(threadName + " 线程打印(In Thread):" + log);
                }
            });
        }
    }

    private void logRece(final String log) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            LogBean logBean = new LogBean(System.currentTimeMillis(), log);
            mReceLogAdapter.getDataList().add(0, logBean);
            mReceLogAdapter.notifyDataSetChanged();
        } else {
            final String threadName = Thread.currentThread().getName();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    logRece(threadName + " 线程打印(In Thread):" + log);
                }
            });
        }

    }

    private void initBle() {
        bleCommRx = new CommRx();
        bleCommTx = new CommTx();
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);
    }

    private void initOKSocket() {
        final Handler handler = new Handler();
        mOkOptions = new OkSocketOptions.Builder()
                .setReconnectionManager(new NoneReconnect())
                .setConnectTimeoutSecond(10)
                .setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
                    @Override
                    public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                        handler.post(runnable);
                    }
                })
                .build();
        SP_IP = MainActivity.this.getSharedPreferences("ip_addr", Context.MODE_PRIVATE);

        mIPListener = new SharedPreferences
                .OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (mOKManager != null) {
                    mOKManager.disconnect();
                    mOKManager.unRegisterReceiver(adapter);
                    mOKManager = null;
                }
                ip_addr = sharedPreferences.getString("IP", null);
                if (ip_addr != null) {
                    BLEDisconnect(bleconnbleDevice);
                    mInfo = new ConnectionInfo(ip_addr, 3338);
                    mOKManager = OkSocket.open(mInfo).option(mOkOptions);
                    mOKManager.registerReceiver(adapter);
                    mOKManager.connect();
                }
             }
        };

        ip_addr = SP_IP.getString("IP", null);
        if (ip_addr != null) {
            mInfo = new ConnectionInfo(ip_addr, 3338);
            mOKManager = OkSocket.open(mInfo).option(mOkOptions);
            mOKManager.registerReceiver(adapter);
        } else {
            mInfo = null;
            mOKManager = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBle();
        initOKSocket();
    }

    @Override
    protected void onStart() {
        super.onStart();
/*
        if (isBLEConnectedDevice() == false) {
            if (BleManager.getInstance().getScanSate() == BleScanState.STATE_IDLE) {
                checkPermissions();

            }
        }
        */

        if(mOKManager != null) {
            if(mOKManager.isConnect() == false) {
                BLEDisconnect(bleconnbleDevice);
                mOKManager.connect();
            }
        }
        SP_IP.registerOnSharedPreferenceChangeListener(mIPListener);

    }@Override
    protected void onStop() {
        SP_IP.unregisterOnSharedPreferenceChangeListener(mIPListener);
        super.onStop();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // showConnectedDevice();
    }

    @Override
    protected void onDestroy() {

        if (mOKManager != null) {
            mOKManager.disconnect();
            mOKManager.unRegisterReceiver(adapter);
        }
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
        super.onDestroy();


    }

    public void onBtnClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.scatter_btn:
                intent.setClass(this, ScatterActivity.class);
                startActivity(intent);
                break;
            case R.id.ecgRealtime_btn:
                intent.setClass(this, ECGRealTimeActivity.class);
                startActivity(intent);
                break;
            case R.id.ecgStatic_btn:
                intent.setClass(this, ECGStaticActivity.class);
                startActivity(intent);
                break;
            case R.id.createimage_btn:
                intent.setClass(this, EcgTransformImageActivity.class);
                startActivity(intent);
                break;

            case R.id.setwfssid_btn:
                if (mOKManager != null) {
                    mOKManager.disconnect();
                    mOKManager.unRegisterReceiver(adapter);
                    mOKManager = null;
                }
                checkPermissions();
                setWifiDialog();
                break;
        }

    }

    private void setWifiDialog() {
        /*@setView 装入一个EditView
         */
        final EditText editText = new EditText(MainActivity.this);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(MainActivity.this);
        inputDialog.setTitle(R.string.ssid_pwd).setView(editText);
        inputDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String ssid_pwd = editText.getText().toString();
                        if(ssid_pwd.contains(" ")) {
                            if (isBLEConnectedDevice()) {
                                try {
                                    //   byte[] pwd = editText.getText().toString().getBytes("UTF-8");
                                    sendCmdWithData(bleCommTx.commTxMakeFrame(CommTx.SET_WIFI_CONFIG, ssid_pwd.getBytes("UTF-8")));
                                } catch (UnsupportedEncodingException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                            }
                        }
                        else
                            Toast.makeText(MainActivity.this, "Input error!!!", Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }
    }

    private void setScanRule() {

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                //      .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, BleDeviceName)   // 只扫描指定广播名的设备，可选
                .setAutoConnect(true)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {

            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                //  mDeviceAdapter.addDevice(bleDevice);
                // mDeviceAdapter.notifyDataSetChanged();

                if (bleDevice.getName().equals(BleDeviceName)) {
                    BleManager.getInstance().cancelScan();
                    BLEconnect(bleDevice);
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                // img_loading.clearAnimation();
                //   img_loading.setVisibility(View.INVISIBLE);
                //  btn_scan.setText(getString(R.string.start_scan));
            }
        });
    }
    private void BLEDisconnect(final BleDevice bleDevice) {
        if (isBLEConnectedDevice())
        BleManager.getInstance().disconnect(bleDevice);
    }
    private void BLEconnect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                //  progressDialog.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                //   img_loading.clearAnimation();
                //   img_loading.setVisibility(View.INVISIBLE);
                //  btn_scan.setText(getString(R.string.start_scan));
                //  progressDialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                //  progressDialog.dismiss();
                //  mDeviceAdapter.addDevice(bleDevice);
                // mDeviceAdapter.notifyDataSetChanged();
                BluetoothGattService bleservice = gatt.getService(UUID.fromString(serviceUUID_str));
                if (bleservice == null)
                    Toast.makeText(MainActivity.this, getString(R.string.device_error), Toast.LENGTH_LONG).show();
                else {
                    Toast.makeText(MainActivity.this, getString(R.string.connect_ok), Toast.LENGTH_LONG).show();
                    bleconnbleDevice = bleDevice;
                    // bleservice.getCharacteristic(UUID.fromString(notifyUUID_str));
                    // bleservice.getCharacteristic(UUID.fromString(writeUUID_str));
                    BleManager.getInstance().notify(
                            bleDevice,
                            serviceUUID_str,
                            notifyUUID_str,
                            new BleNotifyCallback() {
                                @Override
                                public void onNotifySuccess() {
                                    // 打开通知操作成功
                                    Toast.makeText(MainActivity.this, "Notify success !!!", Toast.LENGTH_LONG).show();

                                }

                                @Override
                                public void onNotifyFailure(BleException exception) {
                                    // 打开通知操作失败
                                    Toast.makeText(MainActivity.this, "Notify fail !!!", Toast.LENGTH_LONG).show();

                                }

                                @Override
                                public void onCharacteristicChanged(byte[] data) {
                                    //Toast.makeText(MainActivity.this, "recv data !!!", Toast.LENGTH_LONG).show();
                                    byte[] bytes_frame;
                                    for (short i = 0; i < data.length; i++) {
                                        bytes_frame = bleCommRx.RecvStreamFrame(data[i]);
                                        if (bytes_frame != null) {
                                            if (bytes_frame[1] == CommTx.IPADDR_IND) {
                                                ip_addr = String.valueOf(bytes_frame[3] & 0xFF) + "." + String.valueOf(bytes_frame[4] & 0xFF) + "." + String.valueOf(bytes_frame[5] & 0xFF) + "." + String.valueOf(bytes_frame[6] & 0xFF);
                                                SharedPreferences.Editor e = SP_IP.edit();
                                                e.putString("IP",ip_addr);
                                                if(e.commit() == true)
                                                {
                                                    Toast.makeText(MainActivity.this,
                                                            "recv IP:" + ip_addr,
                                                            Toast.LENGTH_LONG).show();
                                                }else
                                                {
                                                    {
                                                        Toast.makeText(MainActivity.this,
                                                                "recv IP:" + ip_addr+" Save error!!!",
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // 打开通知后，设备发过来的数据将在这里出现
                                }
                            });
                }
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                // progressDialog.dismiss();

                // mDeviceAdapter.removeDevice(bleDevice);
                //  mDeviceAdapter.notifyDataSetChanged();

                if (isActiveDisConnected) {
                    Toast.makeText(MainActivity.this, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    // ObserverManager.getInstance().notifyObserver(bleDevice);
                }

            }
        });
    }

    private boolean isBLEConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        if (deviceList != null)
            for (BleDevice bleDevice : deviceList) {
                if (bleDevice.getName().equals(BleDeviceName))
                    return true;
            }
        return false;
    }

    private void sendCmdWithData(byte[] data) {
        if (isBLEConnectedDevice())
            BleManager.getInstance().write(
                    bleconnbleDevice,
                    serviceUUID_str,
                    writeUUID_str,
                    data,
                    new BleWriteCallback() {

                        @Override
                        public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                            Toast.makeText(MainActivity.this, "sendCmdWithData ok", Toast.LENGTH_LONG).show();

                        }

                        @Override
                        public void onWriteFailure(final BleException exception) {
                            Toast.makeText(MainActivity.this, "sendCmdWithData error", Toast.LENGTH_LONG).show();

                        }
                    });
    }
}
