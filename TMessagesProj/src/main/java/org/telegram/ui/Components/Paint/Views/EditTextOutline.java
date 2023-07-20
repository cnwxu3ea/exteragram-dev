package org.telegram.ui.Components.Paint.Views;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.EditTextBoldCursor;

import java.util.Arrays;

public class EditTextOutline extends EditTextBoldCursor {

    private Canvas mCanvas = new Canvas();
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap mCache;
    private boolean mUpdateCachedBitmap;
    private int mStrokeColor;
    private float mStrokeWidth;
    private int mFrameColor;
    private Path path = new Path();

    public boolean betterFraming;

    private RectF[] lines;
    private boolean isFrameDirty;

    public EditTextOutline(Context context) {
        super(context);

        mStrokeColor = Color.TRANSPARENT;
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        mUpdateCachedBitmap = true;
        isFrameDirty = true;
        setFrameRoundRadius(dp(16));
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    private float lastFrameRoundRadius;
    private void setFrameRoundRadius(float roundRadius) {
        if (Math.abs(lastFrameRoundRadius - roundRadius) > 0.1f) {
            paint.setPathEffect(new CornerPathEffect(lastFrameRoundRadius = roundRadius));
        }
    }

    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        mUpdateCachedBitmap = true;
        isFrameDirty = true;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            mUpdateCachedBitmap = true;
            isFrameDirty = true;
            if (mCache != null) {
                mCache.recycle();
            }
            mCache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } else {
            mCache = null;
        }
    }

    @Override
    public void setGravity(int gravity) {
        super.setGravity(gravity);
        mUpdateCachedBitmap = true;
        isFrameDirty = true;
        invalidate();
    }

    public void setStrokeColor(int strokeColor) {
        mStrokeColor = strokeColor;
        mUpdateCachedBitmap = true;
        invalidate();
    }

    public void setFrameColor(int frameColor) {
        if (mFrameColor == 0 && frameColor != 0) {
            setPadding(dp(7 + 12), dp(7), dp(7 + 12), dp(7));
            setCursorColor(0xffffffff);
//            setCursorColor(0xff000000);
        } else if (mFrameColor != 0 && frameColor == 0) {
            setPadding(dp(7), dp(7), dp(7), dp(7));
            setCursorColor(0xffffffff);
        }
        mFrameColor = frameColor;

        if (mFrameColor != 0) {
            float lightness = AndroidUtilities.computePerceivedBrightness(mFrameColor);
            if (lightness == 0) {
                lightness = Color.red(mFrameColor) / 255.0f;
            }
            if (lightness > 0.87) {
                setTextColor(0xff000000);
            } else {
                setTextColor(0xffffffff);
            }
            isFrameDirty = true;
        }
        mUpdateCachedBitmap = true;
        invalidate();
    }

    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
        mUpdateCachedBitmap = true;
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        if (mCache != null && mStrokeColor != Color.TRANSPARENT) {
            if (mUpdateCachedBitmap) {
                final int w = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
                final int h = getMeasuredHeight();
                final CharSequence text = getText();

                mCanvas.setBitmap(mCache);
                mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

                float strokeWidth = mStrokeWidth > 0 ? mStrokeWidth : (float) Math.ceil(getTextSize() / 11.5f);
                textPaint.setStrokeWidth(strokeWidth);
                textPaint.setColor(mStrokeColor);
                textPaint.setTextSize(getTextSize());
                textPaint.setTypeface(getTypeface());
                textPaint.setStyle(Paint.Style.FILL_AND_STROKE);

                Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
                if (getLayout() != null) {
                    alignment = getLayout().getAlignment();
                }
                StaticLayout sl = new StaticLayout(text, textPaint, w, alignment, 1, 0, true);

                mCanvas.save();
                float ty = (h - getPaddingTop() - getPaddingBottom() - sl.getHeight()) / 2.0f;
                mCanvas.translate(getPaddingLeft(), ty + getPaddingTop());
                sl.draw(mCanvas);
                mCanvas.restore();

                mUpdateCachedBitmap = false;
            }
            canvas.drawBitmap(mCache, 0, 0, textPaint);
        }
        if (mFrameColor != 0) {
            canvas.save();
            if (betterFraming) {
                canvas.translate(getPaddingLeft(), getPaddingTop());
            }
            paint.setColor(mFrameColor);
            Layout layout = getLayout();
            if (layout == null) {
                super.onDraw(canvas);
                return;
            }
            if (lines == null || lines.length != layout.getLineCount()) {
                lines = new RectF[layout.getLineCount()];
                isFrameDirty = true;
            }
            if (isFrameDirty) {
                isFrameDirty = false;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    if (lines[i] == null) {
                        lines[i] = new RectF();
                    }
                    lines[i].set(layout.getLineLeft(i), layout.getLineTop(i), layout.getLineRight(i), layout.getLineBottom(i));

                    if (lines[i].width() > dp(1)) {
                        if (betterFraming) {
                            lines[i].inset(-getTextSize() / 3f, 0);
                            lines[i].top += AndroidUtilities.dpf2(1.2f);
                            lines[i].bottom += AndroidUtilities.dpf2(1);
                            lines[i].left = Math.max(-getPaddingLeft(), lines[i].left);
                            lines[i].right = Math.min(getWidth() - getPaddingLeft(), lines[i].right);
                        } else {
                            lines[i].right += dp(32);
                            lines[i].bottom += dp(6);
                        }
                    } else {
                        lines[i].left = lines[i].right;
                    }

                    if (i > 0 && lines[i - 1].width() > 0) {
                        lines[i - 1].bottom = lines[i].top;
                    }
                }
            }
            path.rewind();
            float h = getHeight();
            for (int i = 0; i < lines.length; ++i) {
                if (lines[i].width() == 0) {
                    continue;
                }
                h = lines[i].bottom - lines[i].top;
            }
            float r = Math.min(h / 3f, dp(16)), mr = r * 1.5f;
            for (int i = 1; i < lines.length; ++i) {
                RectF above = lines[i - 1];
                RectF line = lines[i];
                if (above.width() < dp(1) || line.width() < dp(1)) {
                    continue;
                }
                boolean traceback = false;
                if (Math.abs(above.left - line.left) < mr) {
                    line.left = above.left = Math.min(line.left, above.left);
                    traceback = true;
                }
                if (Math.abs(above.right - line.right) < mr) {
                    line.right = above.right = Math.max(line.right, above.right);
                    traceback = true;
                }
                if (traceback) {
                    for (int j = i; j >= 1; --j) {
                        above = lines[i - 1];
                        line = lines[j];
                        if (Math.abs(above.left - line.left) < mr) {
                            line.left = above.left = Math.min(line.left, above.left);
                        }
                        if (Math.abs(above.right - line.right) < mr) {
                            line.right = above.right = Math.max(line.right, above.right);
                        }
                    }
                }
            }
            for (int i = 0; i < lines.length; ++i) {
                if (lines[i].width() == 0) {
                    continue;
                }
                path.addRect(lines[i], Path.Direction.CW);
            }
            setFrameRoundRadius(r);
            canvas.drawPath(path, paint);
            canvas.restore();
        }
        super.onDraw(canvas);
    }
}