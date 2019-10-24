package com.utvgo.huya.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.lzy.okgo.model.Response;
import com.utvgo.handsome.config.AppConfig;
import com.utvgo.handsome.diff.DiffConfig;
import com.utvgo.handsome.interfaces.JsonCallback;
import com.utvgo.huya.R;
import com.utvgo.huya.beans.BaseResponse;
import com.utvgo.huya.beans.BeanStatistics;
import com.utvgo.huya.beans.OpItem;
import com.utvgo.huya.beans.ProgramContent;
import com.utvgo.huya.beans.ProgramInfoBase;
import com.utvgo.huya.beans.TPageData;
import com.utvgo.huya.beans.VideoInfo;
import com.utvgo.huya.net.NetworkService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MediaAlbumActivity extends BuyActivity {

    @BindView(R.id.tv_count)
    TextView tvCount;

    @BindView(R.id.video)
    VideoView video;

    @BindView(R.id.bg)
    ImageView bgImageView;

    @BindView(R.id.icon_left_narrow)
    ImageView leftImageView;
    @BindView(R.id.icon_right_narrow)
    ImageView rightImageView;

    private String showType = "";
    private boolean isExperience;

    private int freeTime = -1;
    private long nowTime = 0;
    private int playIndex = 0;
    private String albumMid;
    public TPageData beanWLAblumData;
    public List<OpItem> subjectRecordListBeen = new ArrayList<>();


    private int pageIndex = 0;
    final int pageSize = 4;

    private final List<RelativeLayout> itemViewArray = new ArrayList<>();

    private int albumId, channelId, multisetType, currentPage = 0, totalPage = 0;
    private ProgramContent albumData = null;

    long currentMediaDuration = 0;

    public static void show(final Context context, final int albumId)
    {
        Intent intent = new Intent(context, MediaAlbumActivity.class);
        intent.putExtra("albumId", albumId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ablum);

        ButterKnife.bind(this);

        this.albumId = getIntent().getIntExtra("albumId", 61916);
        this.multisetType = getIntent().getIntExtra("multiSetType", 4);
        this.channelId = getIntent().getIntExtra("channelId", 36);

        final int array[] = {R.id.fl_item1, R.id.fl_item2, R.id.fl_item3, R.id.fl_item4 };
        for(int i = 0; i < array.length; i++)
        {
            this.itemViewArray.add((RelativeLayout)findViewById(array[i]));
        }

        setHahaPlayer(video);

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        video.resume();
        /*
        if (hadCallBuyView  && !isExperience) {
            hadCallBuyView = false;
            finish();
        } else {
            playMedia(playIndex);
        }
        */
    }


    @Override
    protected void onStart() {
        super.onStart();
        statusticsHandler.sendEmptyMessageDelayed(TagStartStatisticsPlay, 30 * 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        startStatisticsPlay(this.currentMediaDuration);
        statusticsHandler.removeMessages(TagStartStatisticsPlay);
    }

    @Override
    protected void onPause() {
        video.pause();
        super.onPause();
    }

    private static final int TagStartStatisticsPlay = 10011;
    private String statisticsPlayId = "";

    Handler statusticsHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == TagStartStatisticsPlay) {
                startStatisticsPlay(currentMediaDuration);
                statusticsHandler.sendEmptyMessageDelayed(TagStartStatisticsPlay, 30 * 1000);
            }
            return false;
        }
    });

    private void startStatisticsPlay(final long duration) {
        try{
            if(beanWLAblumData != null && playIndex >= 0 && playIndex < albumData.getVideos().size())
            {
                VideoInfo videoBean = albumData.getVideos().get(playIndex);
                NetworkService.defaultService().statisticsVideo(this,
                        "" + 0,
                        "" + videoBean.getVideoId(),
                        videoBean.getName(),
                        "" + albumData.getSupplierId(),
                        albumData.getSupplierName(),
                        "" + albumData.getPkId(),
                        albumData.getName(),
                        "" + albumData.getChannelId(),
                        "",
                        albumData.getMultiSetType(),
                        "0",
                        DiffConfig.CurrentPurchase.isPurchased() ? "1" : "0",
                        AppConfig.VipCode,
                        statisticsPlayId,
                        "" + 0,
                        duration,
                        new JsonCallback<BaseResponse<BeanStatistics>>() {
                            @Override
                            public void onSuccess(Response<BaseResponse<BeanStatistics>> response) {
                                BaseResponse<BeanStatistics> beanStatistics = response.body();
                                if (beanStatistics != null && beanStatistics.getData() != null) {
                                    statisticsPlayId = beanStatistics.getData().getId();
                                }
                            }
                        }
                );
                stat("视频播放-" + videoBean.getName());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void playMedia(int index) {
        if(index >= this.albumData.getVideos().size() || index < 0)
        {
            index = 0;
        }
        if(this.albumData != null)
        {
            statisticsPlayId = "";
            this.playIndex = index;
            VideoInfo videoBean = this.albumData.getVideos().get(this.playIndex);
            hahaPlayUrl(videoBean.getMediaSourceUrl());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean flag = false;
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            {
                View view = getCurrentFocus();
                if(view != null)
                {
                    int focusViewId = view.getId();
                    if(focusViewId == R.id.btn_fl_item4)
                    {
                        if(((this.currentPage + 1)*this.pageSize) < this.albumData.getVideos().size())
                        {
                            flag = true;
                            this.currentPage++;
                            updateItemCount();
                            updateItemContent();
                            findViewById(R.id.btn_fl_item1).requestFocus();
                        }
                    }
                }
                break;
            }

            case KeyEvent.KEYCODE_DPAD_LEFT:
            {
                View view = getCurrentFocus();
                if(view != null)
                {
                    int focusViewId = view.getId();
                    if(focusViewId == R.id.btn_fl_item1)
                    {
                        if(this.currentPage > 0)
                        {
                            flag = true;
                            this.currentPage--;
                            updateItemCount();
                            updateItemContent();
                            findViewById(R.id.btn_fl_item1).requestFocus();
                        }
                    }
                }
                break;
            }
        }
        if(!flag)
        {
            flag = super.onKeyDown(keyCode, event);
        }
        return flag;
    }


    @Override
    public void getHahaPlayerUrl(String vodID) {
        VideoInfo videoBean = this.albumData.getVideos().get(this.playIndex);
        hahaPlayUrl(videoBean.getMediaSourceUrl());
    }

    @Override
    public void hahaPlayEnd(float v) {
        super.hahaPlayEnd(v);
        playMedia(playIndex + 1);
    }

    @Override
    public void onDuration(long l) {
        this.currentMediaDuration = l;
        startStatisticsPlay(this.currentMediaDuration);
        super.onDuration(l);
    }

    @Override
    public void setPlayTime(long nowTime, long allTime) {
        super.setPlayTime(nowTime, allTime);
        this.nowTime = nowTime;
        needBuy();
    }

    private boolean needBuy() {
        if (freeTime != -1 && !isExperience) {
            if (nowTime / 1000 > freeTime) {
                //showBuy(mvDetail.getMv().getVodId());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        final int viewId = view.getId();
        switch (viewId)
        {
            case R.id.btn_fl_video:
            {
                playVideoFullScreen();
                break;
            }

            case R.id.btn_fl_item1:
            case R.id.btn_fl_item2:
            case R.id.btn_fl_item3:
            case R.id.btn_fl_item4:
            {
                final int array[] = {R.id.btn_fl_item1, R.id.btn_fl_item2, R.id.btn_fl_item3, R.id.btn_fl_item4};
                int index = Arrays.binarySearch(array, viewId);
                if(index >= 0)
                {
                    int playIndex = this.currentPage*pageSize + index;
                    playMedia(playIndex);
                }
            }
        }
    }

    public void playVideoFullScreen() {
        ArrayList<ProgramInfoBase> list = new ArrayList<>();
        list.add(this.albumData);
        PlayVideoActivity.play(this, list, this.playIndex, false);
    }

    @Override
    public void showBuy(String vodID) {
        if (buySingle) {
            super.showBuy(albumMid);
        } else {
            super.showBuy(vodID);
            hadCallBuyView = true;
        }
    }

    @Override
    public void onBackPressed() {
        if (isExperience) {
            isExperience = false;
            showBuy("");
        } else {
            super.onBackPressed();
        }
    }

    void updateAlbumBackground(final String imageUrl)
    {
        if(TextUtils.isEmpty(imageUrl))
        {
            bgImageView.setImageResource(R.mipmap.bg_album);
        }
        else
        {
            loadImage(bgImageView, DiffConfig.generateImageUrl(imageUrl));
        }
    }

    void updateItemCount()
    {
        String text = String.format("< %d/%d >", Math.min(this.currentPage + 1, this.totalPage), this.totalPage);
        tvCount.setText(text);

        leftImageView.setImageResource((this.currentPage > 0)?R.mipmap.icon_previous:R.mipmap.icon_previous_disavle);
        rightImageView.setImageResource(((this.currentPage + 1) >= (this.totalPage))?R.mipmap.icon_next_disable:R.mipmap.icon_next);
    }

    void updateItemContent()
    {
        boolean flag = false;
        for(int j = 0; j < itemViewArray.size(); j++)
        {
            RelativeLayout itemLayout = this.itemViewArray.get(j);
            int contentIndex = this.currentPage*pageSize + j;
            if(contentIndex < this.albumData.getVideos().size())
            {
                itemLayout.setVisibility(View.VISIBLE);
                VideoInfo videoBean = this.albumData.getVideos().get(contentIndex);
                for(int k = 0; k < itemLayout.getChildCount(); k++)
                {
                    flag = true;
                    View view = itemLayout.getChildAt(k);
                    if(view instanceof ImageView)
                    {
                        ImageView imageView = (ImageView)view;
                        String posterUrl = DiffConfig.generateImageUrl(videoBean.getPoster());
                        loadImage(imageView, posterUrl);
                    }
                    if(view instanceof TextView)
                    {
                        TextView textView = (TextView)view;
                        textView.setText("" + (contentIndex + 1) + ". " + videoBean.getName());
                    }
                }
            }
            else
            {
                itemLayout.setVisibility(View.INVISIBLE);
            }
        }
        findViewById(flag ? R.id.btn_fl_item1 : R.id.btn_fl_video).requestFocus();
    }


    /*
    *** Network
     */
    void loadData()
    {
        NetworkService.defaultService().fetchProgramContent(this, this.albumId, this.multisetType + "", this.channelId,  new JsonCallback<BaseResponse<ProgramContent>>() {
            @Override
            public void onSuccess(Response<BaseResponse<ProgramContent>> response) {
                BaseResponse<ProgramContent> data = response.body();
                if(data.isOk())
                {
                    layout(data.getData());
                    stat("专辑播放-" + data.getData().getName());
                }
            }
        });
    }

    void layout(final ProgramContent bean)
    {
        if(bean == null)
        {
            return;
        }

        this.albumData = bean;

        this.currentPage = 0;
        this.totalPage = this.albumData.getVideos().size()/pageSize;
        if(this.albumData.getVideos().size()%pageSize > 0)
        {
            this.totalPage++;
        }

        updateAlbumBackground(bean.getAlbumBackground());
        updateItemCount();
        updateItemContent();

        if(this.albumData.getVideos().size() > 0)
        {
            playMedia(0);
        }
    }
}