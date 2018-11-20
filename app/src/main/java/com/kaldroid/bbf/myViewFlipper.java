package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: myViewFlipper
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: Override view flipper to be able to catch exceptions - early android bug
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

public class myViewFlipper extends ViewFlipper {
	public myViewFlipper(Context context) {
        super(context);
    }

    public myViewFlipper(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }
    
    @Override
    protected void onDetachedFromWindow() {
    	try
        {
           super.onDetachedFromWindow();
        }
        catch( IllegalArgumentException e )
        {
           //Log.w( TAG, "Android issue 6191 workaround." );
           // this bug is fixed in honeycomb... but nothing less!
        }
        finally
        {
           super.stopFlipping();
        }
    }
}