package com.gd.android.GodotBTPlugin;

public class BTErrorCodes
{
    // TODO - standardize error codes

    /** ADAPTER ERRORS */
    public static final int BT_NO_ADAPTER = 0;
    public static final int BT_ADAPTER_OFF = 0;
    public static final int BT_ADAPTER_CANNOT_START_DISCOVERY = 0;
    public static final int BT_ADAPTER_CANNOT_CANCEL_DISCOVERY = 0;
    public static final int BT_ADAPTER_CANNOT_LISTEN = 0;
    public static final int BT_ADAPTER_CANNOT_CREATE_CLIENT_SOCKET = 0;

    /** SERVER SOCKET ERRORS */
    public static final int BT_SERVER_SOCKET_CANNOT_ACCEPT = 0;
    public static final int BT_SERVER_SOCKET_CANNOT_GET_INPUT_STREAM = 0;
    public static final int BT_SERVER_SOCKET_CANNOT_GET_OUTPUT_STREAM = 0;
    public static final int BT_SERVER_SOCKET_INPUT_STREAM_CANNOT_READ = 0;
    public static final int BT_SERVER_SOCKET_OUTPUT_STREAM_CANNOT_WRITE = 0;
    public static final int BT_SERVER_SOCKET_CANNOT_CLOSE = 0;
    public static final int BT_SERVER_SOCKET_CANNOT_CLOSE_CONNECTION_SOCKET = 0;

    /** CLIENT SOCKET ERRORS */
    public static final int BT_CLIENT_SOCKET_CANNOT_CONNECT = 0;
    public static final int BT_CLIENT_SOCKET_CANNOT_GET_INPUT_STREAM = 0;
    public static final int BT_CLIENT_SOCKET_CANNOT_GET_OUTPUT_STREAM = 0;
    public static final int BT_CLIENT_SOCKET_INPUT_STREAM_CANNOT_READ = 0;
    public static final int BT_CLIENT_SOCKET_OUTPUT_STREAM_CANNOT_WRITE = 0;
    public static final int BT_CLIENT_SOCKET_CANNOT_CLOSE = 0;

    /** OTHER ERRORS */
    public static final int COMPATIBILITY_SDK_TOO_OLD = 0;
}
