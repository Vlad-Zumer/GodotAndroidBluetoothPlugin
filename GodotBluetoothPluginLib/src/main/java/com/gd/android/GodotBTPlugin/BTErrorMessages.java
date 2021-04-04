package com.gd.android.GodotBTPlugin;

/**
 * Error messages sent to GODOT game
 */
public class BTErrorMessages
{
    // TODO - standardize error messages

    /** ADAPTER ERRORS */
    public static final String BT_NO_ADAPTER = "";
    public static final String BT_ADAPTER_OFF = "";
    public static final String BT_ADAPTER_CANNOT_START_DISCOVERY = "";
    public static final String BT_ADAPTER_CANNOT_CANCEL_DISCOVERY = "";
    public static final String BT_ADAPTER_CANNOT_LISTEN = "";
    public static final String BT_ADAPTER_CANNOT_CREATE_CLIENT_SOCKET = "";

    /** SERVER SOCKET ERRORS */
    public static final String BT_SERVER_SOCKET_CANNOT_ACCEPT = "";
    public static final String BT_SERVER_SOCKET_CANNOT_GET_INPUT_STREAM = "";
    public static final String BT_SERVER_SOCKET_CANNOT_GET_OUTPUT_STREAM = "";
    public static final String BT_SERVER_SOCKET_INPUT_STREAM_CANNOT_READ = "";
    public static final String BT_SERVER_SOCKET_OUTPUT_STREAM_CANNOT_WRITE = "";
    public static final String BT_SERVER_SOCKET_CANNOT_CLOSE = "";
    public static final String BT_SERVER_SOCKET_CANNOT_CLOSE_CONNECTION_SOCKET = "";

    /** CLIENT SOCKET ERRORS */
    public static final String BT_CLIENT_SOCKET_CANNOT_CONNECT = "";
    public static final String BT_CLIENT_SOCKET_CANNOT_GET_INPUT_STREAM = "";
    public static final String BT_CLIENT_SOCKET_CANNOT_GET_OUTPUT_STREAM = "";
    public static final String BT_CLIENT_SOCKET_INPUT_STREAM_CANNOT_READ = "";
    public static final String BT_CLIENT_SOCKET_OUTPUT_STREAM_CANNOT_WRITE = "";
    public static final String BT_CLIENT_SOCKET_CANNOT_CLOSE = "";

    /** OTHER ERRORS */
    public static final String COMPATIBILITY_SDK_TOO_OLD = "";

}

