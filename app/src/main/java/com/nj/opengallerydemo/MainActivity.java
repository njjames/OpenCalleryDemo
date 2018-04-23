package com.nj.opengallerydemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_FROM_GALLEY = 0;
    private static final int REQUEST_CODE_GET_CONTENT_FROM_GALLEY = 1;
    private TextView mTvUri;
    private TextView mTvInfo;
    private ImageView mIvImage;
    private TextView mTvPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvImage = findViewById(R.id.iv_image);
        mTvInfo = findViewById(R.id.tv_info);
        mTvPath = findViewById(R.id.tv_path);
        findViewById(R.id.btn_show1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFromGalleyPick();
            }
        });
        findViewById(R.id.btn_show2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFromGalleyGetContent();
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }else {
                    Toast.makeText(this, "Permission is denied!" , Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void selectFromGalleyGetContent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_GET_CONTENT_FROM_GALLEY);
    }

    private void selectFromGalleyPick() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLEY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String path = "";
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == REQUEST_CODE_PICK_FROM_GALLEY) {
                    path = processResultBeforeKitkat(uri);
                }else if(requestCode == REQUEST_CODE_GET_CONTENT_FROM_GALLEY) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        path = processResultOnKitkat(uri);
                    }else {
                        path = processResultBeforeKitkat(uri);
                    }
                }
                showBitmapInfos(path);
            }
        }
    }

    private void showBitmapInfos(String path) {
        mTvPath.setText(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int width = options.outWidth;
        int height = options.outHeight;
        options.inJustDecodeBounds = false;
        mTvInfo.setText("图片的信息是：宽：" + width + ",高：" + height);
    }

    private String processResultBeforeKitkat(Uri uri) {
        String path = null;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    return path;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String processResultOnKitkat(Uri uri) {
        String path = "";
        boolean documentUri = DocumentsContract.isDocumentUri(this, uri);
        if (documentUri) {
            String authority = uri.getAuthority();
            String documentId = DocumentsContract.getDocumentId(uri);
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {

            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {

            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String[] split = documentId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(contentUri, null, "_id = ?", new String[]{split[1]}, null);
                    if (cursor != null) {
                        if (cursor.moveToNext()) {
                            int index = cursor.getColumnIndexOrThrow("_data");
                            path = cursor.getString(index);
                            return path;
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }else {
            return processResultBeforeKitkat(uri);
        }
        return null;
    }
}
