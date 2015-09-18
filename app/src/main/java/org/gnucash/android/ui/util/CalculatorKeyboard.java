/**
 * Copyright 2013 Maarten Pennings extended by SimplicityApks
 *
 * Modified by:
 * Copyright 2015 Àlex Magaz Graça <rivaldi8@gmail.com>
 * Copyright 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * If you use this software in a product, an acknowledgment in the product
 * documentation would be appreciated but is not required.
 */

package org.gnucash.android.ui.util;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.support.annotation.LayoutRes;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.gnucash.android.app.GnuCashApplication;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;


/**
 * When an activity hosts a keyboardView, this class allows several EditText's to register for it.
 *
 * Known issues:
 *  - It's not possible to select text.
 *  - When in landscape, the EditText is covered by the keyboard.
 *  - No i18n.
 *
 * @author Maarten Pennings, extended by SimplicityApks
 * @date 2012 December 23
 */
public class CalculatorKeyboard {

    public static final int KEY_CODE_DECIMAL_SEPARATOR = 46;
    /** A link to the KeyboardView that is used to render this CalculatorKeyboard. */
    private KeyboardView mKeyboardView;
    /** A link to the activity that hosts the {@link #mKeyboardView}. */
    private Activity mHostActivity;
    private boolean hapticFeedback;

    private Currency mCurrency = Currency.getInstance(GnuCashApplication.getDefaultCurrencyCode());

    final String mDecimalSeparator = Character.toString(DecimalFormatSymbols.getInstance().getDecimalSeparator());

    private OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {
        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            View focusCurrent = mHostActivity.getWindow().getCurrentFocus();

            /*
            if (focusCurrent == null || focusCurrent.getClass() != EditText.class)
                return;
            */

            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();
            int end = edittext.getSelectionEnd();

            // FIXME: use replace() down
            // delete the selection, if chars are selected:
            if (end > start)
                editable.delete(start, end);

            switch (primaryCode) {
                case KEY_CODE_DECIMAL_SEPARATOR:
                    editable.insert(start, mDecimalSeparator);
                    break;
                case 42:
                case 43:
                case 45:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                    //editable.replace(start, end, Character.toString((char) primaryCode));
                    // XXX: could be android:keyOutputText attribute used instead of this?
                    editable.insert(start, Character.toString((char) primaryCode));
                    break;
                case -5:
                    int deleteStart = start > 0 ? start - 1: 0;
                    editable.delete(deleteStart, end);
                    break;
                case 1001:
                    evaluateEditTextExpression(edittext);
                    break;
                case 1002:
                    // FIXME: show the keyboard too
                    edittext.focusSearch(View.FOCUS_DOWN).requestFocus();
                    break;
            }
        }

        @Override
        public void onPress(int arg0) {
            // vibrate if haptic feedback is enabled:
            if (hapticFeedback && arg0 != 0)
                mKeyboardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        @Override public void onRelease(int primaryCode) { }
        @Override public void onText(CharSequence text) { }
        @Override public void swipeLeft() { }
        @Override public void swipeRight() { }
        @Override public void swipeDown() { }
        @Override public void swipeUp() { }
    };

    /**
     * Create a custom keyboard, that uses the KeyboardView (with resource id <var>viewid</var>) of the <var>host</var> activity,
     * and load the keyboard layout from xml file <var>layoutid</var> (see {@link Keyboard} for description).
     * Note that the <var>host</var> activity must have a <var>KeyboardView</var> in its layout (typically aligned with the bottom of the activity).
     * Note that the keyboard layout xml file may include key codes for navigation; see the constants in this class for their values.
     * Note that to enable EditText's to use this custom keyboard, call the {@link #registerEditText(int)}.
     *
     * @param host The hosting activity.
     * @param keyboardViewId The id of the KeyboardView.
     * @param xmlLayoutResId The id of the xml file containing the keyboard layout.
     */
    public CalculatorKeyboard(Activity host, int keyboardViewId, @LayoutRes int xmlLayoutResId) {
        mHostActivity = host;
        mKeyboardView = (KeyboardView) mHostActivity.findViewById(keyboardViewId);
        Keyboard keyboard = new Keyboard(mHostActivity, xmlLayoutResId);
        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.codes[0] == KEY_CODE_DECIMAL_SEPARATOR){
                key.label = mDecimalSeparator;
                break;
            }
        }
        mKeyboardView.setKeyboard(keyboard);
        mKeyboardView.setPreviewEnabled(false); // NOTE Do not show the preview balloons
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        // Hide the standard keyboard initially
        mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /** Returns whether the CalculatorKeyboard is visible. */
    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    /** Make the CalculatorKeyboard visible, and hide the system keyboard for view v. */
    public void showCustomKeyboard(View v) {
        if (v != null)
            ((InputMethodManager) mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);

        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
    }

    /** Make the CalculatorKeyboard invisible. */
    public void hideCustomKeyboard() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    /**
     * Register <var>EditText<var> with resource id <var>resid</var> (on the hosting activity) for using this custom keyboard.
     *
     * @param resid The resource id of the EditText that registers to the custom keyboard.
     */
    public void registerEditText(int resid) {
        // Find the EditText 'resid'
        final EditText edittext = (EditText) mHostActivity.findViewById(resid);
        // Make the custom keyboard appear
        edittext.setOnFocusChangeListener(new OnFocusChangeListener() {
            // NOTE By setting the on focus listener, we can show the custom keyboard when the edit box gets focus, but also hide it when the edit box loses focus
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    showCustomKeyboard(v);
                else {
                    hideCustomKeyboard();
                    evaluateEditTextExpression((EditText) v);
                }
            }
        });

        edittext.setOnClickListener(new OnClickListener() {
            // NOTE By setting the on click listener we can show the custom keyboard again,
            // by tapping on an edit box that already had focus (but that had the keyboard hidden).
            @Override
            public void onClick(View v) {
                showCustomKeyboard(v);
            }
        });

        // Disable spell check (hex strings look like words to Android)
        edittext.setInputType(edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // FIXME: for some reason, this prevents the text selection from working
        edittext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v != null)
                    ((InputMethodManager) mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);

                return false;
            }
        });
    }

    /**
     * Sets the currency to be used for this calculation
     * @param currency Currency of the amount being computed
     */
    public void setCurrency(Currency currency){
        this.mCurrency = currency;
    }

    /**
     * Enables or disables the Haptic feedback on keyboard touches
     * @param goEnabled true if you want haptic feedback, falso otherwise
     */
    public void enableHapticFeedback(boolean goEnabled) {
        mKeyboardView.setHapticFeedbackEnabled(goEnabled);
        hapticFeedback = goEnabled;
    }

    public boolean onBackPressed() {
        if (isCustomKeyboardVisible()) {
            hideCustomKeyboard();
            return true;
        } else
            return false;
    }

    public void evaluateEditTextExpression(EditText editText) {
        String amountText = editText.getText().toString();
        amountText = amountText.replaceAll(",", ".");
        if (amountText.trim().isEmpty())
            return;

        ExpressionBuilder expressionBuilder = new ExpressionBuilder(amountText);
        Expression expression;

        try {
            expression = expressionBuilder.build();
        } catch (RuntimeException e) {
            // FIXME: i18n
            editText.setError("Invalid expression!");
            String msg = "Invalid expression: " + amountText;
            Log.e(this.getClass().getSimpleName(), msg);
            Crashlytics.log(msg);
            return;
        }

        if (expression != null && expression.validate().isValid()) {
            BigDecimal result = new BigDecimal(expression.evaluate());
            result = result.setScale(mCurrency.getDefaultFractionDigits(), BigDecimal.ROUND_HALF_EVEN);

            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(mCurrency.getDefaultFractionDigits());
            formatter.setGroupingUsed(false);
            String resultString = formatter.format(result.doubleValue());

            editText.setText(resultString);
            editText.setSelection(resultString.length());
        } else {
            // FIXME: i18n
            editText.setError("Invalid expression!");
            // TODO: log error
        }
    }
}
