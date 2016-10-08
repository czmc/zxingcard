package com.czmc.zxingcard.idcard;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.czmc.zxingcard.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;


public class IDCardScanActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private LinearLayout mLinearLayout;
    private PreviewBorderView mPreviewBorderView;
    private SurfaceView mSurfaceView;

    private CameraManager cameraManager;
    private boolean hasSurface;
    private Intent mIntent;
    private static final String DEFAULT_PATH = "/sdcard/";
    private static final String DEFAULT_NAME = "default.jpg";
    private static final String DEFAULT_TYPE = "default";
    private String filePath;
    private String fileName;
    private String type;
    private Button take, light;
    private boolean toggleLight;
    private OkHttpClient mOkHttpClient = new OkHttpClient();
    private View view_scan_result;
    private TextView tv_result;
    private ImageView im_result;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_card_scan);
        initView();
        initIntent();
        initLayoutParams();

    }

    private void initView() {
        view_scan_result = findViewById(R.id.view_scan_result);
        progress=findViewById(R.id.progress);
        tv_result=(TextView) findViewById(R.id.tv);
        im_result=(ImageView)findViewById(R.id.img);
        findViewById(R.id.re_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                startActivity(new Intent(getBaseContext(),IDCardScanActivity.class));
            }
        });
    }

    private void initIntent() {
        mIntent = getIntent();
        filePath = mIntent.getStringExtra("path");
        fileName = mIntent.getStringExtra("name");
        type = mIntent.getStringExtra("type");
        if (filePath == null) {
            filePath = DEFAULT_PATH;
        }
        if (fileName == null) {
            fileName = DEFAULT_NAME;
        }
        if (type == null) {
            type = DEFAULT_TYPE;
        }
        Log.e("TAG", filePath + "/" + fileName + "_" + type);
    }

    /**
     * 重置surface宽高比例为3:4，不重置的话图形会拉伸变形
     */
    private void initLayoutParams() {
        take = (Button) findViewById(R.id.take);
        light = (Button) findViewById(R.id.light);
        take.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraManager.takePicture(null, null, myjpegCallback);
            }
        });
        light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!toggleLight) {
                    toggleLight = true;
                    cameraManager.openLight();
                } else {
                    toggleLight = false;
                    cameraManager.offLight();
                }
            }
        });

        //重置宽高，3:4
        int widthPixels = getScreenWidth(this);
        int heightPixels = getScreenHeight(this);
        mLinearLayout = (LinearLayout) findViewById(R.id.linearlaout);
        mPreviewBorderView = (PreviewBorderView) findViewById(R.id.borderview);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);


//        FrameLayout.LayoutParams surfaceviewParams = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
//        surfaceviewParams.width = heightPixels * 4 / 3;
//        surfaceviewParams.height = heightPixels;
//        mSurfaceView.setLayoutParams(surfaceviewParams);
//
//        FrameLayout.LayoutParams borderViewParams = (FrameLayout.LayoutParams) mPreviewBorderView.getLayoutParams();
//        borderViewParams.width = heightPixels * 4 / 3;
//        borderViewParams.height = heightPixels;
//        mPreviewBorderView.setLayoutParams(borderViewParams);
//
//        FrameLayout.LayoutParams linearLayoutParams = (FrameLayout.LayoutParams) mLinearLayout.getLayoutParams();
//        linearLayoutParams.width = widthPixels - heightPixels * 4 / 3;
//        linearLayoutParams.height = heightPixels;
//        mLinearLayout.setLayoutParams(linearLayoutParams);


        Log.e("TAG","Screen width:"+heightPixels * 4 / 3);
        Log.e("TAG","Screen height:"+heightPixels);

    }


    @Override
    protected void onResume() {
        super.onResume();
        /**
         * 初始化camera
         */
        cameraManager = new CameraManager();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();

        if (hasSurface) {
            // activity在paused时但不会stopped,因此surface仍旧存在；
            // surfaceCreated()不会调用，因此在这里初始化camera
            initCamera(surfaceHolder);
        } else {
            // 重置callback，等待surfaceCreated()来初始化camera
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        initCamera(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    /**
     * 初始camera
     *
     * @param surfaceHolder SurfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            cameraManager.startPreview();
        } catch (Exception ioe) {

        }
    }

    public Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree)
    {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            return bm1;
        } catch (OutOfMemoryError ex) {
        }
        return null;
    }
    @Override
    protected void onPause() {
        /**
         * 停止camera，是否资源操作
         */
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }


    /**
     * 拍照回调
     */
    Camera.PictureCallback myjpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            // 根据拍照所得的数据创建位图
            final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                    data.length);
            Bitmap bitmap2 =  adjustPhotoRotation(bitmap,90);
            int height = bitmap2.getHeight();
            int width = bitmap2.getWidth();
            int scanWidth=width*15/16;
            int scanHeight=(int)(scanWidth*0.63f);
            final Bitmap bitmap1 = Bitmap.createBitmap(bitmap2, (width - scanWidth) / 2, (height-scanHeight) / 2, scanWidth, scanHeight);

            File path=new File(filePath);
            if (!path.exists()){
                path.mkdirs();
            }
            File file = new File(path, type+"_"+fileName);

            FileOutputStream outStream = null;
            try {
                // 打开指定文件对应的输出流
                outStream = new FileOutputStream(file);
                // 把位图输出到指定文件中
                bitmap1.compress(Bitmap.CompressFormat.JPEG,
                        100, outStream);
                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            uploadAndRecognize(file.getAbsolutePath());
//
//            Intent intent = new Intent();
//            Bundle bundle = new Bundle();
//            bundle.putString("path", file.getAbsolutePath());
//            bundle.putString("type", type);
//            intent.putExtras(bundle);
//            setResult(RESULT_OK, intent);
//
//            IDCardScanActivity.this.finish();

        }
    };


    /**
     * 获得屏幕宽度，单位px
     *
     * @param context 上下文
     * @return 屏幕宽度
     */
    public int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }


    /**
     * 获得屏幕高度
     *
     * @param context 上下文
     * @return 屏幕除去通知栏的高度
     */
    public int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels-getStatusBarHeight(context);
    }
    /**
     * 获取通知栏高度
     *
     * @param context 上下文
     * @return 通知栏高度
     */
    public int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object obj = clazz.newInstance();
            Field field = clazz.getField("status_bar_height");
            int temp = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    private void uploadAndRecognize(final String filePath) {
        progress.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(filePath)){
            final File file=new File(filePath);
            //构造上传请求，类似web表单
            RequestBody requestBody = new MultipartBuilder().type(MultipartBuilder.FORM)
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"callbackurl\""), RequestBody.create(null, "/idcard/"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"action\""), RequestBody.create(null, "idcard"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"img\"; filename=\"idcardFront_user.jpg\""), RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();
            //这个是ui线程回调，可直接操作UI
            //进行包装，使其支持进度回调
            final Request request = new Request.Builder()
                    .header("Host", "ocr.ccyunmai.com")
                    .header("Origin", "http://ocr.ccyunmai.com")
                    .header("Referer", "http://ocr.ccyunmai.com/idcard/")
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2398.0 Safari/537.36")
                    .url("http://ocr.ccyunmai.com/UploadImg.action")
                    .post(requestBody)
                    .build();
            //开始请求
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e("TAG", "error");
                }
                @Override
                public void onResponse(Response response) throws IOException {
                    final String result=response.body().string();
                    Document parse = Jsoup.parse(result);
                    Elements select = parse.select("div.left fieldset");
                    Log.e("TAG",select.text());
                    Document parse1 = Jsoup.parse(select.text());
                    final StringBuilder builder=new StringBuilder();
                    String name=parse1.select("name").text();
                    String cardno=parse1.select("cardno").text();
                    String sex=parse1.select("sex").text();
                    String folk=parse1.select("folk").text();
                    String birthday=parse1.select("birthday").text();
                    String address=parse1.select("address").text();
                    String issue_authority=parse1.select("issue_authority").text();
                    String valid_period=parse1.select("valid_period").text();
                    builder.append("name:"+name)
                            .append("\n")
                            .append("cardno:" + cardno)
                            .append("\n")
                            .append("sex:" + sex)
                            .append("\n")
                            .append("folk:" + folk)
                            .append("\n")
                            .append("birthday:" + birthday)
                            .append("\n")
                            .append("address:" + address)
                            .append("\n")
                            .append("issue_authority:" + issue_authority)
                            .append("\n")
                            .append("valid_period:" + valid_period)
                            .append("\n");
                    Log.e("TAG", "name:" + name);
                    Log.e("TAG","cardno:"+cardno);
                    Log.e("TAG","sex:"+sex);
                    Log.e("TAG","folk:"+folk);
                    Log.e("TAG","birthday:"+birthday);
                    Log.e("TAG","address:"+address);
                    Log.e("TAG","issue_authority:"+issue_authority);
                    Log.e("TAG","valid_period:"+valid_period);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPreviewBorderView.setVisibility(View.GONE);
                            view_scan_result.setVisibility(View.VISIBLE);
                            tv_result.setText(builder.toString());
                            im_result.setImageURI(Uri.fromFile(file));
                            progress.setVisibility(View.GONE);
                        }
                    });
                }
            });
        }
    }
}
