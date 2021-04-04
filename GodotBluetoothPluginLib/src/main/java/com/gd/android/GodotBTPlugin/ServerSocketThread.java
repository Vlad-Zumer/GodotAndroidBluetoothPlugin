package com.gd.android.GodotBTPlugin;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerSocketThread extends Thread
{
    private BluetoothServerSocket m_ServerSocket;
    private BluetoothSocket m_ConnectionSocket;
    private Handler m_BTSocketEventHandler;

    private InputStream m_InStream;
    private OutputStream m_OutStream;

    private Object m_ServerSocketSyncObj;
    private Object m_ConnectedSocketSyncObj;
    private boolean m_ServerSocketDisconnected;
    private boolean m_ConnectedSocketDisconnected;


    public ServerSocketThread (BluetoothServerSocket serverSocket, Handler handler)
    {
        m_ConnectionSocket = null;
        m_InStream = null;
        m_OutStream = null;

        m_ServerSocket = serverSocket;
        m_BTSocketEventHandler = handler;

        m_ServerSocketDisconnected = false;
        m_ConnectedSocketDisconnected = false;

        m_ServerSocketSyncObj = new Object();
        m_ConnectedSocketSyncObj = new Object();
    }

    @Override
    public void run ()
    {
        try
        {
            m_ConnectionSocket = m_ServerSocket.accept();
            CloseSocket();
        }
        catch (IOException e)
        {
            SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);

            return;
        }

        if (m_ConnectionSocket != null)
        {
            // TODO use standardized errors
            try
            {
                m_InStream = m_ConnectionSocket.getInputStream();
            }
            catch (IOException e)
            {
                SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);

                return;
            }

            try
            {
                m_OutStream = m_ConnectionSocket.getOutputStream();
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
                        SendMessage(BTSocketMessageType.BT_MESSAGE_RECEIVED, GodotMessageHelperMethods.CreateBTReceivedMessage(readBuffer.clone(), m_ConnectionSocket.getRemoteDevice().getName(),"SERVER"));
                    }
                    catch (IOException e)
                    {
                        synchronized (m_ConnectedSocketSyncObj)
                        {
                            if (!m_ConnectedSocketDisconnected)
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
     * Closes the server socket, this will stop any operation on this socket.
     */
    public void CloseSocket ()
    {
        synchronized (m_ServerSocketSyncObj)
        {
            m_ServerSocketDisconnected = true;
        }

        try
        {
            m_ServerSocket.close();
        }
        catch (IOException e)
        {
            SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);
        }
    }

    /**
     * Closes the connection thread got from accepting a BT connection
     */
    public void CloseConnection ()
    {
        if (m_ConnectionSocket != null)
        {
            synchronized (m_ConnectedSocketSyncObj)
            {
                m_ConnectedSocketDisconnected = true;
            }

            try
            {
                m_ConnectionSocket.close();
            }
            catch (IOException e)
            {
                SendMessage(BTSocketMessageType.BT_SOCKET_ERROR,e);
            }
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
                synchronized (m_ConnectedSocketSyncObj)
                {
                    if(!m_ConnectedSocketDisconnected)
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
