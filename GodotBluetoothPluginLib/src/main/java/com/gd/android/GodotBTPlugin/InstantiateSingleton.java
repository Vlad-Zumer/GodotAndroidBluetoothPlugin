package com.gd.android.GodotBTPlugin;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class InstantiateSingleton extends GodotPlugin
{

    private final Godot m_Godot;
    private final Activity m_Activity;
    private final BluetoothAdapter m_BTAdaptor;
    private final List<BluetoothDevice> m_BTDevicesFound;

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_DISCOVER_BT = 2;
    private final int REQUEST_PERMISSION_FINE_LOCATION = 3;

    private ServerSocketThread m_ServerSocketThread;
    private ClientSocketThread m_ClientSocketThread;

    /////////////////////////////////////////
    // GODOT Functions
    /////////////////////////////////////////

    /**
     * Constructor called by GODOT at game start
     *
     * @param godot
     */
    public InstantiateSingleton (Godot godot)
    {
        super(godot);
        m_Godot = godot;
        m_Activity = getActivity();
        m_BTAdaptor = BluetoothAdapter.getDefaultAdapter();
        m_BTDevicesFound = new ArrayList<BluetoothDevice>();

        m_ServerSocketThread = null;
        m_ClientSocketThread = null;
    }

    @Nullable
    @Override
    public View onMainCreate (Activity activity)
    {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        m_Activity.registerReceiver(m_BTFoundDeviceReceiver, filter);
        return super.onMainCreate(activity);
    }

    @Override
    public void onMainDestroy ()
    {
        m_Activity.unregisterReceiver(m_BTFoundDeviceReceiver);
        super.onMainDestroy();
    }

    @NonNull
    @Override
    public String getPluginName ()
    {
        return "GodotBTPlugin";
    }

    @NonNull
    @Override
    public List<String> getPluginMethods ()
    {

        return Arrays.asList(
                // BT Functions
                "HasBTAdaptor",
                "IsBTActive",
                "AskTurnOnBT",
                "AskMakeDiscoverable",
                "TurnOffBT",
                "GetDiscoveredDevices",
                "StartDiscovering",
                "StopDiscovering",
                // TESTING
                "AskCoarseLocationPermission",
                // SOCKETS
                "OpenServerSocket",
                "CloseServerSocket",
                "CloseServerConnection",
                "ServerConnectionWrite",
                "ConnectClient",
                "CloseClientConnection",
                "ClientConnectionWrite"
        );
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals ()
    {
        Set<SignalInfo> signals = new HashSet<>();

        signals.add(new SignalInfo("OnSignalTriggered", String.class));
        signals.add(new SignalInfo("OnBTError", String.class));
        signals.add(new SignalInfo("OnBTRequest", Dictionary.class));
        signals.add(new SignalInfo("OnBTDeviceFound", Dictionary.class));
        signals.add(new SignalInfo("OnEventLog", String.class));
        signals.add(new SignalInfo("OnBTMessageReceived", Dictionary.class));

        return signals;

    }


    /**
     * callBack from onActivity result from the main activity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onMainActivityResult (int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_ENABLE_BT:
            {
                if (resultCode == RESULT_OK)
                {
                    emitSignal("OnBTRequest", CreateBTRequestResponse("BT_ENABLE", true));
                }
                else
                {
                    emitSignal("OnBTRequest", CreateBTRequestResponse("BT_ENABLE", false));
                }
                break;
            }
            case REQUEST_DISCOVER_BT:
            {
                if (resultCode != RESULT_CANCELED)
                {
                    emitSignal("OnBTRequest", CreateBTRequestResponse("BT_DISCOVER", true));
                }
                else
                {
                    emitSignal("OnBTRequest", CreateBTRequestResponse("BT_DISCOVER", false));
                }
                break;
            }
        }
        super.onMainActivityResult(requestCode, resultCode, data);
    }

    // TODO move this out
    private Dictionary CreateBTRequestResponse (String requestName, boolean success)
    {
        Dictionary outVal = new Dictionary();
        outVal.put("ReqName", requestName);
        outVal.put("ReqSucc", success);
        return outVal;
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver m_BTFoundDeviceReceiver = new BroadcastReceiver()
    {
        public void onReceive (Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address


                m_BTDevicesFound.add(device);

                Dictionary dictionary = new Dictionary();
                dictionary.put("DeviceName", deviceName);
                dictionary.put("DeviceMacAddr", deviceHardwareAddress);
                emitSignal("OnBTDeviceFound", dictionary);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                emitSignal("OnBTRequest", CreateBTRequestResponse("BT_DISCOVERY_FINISHED", true));
            }
        }
    };

    private final Handler m_BTSocketMessageHandler = new Handler(Looper.myLooper())
    {
        @Override
        public void handleMessage (Message msg)
        {
            switch (msg.what)
            {
                case BTSocketMessageType.BT_MESSAGE_RECEIVED:
                {
                    Dictionary dict = (Dictionary) msg.obj;
                    dict.put("ReceiverName", m_BTAdaptor.getName());
                    emitSignal("OnBTMessageReceived", dict);
                    break;
                }
                case BTSocketMessageType.BT_SOCKET_ERROR:
                {
                    emitSignal("OnBTError", msg.obj.toString());
                }
                default:
                {
                    break;
                    //emitSignal("OnBTError", "Message Type Not Recognized");
                }
            }
        }
    };


    ////////////////////////////////////////
    // TESTING
    ///////////////////////////////////////

    /**
     * Start a server thread that will accept 1 connection
     *
     * @param name     name of the service/game you'll be using this bluetooth socket for
     * @param UUID_str the UUI as a String for this service
     */
    public void OpenServerSocket (String name, String UUID_str)
    {
        if (HasBTAdaptor())
        {
            if (IsBTActive())
            {
                try
                {
                    BluetoothServerSocket btServerSocket = m_BTAdaptor.listenUsingRfcommWithServiceRecord(name, UUID.fromString(UUID_str));
                    m_ServerSocketThread = new ServerSocketThread(btServerSocket, m_BTSocketMessageHandler);
                    m_ServerSocketThread.start();
                }
                catch (IOException e)
                {
                    // TODO error
                    emitSignal("OnBTError", "Failed to create server socket");
                }

            }
            else
            {
                emitSignal("OnBTError", "BT adaptor is turned off.");
            }
        }
        else

        {
            emitSignal("OnBTError", "Could not get device's BT adapter.");
        }

    }

    /**
     * Closes the server socket
     * This will be called once a client has connected
     * Use this to stop the socket from allowing connections anymore
     */
    public void CloseServerSocket ()
    {
        if (m_ServerSocketThread != null)
        {
            m_ServerSocketThread.CloseSocket();
        }
    }

    /**
     * Closes the connection with the client
     * Call this when there is no more data to send/receive
     */
    public void CloseServerConnection ()
    {
        if (m_ServerSocketThread != null)
        {
            m_ServerSocketThread.CloseConnection();
        }
    }

    /**
     * Write a message from the server to the client
     *
     * @param message the message as a string
     */
    public void ServerConnectionWrite (String message)
    {
        if (m_ServerSocketThread != null)
        {
            m_ServerSocketThread.WriteToBTSocket(message);
        }
        else
        {
            emitSignal("OnBTError", "Server not started");
        }
    }


    /**
     * Start a client thread that will try to connect to a server
     *
     * @param UUID_str       the UUI as a String for this service
     * @param MACAddress_str the MAC address of the server you want to connect to
     */
    public void ConnectClient (String UUID_str, String MACAddress_str)
    {
        if (HasBTAdaptor())
        {
            if (IsBTActive())
            {
                if (BluetoothAdapter.checkBluetoothAddress(MACAddress_str))
                {
                    BluetoothDevice remoteBTDevice = m_BTAdaptor.getRemoteDevice(MACAddress_str);
                    try
                    {
                        BluetoothSocket socketToServer = remoteBTDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUID_str));
                        m_ClientSocketThread = new ClientSocketThread(socketToServer, m_BTSocketMessageHandler);
                        m_ClientSocketThread.start();
                    }
                    catch (IOException e)
                    {
                        // TODO
                        emitSignal("OnBTError", "Failed to create client socket");
                    }
                }
                else
                {
                    // TODO
                    emitSignal("OnBTError", "Invalid MAC Address");
                }
            }
            else
            {
                emitSignal("OnBTError", "BT adaptor is turned off.");
            }
        }
        else
        {
            emitSignal("OnBTError", "Could not get device's BT adapter.");
        }
    }

    /**
     * Close connection to the server.
     * Use this when you have no more data to send/receive
     */
    public void CloseClientConnection ()
    {
        if (m_ClientSocketThread != null)
        {
            m_ClientSocketThread.CloseConnection();
        }
    }

    /**
     * Write a message to the server
     *
     * @param message message to write as a string
     */
    public void ClientConnectionWrite (String message)
    {
        if (m_ClientSocketThread != null)
        {
            m_ClientSocketThread.WriteToBTSocket(message);
        }
        else
        {
            emitSignal("OnBTError", "Client not started");
        }
    }

    /////////////////////////////////////////
    // Plugin Functions
    /////////////////////////////////////////

    /**
     * In order to discover and be discovered via bluetooth, we need access to the location
     * (because FU google)
     */
    public void AskCoarseLocationPermission ()
    {

        // TODO do checks over SDK to figure out what permissions are needed

        // The minimum SDK that supports runtime permissions is Marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) // m is marshmallow
        {
            if (m_Godot.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                m_Godot.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                m_Godot.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                emitSignal("OnEventLog", "PERMISSION ALREADY GRANTED");
            }
            else
            {
                if (m_Godot.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
                {
                    emitSignal("OnEventLog", "SHOW PERMISSION RATIONALE");
                }

                m_Godot.requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                }, REQUEST_PERMISSION_FINE_LOCATION);
            }
        }
        else
        {
            emitSignal("OnBTError", "Android SDK < Marshmallow");
        }
    }

    // TODO -- take this out of here
    @Override
    public void onMainRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_PERMISSION_FINE_LOCATION)
        {
            for (int i = 0; i < permissions.length; i++)
            {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                {
                    emitSignal("OnEventLog", permissions[i] + " PERMISSION GRANTED");
                }
                else
                {
                    emitSignal("OnEventLog", permissions[i] + " PERMISSION DENIED");
                }
            }
        }
        else
        {
            super.onMainRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * @return True if the deice has a bluetooth adaptor, False otherwise
     */
    public boolean HasBTAdaptor ()
    {
        return (m_BTAdaptor != null);
    }

    /**
     * @return True if the device is active, False otherwise
     */
    public boolean IsBTActive ()
    {
        if (HasBTAdaptor())
        {
            return m_BTAdaptor.isEnabled();
        }
        else
        {
            emitSignal("OnBTError", "BTMissingAdaptor");
            return false;
        }
    }

    /**
     * Will ask the user to turn on the bluetooth device
     */
    public void AskTurnOnBT ()
    {
        if (HasBTAdaptor())
        {
            if (!IsBTActive())
            {
                // ask to be active
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                m_Activity.startActivityForResult(enableBT, REQUEST_ENABLE_BT);
            }
        }
        else
        {
            emitSignal("OnBTError", "BTMissingAdaptor");
        }
    }

    /**
     * Will ask the user to make the device discoverable
     */
    public void AskMakeDiscoverable ()
    {
        if (HasBTAdaptor())
        {
            if (IsBTActive())
            {
                if (!m_BTAdaptor.isDiscovering())
                {
                    // ask to be discoverable
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    m_Activity.startActivityForResult(intent, REQUEST_DISCOVER_BT);

                }
            }
            else
            {
                emitSignal("OnBTError", "BTNotActive");
            }
        }
        else
        {
            emitSignal("OnBTError", "BTMissingAdaptor");
        }
    }

    /**
     * Will turn off the bluetooth
     */
    public void TurnOffBT ()
    {
        if (HasBTAdaptor())
        {
            m_BTAdaptor.disable();
        }
        else
        {
            emitSignal("OnBTError", "BTMissingAdaptor");
        }
    }

    /**
     * Will start the discovery procedure
     * May ask user for permission to do so
     */
    // TODO ask to enalbe location services because F U GOOGLE
    public void StartDiscovering ()
    {
        if (HasBTAdaptor())
        {
            if (!m_BTAdaptor.startDiscovery())
            {
                emitSignal("OnBTError", "BTCannotStartDiscovery");
            }
            else
            {
                emitSignal("OnBTRequest", CreateBTRequestResponse("BT_START_DISCOVERING", true));
            }
        }
        else
        {
            emitSignal("OnBTError", "BTMissingAdaptor");
        }
    }

    /**
     * Will stop the discovery procedure
     */
    public void StopDiscovering ()
    {
        if (HasBTAdaptor())
        {
            if (!m_BTAdaptor.cancelDiscovery())
            {
                emitSignal("OnBTError", "BTCannotCancelDiscovery");
            }
            else
            {
                emitSignal("OnBTRequest", CreateBTRequestResponse("BT_STOP_DISCOVERING", true));
            }
        }
        else
        {
            emitSignal("OnBTError", "BTMissingAdaptor");
        }
    }

    /**
     * @return all devices discovered as a dictionary with the following structure
     * {"indexValue" : { DeviceName: "DeviceName", DeviceMacAddress:"Device-MAC-Address" }}
     */
    //TODO make the returned dictionary better
    public Dictionary GetDiscoveredDevices ()
    {
        Dictionary outVal = new Dictionary();
        for (int index = 0; index < m_BTDevicesFound.size(); index++)
        {
            BluetoothDevice btDev = m_BTDevicesFound.get(index);
            Dictionary dictionary = new Dictionary();
            dictionary.put("DeviceName", btDev.getName());
            dictionary.put("DeviceMacAddr", btDev.getAddress());
            outVal.put(String.valueOf(index), dictionary);
        }
        return outVal;
    }
}
