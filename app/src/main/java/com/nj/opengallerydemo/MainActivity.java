package com.nj.opengallerydemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PICK_FROM_GALLEY = 0;
    private static final int REQUEST_CODE_GET_CONTENT_FROM_GALLEY = 1;
    private static final int REQUEST_CODE_CROP_PIC = 2;
    private TextView mTvUri;
    private TextView mTvInfo;
    private ImageView mIvImage;
    private TextView mTvPath;
    private TextView mTvSize;
    private Bitmap mBitmap;
    private Uri mUri;
    private int mWidth;
    private int mHeight;
    private Uri mCropUri;
    private String mPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvImage = findViewById(R.id.iv_image);
        mTvInfo = findViewById(R.id.tv_info);
        mTvPath = findViewById(R.id.tv_path);
        mTvSize = findViewById(R.id.tv_size);
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
        findViewById(R.id.btn_compress1).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                bitmapCompress1();
            }
        });
        findViewById(R.id.btn_compress2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bitmapCompress2();
            }
        });
        findViewById(R.id.btn_compress3).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                bitmapCompress3();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void bitmapCompress3() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPath, options);
        int width = options.outWidth / 2;
        int height = options.outHeight / 2;
        int reqWidth = 200;
        int reqHeight = 200;
        int inSampleSize = 1;
        while (width / inSampleSize >= reqWidth && height / inSampleSize >= reqHeight) {
            inSampleSize = inSampleSize * 2;
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(mPath, options);
        showBitmapInfos(mPath);
        bitmapShow(bitmap);
    }

    private void bitmapCompress2() {
        if (mBitmap != null) {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(mUri, "image/*");
            cropIntent.putExtra("cropWidth", "true");
            //下面两参数控制截取的宽和高
            cropIntent.putExtra("outputX", mWidth / 2);
            cropIntent.putExtra("outputY", mHeight / 2);
            String filePath = getFilePath();
            File file = new File(filePath, System.currentTimeMillis() + "compress2.jpg");
            mCropUri = Uri.fromFile(file);
            cropIntent.putExtra("output", mCropUri);
            startActivityForResult(cropIntent, REQUEST_CODE_CROP_PIC);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void bitmapCompress1() {
        if (mBitmap != null) {
            String absolutePath = getFilePath();
            Log.d(TAG, "absolutePath: " + absolutePath);
            File file = new File(absolutePath, System.currentTimeMillis() + "compress1.jpg");
            Uri uri = Uri.fromFile(file);
            Log.d(TAG, "Uri.fromFile(file): " + uri.toString());
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
                boolean compress = mBitmap.compress(format, 0, os);
                if (compress) {
                    Toast.makeText(this, "compress success", Toast.LENGTH_SHORT).show();
                }
                String pathName = uri.toString().substring(7);
                Log.d(TAG, "path: " + pathName);
                showBitmapInfos(pathName);
                Bitmap bitmap = BitmapFactory.decodeFile(pathName);
                if (bitmap != null) {
                    bitmapShow(bitmap);
                }
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    private String getFilePath() {
        File cachePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cachePath = getExternalCacheDir();
        }else {
            cachePath = getCacheDir();
        }
        cachePath.mkdirs();
        return cachePath.getAbsolutePath();
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == REQUEST_CODE_CROP_PIC) {
                String pathname = mCropUri.toString().substring(7);
                showBitmapInfos(pathname);
                Bitmap bitmap = BitmapFactory.decodeFile(pathname);
                if (bitmap != null) {
                    bitmapShow(bitmap);
                }
            }else {
                mUri = data.getData();
                if (mUri != null) {
                    if (requestCode == REQUEST_CODE_PICK_FROM_GALLEY) {
                        mPath = processResultBeforeKitkat(mUri);
                    } else if (requestCode == REQUEST_CODE_GET_CONTENT_FROM_GALLEY) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            mPath = processResultOnKitkat(mUri);
                        } else {
                            mPath = processResultBeforeKitkat(mUri);
                        }
                    }
                    showBitmapInfos(mPath);
                    mBitmap = BitmapFactory.decodeFile(mPath);
                    if (mBitmap != null) {
                        bitmapShow(mBitmap);
                        //                    bitmap = null;
                    } else {
                        Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void bitmapShow(Bitmap bitmap) {
        mIvImage.setImageBitmap(bitmap);
        int count = bitmap.getByteCount();
        int allocationByteCount = bitmap.getAllocationByteCount();
        String countStr = Formatter.formatFileSize(this, count);
        String allStr = Formatter.formatFileSize(this, allocationByteCount);
        String resutlt = "这张图片占用内存大小:" + "getByteCount()= " + countStr +
                "getAllocationByteCount()= " + allStr;
        mTvSize.setText(resutlt);
    }

    private void showBitmapInfos(String path) {
        mTvPath.setText(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        mWidth = options.outWidth;
        mHeight = options.outHeight;
        options.inJustDecodeBounds = false;
        mTvInfo.setText("图片的信息是：宽：" + mWidth + ",高：" + mHeight);
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
