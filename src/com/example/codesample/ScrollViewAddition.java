package com.example.codesample;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
/**
 * Created by Chris on 9/23/2014.
 */



public class ScrollViewAddition extends ScrollView
{
    private ScrollViewListener scrollViewListener = null;

    public ScrollViewAddition(Context context)
    {
        super(context);
    }

    public ScrollViewAddition(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public ScrollViewAddition(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener)
    {
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int l_old, int t_old)
    {
        super.onScrollChanged(l, t, l_old, t_old);
        if (scrollViewListener != null)
        {
            scrollViewListener.onScrollChanged(this, l, t, l_old, t_old);
        }
    }
}