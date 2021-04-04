package com.gd.android.GodotBTPlugin;

import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClientSocketThread extends Thread
{
    private BluetoothSocket m_BTSocket;
    private Handler m_BTSocketEventHandler;

    private InputStream m_InStream;
    private OutputStream m_OutStream;

    private Object m_SocketSyncObj;
    private boolean m_SocketDisconnected;


    public ClientSocketThread (BluetoothSocket serverSocket, Handler handler)
    {
        m_InStream = null;
        m_OutStream = null;

        m_BTSocket = serverSocket;
        m_BTSocketEventHandler = handler;

        m_SocketDisconnected = false;
        m_SocketSyncObj = new Object();
    }

    @Override
    public void run ()
    {
        try
        {
            SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,"Connecting");
            m_BTSocket.connect();
            SendMessage(BTSocketMessageType.BT_SOCKET_ERROR, "CONNECTED");
        }
        catch (IOException e)
        {
            SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);

            return;
        }

        if (m_BTSocket.isConnected())
        {
            // TODO use standardized errors
            try
            {
                m_InStream = m_BTSocket.getInputStream();
            }
            catch (IOException e)
            {
                SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);

                return;
            }

            try
            {
                m_OutStream = m_BTSocket.getOutputStream();
            }
            catch (IOException e)
            {
                SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);

                return;
            }

            // KEEP READING MESSAGES
            {
                boolean reading = true;
                while (reading)
                {
                    byte[] readBuffer = new byte[2048];
                    int numReadBytes;

                    try
                    {
                        numReadBytes = m_InStream.read(readBuffer);
                        SendMessage(BTSocketMessageType.BT_MESSAGE_RECEIVED, GodotMessageHelperMethods.CreateBTReceivedMessage(readBuffer.clone(),m_BTSocket.getRemoteDevice().getName(),"CLIENT"));
                    }
                    catch (IOException e)
                    {
                        synchronized (m_SocketSyncObj)
                        {
                            if (!m_SocketDisconnected)
                            {
                                SendMessage(BTSocketMessageType.BT_SOCKET_ERROR, e);
                            }
                        }
                        reading = false;
                    }
                }
            }
        }
    }

    /**
     *
     * @param what what type of message is this
     * @param obj the object to send with the message
     */
    private void SendMessage(int what, Object obj)
    {
        Message message = m_BTSocketEventHandler.obtainMessage(what,obj);
        message.sendToTarget();
    }

    /**
     * Closes the connection thread got from accepting a BT connection
     */
    public void CloseConnection ()
    {
        synchronized (m_SocketSyncObj)
        {
            m_SocketDisconnected = true;
        }

        try
        {
            m_BTSocket.close();
        }
        catch (IOException e)
        {
            SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);
        }
    }

    /**
     *
     * @param bytes
     */
    public void WriteToBTSocket(byte[] bytes)
    {
        if(m_OutStream != null)
        {
            try
            {
                m_OutStream.write(bytes);
                SendMessage(BTSocketMessageType.BT_MESSAGE_SENT,null);
            }
            catch (IOException e)
            {
                synchronized (m_SocketSyncObj)
                {
                    if(!m_SocketDisconnected)
                    {
                        SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);
                    }
                }
            }
        }
    }

    /**
     *
     * @param string
     */
    public void WriteToBTSocket(String string)
    {
        WriteToBTSocket(string.getBytes(BTMiscConstants.BT_STRING_MESSAGE_CHARSET));
    }

}
