package com.gd.android.GodotBTPlugin;

import androidx.annotation.Nullable;

import org.godotengine.godot.Dictionary;

public class GodotMessageHelperMethods
{
    public static Dictionary CreateError(int errorCode, String errorMessage, @Nullable String explanation)
    {
        Dictionary out = new Dictionary();

        out.put("ErrorCode", errorCode);

        // If there is an explanation include it in the error message
        if(explanation != null)
        {
            out.put("ErrorMessage", errorMessage +" (" + explanation + ")");
        }
        else
        {
            out.put("ErrorMessage", errorMessage);
        }

        return out;
    }

    public static Dictionary CreateError (int errorCode, String errorMessage)
    {
        return CreateError(errorCode, errorMessage, null);
    }

    public static Dictionary CreateBTReceivedMessage(Object obj, String senderName, String receiverType)
    {
        Dictionary out = new Dictionary();

        out.put("Object", obj);
        out.put("SenderName", senderName);
        out.put("Type", receiverType);

        return out;
    }
}
