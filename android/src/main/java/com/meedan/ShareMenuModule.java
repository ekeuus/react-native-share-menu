package com.meedan;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.meedan.ShareMenuPackage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class ShareMenuModule extends ReactContextBaseJavaModule {

  private ReactContext mReactContext;

  public ShareMenuModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mReactContext = reactContext;
  }

  private String getRealPathFromURI(Context context, Uri contentUri) {
    Cursor cursor = null;
    try {
      String[] proj = { MediaStore.Images.Media.DATA };
      cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
    } catch (Exception e) {
      return "";
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  @Override
  public String getName() {
    return "ShareMenu";
  }

  protected void onNewIntent(Intent intent) {
    Activity mActivity = getCurrentActivity();
    
    if(mActivity == null) { return; }

    mActivity.setIntent(intent);
  }  

 private static void writeBytesToFileClassic(byte[] bFile, String fileDest) {
    FileOutputStream fileOuputStream = null;

    try {
      fileOuputStream = new FileOutputStream(fileDest);
      fileOuputStream.write(bFile);

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (fileOuputStream != null) {
        try {
          fileOuputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

  @ReactMethod
  public void getSharedText(Callback successCallback) {
    Activity mActivity = getCurrentActivity();
    
    if(mActivity == null) { return; }
    
    Intent intent = mActivity.getIntent();
    String action = intent.getAction();
    String type = intent.getType();

    if (Intent.ACTION_SEND.equals(action) && type != null) {
      if ("text/plain".equals(type)) {
        String input = intent.getStringExtra(Intent.EXTRA_TEXT);
        successCallback.invoke(input);
      } else if (type.startsWith("image/") || type.startsWith("video/") || type.endsWith("pdf") || type.endsWith("ms-powerpoint") || type.endsWith("openxmlformats-officedocument.presentationml.presentation")) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
          if (type.endsWith("pdf")) { // get pdf path
            String res = FileUtils.getPath(mReactContext, imageUri);
            successCallback.invoke(res);
          } else if (type.endsWith("ms-powerpoint") || type.endsWith("openxmlformats-officedocument.presentationml.presentation")) {
            InputStream iStream;
            try {
              iStream = mReactContext.getContentResolver().openInputStream(imageUri);
              File file = new File(mReactContext.getCacheDir(), imageUri.getLastPathSegment());
              byte[] info = getBytesFromInputStream(iStream);
              writeBytesToFileClassic(info, file.getCanonicalPath());
              successCallback.invoke(file.getAbsolutePath());
            } catch (FileNotFoundException e) {
              e.printStackTrace();
              successCallback.invoke("");
            } catch (IOException e) {
              e.printStackTrace();
              successCallback.invoke("");
            }
          } else {
            successCallback.invoke(imageUri.toString());
          }
        } else {
          successCallback.invoke("");
        }
      } else {
        Toast.makeText(mReactContext, "Type is not support", Toast.LENGTH_SHORT).show();
        successCallback.invoke(null);
      }
    } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
        if (type.startsWith("image/") || type.startsWith("video/")) {
          ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
          if (imageUris != null) {
            String completeString = new String();
            for (Uri uri: imageUris) {
              completeString += uri.toString() + ",";
            }
            successCallback.invoke(completeString);
          }
        } else {
          Toast.makeText(mReactContext, "Type is not support", Toast.LENGTH_SHORT).show();
          successCallback.invoke("");
        }
    } else {
      successCallback.invoke("");
    }
  }

  @ReactMethod
  public void clearSharedText() {
    Activity mActivity = getCurrentActivity();
    
    if(mActivity == null) { return; }

    Intent intent = mActivity.getIntent();
    String type = intent.getType();
    if ("text/plain".equals(type)) {
      intent.removeExtra(Intent.EXTRA_TEXT);
    } else if (type.startsWith("image/") || type.startsWith("video/")) {
      intent.removeExtra(Intent.EXTRA_STREAM);
    }
  }
}
