/**
The MIT License (MIT)

Copyright (c) 2014 singwhatiwanna
https://github.com/singwhatiwanna
http://blog.csdn.net/singwhatiwanna

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.example.zs.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListView;

import java.util.ArrayList;

public class PinnedHeaderExpandableListView extends ExpandableListView implements OnScrollListener {
    private static final String TAG = "PinnedHeaderExpandableListView";
    private static final boolean DEBUG = true;

    //我加的
    private final int PULL_TO_REFRESH = 0;
    private final int RELEASE_TO_REFRESH = 1;
    private final int REFRESHING = 2;
    private View footView;

    public interface OnHeaderUpdateListener {
        /**
         * 返回一个view对象即可
         * 注意：view必须要有LayoutParams
         */
        public View getPinnedHeader();

        public void updatePinnedHeader(View headerView, int firstVisibleGroupPos);
    }

    public View mHeaderView;
    private int mHeaderWidth;
    private int mHeaderHeight;

    private View mTouchTarget;

    private OnScrollListener mScrollListener;
    private OnHeaderUpdateListener mHeaderUpdateListener;
    //private OnRemindGroupExpandStateListener mOnRemindGroupExpandStateListener;
    private boolean mActionDownHappened = false;
    protected boolean mIsHeaderGroupClickable = true;
    //我加的
    private int mCurrentSta = PULL_TO_REFRESH;
    private int startY = -1;
    //主要用在更新头部
    private int dy = -1;

    public PinnedHeaderExpandableListView(Context context) {
        super(context);
        initView();
    }

    public PinnedHeaderExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PinnedHeaderExpandableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }
   public void initFootView(View footView){

       addFooterView(footView);

   }



    private void initView() {
        setFadingEdgeLength(0);
        setOnScrollListener(this);
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        if (l != this) {
            mScrollListener = l;
        } else {
            mScrollListener = null;
        }
        super.setOnScrollListener(this);
    }

    /**
     * 给group添加点击事件监听
     * @param onGroupClickListener 监听
     * @param isHeaderGroupClickable 表示header是否可点击<br/>
     * note : 当不想group可点击的时候，需要在OnGroupClickListener#onGroupClick中返回true，
     * 并将isHeaderGroupClickable设为false即可
     */
    public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener, boolean isHeaderGroupClickable) {
        mIsHeaderGroupClickable = isHeaderGroupClickable;
        super.setOnGroupClickListener(onGroupClickListener);
    }

    public void setOnHeaderUpdateListener(OnHeaderUpdateListener listener) {
        mHeaderUpdateListener = listener;
        if (listener == null) {
            mHeaderView = null;
            mHeaderWidth = mHeaderHeight = 0;
            return;
        }

        int firstVisiblePos = getFirstVisiblePosition();
      //  Log.i("xuanyan","9090909090:"+firstVisiblePos);


            int firstVisibleGroupPos = getPackedPositionGroup(getExpandableListPosition(firstVisiblePos));
            if(firstVisibleGroupPos!=-1) {
                mHeaderView = listener.getPinnedHeader();
            }
            listener.updatePinnedHeader(mHeaderView, firstVisibleGroupPos);
            requestLayout();
            postInvalidate();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView == null) {
            return;
        }
        measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
        mHeaderWidth = mHeaderView.getMeasuredWidth();
        mHeaderHeight = mHeaderView.getMeasuredHeight();
    }

    public int getStartY(){
        return startY;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mHeaderView == null) {
            return;
        }
        int delta = mHeaderView.getTop();
        mHeaderView.layout(0, delta, mHeaderWidth, mHeaderHeight + delta);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mHeaderView != null) {
            drawChild(canvas, mHeaderView, getDrawingTime());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
      //  Log.i("haha","进入dispatchTouchEvent！！！！");
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int pos = pointToPosition(x, y);
//        int dy;
       /* if (mHeaderView != null && y >= mHeaderView.getTop() && y <= mHeaderView.getBottom())*/
            if (mHeaderView != null && y >= mHeaderView.getTop()&&y<=mHeaderView.getBottom()){
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                mTouchTarget = getTouchTarget(mHeaderView, x, y);
                mActionDownHappened = true;
                //我加的,此句至关重要
                startY = y;
            }/* else if(ev.getAction() == MotionEvent.ACTION_MOVE){
                Log.i("haha","进入dispatchTouchEvent 的Move！！！！");
                if(mCurrentSta != REFRESHING){
                int endY = (int) ev.getRawY();
                dy =  endY - startY;
                    Log.i("haha","dy = "+dy);
                if(dy>0){
                    Log.i("haha","进入dy>0!!!!");
                        if(dy>mHeaderHeight){
                            mCurrentSta = RELEASE_TO_REFRESH;
                            this.dy = dy;
                            Log.i("haha","调用refreshHeader()!!!!");
                            refreshHeader();
                        }else{
                            mCurrentSta = PULL_TO_REFRESH;
                            Log.i("haha","进入dy< mHeaderHeight!!!!"+mHeaderView.getHeight());

                        }
                     }
                }
            }*/else if (ev.getAction() == MotionEvent.ACTION_UP) {

                View touchTarget = getTouchTarget(mHeaderView, x, y);
                if (touchTarget == mTouchTarget && mTouchTarget.isClickable()) {
                    mTouchTarget.performClick();
                    invalidate(new Rect(0, 0, mHeaderWidth, mHeaderHeight));
                } else if (mIsHeaderGroupClickable){
                    int groupPosition = getPackedPositionGroup(getExpandableListPosition(pos));
                    if (groupPosition != INVALID_POSITION && mActionDownHappened) {
                        if (isGroupExpanded(groupPosition)) {
                            collapseGroup(groupPosition);

                        } else {
                            expandGroup(groupPosition);
                        }
                    }
                }
                mActionDownHappened = false;
        }
            return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    private View getTouchTarget(View view, int x, int y) {
        if (!(view instanceof ViewGroup)) {
            return view;
        }

        ViewGroup parent = (ViewGroup)view;
        int childrenCount = parent.getChildCount();
        final boolean customOrder = isChildrenDrawingOrderEnabled();
        View target = null;
        for (int i = childrenCount - 1; i >= 0; i--) {
            final int childIndex = customOrder ? getChildDrawingOrder(childrenCount, i) : i;
            final View child = parent.getChildAt(childIndex);
            if (isTouchPointInView(child, x, y)) {
                target = child;
                break;
            }
        }
        if (target == null) {
            target = parent;
        }

        return target;
    }

    private boolean isTouchPointInView(View view, int x, int y) {
        if (view.isClickable() && y >= view.getTop() && y <= view.getBottom()
                && x >= view.getLeft() && x <= view.getRight()) {
            return true;
        }
        return false;
    }

    public void requestRefreshHeader() {
        refreshHeader();
        invalidate(new Rect(0, 0, mHeaderWidth, mHeaderHeight));
    }

    protected void refreshHeader() {
        if (mHeaderView == null) {
            return;
        }
//        Log.i("haha","进入refreshHeader()！！！！！！");
        int firstVisiblePos = getFirstVisiblePosition();
        int pos = firstVisiblePos + 1;
        int firstVisibleGroupPos = getPackedPositionGroup(getExpandableListPosition(firstVisiblePos));
        int group = getPackedPositionGroup(getExpandableListPosition(pos));
 /*       if (DEBUG) {
           // Log.d(TAG, "refreshHeader firstVisibleGroupPos=" + firstVisibleGroupPos);
        }
*/
        if (group == firstVisibleGroupPos + 1) {
            View view = getChildAt(1);
   /*         if (view == null) {
               // Log.w(TAG, "Warning : refreshHeader getChildAt(1)=null");
                return;
            }*/
            if (view.getTop() <= mHeaderHeight) {
                int delta = mHeaderHeight - view.getTop();
                mHeaderView.layout(0, -delta, mHeaderWidth, mHeaderHeight - delta);
            } else {
                //TODO : note it, when cause bug, remove it
                mHeaderView.layout(0, 0, mHeaderWidth, mHeaderHeight);
            }
           /* if (view.getTop() < mHeaderHeight) {
                Log.i("haha","view.getTop() < mHeaderHeight:"+view.getTop());
                int delta = mHeaderHeight - view.getTop();
                if(mCurrentSta ==PULL_TO_REFRESH){
                    mHeaderView.layout(0,-delta,0,0);
                }

                mHeaderView.layout(0, -delta, mHeaderWidth, mHeaderHeight - delta);
            } else {
                Log.i("haha","dy = "+this.dy);
                mHeaderView.layout(0,0,mHeaderWidth,mHeaderHeight+this.dy);
                //TODO : note it, when cause bug, remove it
            }

*/
        } else {
            mHeaderView.layout(0, 0, mHeaderWidth, mHeaderHeight);
        }

        if (mHeaderUpdateListener != null) {
            mHeaderUpdateListener.updatePinnedHeader(mHeaderView, firstVisibleGroupPos);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(view, scrollState);
        }

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        if (totalItemCount > 0) {
//            Log.i("haha","进入onScroll！！");
            refreshHeader();
        }
        if (mScrollListener != null) {
            mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

/*    public void setOnRemindGroupExpandStateListener(OnRemindGroupExpandStateListener onRemindGroupExpandStateListener){
        mOnRemindGroupExpandStateListener = onRemindGroupExpandStateListener;
    }

    public interface OnRemindGroupExpandStateListener{

        public void setCurrentGroupExpandStateListner(int groupPosition,boolean isExpand);
    }*/




}