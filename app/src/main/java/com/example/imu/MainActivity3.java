package com.example.imu;


import static com.example.imu.BluetoothService.ACTION_DISCONNECT;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity3 extends AppCompatActivity {
    private static final String TAG = "MainActivity3";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 2;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private static BluetoothDevice selectedDevice;
    public static  int sampleFrequency = 500;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayAdapter<String> bluetoothArrayAdapter;
    private AlertDialog alertDialog;
    private ArrayAdapter<String> arrayAdapter;
    boolean Tocalibrate;
    BroadcastReceiver broadcastReceiver;
    List<short[]> calibrationData;
    private String receivedData;
    boolean isconnected = false;
    TextView lastcalibrated;
    TextView xlable;
    TextView ylable;
    boolean isReceiverRegistered;
    List<String> bleAddress;
    Button connect;
    Button calibrate;
    LineData lineData;
    Button startprogress;
    private static final int Calibaration_data_size = 2500;
    private LineChart lineChart;
    boolean ToUpdate;
    CardView chart;
    float gxsum = 0;
    float gysum = 0;
    float gzsum = 0;
    float meanx = 0;
    float meany = 0;
    float meanz = 0;
    private static final int CHART_UPDATE_BUFFER_SIZE = 100;
    private LineDataSet lineDataSet1;
    private LineDataSet lineDataSet2;
    private LineDataSet lineDataSet3;
    private ArrayList<Entry> chartEntries1;
    private ArrayList<Entry> chartEntries2;
    private ArrayList<Entry> chartEntries3;
    private List<Entry> chartDataBuffer1 = new ArrayList<>();
    private List<Entry> chartDataBuffer2 = new ArrayList<>();
    private List<Entry> chartDataBuffer3 = new ArrayList<>();
    public static String Address;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        calibrationData = new ArrayList<>();
//        bleDevices = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        bleAddress = new ArrayList<>();
        Intent intent = getIntent();
        // Check if the intent has the extra data
        if (intent != null && intent.hasExtra("selectedData")) {
            // Extract the data
            receivedData = intent.getStringExtra("selectedData");
            Log.d("menuactivity", "Selected data: " + receivedData);
        }
        // Initialize TextViews after setting the content view
        chart = findViewById(R.id.chart);
        connect = findViewById(R.id.connect);
        calibrate = findViewById(R.id.calibrate);

        lastcalibrated = findViewById(R.id.lastcalibrated);

        startprogress = findViewById(R.id.startprogress);
        startprogress.setVisibility(View.GONE);
        setup();
        xlable = findViewById(R.id.xAxisLabel);
        ylable = findViewById(R.id.yAxisLabel);
        xlable.setVisibility(View.GONE);
        ylable.setVisibility(View.GONE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                android.Manifest.permission.BLUETOOTH_SCAN,
                                android.Manifest.permission.BLUETOOTH_CONNECT,
                                android.Manifest.permission.BLUETOOTH_ADMIN
                        }, REQUEST_PERMISSIONS);
            } else {
                checkLocationServicesAndStartDiscovery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }, REQUEST_PERMISSIONS);
            } else {
                checkLocationServicesAndStartDiscovery();
            }
        }

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothService.BLUETOOTH_CONNECTED.equals(intent.getAction())) {
                    connect.setText("CONNECTED");
                    connect.setTextSize(18f);
                    connect.setBackgroundColor(Color.rgb(76, 175, 80));
                    isconnected = true;

                } else if (BluetoothService.BLUETOOTH_DISCONNECTED.equals(intent.getAction())) {
                    connect.setText("DISCONNECTED");
                    connect.setTextSize(13f);
                    connect.setBackgroundColor(Color.RED);
                    isconnected = false;
                } else if ("sendData".equals(intent.getAction())) {
                    short[] data = intent.getShortArrayExtra("data");
//                   Log.d(TAG,"received data"+Arrays.toString(data));
                   Receiveddata(data);
                }

            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothService.BLUETOOTH_CONNECTED);
        filter.addAction(BluetoothService.BLUETOOTH_DISCONNECTED);
        filter.addAction("sendData");
        filter.addAction("session");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isconnected) {
                    checkLocationServicesAndStartDiscovery();
                    // Set up AlertDialog to show BLE device names
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity3.this);
                    builder.setTitle("BLE Devices");
                    builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int position) {
                            // Handle item click if needed
                            System.out.println(bleAddress.get(position));
                            if (ActivityCompat.checkSelfPermission(MainActivity3.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
//                            bluetoothLeScanner.stopScan(scanCallback);
                            Intent intent = new Intent(MainActivity3.this, BluetoothService.class);
                            intent.putExtra("device_address", bleAddress.get(position));
                            Address = bleAddress.get(position);
                            startService(intent);
                            connect.setText("connecting..");
//                            connect.setTextSize(13f);
                            last_calibrated_data(Address);
//                              startService()
//                            BluetoothDevice selectedDevice = bleDevices.get(position);
//                            connectToDevice(selectedDevice);
                        }
                    });

                    builder.show();
                }
                else{
                    showToast("Already connected");
                }

            }
        });

        calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isconnected) {
                    Tocalibrate = true;
                    startprogress.setVisibility(View.GONE);
                    chart.setVisibility(View.VISIBLE);
                    xlable.setVisibility(View.VISIBLE);
                    ylable.setVisibility(View.VISIBLE);
                    calibrate.setText("CALIBRATING..");
                    if (lineChart != null && lineData != null) {

                        lineChart.clear();
                        lineData.clearValues();
                        chartEntries1.clear();
                        chartEntries2.clear();
                        chartEntries3.clear();
                    }
                }
                else {
                    showToast("connect to device");
                }
            }
        });
        startprogress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMenuActivity();
            }
        });

    }
    private void startMenuActivity() {
        Intent intent = new Intent(MainActivity3.this,menuActivity.class);

        intent.putExtra("selectedData", receivedData); // Pass the received data
        // Pass the received data
        startActivity(intent);
    }

    public static String device(){
      return Address;
    }
    private void last_calibrated_data(String device_address){
        new Thread(()->{
            File internalStorageDir = getFilesDir();
            File Folder = new File(internalStorageDir, device_address);
            if(Folder.exists()){
                String filename = device_address+".json";
                File newFile = new File(Folder, filename);
                if (newFile.exists()){
                    long modifed_data = newFile.lastModified();
//                    System.out.println(modifed_data);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startprogress.setVisibility(View.VISIBLE);
                            lastcalibrated.setHint(getRelativeTimeString(modifed_data));
                            lastcalibrated.setHintTextColor(Color.rgb(76,175,80));
                        }
                    });

                }
            }
            else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lastcalibrated.setHint("New_Device");
                        lastcalibrated.setHintTextColor(Color.RED);
                    }
                });

            }
        }).start();

    }
    float mxvalue = 0;

    public static String getRelativeTimeString(long lastModifiedTime) {
        long currentTime = System.currentTimeMillis();
        long timeDiffInMillis = currentTime - lastModifiedTime;

        if (timeDiffInMillis < 60000) {
            return "just now";
        } else if (timeDiffInMillis < 3600000) {
            return String.format("%d min ago", TimeUnit.MILLISECONDS.toMinutes(timeDiffInMillis));
        } else if (timeDiffInMillis < 86400000) {
            return String.format("%d hr ago", TimeUnit.MILLISECONDS.toHours(timeDiffInMillis));
        } else if (timeDiffInMillis < 2592000000L) {
            return String.format("%d day ago", TimeUnit.MILLISECONDS.toDays(timeDiffInMillis));
        } else {
            return String.format("%d month ago", TimeUnit.MILLISECONDS.toDays(timeDiffInMillis) / 30);
        }
    }
    private void ReceiveData(short[] data) {
        if (ToUpdate) {
            float gx = data[0];
            float gy = data[1];
            float gz = data[2];
            float xTime =  (mxvalue/ sampleFrequency);
            chartDataBuffer1.add(new Entry(xTime, gx));
            chartDataBuffer2.add(new Entry(xTime, gy));
            chartDataBuffer3.add(new Entry(xTime, gz));
            mxvalue++;
            updateChart();
        }
    }
            private void updateChart() {
            if (chartDataBuffer1.size() >= CHART_UPDATE_BUFFER_SIZE) {
                runOnUiThread(() -> {
                    List<Entry> newEntries1 = new ArrayList<>(chartDataBuffer1);
                    List<Entry> newEntries2 = new ArrayList<>(chartDataBuffer2);
                    List<Entry> newEntries3 = new ArrayList<>(chartDataBuffer3);
                    chartDataBuffer1.clear();
                    chartDataBuffer2.clear();
                    chartDataBuffer3.clear();
                    chartEntries1.addAll(newEntries1);
                    chartEntries2.addAll(newEntries2);
                    chartEntries3.addAll(newEntries3);
                    lineDataSet1 = new LineDataSet(chartEntries1, "gX");
                    lineDataSet2 = new LineDataSet(chartEntries2, "gY");
                    lineDataSet3 = new LineDataSet(chartEntries3, "gZ");
                    lineData = new LineData(lineDataSet1,lineDataSet2,lineDataSet3);
                    lineDataSet1.setLineWidth(2);
                    lineDataSet1.setDrawCircles(false);
                    lineDataSet1.setColor(Color.RED);
                    lineDataSet2.setLineWidth(2);
                    lineDataSet2.setDrawCircles(false);
                    lineDataSet2.setColor(Color.BLUE);
                    lineDataSet3.setLineWidth(2);
                    lineDataSet3.setDrawCircles(false);
                    lineDataSet3.setColor(Color.GREEN);
                    lineChart.setData(lineData);
                    lineChart.invalidate();
                });
            }
//        chartUpdateHandler.postDelayed(chartUpdateRunnable, CHART_UPDATE_INTERVAL);
        }
    private void Receiveddata(short[] data){
      if(Tocalibrate){
          float gx = data[0];
          float gy = data[1];
          float gz = data[2];
          float x = (float) (gx/65.5);
          float y = (float) (gy/65.5);
          float z = (float) (gz/65.5);
          gxsum += x * x;
          meanx += x;
          gysum += y * y;
          meany += y;
          gzsum += z * z;
          meanz += z;
          calibrationData.add(data);
          ToUpdate = true;
          ReceiveData(data);
          if(calibrationData.size()==Calibaration_data_size) {
              float P_M_gx = meanx / Calibaration_data_size;
              float P_M_gy = meany / Calibaration_data_size;
              float P_M_gz = meanz / Calibaration_data_size;
              float sdx = (gxsum / Calibaration_data_size) - (P_M_gx * P_M_gx);
              float sdy = (gysum / Calibaration_data_size) - (P_M_gy * P_M_gy);
              float sdz = (gzsum / Calibaration_data_size) - (P_M_gz * P_M_gz);
              float max = Math.max(Math.max(sdx, sdy), sdz);
              if (max < 1) {
                  calculateAndStoreCalibrationData(calibrationData,Address);
                  mxvalue = 0;
              }else{
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          showToast("Calibration failed");
                          calibrate.setText("start calibration");
                          lineChart.clear();
                          lineData.clearValues();
                          chartEntries1.clear();
                          chartEntries2.clear();
                          chartEntries3.clear();
                          mxvalue = 0;
                      }
                  });
              }
              calibrationData.clear();
              gxsum = 0;
              meanx = 0;
              gysum = 0;
              meany = 0;
              gzsum = 0;
              meanz = 0;
              Tocalibrate = false;
              ToUpdate=false;
          }
      }
    }
    private void setup(){
        lineChart = findViewById(R.id.lineChart);
        chartEntries1 = new ArrayList<>();
        chartEntries2 = new ArrayList<>();
        chartEntries3 = new ArrayList<>();

        // Customize the chart
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);

        lineChart.getDescription().setEnabled(false); // Disable the description
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawLabels(true);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        rightAxis.setDrawGridLines(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.invalidate();
    }
//    private void checkLocationServicesAndStartDiscovery() {
//        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            new AlertDialog.Builder(this)
//                    .setTitle("Enable Location")
//                    .setMessage("Location services are required for Bluetooth scanning. Please enable location services.")
//                    .setPositiveButton("Settings", (dialog, which) -> {
//                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                        startActivity(intent);
//                    })
//                    .setNegativeButton("Cancel", null)
//                    .show();
//        } else {
////            startBluetoothDiscovery();
//            startBleScan();
//
//        }
//    }
private void checkLocationServicesAndStartDiscovery() {
    // Check if location services are enabled
    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        // Prompt user to enable location services
        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("Location services are required for Bluetooth scanning. Please enable location services.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    } else {
        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Location permissions granted, start BLE scan
            startBleScan();
        }
    }
}
    private void startBleScan() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothLeScanner.startScan(null, settings, scanCallback);
        } else {
            Log.e("BLE", "BluetoothLeScanner is null");
        }
    }
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice newDevice = result.getDevice();
            if (newDevice != null && !deviceList.contains(newDevice)) {
                deviceList.add(newDevice);
                if (ActivityCompat.checkSelfPermission(MainActivity3.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String devicename = newDevice.getName();

                if (devicename != null) {
                    arrayAdapter.add(newDevice.getName());
                    arrayAdapter.notifyDataSetChanged();
//                    blename.add(newDevice.getName());
                    bleAddress.add(newDevice.getAddress());
                    Log.e("BLE", "device found" + newDevice.getAddress());
                } else {
                    Log.e("ble", "device found with null name");
                }
            }
        }
    };

    private void startBluetoothDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothAdapter.startDiscovery();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, intentFilter);
        isReceiverRegistered = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scanning for Bluetooth devices...");
        builder.setAdapter(bluetoothArrayAdapter, (dialog, which) -> {
            selectedDevice = deviceList.get(which);
            if (selectedDevice != null) {
                System.out.println(selectedDevice.getAddress());
//                start the serviceclass
                Intent intent = new Intent(MainActivity3.this, BluetoothService.class);
                intent.putExtra("device_address", selectedDevice);
                startService(intent);
                connect.setText("connecting..");
                connect.setTextSize(13f);
                deviceList.clear();
                System.out.println(selectedDevice.getAddress());
                last_calibrated_data(selectedDevice.getAddress());
            } else {
                Toast.makeText(MainActivity3.this, "Device not found", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            bluetoothAdapter.cancelDiscovery();
            deviceList.clear();
            bluetoothArrayAdapter.clear();
            bluetoothArrayAdapter.notifyDataSetChanged();
        });
        alertDialog = builder.create();
        alertDialog.show();
    }


    private void calculateAndStoreCalibrationData(List<short[]> calibrationData,String device_address) {
        long sumGx = 0, sumGy = 0, sumGz = 0;

        for (short[] data : calibrationData) {
            sumGx += data[0];
            sumGy += data[1];
            sumGz += data[2];
        }

        float meanGx = (float) sumGx / calibrationData.size();
        float meanGy = (float) sumGy / calibrationData.size();
        float meanGz = (float) sumGz / calibrationData.size();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("meanGx", meanGx);
            jsonObject.put("meanGy", meanGy);
            jsonObject.put("meanGz", meanGz);
            File internalStorageDir = getFilesDir();
            File Folder = new File(internalStorageDir, device_address);
            if (!Folder.exists()) {
                Folder.mkdir();
            }
            String filename = device_address+".json";
            File newFile = new File(Folder, filename);
            if(newFile.exists()){
                newFile.delete();
            }
            if (!newFile.exists()) {
                newFile.createNewFile();
                String jsonString = jsonObject.toString();
                FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
                fos.write(jsonString.getBytes());
                fos.flush();
                fos.close();
                System.out.println("calibrated data");
                System.out.println(jsonString);
            }
            runOnUiThread(() -> {
                showToast("Calibration completed successfully");
                last_calibrated_data(Address);
                calibrate.setText("CALIBRATED");
            });
        } catch (Exception e) {
            Log.e(TAG, "Error storing calibration data", e);
            runOnUiThread(() -> showToast("Error storing calibration data"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationServicesAndStartDiscovery();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth enabling cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity3.this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(MainActivity3.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (device != null && device.getName() != null) {
                    deviceList.add(device);
                    bluetoothArrayAdapter.add(device.getName());
                    bluetoothArrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity3.this, message, Toast.LENGTH_SHORT).show());
    }


    private void stop() {
        Intent intent = new Intent(this, BluetoothService.class);
        intent.setAction(ACTION_DISCONNECT);
        startService(intent); // Sends a disconnect action to the service
        stopService(intent); // Stops the service
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        stop(); // Call stop to disconnect
    }


}
