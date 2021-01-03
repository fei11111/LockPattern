package com.fei.lockpattern;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * @ClassName: LockPatternView
 * @Description: 图案解锁
 * @Author: Fei
 * @CreateDate: 2020-12-31 21:04
 * @UpdateUser: 更新者
 * @UpdateDate: 2020-12-31 21:04
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class LockPatternView extends View {

    private final String TAG = "LockPatternView";
    //圆画笔
    private Paint mNormalPaint;
    private Paint mSelectPaint;
    private Paint mErrorPaint;
    private Paint mSuccessPaint;
    private Paint mNormalArrowPaint;
    private Paint mSelectArrowPaint;
    private Paint mErrorArrowPaint;
    private Paint mSuccessArrowPaint;

    private int mNormalColor = Color.GRAY;//默认颜色
    private int mSelectColor = Color.BLUE;//选中颜色
    private int mErrorColor = Color.RED;//错误颜色
    private int mSuccessColor = Color.GREEN;//成功颜色

    //圆环宽度
    private float mStrokeWidth = 2f;

    //9个点
    private Point[][] mPoints = new Point[3][3];
    //选中的点
    private List<Point> mPointList = new ArrayList<>();

    //外圆半径
    private float mOuterRadius;
    //内圆半径
    private float mInnerRadius;
    //移动坐标
    private float mX;
    private float mY;
    //密码
    private String mPassword = "5236";

    public LockPatternView(Context context) {
        this(context, null);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //获取颜色
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LockPatternView);
        mNormalColor = typedArray.getColor(R.styleable.LockPatternView_normalColor, mNormalColor);
        mSelectColor = typedArray.getColor(R.styleable.LockPatternView_selectColor, mSelectColor);
        mErrorColor = typedArray.getColor(R.styleable.LockPatternView_errorColor, mErrorColor);
        mSuccessColor = typedArray.getColor(R.styleable.LockPatternView_successColor, mSuccessColor);
        mStrokeWidth = typedArray.getDimension(R.styleable.LockPatternView_strokeWidth, dp2px(mStrokeWidth));
        typedArray.recycle();

        mNormalPaint = getPaint(mNormalColor);
        mSelectPaint = getPaint(mSelectColor);
        mErrorPaint = getPaint(mErrorColor);
        mSuccessPaint = getPaint(mSuccessColor);

        mNormalArrowPaint = getArrowPaint(mNormalColor);
        mSelectArrowPaint = getArrowPaint(mSelectColor);
        mErrorArrowPaint = getArrowPaint(mErrorColor);
        mSuccessArrowPaint = getArrowPaint(mSuccessColor);
    }

    /***
     * 箭头画笔
     */
    private Paint getArrowPaint(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        return paint;
    }

    /**
     * 绘制成正方形
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > height) {
            setMeasuredDimension(height, height);
        } else {
            setMeasuredDimension(width, width);
        }

        Log.e(TAG, "onMeasure");
        //初始化所有圆的中心点位置
        int squareWidth = getMeasuredWidth() / 3;
        int first = squareWidth / 2;
        int second = squareWidth + first;
        int third = second + squareWidth;

        mPoints[0][0] = new Point(first, first, 1);
        mPoints[0][1] = new Point(second, first, 2);
        mPoints[0][2] = new Point(third, first, 3);
        mPoints[1][0] = new Point(first, second, 4);
        mPoints[1][1] = new Point(second, second, 5);
        mPoints[1][2] = new Point(third, second, 6);
        mPoints[2][0] = new Point(first, third, 7);
        mPoints[2][1] = new Point(second, third, 8);
        mPoints[2][2] = new Point(third, third, 9);

        //半径
        mOuterRadius = squareWidth / 4f - mStrokeWidth / 2f;
        mInnerRadius = squareWidth / 12f - mStrokeWidth / 2f;
    }

    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    /**
     * 初始化画笔
     */
    private Paint getPaint(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mStrokeWidth);
        paint.setColor(color);
        return paint;
    }

    //画圆，包含内圆，外圆
    //按下，坐标离圆距离小于半径，画选中圆
    //移动，坐标离圆距离小于半径，画选中圆
    //移动，画圆与圆之间的线
    //在线上画三角形
    //手指抬起判断密码是否正确，回调
    //错误则画错误圆，隔1秒后清楚状态


    @Override
    protected void onDraw(Canvas canvas) {
        //画圆
        drawCircle(canvas);
        //移动，画圆与圆之间的线
        drawLine(canvas);
        //判断是否需要回调
        judgeCallBack();
    }

    /**
     * 判断是否需要回调
     */
    private void judgeCallBack() {
        if (mPointList.size() > 0) {
            Point point = mPointList.get(0);
            if (point.getStatus() == PointStatus.ERROR) {
                //如果错误，2秒后恢复
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reset();
                        if (mCallBack != null) {
                            mCallBack.onFail();
                        }
                    }
                }, 1000);
            } else if (point.getStatus() == PointStatus.SUCCESS) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reset();
                        if (mCallBack != null) {
                            mCallBack.onSuccess();
                        }
                    }
                }, 1000);
            }
        }
    }

    /**
     * 移动，画圆与圆之间的线
     *
     * @param canvas
     */
    private void drawLine(Canvas canvas) {
        if (mPointList.size() == 0) return;
        if (mPointList.size() > 1) {
            for (int i = 1; i < mPointList.size(); i++) {
                Point lastPoint = mPointList.get(i - 1);
                Point point = mPointList.get(i);
                Paint paint = getPaint(point);
                Paint arrowPaint = getArrowPaint(point);
                drawLineAndArrow(lastPoint.centerX, lastPoint.centerY, point.centerX, point.centerY, canvas, paint, arrowPaint);
            }
        }
        //画最后一个圆到手指的线
        Point point = mPointList.get(mPointList.size() - 1);
        if (point.getStatus() == PointStatus.ERROR || point.getStatus() == PointStatus.SUCCESS) {
            return;
        }
        drawLineAndArrow(point.centerX, point.centerY, mX, mY, canvas, mSelectPaint, mSelectArrowPaint);
    }

    /**
     * 画线和箭头
     */
    private void drawLineAndArrow(float startX, float startY, float endX, float endY, Canvas canvas, Paint paint, Paint arrowPaint) {
        //利用三角形，获取斜边长度
        double distance = getPositionDistance(startX, startY, endX, endY);
        //获取相差的x距离和y距离
        double radio = mInnerRadius / distance;
        float dx = (float) (radio * (endX - startX));
        float dy = (float) (radio * (endY - startY));
        canvas.drawLine(startX + dx, startY + dy, endX - dx, endY - dy, paint);
        //画箭头
        drawTriangle(startX + dx, startY + dy, endX - dx, endY - dy, mInnerRadius, mInnerRadius / 2, canvas, arrowPaint);
    }

    /***
     * 三角形
     * @param fromX
     * @param fromY
     * @param toX
     * @param toY
     * @param height
     * @param bottom
     * @param canvas
     * @param paint
     */
    private void drawTriangle(float fromX, float fromY, float toX, float toY,
                              float height, float bottom, Canvas canvas, Paint paint) {
        // height和bottom分别为三角形的高与底的一半,调节三角形大小
        float distance = (float) getPositionDistance(fromX, fromY, toX, toY);// 获取线段距离
        float dx = toX - fromX;// 有正负，不要取绝对值
        float dy = toY - fromY;// 有正负，不要取绝对值
        float vX = toX - (height / distance * dx);
        float vY = toY - (height / distance * dy);
        //终点的箭头
        Path path = new Path();
        path.moveTo(toX, toY);// 此点为三边形的起点
        path.lineTo(vX + (bottom / distance * dy), vY
                - (bottom / distance * dx));
        path.lineTo(vX - (bottom / distance * dy), vY
                + (bottom / distance * dx));
        path.close(); // 使这些点构成封闭的三边形
        canvas.drawPath(path, paint);
    }

    /**
     * 画正常圆
     *
     * @param canvas
     */
    private void drawCircle(Canvas canvas) {
        for (Point[] mPoint : mPoints) {
            for (Point point : mPoint) {
                drawCircle(canvas, point);
            }
        }
    }


    /**
     * 画圆
     *
     * @param canvas
     * @param point
     */
    private void drawCircle(Canvas canvas, Point point) {
        Paint paint = getPaint(point);
        canvas.drawCircle(point.centerX, point.centerY, mOuterRadius, paint);
        canvas.drawCircle(point.centerX, point.centerY, mInnerRadius, paint);
    }

    private Paint getPaint(Point point) {
        Paint paint = null;
        if (point.getStatus() == PointStatus.NORMAL) {
            paint = mNormalPaint;
        } else if (point.getStatus() == PointStatus.SELECT) {
            paint = mSelectPaint;
        } else if (point.getStatus() == PointStatus.ERROR) {
            paint = mErrorPaint;
        } else {
            paint = mSuccessPaint;
        }
        return paint;
    }

    private Paint getArrowPaint(Point point) {
        Paint paint = null;
        if (point.getStatus() == PointStatus.NORMAL) {
            paint = mNormalArrowPaint;
        } else if (point.getStatus() == PointStatus.SELECT) {
            paint = mSelectArrowPaint;
        } else if (point.getStatus() == PointStatus.ERROR) {
            paint = mErrorArrowPaint;
        } else {
            paint = mSuccessArrowPaint;
        }
        return paint;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //如果错误就不画了，等错误消失后才能移动
        mX = event.getX();
        mY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                //按下，坐标离圆距离小于半径，画选中圆
                //移动，坐标离圆距离小于半径，画选中圆
                addSelectPoint();
                if (mPointList.size() == 9) {
                    //如果连了9个点就不用继续连了，直接判断
                    judgePassword();
                }
                break;
            case MotionEvent.ACTION_UP:
                judgePassword();
                break;
        }
        invalidate();
        return true;
    }

    /**
     * 更新选中圆
     */
    private void addSelectPoint() {
        Point point = getPoint(mX, mY);
        if (point != null && !mPointList.contains(point)) {
            point.setStatus(PointStatus.SELECT);
            mPointList.add(point);
        }
    }

    /**
     * 判断手指所触摸的点
     *
     * @param x
     * @param y
     * @return
     */
    private Point getPoint(float x, float y) {
        for (Point[] mPoint : mPoints) {
            for (Point point : mPoint) {
                //离圆中心点位置小于半径，说明在园内
                double distance = getPositionDistance(x, y, point.centerX, point.centerY);
                if (distance < mOuterRadius) {
                    return point;
                }
            }
        }
        return null;
    }

    /**
     * 获取两点之间的距离
     */
    private double getPositionDistance(float startX, float startY, float endX, float endY) {
        float dx = startX - endX;
        float dy = startY - endY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 判断密码是否正确
     */
    private void judgePassword() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Point point : mPointList) {
            stringBuilder.append("" + point.index);
        }
        Log.e(TAG, "password = " + mPassword);
        Log.e(TAG, "str = " + stringBuilder.toString());
        //这里可以加密算法判断
        if (!mPassword.equals(stringBuilder.toString())) {
            //不相等
            setPointsError();
            Toast.makeText(getContext(), "密码错误", Toast.LENGTH_SHORT).show();
        } else {
            setPointsSuccess();
            Toast.makeText(getContext(), "解锁成功", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 密码错误
     */
    private void setPointsError() {
        for (Point point : mPointList) {
            point.setStatus(PointStatus.ERROR);
        }
    }

    /**
     * 密码正确
     */
    private void setPointsSuccess() {
        for (Point point : mPointList) {
            point.setStatus(PointStatus.SUCCESS);
        }
    }

    /**
     * 恢复初始状态
     */
    private void reset() {
        mPointList.clear();
        for (Point[] mPoint : mPoints) {
            for (Point point : mPoint) {
                point.setStatus(PointStatus.NORMAL);
            }
        }
        invalidate();
    }


    //记录圆坐标
    private class Point {
        private int centerX;
        private int centerY;
        private int index;
        private PointStatus status = PointStatus.NORMAL;//状态

        public Point(int centerX, int centerY, int index) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.index = index;
        }

        public PointStatus getStatus() {
            return status;
        }

        public void setStatus(PointStatus status) {
            this.status = status;
        }
    }

    enum PointStatus {
        NORMAL,
        SELECT,
        ERROR,
        SUCCESS
    }

    /**
     * 设置密码
     *
     * @param password
     */
    public void setPassword(String password) {
        this.mPassword = password;
    }

    /**
     * 回调
     *
     * @param callBack
     */
    public void setCallBack(CallBack callBack) {
        this.mCallBack = callBack;
    }

    private CallBack mCallBack;

    public interface CallBack {
        void onSuccess();

        void onFail();
    }
}
