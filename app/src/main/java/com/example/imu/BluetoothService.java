package com.example.imu;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    public static final String ACTION_DISCONNECT = "com.example.ACTION_DISCONNECT";
    private ViewModel viewModel;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt1;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;

    private String deviceAddress;
    boolean isReceiving;
    int connected = 0;
    public static final String BLUETOOTH_CONNECTED = "DEVICE_CONNECTED";
    public static final String BLUETOOTH_DISCONNECTED = "DEVICE_DISCONNECTED";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth");
            stopSelf(); // Stop service if the device doesn't support Bluetooth
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String device = intent.getStringExtra("device_address");
        if (device != null) {
//            connectToDevice(device);
//            Log.e("connection","connect"+device.getAddress());
            System.out.println(device);
            connectToDevice1(device);
        } else {
            System.out.println("address is null");
        }
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_DISCONNECT.equals(action)) {
                disconnectDevice();
            }
        }
        return START_STICKY;
    }



    public void disconnectDevice() {
        if (bluetoothGatt1 != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            bluetoothGatt1.disconnect(); // Initiate disconnection

            bluetoothGatt1.close();       // Close the GATT connection
            bluetoothGatt1 = null;        // Clear the reference
            sendBroadcastData(BLUETOOTH_DISCONNECTED); // Notify about disconnection
            Log.d(TAG, "Device disconnected successfully");
        } else {
            Log.e(TAG, "BluetoothGatt is null, cannot disconnect");
        }
    }
    private void connectToDevice1(String deviceAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt1 = device.connectGatt(this, false, mygattCallback1);
        // connect to device
    }

    private final BluetoothGattCallback mygattCallback1 = new BluetoothGattCallback() {
        //once device get connected to app, callback fuctions will call for each state
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // call`s when state of connection change CONNECT AND DISCONNECT
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice device = gatt.getDevice();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("conncetion", "connected successfully");
                sendBroadcastData(BLUETOOTH_CONNECTED);
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("conncetion", "disconnceted");
                sendBroadcastData(BLUETOOTH_DISCONNECTED);
                stopSelf();
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //call`s when required service is discovered
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("7271f06e-5088-46c9-ab77-4e246b3ea3cb"));
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("660c4a6f-16d8-4e57-8fdb-a4058934242d"));
                    if (characteristic != null) {
                        if (ActivityCompat.checkSelfPermission(BluetoothService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                            return;
                        }
                        Log.d("BLE", "Service discovered: " + (service != null));
                        Log.d("BLE", "Characteristic discovered: " + (characteristic != null));
                        System.out.println("serveice working");
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        if (descriptor != null) {
                            System.out.println("discriptor working");
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();
//            Log.d("onCharacteristicChanged", "Received data: " + Arrays.toString(value));
            if (value.length >= 10) {
                byte[] xvalue = Arrays.copyOfRange(value,0,2);
                byte[] yvalue= Arrays.copyOfRange(value,2,4);
                byte[] zvalue= Arrays.copyOfRange(value,4,6);
                byte[] epoch = Arrays.copyOfRange(value,6,14);
                ByteBuffer epochBuffer = ByteBuffer.wrap(epoch).order(ByteOrder.LITTLE_ENDIAN);
                long epochValue = epochBuffer.getLong();
                short x = ByteBuffer.wrap(xvalue).order(ByteOrder.LITTLE_ENDIAN).getShort();
                short y = ByteBuffer.wrap(yvalue).order(ByteOrder.LITTLE_ENDIAN).getShort();
                short z = ByteBuffer.wrap(zvalue).order(ByteOrder.LITTLE_ENDIAN).getShort();
                short[] data = new short[3];
                 data[0] = x;
                 data[1] = y;
                 data[2] = z;
                sendBroadcastData(data,epochValue);
//                sendBroadcastData(epochValue);
//               System.out.println(x+" "+y+" "+z+" "+epochValue);
            }
        }
    };





    public void handleDisconnection() {
        isReceiving = false;
        connected = 0;

        try {
            if (inputStream != null) {
                inputStream.close();
            }

            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing streams", e);
        }

    }
    private void sendBroadcastData(String action){
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(BluetoothService.this).sendBroadcast(intent);
    }
    private void sendBroadcastData(short[] data,Long time){
        Intent intent = new Intent("sendData");
        intent.putExtra("data",data);
        intent.putExtra("time",time);
        LocalBroadcastManager.getInstance(BluetoothService.this).sendBroadcast(intent);
    }
    private void sendBroadcastData(long time){
        Intent intent = new Intent("sendData");
        intent.putExtra("time",time);
        LocalBroadcastManager.getInstance(BluetoothService.this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       handleDisconnection();
       disconnectDevice();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}