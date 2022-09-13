package comp5216.sydney.edu.au.mediarecording;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import comp5216.sydney.edu.au.mediarecording.adapter.MediaAdapter;
import comp5216.sydney.edu.au.mediarecording.entity.MediaInfo;
import comp5216.sydney.edu.au.mediarecording.utils.Utils;

public class MainActivity extends AppCompatActivity {

    //Define variables
    public final String APP_TAG = "MediaRecording";

    public String photoFileName = "photo.jpg";
    public String videoFileName = "video.mp4";

    // 视频或者图片的存储文件
    private File file;

    // collection 名称 表名
    private String mFireStoreName = "media_info";

    // adapter 数据
    List<MediaInfo> items = new ArrayList<>();

    MediaAdapter mediaAdapter;
    ListView mListView;
    FloatingActionButton floatingActionButton;

    //request codes  拍照，读照片、录像、读录像
    private static final int MY_PERMISSIONS_REQUEST_OPEN_CAMERA = 101;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHOTOS = 102;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_VIDEO = 103;
    private static final int MY_PERMISSIONS_REQUEST_READ_VIDEOS = 104;


    // 权限类
    MarshmallowPermission marshmallowPermission = new MarshmallowPermission(this);

    // 定位的类
    FusedLocationProviderClient fusedLocationClient;

    // 用户默认城市
    String currLocationAddress = "Sydney";

    // Firebase storage
    FirebaseStorage storage = FirebaseStorage.getInstance();

    // storage reference
    StorageReference storageRef;

    // firebase 数据库实例
    FirebaseFirestore mFirebaseFirestore = FirebaseFirestore.getInstance();

    // 上传任务集合
    List<UploadTask> mUploadTaskList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取页面元素
        mListView = findViewById(R.id.listView);

        readFromDatabase();

        mediaAdapter = new MediaAdapter(this, items);
        mListView.setAdapter(mediaAdapter);
        floatingActionButton = findViewById(R.id.fab);


        // 获取用户位置
        requestLocation();

        // 事件监听
        handler();
    }


    public void handler() {
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChoose();
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, MediaShowActivity.class);
                intent.putExtra("type", items.get(i).getType());
                intent.putExtra("url", items.get(i).getUrl());

                startActivity(intent);
            }
        });
    }

    // 同步任务
    public void syncFireStorageTask(List<MediaInfo> mediaInfoList) {

        // 1. 任务开始之前，  判断是否是wifi   省流
        if (!Utils.isWifiConnected(this)) {
            return;
        }
        // 是否链接网络
        if (!Utils.isOpenNetwork(this)) {
            return;
        }

        // Create a storage reference from our app
        storageRef = storage.getReference();

        for (MediaInfo mediaInfo :
                mediaInfoList) {
            /**
             * 1.  已经上传完成的不进入到循环内
             * 2.  判断上传百分比，  如果>0 <100  查找本地上传进度，  找不到再重新上传
             * 3、 0 的    直接上传， 创建任务
             */
            if (mediaInfo.getSyncStatus() != 100) {

                UploadTask uploadTask;
                // File or Blob
                Uri file = Uri.fromFile(new File(mediaInfo.getUrl()));

                Uri sessionUri = Utils.readSharedPreferences(MainActivity.this, mediaInfo.getDocumentId());
                if (mediaInfo.getSyncStatus() != 0 && sessionUri != null) {
                    // sessionUri  从 缓存里面拿出来    继续上传
                    uploadTask = storageRef.putFile(file, new StorageMetadata.Builder().build(), sessionUri);
                } else {
                    // =0    新任务
                    uploadTask = storageRef.child(mediaInfo.getCity() + "/" + file.getLastPathSegment()).putFile(file);
                }

                // 加到任务
                mUploadTaskList.add(uploadTask);

                // Listen for state changes, errors, and completion of the upload.
                uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                        // 每上传完一部分内容之后， 存储进度    SharedPreferences
                        Uri sessionUri = taskSnapshot.getUploadSessionUri();
                        if (sessionUri != null) {
                            Utils.saveSharedPreferences(MainActivity.this, mediaInfo.getDocumentId(), sessionUri);
                        }
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        mediaInfo.setSyncStatus(progress);
                        updateDataFromFireStore(mediaInfo);
                        Log.d(APP_TAG, "onProgress Upload is " + progress + "% done");
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Handle successful uploads on complete
                        // ...
                        // 删除上传进度
                        Utils.deleteSharedPreferences(MainActivity.this, mediaInfo.getDocumentId());
                        mediaInfo.setSyncStatus(100);
                        updateDataFromFireStore(mediaInfo);
                        Log.d(APP_TAG, "onSuccess");
                    }
                });
            }
        }
    }

    /**
     * 更新数据
     *
     * @param mediaInfo
     */
    public void updateDataFromFireStore(MediaInfo mediaInfo) {
        mediaAdapter.notifyDataSetChanged();
        Map mediaInfoMap = JSON.parseObject(JSON.toJSONString(mediaInfo), Map.class);
        mFirebaseFirestore.collection(mFireStoreName).document(mediaInfo.getDocumentId()).update(mediaInfoMap);
    }

    /**
     * 定位方法
     * 1. 不使用谷歌地图api 是因为 只需要获取用户所在城市这一个简单功能，  就不需要引入全量谷歌地图 ， 减少包体积和代码量，    为了节能，谷歌地图占用资源较多
     * 2. 谷歌地图   联网，  网络请求获取地址
     * 目前的解决方案是不需要联网的，   gps 获取到经纬度，  内置方法反查城市
     */
    public void requestLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 动态请求位置信息
            Toast.makeText(this, "GPS or NETWORK error", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // 初始化定位
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            Log.e(APP_TAG, location.getLatitude() + "," + location.getLongitude());
                            // 坐标反查城市
                            Geocoder gc = new Geocoder(MainActivity.this, Locale.getDefault());
                            try {
                                List<Address> address = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                currLocationAddress = address.get(0).getLocality();
                                Log.e(APP_TAG, currLocationAddress);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }


    // 查库
    public void readFromDatabase() {
        mFirebaseFirestore.collection(mFireStoreName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            List<MediaInfo> mediaInfoList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // 查询到的数据转成  MediaInfo 对象
                                MediaInfo mediaInfo = JSON.parseObject(JSON.toJSONString(document.getData()), MediaInfo.class);
                                mediaInfo.setDocumentId(document.getId());
                                mediaInfoList.add(mediaInfo);
                                Log.d(APP_TAG, document.getId() + " => " + document.getData());
                            }
                            items.clear();
                            items.addAll(mediaInfoList);
                            syncFireStorageTask(mediaInfoList);
                            mediaAdapter.notifyDataSetChanged();
                        } else {
                            Log.d(APP_TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public void buildData(String url, String city, Integer type, int syncStatus) {
        //实例化对象
        MediaInfo mediaInfo = new MediaInfo();
        //设置属性
        mediaInfo.setId(System.currentTimeMillis());
        mediaInfo.setUrl(url);
        mediaInfo.setCity(city);
        mediaInfo.setType(type);
        mediaInfo.setSyncStatus(syncStatus);

        // Add a new document with a generated ID
        mFirebaseFirestore.collection("media_info")
                .add(mediaInfo)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(APP_TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        mediaInfo.setDocumentId(documentReference.getId());
                        // 新的数据加到list 里面
                        items.add(mediaInfo);
                        mediaAdapter.notifyDataSetChanged();

                        // 新增一个同步任务
                        List<MediaInfo> mediaInfoList = new ArrayList<>();
                        mediaInfoList.add(mediaInfo);
                        syncFireStorageTask(mediaInfoList);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(APP_TAG, "Error adding document", e);
                    }
                });
    }

    public void onLoadPhotoClick() {
        if (!marshmallowPermission.checkPermissionForReadfiles()) {
            marshmallowPermission.requestPermissionForReadfiles();
        } else {
            // Create intent for picking a photo from the gallery
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // Bring up gallery to select a photo
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_PHOTOS);
        }
    }

    public void onLoadVideoClick() {
        if (!marshmallowPermission.checkPermissionForReadfiles()) {
            marshmallowPermission.requestPermissionForReadfiles();
        } else {
            // Create intent for picking a video from the gallery
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            // Bring up gallery to select a video
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_VIDEOS);
        }
    }

    public void onTakePhotoClick() {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()
                || !marshmallowPermission.checkPermissionForExternalStorage()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
            photoFileName = "IMG_" + timeStamp + ".jpg";
            // Create a photo file reference
            Uri file_uri = getFileUri(photoFileName);
            // Add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);
            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, MY_PERMISSIONS_REQUEST_OPEN_CAMERA);
            }
        }
    }

    public void onRecordVideoClick() {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()
                || !marshmallowPermission.checkPermissionForExternalStorage()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to capture a video and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
            videoFileName = "VIDEO_" + timeStamp + ".mp4";
            // Create a video file reference
            Uri file_uri = getFileUri(videoFileName);
            // add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);
            // Start the video record intent to capture video
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_RECORD_VIDEO);
        }
    }


    // 创建一个空的本地文件
    public File createFile(String fileName) {
        File mediaStorageDir = new
                File(getExternalFilesDir(Environment.getExternalStorageDirectory().toString()), "Utils");
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d("Utils", "failed to create directory");
        }
        // Create the file target for the media based on filename
        file = new File(mediaStorageDir, fileName);

        return mediaStorageDir;
    }


    // Returns the Uri for a photo/media stored on disk given the fileName
    public Uri getFileUri(String fileName) {
        Uri fileUri = null;
        try {
            File mediaStorageDir = createFile(fileName);
            // Wrap File object into a content provider, required for API >= 24
            // See https://guides.codepath.com/android/Sharing-Content-withIntents#sharing-files-with-api-24-or-higher
            if (Build.VERSION.SDK_INT >= 24) {
                fileUri = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "comp5216.sydney.edu.au.mediarecording.fileProvider", file);
            } else {
                fileUri = Uri.fromFile(mediaStorageDir);
            }
            return fileUri;
        } catch (Exception ex) {
            Log.e("getFileUri", ex.getStackTrace().toString());
            return null;
        }

    }

    /**
     *
     */
    private void showChoose() {
        //    指定下拉列表的显示数据
        final String[] actions = {"Take Photo", "Load Photo", "Record Video", "Load Video"};
        //    设置一个下拉的列表选择项
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("Please Pick one ？")
                .setIcon(R.drawable.ic_launcher_background)
                .setItems(actions, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                onTakePhotoClick();
                                break;
                            case 1:
                                onLoadPhotoClick();
                                break;
                            case 2:
                                onRecordVideoClick();
                                break;
                            case 3:
                                onLoadVideoClick();
                                break;
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_PERMISSIONS_REQUEST_OPEN_CAMERA) {
            if (resultCode == RESULT_OK) {
                // 通过源文件，压缩成一个新的文件   webp 格式
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.WEBP, 50, baos);
                try {
                    // 1. 创建文件
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date());
                    photoFileName = "IMG_" + timeStamp + ".webp";
                    createFile(photoFileName);

                    // 压缩后的流，  写到创建的文件里
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(baos.toByteArray());
                    fos.flush();
                    fos.close();
                    buildData(file.getAbsolutePath(), currLocationAddress, 1, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHOTOS) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 50, baos);
                    // 1. 创建文件
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date());
                    photoFileName = "IMG_" + timeStamp + ".webp";
                    createFile(photoFileName);

                    // 压缩后的流，  写到创建的文件里
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(baos.toByteArray());
                    fos.flush();
                    fos.close();
                    buildData(file.getAbsolutePath(), currLocationAddress, 1, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_VIDEOS) {
            if (resultCode == RESULT_OK) {

                // 拿到视频的地址
                Uri selectedVideo = data.getData();
                String[] filePathColumn = {MediaStore.Video.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedVideo,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String videoPath = cursor.getString(columnIndex);
                cursor.close();


                buildData(videoPath, currLocationAddress, 2, 0);
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_VIDEO) {
            if (resultCode == RESULT_OK) {
                buildData(file.getAbsolutePath(), currLocationAddress, 2, 0);
            }
        }
    }
}