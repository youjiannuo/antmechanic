package com.yn.framework.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.yn.framework.R;
import com.yn.framework.controller.AnnotationController;
import com.yn.framework.exception.YNInitDataException;
import com.yn.framework.exception.YNInitSetDataViewException;
import com.yn.framework.exception.YNInitTopBarException;
import com.yn.framework.exception.YNInitViewException;
import com.yn.framework.imageLoader.TaskQueue;
import com.yn.framework.interfaceview.YNOperationRemindView;
import com.yn.framework.manager.YNFragmentTransaction;
import com.yn.framework.remind.RemindAlertDialog;
import com.yn.framework.remind.ToastUtil;
import com.yn.framework.service.YNService;
import com.yn.framework.service.YNServiceConnection;
import com.yn.framework.system.BuildConfig;
import com.yn.framework.system.OnUnCaughtExceptionListener;
import com.yn.framework.system.SystemUtil;
import com.yn.framework.system.ViewUtil;
import com.yn.framework.system.YNApplication;
import com.yn.framework.thread.YNAsyncTask;
import com.yn.framework.view.NavigationBarView;
import com.yn.framework.view.YNFrameWork;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


/**
 * Created by youjiannuo on 15/7/1.
 */


public class YNCommonActivity extends FragmentActivity implements View.OnClickListener,
        RemindAlertDialog.OnClickListener, YNOperationRemindView,
        OnUnCaughtExceptionListener, RemindAlertDialog.OnKeyListener {

    //key type
    protected final static String KEY_TYPE = "1";
    protected final static String KEY_TITLE = "KEY_TITLE";
    protected final static String KEY_URL = "KEY_URL";
    public final static String KEY_ID = "KEY_ID";

    //导航栏
    protected NavigationBarView mBarView = null;
    //整体布局
    protected YNFrameWork mYNFrameWork = null;
    //显示界面
    protected View mShowView = null;
    //application
    protected YNApplication mYNApplication;
    //提醒按钮宽
    protected RemindAlertDialog mRemindAlertDialog;
    //加载出问题了
    protected FrameLayout mDataFailFrameLayout = null;
    //加载的数据为空
    protected ImageView mLoadDataNullView = null;
    //加载数据
    protected TextView mLoadDataNullMsgView;
    //加载文件
    protected FrameLayout mProgress = null;
    //异常退出的弹出框
    public static final int REMIND_EXCEPTION = -2000;
    //是否是处在栈顶
    private boolean mIsTopActivity = false;
    //错误的次数就显示一次
    private boolean mIsException = false;
    private boolean mIsWindow = true;
    //从加载错误重新新加载加载的监听事件
    private OnErrorReLoadListener mOnErrorReLoadListener;
    private int mTopProgress = 0;
    private YNFragmentTransaction mYnFragmentTransaction;
    //获取View的位置信息
    private ViewUtil.ScreenInfo mScreenInfo;
    //resume只能执行一次
    protected boolean mIsResume = false;
    private Handler mHandler;
    //服务
    private YNServiceConnection mServiceConnection;

    protected AnnotationController mAnnotationController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mAnnotationController = new AnnotationController(this);
        if (mAnnotationController.mLayoutId != 0) {
            if (!mAnnotationController.isTopBar()) {
                this.onCreate(savedInstanceState, mAnnotationController.mLayoutId);
            } else {
                this.onCreate(savedInstanceState, mAnnotationController.mLayoutId, -1);
            }
        } else {
            super.onCreate(savedInstanceState);
        }
    }

    /**
     * 调用这个方法是没有头部导航栏
     *
     * @param saveInstanceState
     * @param layoutResId       布局资源
     */
    protected void onCreate(Bundle saveInstanceState, int layoutResId) {
        this.onCreates(saveInstanceState, layoutResId, R.layout.y_activity_framework_not_with_bar_view);
    }

    /**
     * @param saveInstanceState
     * @param layoutResId       布局资源
     * @param titleResId        标题的字符串资源
     */
    protected void onCreate(Bundle saveInstanceState, int layoutResId, int titleResId) {
        onCreate(saveInstanceState, layoutResId, titleResId < 0 ? "" : getString(titleResId), "");
    }

    /**
     * @param saveInstanceState
     * @param layoutResId       布局资源
     * @param titleResId        标题字符串资源
     * @param rightResId        右边按钮资源，</br>注意：这个可以传入字符串资源，也可以是照片资源
     */
    protected void onCreate(Bundle saveInstanceState, int layoutResId, int titleResId, int rightResId) {
        Bitmap drawable = getRightDrawable(rightResId);
        if (drawable == null) {
            onCreate(saveInstanceState, layoutResId, titleResId < 0 ? "" : getString(titleResId), getString(rightResId));
        } else {
            onCreate(saveInstanceState, layoutResId, titleResId);
//            mBarView.setRightImageButton(drawable);
            mBarView.setRightImageButton(rightResId);
        }

    }


    /**
     * @param saveInstanceState
     * @param layoutResId       布局资源文件
     * @param titleString       标题字符串
     * @param rightString       左边按钮字符串
     */
    protected void onCreate(Bundle saveInstanceState, int layoutResId, String titleString, String rightString) {
        this.onCreates(saveInstanceState, layoutResId, R.layout.y_activity_framework_with_bar_view);

        if (titleString.length() != 0)
            mBarView.setTitle(titleString);
        if (rightString.length() != 0)
            mBarView.setRightTextView(rightString);

        mBarView.getLeftView().setOnClickListener(this);
        mBarView.getRightView().setOnClickListener(this);
        if (mAnnotationController != null) {
            mAnnotationController.mBarView = mBarView;
        }
        try {
            initTopBarView();
        } catch (Exception e) {
            e.printStackTrace();
            new YNInitTopBarException(e).throwException();
        }
    }

    /**
     * @param saveInstanceState
     * @param layoutResId           布局资源文件
     * @param titleLeftButtonResId  头部左边按钮字符串资源文件
     * @param titleRightButtonResId 头部右边按钮字符串资源文件
     * @param rightResId            右边资源文件
     */
    protected void onCreate(Bundle saveInstanceState, int layoutResId, int titleLeftButtonResId, int titleRightButtonResId, int rightResId) {
        onCreate(saveInstanceState, layoutResId, -1, rightResId);

        mBarView.setTitleLeftButton(getString(titleLeftButtonResId));
        mBarView.setTitleRightButton(getString(titleRightButtonResId));
        mBarView.getTitleLeftButton().setOnClickListener(this);
        mBarView.getTitleRightButton().setOnClickListener(this);
    }

    /**
     * @param saveInstanceState
     * @param layoutResId            布局文件
     * @param titleLeftButtonString  头部左边按钮字符串
     * @param titleRightButtonString 头部右边按钮字符串
     * @param rightString            右边按钮字符串
     */
    protected void onCreate(Bundle saveInstanceState, int layoutResId, String titleLeftButtonString, String titleRightButtonString, String rightString) {
        onCreate(saveInstanceState, layoutResId, "", rightString);
        mBarView.setTitleLeftButton(titleLeftButtonString);
        mBarView.setTitleRightButton(titleRightButtonString);
        mBarView.getTitleLeftButton().setOnClickListener(this);
        mBarView.getTitleRightButton().setOnClickListener(this);
    }

    protected void setViewVisible(int... ids) {
        setViewVisible(VISIBLE, ids);
    }

    protected void setViewGone(int... ids) {
        setViewVisible(GONE, ids);
    }

    private void setViewVisible(int visible, int... ids) {
        for (int id : ids) {
            findViewById(id).setVisibility(visible);
        }
    }

    protected void setClickListeners(View... vs) {
        for (View v : vs) {
            v.setOnClickListener(this);
        }
    }

    protected View[] setClickListeners(int... ids) {
        View views[] = new View[ids.length];
        int i = 0;
        for (int id : ids) {
            views[i] = findViewById(id);
            views[i++].setOnClickListener(this);
        }
        return views;
    }

    protected void setClickListenersAndSetTagNum(int... ids) {
        int i = 0;
        for (int id : ids) {
            View v = findViewById(id);
            v.setTag(i++);
            v.setOnClickListener(this);
        }
    }




    private void onCreates(Bundle savedInstanceState, int layoutResId, int frameWork) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mIsTopActivity = true;
        notRunSetContentView();
        initIntentSave(savedInstanceState);
        initFramework(layoutResId, frameWork);
        setContentView(mYNFrameWork);
        if (mBarView != null) {
            int top = getResources().getDimensionPixelSize(R.dimen.y_top_bar_height);
            if (isStatusBar()) {
                SystemUtil.setTranslucentStatus(this, true);
                if (Build.VERSION.SDK_INT >= 19) {
                    int height = mBarView.setValueView();
                    top += height;
                }
            }
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.topMargin = top;
            mShowView.setLayoutParams(params);
            //存在导航栏的设置一下
            mYNFrameWork.setOperationPosition(top);
        }
        //设置提醒控件的位置
        addUnCaughtExceptionListener();
        try {
            initData();
        } catch (Exception e) {
            e.printStackTrace();
            new YNInitDataException(e).throwException();
        }
        try {
            initView();
        } catch (Exception e) {
            e.printStackTrace();
            new YNInitViewException(e).throwException();
        }
        try {
            setViewData();
        } catch (Exception e) {
            e.printStackTrace();
            new YNInitSetDataViewException(e).throwException();
        }
        //是否需要启动线程
        if (isDoBackgroundRun()) {
            showProgressDialog();
            startThreadRun("");
        }
        //是否启动服务器
        if (isStartServer()) {
            startServer();
        }


//        setStatusBarColor();
        //添加的是图片缓存
        TaskQueue.TASK_QUEUE.onCreate(this);
    }



    protected void notRunSetContentView() {

    }

    //是否需要显示等在进度条
    protected boolean isStartThreadShowProgress() {
        return mAnnotationController == null || mAnnotationController.mIsThreadShowProgress;
    }

    protected boolean isDoBackgroundRun() {
        return mAnnotationController != null && mAnnotationController.mThread != -1;
    }


    //设置状态栏的颜色
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor() {
        try {
            //5.0版本处理
            if (SystemUtil.getAndroidApi() > 21) {
                Window window = getWindow();
                //取消设置透明状态栏,使 ContentView 内容不再覆盖状态栏
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                //需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                //设置状态栏颜色
                window.setStatusBarColor(getStatusBarColor());

                ViewGroup mContentView = (ViewGroup) findViewById(Window.ID_ANDROID_CONTENT);
                View mChildView = mContentView.getChildAt(0);
                if (mChildView != null) {
                    //注意不是设置 ContentView 的 FitsSystemWindows, 而是设置 ContentView 的第一个子 View . 预留出系统 View 的空间.
                    ViewCompat.setFitsSystemWindows(mChildView, true);
                }
            }
        } catch (Exception | NoSuchMethodError e) {

        }


    }

    protected int getStatusBarColor() {
        return getResources().getColor(R.color.yn_status_bar_bg_color);
    }

    public View getShowView() {
        return mShowView;
    }


    public boolean isTopActivity() {
        return mIsTopActivity;
    }

    public Handler getMainHandler() {
        if (mHandler == null) return mHandler = new Handler(Looper.getMainLooper());
        return mHandler;
    }

    public <T> T findView(int id) {
        return (T) findViewById(id);
    }

    private void initFramework(int layoutId, int layoutFrameworkId) {
        LayoutInflater layout = LayoutInflater.from(this);
        mYNFrameWork = (YNFrameWork) layout.inflate(layoutFrameworkId, null);
        mBarView = (NavigationBarView) mYNFrameWork.findViewById(R.id.barView);
        mDataFailFrameLayout = (FrameLayout) mYNFrameWork.findViewById(R.id.fail);
        mProgress = (FrameLayout) mYNFrameWork.findViewById(R.id.progress1);
        mLoadDataNullView = (ImageView) mYNFrameWork.findViewById(R.id.upLoadNull);
        mLoadDataNullMsgView = (TextView) mYNFrameWork.findViewById(R.id.nullMsg);
        if (mProgress != null) {
            mProgress.setOnClickListener(this);
            mProgress.setSelected(false);
        }
        if (mDataFailFrameLayout != null) {
            mDataFailFrameLayout.setOnClickListener(this);
        }

        mShowView = layout.inflate(layoutId, null);
        int index = 1;
        if (mBarView == null) {
            index = 0;
        }
        mYNFrameWork.addView(mShowView, index);

    }

    //是否设置状态
    public boolean isStatusBar() {
        return SystemUtil.getAndroidApi() >= 23;
    }


    public void showRemindBox(int[] buttonId, int messageId, int titleId) {
        showRemindBox(buttonId, messageId, titleId, -1);
    }

    protected void showRemindBox(int[] buttonId, int messageId, int titleId, int type) {
        String button[] = new String[buttonId.length];
        String message = messageId == -1 ? "" : getString(messageId);
        String title = titleId == -1 ? "" : getString(titleId);
        for (int i = 0; i < button.length; i++) {
            button[i] = getString(buttonId[i]);
        }
        showRemindBox(button, message, title, type);
    }

    public void showRemindBox(String[] button, String message, String title) {
        showRemindBox(button, message, title, -1, -1);
    }

    protected void showRemindBox(String[] button, String message, String title, int type) {
        showRemindBox(button, message, title, -1, type);
    }

    protected void showRemindBox(String[] button, String message, String title, int icon, int type) {
        if (isFinishing()) return;
        if (mRemindAlertDialog == null) {
            mRemindAlertDialog = installRemindAlertDialog();
            mRemindAlertDialog.setOnKeyListener(this);
        }
        mRemindAlertDialog.setType(type);
        try {
            mRemindAlertDialog.show(button, title, message, icon, this);
        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    protected RemindAlertDialog installRemindAlertDialog() {
        return new RemindAlertDialog(this);
    }

    //显示加载数据为空
    public void showLoadDataNullView() {
        if (mLoadDataNullView != null) {
            mLoadDataNullView.setVisibility(VISIBLE);
        }
    }

    //关闭加载为空
    public void closeLoadDataNullView() {
        if (mLoadDataNullView != null) {
            mLoadDataNullView.setVisibility(GONE);
        }
    }


    public Intent newIntent(Class cls, Object... keyAndValue) {
        Intent intent = new Intent(this, cls);
        if (keyAndValue != null && keyAndValue.length != 0) {
            for (int i = 0; i < keyAndValue.length; i += 2) {
                if (keyAndValue[i + 1] instanceof String) {
                    intent.putExtra(keyAndValue[i].toString(), keyAndValue[i + 1].toString());
                } else if (keyAndValue[i + 1] instanceof Integer) {
                    intent.putExtra(keyAndValue[i].toString(), (Integer) keyAndValue[i + 1]);
                } else if (keyAndValue[i + 1] instanceof Boolean) {
                    intent.putExtra(keyAndValue[i].toString(), (boolean) keyAndValue[i + 1]);
                } else if (keyAndValue[i + 1] instanceof Double) {
                    intent.putExtra(keyAndValue[i].toString(), (Double) keyAndValue[i + 1]);
                }
            }
        }
        return intent;
    }

    public void openNewActivityForResult(Class cls, int requestCode, Object... keyAndValue) {
        startActivityForResult(newIntent(cls, keyAndValue), requestCode);
    }

    public void openNewActivity(Class cls, Object... keyAndValue) {
        startActivity(newIntent(cls, keyAndValue));
    }

    public void showTopProgress() {
        if (mBarView != null) {
            mTopProgress++;
            mBarView.showTopProgress();
        }
    }

    public void closeTopProgress() {
        mTopProgress--;
        if (mBarView != null && mTopProgress <= 0) {
            mBarView.closeTopProgress();
            mTopProgress = 0;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mIsWindow) {
            mIsWindow = false;
            onWindowInitComplete();
        }
    }

    //window初始化完成
    protected void onWindowInitComplete() {

    }

    private Bitmap getRightDrawable(int drawId) {
        try {
            return BitmapFactory.decodeResource(getResources(), drawId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    protected final void showProgressDialog(int resId) {
        showProgressDialog(getText(resId));
    }


    protected final void showProgressDialog(Object obj) {

    }

    public void setOnErrorReLoadListener(OnErrorReLoadListener l) {
        mOnErrorReLoadListener = l;
    }

    /**
     * 加载错误
     */
    public void showLoadFailDialog() {
        if (mDataFailFrameLayout != null) {
            mDataFailFrameLayout.setVisibility(VISIBLE);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    /**
     * 关闭加载错误
     */
    public void closeLoadFailDialog() {
        mDataFailFrameLayout.setVisibility(GONE);
    }

    @Override
    public boolean isShowLoadFailDialog() {
        return mDataFailFrameLayout.getVisibility() == VISIBLE;
    }

    //是否可以開啟等待進度條
    public boolean isOkShowProgressDialog() {
        return !(mDataFailFrameLayout == null || mProgress == null);
    }

    /**
     * 显示加载进度条
     *
     * @param s
     */
    public void showProgressDialog(String s) {
        if (isFinishing()) return;
        if (mDataFailFrameLayout == null || mProgress == null) return;
        mDataFailFrameLayout.setVisibility(GONE);
        mProgress.setVisibility(VISIBLE);
        View v = mProgress.getChildAt(0);
        Animation mAnimation = AnimationUtils.loadAnimation(this, R.anim.y_progress);
        LinearInterpolator lin = new LinearInterpolator();
        mAnimation.setInterpolator(lin);
        v.startAnimation(mAnimation);
        //屏蔽不可以点击1
        mProgress.setSelected(true);
    }

    public void showProgressDialog() {
        showProgressDialog("");
    }

    public final void showProgressDialogMarginTop(int top) {
        showProgressDialogMarginBottom(top, 0);
    }

    public final void showProgressDialogMarginBottom(int bottom) {
        showProgressDialogMarginBottom(0, bottom);
    }

    public final void showProgressDialogMarginBottom(int top, int bottom) {
        showProgressDialog();
        setProgressViewMarginTopAndBottom(top, bottom);
    }

    public void closeProgressDialog() {
        if (mProgress != null) {
            mProgress.setVisibility(GONE);
            mProgress.setSelected(false);
        }
    }

    //取消网络请求回调
    public void cancelProgress() {

    }

    //等待进度条设置头部不被控制的区域
    public void setProgressViewMarginTop(int top) {
        setProgressViewMarginTopAndBottom(top, 0);
    }

    //等待进度条设置低部不被控制的区域
    public void setProgressViewMarginBottom(int bottom) {
        setProgressViewMarginTopAndBottom(0, bottom);
    }

    //等待进度条是否显示
    public final boolean isProgressShow() {
        return mProgress.getVisibility() == VISIBLE;
    }

    //同上
    public void setProgressViewMarginTopAndBottom(int top, int bottom) {
        if (mProgress != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mProgress.getLayoutParams();
            params.bottomMargin = bottom;
            params.topMargin = top;
            mProgress.setLayoutParams(params);
        }
    }

//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            onLeftButtonClick(null);
//            return true;
//        }
//        return super.onKeyUp(keyCode, event);
//    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && mProgress != null && isProgressShow()) {
                cancelProgress();
                closeProgressDialog();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 添加异常错误的监听
     * 只要程序崩溃了，可以在子类的activity调用这个方法，那么在子类的activity里面
     * 可以在一次实现方法onException();
     */
    public void addUnCaughtExceptionListener() {
        getApplications().addOnUnCaughtExceptionListener(this);
    }

    public void removeUnCaughtExceptionListener() {
        getApplications().removeOnUnCaughtExceptionListener(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //移除异常监听
        removeUnCaughtExceptionListener();
        TaskQueue.TASK_QUEUE.destroyAllTask(this);
        if (mYnFragmentTransaction != null) {
            List<Fragment> fragments = mYnFragmentTransaction.getFragments();
            for (int i = 0; fragments != null && i < fragments.size(); i++) {
                Fragment fragment = fragments.get(i);
                if (!fragment.isRemoving()) {
                    mYnFragmentTransaction.remove(fragment);
                }
                fragment.onDestroy();
            }
        }
        if (mServiceConnection != null) {
            mServiceConnection.unbindService(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mYnFragmentTransaction != null) {
            List<Fragment> fragments = mYnFragmentTransaction.getFragments();
            for (int i = 0; i < fragments.size(); i++) {
                Fragment fragment = fragments.get(i);
                if (fragment == null) {
                    continue;
                }
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsTopActivity = false;
//        if (mRemindAlertDialog != null) {
//            mRemindAlertDialog.close();
//        }
        TaskQueue.TASK_QUEUE.pauseAllTask(this);
        MobclickAgent.onPageStart(getClass().toString());
        MobclickAgent.onPause(this);
        if (mYnFragmentTransaction != null) {
            for (Fragment fragment : mYnFragmentTransaction.getShowFragment()) {
                fragment.onPause();
            }
        }
    }


    /**
     * 添加Umeng事件监听
     *
     * @param eventId
     */
    public void addUmengClickStatistics(String eventId) {

    }


    @Override
    protected void onResume() {
        super.onResume();
        mIsTopActivity = true;
        TaskQueue.TASK_QUEUE.resumeAllTask(this);
        MobclickAgent.onPageStart(getClass().toString());
        MobclickAgent.onResume(this);
        if (mYnFragmentTransaction != null) {
            for (Fragment fragment : mYnFragmentTransaction.getShowFragment()) {
                fragment.onResume();
            }
        }

        if (!mIsResume) {
            mIsResume = true;
        } else {
            notRunStartResume();
        }

    }

    protected void notRunStartResume() {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mYnFragmentTransaction != null) {
            for (Fragment fragment : mYnFragmentTransaction.getFragments()) {
                fragment.onStop();
            }
        }
    }

    public final YNApplication getApplications() {
        return mYNApplication = (YNApplication) super.getApplication();
    }

    protected void initIntentSave(Bundle saveBundle) {

    }

    protected void initView() {

    }

    protected void initData() {

    }

    protected void setViewData() {

    }

    protected void initTopBarView() {
        if (mAnnotationController != null) {
            mAnnotationController.initTopBar();
        }
    }

    public YNFragmentTransaction getYNFragmentTransaction() {
        if (mYnFragmentTransaction == null) {
            mYnFragmentTransaction = new YNFragmentTransaction(this);
        }
        return mYnFragmentTransaction;
    }


    /**
     * 导航栏左边按钮的触发事件
     *
     * @param v
     */
    public void onLeftButtonClick(View v) {
        finish();
    }


    /**
     * 导航栏右边按钮的触发事件
     *
     * @param v
     */
    public void onRightButtonClick(View v) {
        isCloseProgress();
    }

    protected boolean isCloseProgress() {
        //判断等待进度条是否显示
        if (isProgressShow()) {
            closeProgressDialog();
            return true;
        }
        return false;
    }

    @Override
    public boolean onRemindItemClick(int position, int type) {
        // TODO Auto-generated method stub
        if (position == RemindAlertDialog.LEFTBUTTON) {
            if (!onRemindBoxExceptionClick(type)) {
                onRemindBoxLeftButtonClick(type);
            }
        } else if (position == RemindAlertDialog.CENTERBUTTON) {
            onRemindBoxCenterButtonClick(type);
        } else if (position == RemindAlertDialog.RIGHTBUTTON) {
            onRemindBoxRightButtonClick(type);
        }
        return false;
    }


    /**
     * 消息选择宽的左边按钮
     */
    protected void onRemindBoxLeftButtonClick(int type) {

    }

    /**
     * 消息选择宽的中间按钮
     */
    protected void onRemindBoxCenterButtonClick(int type) {

    }

    /**
     * 消息选择框的右边按钮
     */
    protected void onRemindBoxRightButtonClick(int type) {

    }

    /**
     * 点击头部左边
     *
     * @param v
     */
    protected void onTopLeftClick(View v) {
        isCloseProgress();
    }

    /**
     * 点击头部右边
     *
     * @param v
     */
    @Deprecated
    protected void onTopRightClick(View v) {
        isCloseProgress();
    }

    /**
     * 如果加载数据错误，
     */
    protected void onReLoadDataFromNetwork() {
        closeLoadFailDialog();
//        showLoadNewDataDialog();
        showProgressDialog();
        if (mOnErrorReLoadListener != null) {
            mOnErrorReLoadListener.onErrorReLoad();
        }
        onError();
    }

    protected void onError() {

    }

    /**
     * 添加头部右边按钮
     *
     * @param view
     */
    public void addTopRightView(View view) {
        mBarView.addRightChildView(view);
        mBarView.getRightView().setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mBarView != null) {
            if (mBarView.getRightView() == v) {
                if (!isProgressShow()) {
                    onRightButtonClick(v);
                }
            } else if (mBarView.getLeftView() == v) {
                onLeftButtonClick(v);
            } else if (mBarView.getTitleLeftButton() == v || mBarView.getTitleRightButton() == v) {
                if (mBarView.getTitleLeftButton() == v) {
                    if (mBarView.getTitleLeftButtonTop().getVisibility() == VISIBLE) return;
                    onTopLeftClick(v);
                    mBarView.getTitleLeftButtonTop().setVisibility(VISIBLE);
                    mBarView.getTitleRightButtonTop().setVisibility(View.INVISIBLE);
                } else {
                    if (mBarView.getTitleRightButtonTop().getVisibility() == VISIBLE) return;
                    onTopRightClick(v);
                    mBarView.getTitleRightButtonTop().setVisibility(VISIBLE);
                    mBarView.getTitleLeftButtonTop().setVisibility(View.INVISIBLE);
                }
            }
        }

        if (mDataFailFrameLayout == v) {
            onReLoadDataFromNetwork();
        }

    }


    protected View getContextView() {
        return mYNFrameWork;
    }

    /**
     * 用来处理异常错误
     * 在activity里面需要使用这个方法，
     * 必须要先调用addUnCaughExceptionListener();
     */
    @Override
    public void dispatchException() {
        final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        if (mIsException) return;
        mIsException = true;

        if (isFinishing()) {
            //activity已经被关闭的
            removeUnCaughtExceptionListener();
        } else if (isTopActivity()) {
            //当前处在栈顶
            onInterceptException();
        } else {
            //不在栈顶触发的异常错误
            onActivityNotRootException();
        }
    }

    /**
     * 当前的activity处于栈顶回调该方法
     *
     * @return
     */
    public final boolean onInterceptException() {

        if (!onExceptions()) {

//            new Thread() {
//                @Override
//                public void run() {
//                    if (mRemindAlertDialog == null || !mRemindAlertDialog.isShowing()) {
//                        Looper.prepare();
//                        showRemindBox(new int[]{R.string.yn_i_know}, R.string.yn_program_erro_resume, R.string.yn_toast, REMIND_EXCEPTION);
//                        Looper.loop();
//                    }
//                }
//            }.start();
        }
        return false;
    }

    /**
     * 当前的activity处于栈顶回调该方法
     *
     * @return
     */
    public boolean onExceptions() {
        return false;
    }

    public String getKeyIdString() {
        return getIntentString("id");
    }

    public NavigationBarView getBarView() {
        return mBarView;
    }

    /**
     * 不在栈顶的回调方法
     *
     * @return
     */
    public boolean onActivityNotRootException() {
        return false;
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, int type, KeyEvent event) {

        if (type == REMIND_EXCEPTION) {
            //异常错误
            onRemindBoxExceptionClick(type);
        }
        return false;
    }

    private boolean onRemindBoxExceptionClick(int type) {
        if (type == REMIND_EXCEPTION) {
            Intent intent = new Intent();
            try {
                intent.setClass(this, Class.forName(getString(R.string.yn_home_activity_class)));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            android.os.Process.killProcess(android.os.Process.myPid());
            return true;
        }
        return false;
    }

    public boolean startCheckoutActivity(String className) {
        return false;
    }

    protected int getKeyType() {
        return getIntent().getIntExtra(KEY_TYPE, 0);
    }

    protected String getKeyTitle() {
        return getIntent().getStringExtra(KEY_TITLE);
    }

    protected String getKeyId() {
        return getIntentString(KEY_ID);
    }

    public static void setIntent(Intent intent, int type) {
        intent.putExtra(KEY_TYPE, type);
    }


    public String getIntentString(String key) {
        return getIntent().getStringExtra(key);
    }

    public int getIntentInt(String key) {
        return getIntent().getIntExtra(key, -1);
    }

    public interface OnErrorReLoadListener {
        void onErrorReLoad();
    }

    public void setLoadNullView(View view) {

        if (mScreenInfo == null) {
            mScreenInfo = ViewUtil.getScreenInfo(view, mShowView);
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mLoadDataNullView.getLayoutParams();
        params.topMargin = mScreenInfo.y + SystemUtil.dipTOpx(30);
        mLoadDataNullView.setLayoutParams(params);
    }

    protected void setViewClickListener(int id) {
        findViewById(id).setOnClickListener(this);
    }

    public final void startThreadRun(final Object... param) {
        String log = "";
        if (!BuildConfig.ENVIRONMENT) {
            StackTraceElement stackTraceElement[] = Thread.currentThread().getStackTrace();
            if (stackTraceElement.length >= 7) {
                log = stackTraceElement[5].getFileName().split("\\.")[0] + ":"
                        + stackTraceElement[5].getLineNumber() + ":"
                        + stackTraceElement[5].getMethodName() + "()";
            }
        }
        YNAsyncTask task = new YNAsyncTask<Object, Void, Object>(this, isFinishActivityStopRun(), log) {

            @Override
            protected Object doInBackground(Object... v) {
                return doBackground(v);
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (!isFinishing()) {
                    handlerMessage(o);
                    if (isStartThreadShowProgress()) {
                        closeProgressDialog();
                    }
                }
            }
        };
        if (!isSingleThread()) {
            task.executeOnExecutor(param);
        } else {
            task.execute(param);
        }
    }

    //是否加载单个线程
    //如果返回的true，将会执行单个
    protected boolean isSingleThread() {
        return mAnnotationController != null && mAnnotationController.mThread == 2;
    }

    //activity被finish掉了，停止线程的执行
    protected boolean isFinishActivityStopRun() {
        return false;
    }


    protected void publishProgress(final Object... params) {
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                progress(params);
            }
        });
    }

    protected void progress(Object... params) {

    }

    protected Object doBackground(Object... params) {

        return null;
    }

    protected void handlerMessage(Object obj) {

    }

    public String getAnnotationString() {
        return null;
    }

    protected boolean isStartServer() {
        return false;
    }

    protected Intent getServerIntent() {
        return null;
    }

    protected int getServerFlag() {
        return BIND_AUTO_CREATE;
    }

    //开启服务器
    protected void startServer() {
        mServiceConnection = new YNServiceConnection() {
            @Override
            protected void bindSuccess(YNService service) {
                if (service == null) {
                    ToastUtil.showFailMessageHandler(R.string.yn_service_start_fail);
                    finish();
                    return;
                }
                service.setHandler(getMainHandler());
                onServerBindSuccess(service);
            }
        };
        bindService(getServerIntent(), mServiceConnection, getServerFlag());
    }

    protected void onServerBindSuccess(YNService service) {

    }
}
