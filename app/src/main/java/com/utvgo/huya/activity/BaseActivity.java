package com.utvgo.huya.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.lzy.okgo.model.Response;
import com.utvgo.handsome.config.AppConfig;
import com.utvgo.handsome.diff.DiffConfig;
import com.utvgo.handsome.diff.TOPWAYBox;
import com.utvgo.handsome.interfaces.JsonCallback;
import com.utvgo.handsome.utils.TopWayBroacastUtils;
import com.utvgo.huya.BuildConfig;
import com.utvgo.huya.HuyaApplication;
import com.utvgo.huya.R;
import com.utvgo.huya.beans.BaseResponse;
import com.utvgo.huya.beans.OpItem;
import com.utvgo.huya.beans.ProgramContent;
import com.utvgo.huya.beans.ProgramInfoBase;
import com.utvgo.huya.beans.TypesBean;
import com.utvgo.huya.constant.ConstantEnum;
import com.utvgo.huya.net.NetworkService;
import com.utvgo.huya.utils.DensityUtil;
import com.utvgo.huya.utils.HiFiDialogTools;
import com.utvgo.huya.utils.StringUtils;
import com.utvgo.huya.utils.ToastUtil;
import com.utvgo.huya.views.FocusBorderView;

import java.io.File;
import java.util.ArrayList;

import static com.utvgo.huya.constant.ConstantEnumHuya.Asset_Id;
import static com.utvgo.huya.constant.ConstantEnumHuya.Category_Id;

public class BaseActivity extends RooterActivity implements View.OnFocusChangeListener,
        AdapterView.OnItemSelectedListener,
        AdapterView.OnItemClickListener,
        View.OnClickListener,
        ViewTreeObserver.OnTouchModeChangeListener,   // 用于监听 Touch 和非 Touch 模式的转换
        ViewTreeObserver.OnGlobalLayoutListener,     // 用于监听布局之类的变化，比如某个空间消失了
        ViewTreeObserver.OnPreDrawListener,        // 用于在屏幕上画 View 之前，要做什么额外的工作
        ViewTreeObserver.OnGlobalFocusChangeListener // 用于监听焦点的变化
{
    public String TAG = "huyatest";
    public float scale = 1.0f;
    public View focusView = null;
    Handler handler = new Handler();

    public HiFiDialogTools hiFiDialogTools = new HiFiDialogTools();
    private FrameLayout topLayout;
    private View viewTip;
    public FocusBorderView borderView;
    public boolean hadCallBuyView = false;
    public boolean needBringFront = true;
    private boolean needTransition = true;
    public static String playingTitle;
    TypesBean typesBean = new TypesBean();
    public Handler baseHandle = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what == 257){
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DensityUtil.init(this, 1280);
        super.onCreate(savedInstanceState);
        //registerReceiver(mHomeKeyEventReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));


    }

    @Override
    protected void onDestroy() {

       //  unregisterReceiver(mHomeKeyEventReceiver);
         //unregisterReceiver(orderBroadcastReceiver);
        borderView = null;
        hiFiDialogTools = null;
        focusView = null;
        topLayout = null;
        viewTip = null;
        super.onDestroy();


    }

    //对 专题 聚焦框进行另一种处理 （与showViewByHandler并行项目聚焦框）
    public void focusOn1stView(final View view, final int borderId, final int borderViewAndX, final int borderViewAndY) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (view == null) {
                    return;
                }
                if (borderView != null) {
                    borderView.setBorderBitmapResId(borderId, borderViewAndX, borderViewAndY);
                }
                view.clearFocus();
                view.requestFocus();
            }
        });
    }

    //延时设置view获得焦点
    public void showViewByHandler(final View view) {
        view.clearFocus();//不清除让系统认为同个view再次聚焦，不会调用onFocusChange的
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (borderView != null) {
                    borderView.setVisibility(View.VISIBLE);
                }
                view.requestFocus();
            }
        }, 500);
    }

    /**
     * @param activity
     */
    @Override
    public void createBorderView(Activity activity) {
        borderView = new FocusBorderView(this);
        borderView.setBorderBitmapResId(R.mipmap.border_focus_style_default);
        topLayout = (FrameLayout) getRootView(activity);
        borderView.setBorderViewHide();

        if (topLayout != null) {
            topLayout.addView(borderView);
        }
    }

    public View getRootView(Activity context) {
        return ((ViewGroup) context.findViewById(android.R.id.content)).getChildAt(0).findViewById(R.id.activity_RootView);
    }

    /**
     * 遍历所有view,实现focus监听
     *
     * @param activity
     */
    public void traversalView(Activity activity) {
        ViewGroup viewGroup = (ViewGroup) getRootView(activity);
        traversal(viewGroup);

    }

    public void traversal(ViewGroup viewGroup) {
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = viewGroup.getChildAt(i);
            doView(view);
            if (view instanceof ViewGroup)
                traversal((ViewGroup) view);
        }
    }

    /**
     * 处理view
     *
     * @param view
     */
    private void doView(View view) {
        if (view == null)
            return;
        if (view.isFocusable()) {
            view.setOnFocusChangeListener(this);
        }


//        Object tagO = view.getTag();
//        if (tagO == null)
//            return;
//        String tag = tagO.toString();
//        if (TextUtils.isEmpty(tag))
//            return;
//        if (TextUtils.equals(tag, this.getResources().getString(R.string.tagForFocus))) {
//            view.setFocusable(true);
//            view.setOnFocusChangeListener(this);
//        }else {
//            view.setFocusable(false);
//        }
    }


    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (borderView == null) {
            return;
        }
        if (hasFocus) {
            if (needBringFront) {
                v.bringToFront();
                v.getParent().bringChildToFront(v);
            }
            v.requestLayout();
            v.invalidate();
            borderView.setHideView(false);//这一句非常重要
            borderView.setFocusView(v, scale);
            focusView = v;
        } else {
            borderView.setUnFocusView(v);
        }
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {

    }

    @Override
    public void onGlobalLayout() {

    }

    @Override
    public boolean onPreDraw() {
        return false;
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void startActivity(Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//设置切换没有动画，用来实现活动之间的无缝切换
        super.startActivity(intent);
    }

    /**
     * 重点，在这里设置按下返回键，或者返回button的时候无动画
     */
    @Override
    public void finish() {
        if (!needTransition) {
            overridePendingTransition(0, 0);//设置返回没有动画
        }
//        ViewGroup view = (ViewGroup) getWindow().getDecorView();
//        view.removeAllViews();
        super.finish();
    }

    public void changeFragment(Fragment fragment) {

    }

    public void showOther(ViewGroup viewGroup, int value) {
//        SpringChain springChain = SpringChain.create(40, 6, 50, 7);
//        int childCount = viewGroup.getChildCount();
//        for (int i = 0; i < childCount; i++) {
//            final View view = viewGroup.getChildAt(i);
//            springChain.addSpring(new SimpleSpringListener() {
//                @Override
//                public void onSpringUpdate(Spring spring) {
//                    view.setTranslationX((float) spring.getCurrentValue());
//                }
//            });
//        }
//        List<Spring> springs = springChain.getAllSprings();
//        for (int i = 0; i < springs.size(); i++) {
//            springs.get(i).setCurrentValue(value);
//        }
//        springChain.setControlSpringIndex(2).getControlSpring().setEndValue(0);
    }

    public void showViewTip(String tipStr) {
        if (viewTip == null) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            viewTip = View.inflate(this, R.layout.layout_empty, null);
            topLayout.addView(viewTip, params);
        }
        TextView tipTV = (TextView) viewTip.findViewById(R.id.tv_tip);
        viewTip.setVisibility(View.VISIBLE);
        tipTV.setText(tipStr);
    }

    public void hideViewTip() {
        if (viewTip != null) {
            viewTip.setVisibility(View.GONE);
        }
    }

    /**
     * 监听是否点击了home键将客户端推到后台
     */
    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)) {
                    HuyaApplication.beanActivity = null;
                    TopWayBroacastUtils.getInstance().pressHomeKey(context);
                    clearCacheHomeKey();
                    //表示按了home键,程序到了后台
//                    finish();
//                    android.os.Process.killProcess(android.os.Process.myPid());
//                    Process.killProcess(Process.myPid());
                }
            }
        }
    };


    private BroadcastReceiver orderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int payState = intent.getIntExtra("payState",-9);
            String info = intent.getStringExtra("info");
            String timestamp = intent.getStringExtra("timestamp");
           // this.clearAbortBroadcast();
            Log.d("BroadcastReceiver", "onReceive: payState："+intent.getAction()+" info:"+info+"timestamp:"+timestamp);
        }
    };


    public void netBack(int requestTag, Object object) {

    }

    public void buyDialogDimiss() {

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d("BaseActivity", "====BaseActivity has been recycled!");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            keyCode = KeyEvent.KEYCODE_DPAD_CENTER;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View view) {

    }

    protected void stat(final String name, final String title,final String pageType) {
        try {
            TopWayBroacastUtils.getInstance().pageEvent(this,pageType,name,Asset_Id,Category_Id);
            NetworkService.defaultService().statisticsVisit(this, "app-" + name, title, "");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stat(final String name,final String pageType) {
        stat(name, "",pageType);
    }

    public void loadImage(final ImageView imageView, final String imageUrl) {
        Glide.with(this)
                .load(DiffConfig.generateImageUrl(imageUrl))
                .into(imageView);
    }

    public boolean actionOnOp(final OpItem opItem)
    {
        boolean ret = false;
        if(opItem != null)
        {
            ret = true;
            final Context context = this;
            //if (BuildConfig.DEBUG){opItem.setHrefType("0");}
            ConstantEnum.OpType opType = ConstantEnum.OpType.fromTypeString(opItem.getHrefType());
            String href = opItem.getHref();
            Uri uri = Uri.parse(href);
            switch (opType)
            {
                case web:
                {   if(!href.startsWith("http:")){
                        href=DiffConfig.WebUrlBase+href;
                     }

                    QWebViewActivity.navigateUrl(this, href);
                    break;
                }
                case smallVideo:
                    break;
                case program:
                {
                    String channelId = uri.getQueryParameter("channelId");
                    ProgramListActivity.show(this, StringUtils.intValueOfString(channelId),
                            opItem.getName(), 29, 0);

                    /*String programId = uri.getQueryParameter("pkId");
                    String multisetType = uri.getQueryParameter("multisetType");
                    String channelId = uri.getQueryParameter("channelId");
                    NetworkService.defaultService().fetchProgramContent(this, StringUtils.intValueOfString(programId),
                            multisetType, StringUtils.intValueOfString(channelId), new JsonCallback<BaseResponse<ProgramContent>>() {
                                @Override
                                public void onSuccess(Response<BaseResponse<ProgramContent>> response) {
                                    BaseResponse<ProgramContent> data = response.body();
                                    if(data.isOk())
                                    {
                                        ArrayList<ProgramInfoBase> list = new ArrayList<>();
                                        list.add(data.getData());
                                        PlayVideoActivity.play(context, list, 0, false);
                                    }
                                }
                            });*/
                    break;
                }

                case album:{
                    String albumId = uri.getQueryParameter("pkId");
                    MediaAlbumActivity.show(this, StringUtils.intValueOfString(albumId));
                    break;
                }

                case topic:{
                    String topicId = uri.getQueryParameter("themId");
                    String styleId = uri.getQueryParameter("styleID");
                    TopicActivity.show(this, topicId, styleId);
                    break;
                }
                case activity:{
                    if (HuyaApplication.hadBuy()) {
                        //会员不弹活动
                        ToastUtil.showLong(BaseActivity.this,"你已是虎牙TV的会员");
                        break;
                    } else{
                        QWebViewActivity.navigateUrl(this, opItem.getHref(), null,null);
                    }
//                    Intent intent = new Intent(BaseActivity.this, ActivityActivity.class);
//                    intent.putExtra("bgImageUrl",DiffConfig.generateImageUrl(opItem.getImgUrl()));
//                    startActivity(intent);
                    break;
                }
                case topicPage:
                case topicCollection:
                    String pkgId = uri.getQueryParameter("typeId");
                    CategoryListActivity.show(context,Integer.valueOf(pkgId), JSON.toJSONString(typesBean));
                    break;
                case albumList:
                    String typeId = uri.getQueryParameter("typeId");
                    AlbumListActivity.show(context,Integer.valueOf(typeId.trim()), opItem.getName(),JSON.toJSONString(typesBean));
                case more:
                case back:

                        default:
                {
                    ret = false;
                    break;
                }
            }
        }
        return ret;
    }
    public boolean actionOnNavOp(TypesBean.DataBean.navigationBarBean navigationBarBean)
    {
        boolean ret = false;
        if(navigationBarBean != null)
        {
            ret = true;
            final Context context = this;
            //if (BuildConfig.DEBUG){opItem.setHrefType("0");}
            ConstantEnum.OpType opType = ConstantEnum.OpType.fromTypeString(navigationBarBean.getHrefType());
            String href = navigationBarBean.getHref();
            Uri uri = Uri.parse(href);
            switch (opType)
            {
                case web:
                {   if(!href.startsWith("http:")){
                    href=DiffConfig.WebUrlBase+href;
                }
                    if(BuildConfig.DEBUG){href="http://172.16.146.56/huyaTV/activityHalf.html";}
                    QWebViewActivity.navigateUrl(this, href);
                    break;
                }
                case program:
                {
                    String channelId = uri.getQueryParameter("channelId");
                    ProgramListActivity.show(this, StringUtils.intValueOfString(channelId),
                            navigationBarBean.getColumnName(), 29, 0);

                    /*String programId = uri.getQueryParameter("pkId");
                    String multisetType = uri.getQueryParameter("multisetType");
                    String channelId = uri.getQueryParameter("channelId");
                    NetworkService.defaultService().fetchProgramContent(this, StringUtils.intValueOfString(programId),
                            multisetType, StringUtils.intValueOfString(channelId), new JsonCallback<BaseResponse<ProgramContent>>() {
                                @Override
                                public void onSuccess(Response<BaseResponse<ProgramContent>> response) {
                                    BaseResponse<ProgramContent> data = response.body();
                                    if(data.isOk())
                                    {
                                        ArrayList<ProgramInfoBase> list = new ArrayList<>();
                                        list.add(data.getData());
                                        PlayVideoActivity.play(context, list, 0, false);
                                    }
                                }
                            });*/
                    break;
                }

                case album:{
                    String albumId = uri.getQueryParameter("pkId");
                    MediaAlbumActivity.show(this, StringUtils.intValueOfString(albumId));
                    break;
                }

                case topic:{
                    String topicId = uri.getQueryParameter("themId");
                    String styleId = uri.getQueryParameter("styleID");
                    TopicActivity.show(this, topicId, styleId);
                    break;
                }
                case activity:{
                    if (HuyaApplication.hadBuy()) {
                        //会员不弹活动
                        ToastUtil.showLong(BaseActivity.this,"你已是虎牙TV的会员");
                        break;
                    } else{
                        QWebViewActivity.navigateUrl(this, navigationBarBean.getHref(), null,null);
                    }
//                    Intent intent = new Intent (BaseActivity.this, ActivityActivity.class);
//                    intent.putExtra("bgImageUrl",DiffConfig.generateImageUrl(opItem.getImgUrl()));
//                    startActivity(intent);
                    break;
                }
                case topicPage:
                case topicCollection:
                    String pkId = uri.getQueryParameter("typeId");
                    CategoryListActivity.show(context,Integer.valueOf(pkId), JSON.toJSONString(typesBean));
                    break;
                case albumList:
                    String typeId = uri.getQueryParameter("typeId");
                    String name = navigationBarBean.getColumnName();
                    AlbumListActivity.show(context,Integer.valueOf(typeId.trim()), name,JSON.toJSONString(typesBean));
                    break;
                case more:
                case back:

                default:
                {
                    ret = false;
                    break;
                }
            }
        }
        return ret;
    }
    public  void actionProgram(String channelId,String name){
        ProgramListActivity.show(this, StringUtils.intValueOfString(channelId),
                name, StringUtils.intValueOfString(AppConfig.PackageId), 0);

    }
    public void jumpAppStore(){
        try{
            if(DiffConfig.CurrentTVBox instanceof TOPWAYBox){
                Intent intent = new Intent();
                ComponentName componentName = new ComponentName("com.topway.tvappstore","com.topway.tvappstore.AppDetailActivity");
                //ComponentName compon entName = new ComponentName("com.utvgo.huya","com.utvgo.huya.activity.LaunchActivity");
                Log.d("", "jumpAppStore: "+getApplicationContext().getPackageName());
                intent.setComponent(componentName);
                intent.putExtra("packageName",getApplicationContext().getPackageName());
                intent.setComponent(componentName);
//                Stri ng category = "android.intent.category.DEFAULT";
//                intent.addCategory(category);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }else {

                Intent intent = new Intent();
                ComponentName componentName = new ComponentName("com.huan.appstore", "com.huan.appstore.ui.AppDetailActivity");
                //ComponentName componentName = new ComponentName("com.utvgo.huya","com.utvgo.huya.activity.LaunchActivity");
                Log.d("", "jumpAppStore: " + getApplicationContext().getPackageName());
                intent.setComponent(componentName);
                intent.putExtra("packagename", getApplicationContext().getPackageName());
                intent.setComponent(componentName);
                String category = "android.intent.category.DEFAULT";
                intent.addCategory(category);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }catch (Exception e){
            e.printStackTrace();
            onBackPressed();
        }
    }
    public void clearCache(){
        final Context context = this;

        Glide.get(context).clearMemory();
        Log.d(TAG, "clearCache: clearMemory");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(context).clearDiskCache();
                Log.d(TAG, "clearCache: clearDiskCache");
                try{
                    File cacheDir = getCacheDir();
                    deleteFilesByDirectory(cacheDir);
                }catch(Exception e){
                    e.printStackTrace();

                }
            }
        }).start();
    }
    /** * 删除方法 这里只会删除某个文件夹下的文件，如果传入的directory是个文件，将不做处理 * * @param directory */
    public static void deleteFilesByDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            for (File item : directory.listFiles()) {
                boolean ret = item.delete();
                Log.d("deleteFiles", ret ? "OK"+  item.getAbsolutePath() : "Failed" + ": delete file " + item.getAbsolutePath());
            }
        }
    }
    /**
     * 主页键清缓存，杀进程
     *
     * */
    public void clearCacheHomeKey(){
        final Context context = this;

        Glide.get(context).clearMemory();
        Log.d(TAG, "clearCache: clearMemory");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(context).clearDiskCache();
                Log.d(TAG, "clearCache: clearDiskCache");
                try{
                    File cacheDir = getCacheDir();
                    deleteFilesByDirectory(cacheDir);
                }catch(Exception e){
                    e.printStackTrace();

                }
                baseHandle.sendEmptyMessage(257);
            }
        }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");;
    }
}
