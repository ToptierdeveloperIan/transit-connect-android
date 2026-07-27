package com.example.imanicommunityapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class TriangleView extends View {

    private Paint paint;
    private Paint paint2;
    private float offset1 = 0f;
    private float offset2 = 0f;
    private float alpha1 = 255f; // for fade
    private float alpha2 = 0f;

    public TriangleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TriangleView);

        int color1 = a.getColor(R.styleable.TriangleView_triangleColor, 0xFF2196F3); // default blue
        int color2 = a.getColor(R.styleable.TriangleView_triangleColor2, 0xFF808080); // default grey

        // Paint for triangle 1
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color1);
        paint.setStyle(Paint.Style.FILL);

        // Paint for triangle 2
        paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint2.setColor(color2);
        paint2.setStyle(Paint.Style.FILL);

        a.recycle();
    }

    private Path getPath1() {
        Path path = new Path();
        path.moveTo(0, getHeight());          // bottom-left (right angle)
        path.lineTo(getWidth(), getHeight()); // bottom-right
        path.lineTo(0, 0);                    // top-left
        path.close();
        return path;
    }


    private Path getPath2() {
        Path path = new Path();
        path.moveTo(getWidth(), 0);           // top-right (right angle)
        path.lineTo(getWidth(), getHeight()); // bottom-right
        path.lineTo(0, 0);                    // top-left
        path.close();
        return path;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(-offset1,offset1);
        canvas.drawPath(getPath1(),paint);
        canvas.restore();


        canvas.save();
        canvas.translate(-offset2,offset2);
        canvas.drawPath(getPath2(),paint2);
        canvas.restore();

    }

    // 🔥 Start the swap animation
    public void startSwapAnimation() {
        float moveDistance = getWidth(); // how far to move (swap)
        ValueAnimator animator = ValueAnimator.ofFloat(0f, moveDistance);
        animator.setDuration(2000);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            Float value = (float) animation.getAnimatedValue();
            offset1 = value;
            offset2 = -value;

            invalidate(); // redraw
        });

        animator.start();
    }
}
