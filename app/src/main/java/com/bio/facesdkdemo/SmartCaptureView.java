package com.bio.facesdkdemo;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

public class SmartCaptureView extends View implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {

    enum VIEW_MODE {
        VIEW_WAIT,
        VIEW_CAPTURE,
        VIEW_SUCCESS,
    }

    private Context context;

    private Paint overlayPaint;

    private Paint eraserPaint;

    private Paint statusRoundPaint;

    private Size frameSize = new Size(720, 1280);

    private float animateValue;

    private ValueAnimator valueAnimator;

    public VIEW_MODE viewMode = VIEW_MODE.VIEW_WAIT;

    private ViewModeChanged viewModeInterface;

    private Bitmap capturedBitmap;

    private Bitmap roiBitmap;

    interface ViewModeChanged
    {
        public void capture_finished();
    }

    public void setViewModeInterface(ViewModeChanged viewMode) {
        viewModeInterface = viewMode;
    }

    public SmartCaptureView(Context context) {
        this(context, null);
        this.context = context;
        init();
    }

    public SmartCaptureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        overlayPaint = new Paint();

        eraserPaint = new Paint();
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        statusRoundPaint = new Paint();
    }

    public void setFrameSize(Size frameSize)
    {
        this.frameSize = frameSize;
    }

    public void setCapturedBitmap(Bitmap bitmap) {
        capturedBitmap = bitmap;

        RectF roiRect = SmartCaptureView.getROIRect1(frameSize);

        float ratioView = getWidth() / (float)getHeight();
        float ratioFrame = frameSize.getWidth() / (float)frameSize.getHeight();
        RectF roiViewRect = new RectF();

        if(ratioView < ratioFrame) {
            float dx = ((getHeight() * ratioFrame) - getWidth()) / 2;
            float dy = 0f;
            float ratio = getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        } else {
            float dx = 0;
            float dy = ((getWidth() / ratioFrame) - getHeight()) / 2;
            float ratio = getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        }

        Rect roiRectSrc = new Rect();
        Rect roiViewRectSrc = new Rect();
        roiRect.round(roiRectSrc);
        roiViewRect.round(roiViewRectSrc);
        roiBitmap = Bitmap.createBitmap(roiRectSrc.width(), roiRectSrc.height(), Bitmap.Config.ARGB_8888);

        final Canvas canvas1 = new Canvas(roiBitmap);
        canvas1.drawBitmap(capturedBitmap, roiRectSrc, new Rect(0, 0, roiRectSrc.width(), roiRectSrc.height()), null);
    }

    public void setViewMode(VIEW_MODE mode) {
        this.viewMode = mode;

        if(valueAnimator != null) {
            valueAnimator.pause();
        }

        if(viewMode == VIEW_MODE.VIEW_WAIT) {
            invalidate();
            return;
        } else if(viewMode == VIEW_MODE.VIEW_CAPTURE) {
            ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 1.07f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(1);
            animator.setDuration(200);

            valueAnimator = animator;
        } else if(viewMode == VIEW_MODE.VIEW_SUCCESS) {
            ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 0.8f);
            animator.addUpdateListener(this);
            animator.addListener(this);
            animator.setDuration(500);

            valueAnimator = animator;
        }

        valueAnimator.start();
    }

    @Override
    public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
        float value = (float)valueAnimator.getAnimatedValue();
        animateValue = value;
        invalidate();
    }

    @Override
    public void onAnimationStart(@NonNull Animator animator) {
    }

    @Override
    public void onAnimationEnd(@NonNull Animator animator) {
        if(viewMode == VIEW_MODE.VIEW_CAPTURE) {
            setViewMode(VIEW_MODE.VIEW_SUCCESS);
        } else if(viewMode == VIEW_MODE.VIEW_SUCCESS) {
            if(viewModeInterface != null) {
                viewModeInterface.capture_finished();
            }
        }
    }

    @Override
    public void onAnimationCancel(@NonNull Animator animator) {
    }

    @Override
    public void onAnimationRepeat(@NonNull Animator animator) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawOverlay(viewMode, canvas);

        RectF roiRect = SmartCaptureView.getROIRect1(frameSize);

        float ratioView = canvas.getWidth() / (float)canvas.getHeight();
        float ratioFrame = frameSize.getWidth() / (float)frameSize.getHeight();

        RectF roiViewRect = new RectF();

        if(ratioView < ratioFrame) {
            float dx = ((canvas.getHeight() * ratioFrame) - canvas.getWidth()) / 2;
            float dy = 0f;
            float ratio = canvas.getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        } else {
            float dx = 0;
            float dy = ((canvas.getWidth() / ratioFrame) - canvas.getHeight()) / 2;
            float ratio = canvas.getHeight() / (float)frameSize.getHeight();

            float x1 = roiRect.left * ratio - dx;
            float y1 = roiRect.top * ratio - dy;
            float x2 = roiRect.right * ratio -  dx;
            float y2 = roiRect.bottom * ratio - dy;

            roiViewRect = new RectF(x1, y1, x2, y2);
        }

        if(viewMode == VIEW_MODE.VIEW_WAIT) {
            canvas.drawRoundRect(roiViewRect, roiViewRect.width() / 2, roiViewRect.height() / 2, eraserPaint);
            drawStatusOval(false, roiViewRect, canvas);
        } else if(viewMode == VIEW_MODE.VIEW_CAPTURE) {
            RectF scaleRoiRect = roiViewRect;
            SmartCaptureView.scale(scaleRoiRect, animateValue);
            canvas.drawRoundRect(scaleRoiRect, scaleRoiRect.width() / 2, scaleRoiRect.height() / 2, eraserPaint);
            drawStatusOval(true, scaleRoiRect, canvas);

        } else if(viewMode == VIEW_MODE.VIEW_SUCCESS) {
            Paint paint1 = new Paint();
            paint1.setStyle(Paint.Style.STROKE);
            paint1.setColor(ContextCompat.getColor(context, R.color.md_theme_light_success));
            paint1.setStrokeWidth(32);
            paint1.setAntiAlias(true);

            Path path = new Path();

            float maxCornerRadius = Math.min(roiViewRect.width(), roiViewRect.height()) / 2f;
            float cornerRadius = (1.0f - animateValue) * maxCornerRadius;


            path.addRoundRect(roiViewRect, cornerRadius, cornerRadius, Path.Direction.CCW);

            canvas.save();
            canvas.clipPath(path);

            canvas.drawBitmap(roiBitmap, null, roiViewRect, null);
            canvas.drawRoundRect(roiViewRect, cornerRadius, cornerRadius, paint1);
        }
    }

    private void drawOverlay(VIEW_MODE currentView, Canvas canvas) {
        if(currentView == VIEW_MODE.VIEW_SUCCESS) {
            overlayPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_light_surface));
            overlayPaint.setAlpha(255);
        } else {
            overlayPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_dark_surface));
            overlayPaint.setAlpha(180);
        }

        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), overlayPaint);
    }

    private void drawStatusOval(Boolean isSuccess, RectF rect, Canvas canvas) {
        statusRoundPaint.setStyle(Paint.Style.STROKE);
        statusRoundPaint.setStrokeWidth(16);
        statusRoundPaint.setAntiAlias(true);
        if (isSuccess)
            statusRoundPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_light_success));
        else
            statusRoundPaint.setColor(ContextCompat.getColor(context, R.color.lineColor));

        canvas.drawOval(rect, statusRoundPaint);
    }
    private static void scale(RectF rect, float factor){
        float diffHorizontal = (rect.right-rect.left) * (factor-1f);
        float diffVertical = (rect.bottom-rect.top) * (factor-1f);

        rect.top -= diffVertical/2f;
        rect.bottom += diffVertical/2f;

        rect.left -= diffHorizontal/2f;
        rect.right += diffHorizontal/2f;
    }

    public static RectF getROIRect(Size frameSize) {
        int margin = frameSize.getWidth() / 6;
        int rectHeight = (frameSize.getWidth() - 2 * margin) * 6 / 5;

        RectF roiRect = new RectF(margin,  (frameSize.getHeight() - rectHeight) / 2,
                frameSize.getWidth() - margin, (frameSize.getHeight() - rectHeight) / 2 + rectHeight);
        return roiRect;
    }

    public static RectF getROIRect1(Size frameSize) {
        int margin = frameSize.getWidth() / 6;
        int rectWidth = (frameSize.getWidth() - 2 * margin);
        int rectHeight = rectWidth * 5 / 4;

        RectF roiRect = new RectF(margin,  (frameSize.getHeight() - rectHeight) / 2,
                frameSize.getWidth() - margin, (frameSize.getHeight() - rectHeight) / 2 + rectHeight);
        return roiRect;
    }
}
