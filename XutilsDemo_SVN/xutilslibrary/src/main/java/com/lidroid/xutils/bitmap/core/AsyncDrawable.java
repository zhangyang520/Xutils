package com.lidroid.xutils.bitmap.core;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.lidroid.xutils.BitmapUtils;

import java.lang.ref.WeakReference;

/**
 * 异步的Drawable
 * Author: wyouflf
 * Date: 13-11-17
 * Time: 上午11:42
 */
public class AsyncDrawable<T extends View> extends Drawable {

    //位图下载任务的(软引用的)绑定
    private final WeakReference<BitmapUtils.BitmapLoadTask<T>> bitmapLoadTaskReference;

    private final Drawable baseDrawable;//背景

    public AsyncDrawable(Drawable drawable, BitmapUtils.BitmapLoadTask<T> bitmapWorkerTask) {
        if (bitmapWorkerTask == null) {
            throw new IllegalArgumentException("bitmapWorkerTask may not be null");
        }
        baseDrawable = drawable;
        //创建软引用的实例:将任务的实例放入其中！
        bitmapLoadTaskReference = new WeakReference<BitmapUtils.BitmapLoadTask<T>>(bitmapWorkerTask);
    }

    /**
     * 进行获取位图加载的任务
     * @return
     */
    public BitmapUtils.BitmapLoadTask<T> getBitmapWorkerTask() {
        //通过相关联的软引用返回相关联的实例
        return bitmapLoadTaskReference.get();
    }

    /**
     * 进行绘制:传入Canvas画饼:将背景Drawable画到画饼上!
     * @param canvas
     */
    @Override
    public void draw(Canvas canvas) {
        if (baseDrawable != null) {
            baseDrawable.draw(canvas);
        }
    }

    //设置透明度
    @Override
    public void setAlpha(int i) {
        if (baseDrawable != null) {
            baseDrawable.setAlpha(i);
        }
    }

    //设置颜色的过滤器
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (baseDrawable != null) {
            baseDrawable.setColorFilter(colorFilter);
        }
    }

    //进行获取不透明度
    @Override
    public int getOpacity() {
        return baseDrawable == null ? Byte.MAX_VALUE : baseDrawable.getOpacity();
    }

    /**
     * 进行设置范围()
     * @param left 左
     * @param top 顶部
     * @param right 右部
     * @param bottom 底部
     */
    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        if (baseDrawable != null) {
            baseDrawable.setBounds(left, top, right, bottom);
        }
    }


    @Override
    public void setBounds(Rect bounds) {
        if (baseDrawable != null) {
            baseDrawable.setBounds(bounds);
        }
    }

    //设置改变的配置
    @Override
    public void setChangingConfigurations(int configs) {
        if (baseDrawable != null) {
            baseDrawable.setChangingConfigurations(configs);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return baseDrawable == null ? 0 : baseDrawable.getChangingConfigurations();
    }

    /**
     * 进行设置抖动
     * @param dither
     */
    @Override
    public void setDither(boolean dither) {
        if (baseDrawable != null) {
            baseDrawable.setDither(dither);
        }
    }

    /**
     * 是否设置过滤
     * @param filter
     */
    @Override
    public void setFilterBitmap(boolean filter) {
        if (baseDrawable != null) {
            baseDrawable.setFilterBitmap(filter);
        }
    }

    /**
     * 进行重新绘制自己
     */
    @Override
    public void invalidateSelf() {
        if (baseDrawable != null) {
            baseDrawable.invalidateSelf();
        }
    }

    @Override
    public void scheduleSelf(Runnable what, long when) {
        if (baseDrawable != null) {
            baseDrawable.scheduleSelf(what, when);
        }
    }

    @Override
    public void unscheduleSelf(Runnable what) {
        if (baseDrawable != null) {
            baseDrawable.unscheduleSelf(what);
        }
    }

    @Override
    public void setColorFilter(int color, PorterDuff.Mode mode) {
        if (baseDrawable != null) {
            baseDrawable.setColorFilter(color, mode);
        }
    }

    @Override
    public void clearColorFilter() {
        if (baseDrawable != null) {
            baseDrawable.clearColorFilter();
        }
    }

    @Override
    public boolean isStateful() {
        return baseDrawable != null && baseDrawable.isStateful();
    }

    @Override
    public boolean setState(int[] stateSet) {
        return baseDrawable != null && baseDrawable.setState(stateSet);
    }

    @Override
    public int[] getState() {
        return baseDrawable == null ? null : baseDrawable.getState();
    }

    @Override
    public Drawable getCurrent() {
        return baseDrawable == null ? null : baseDrawable.getCurrent();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        return baseDrawable != null && baseDrawable.setVisible(visible, restart);
    }

    @Override
    public Region getTransparentRegion() {
        return baseDrawable == null ? null : baseDrawable.getTransparentRegion();
    }

    @Override
    public int getIntrinsicWidth() {
        return baseDrawable == null ? 0 : baseDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return baseDrawable == null ? 0 : baseDrawable.getIntrinsicHeight();
    }

    @Override
    public int getMinimumWidth() {
        return baseDrawable == null ? 0 : baseDrawable.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight() {
        return baseDrawable == null ? 0 : baseDrawable.getMinimumHeight();
    }

    @Override
    public boolean getPadding(Rect padding) {
        return baseDrawable != null && baseDrawable.getPadding(padding);
    }

    @Override
    public Drawable mutate() {
        return baseDrawable == null ? null : baseDrawable.mutate();
    }

    @Override
    public ConstantState getConstantState() {
        return baseDrawable == null ? null : baseDrawable.getConstantState();
    }
}
