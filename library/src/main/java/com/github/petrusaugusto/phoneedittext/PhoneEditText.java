/*
Copyright 2017 Petrus Augusto (tecozc@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.github.petrusaugusto.phoneedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by Petrus A. (R@G3), ESPE... On 04/05/2017.
 */

public class PhoneEditText extends EditText implements TextWatcher {
    public enum NumDigits {
        DIGITS_BOTH, DIGITS_8, DIGITS_9;
    };

    public class Fields {
        final static public int PHONE = 0x01;
        final static public int LOCAL = 0x02;
        final static public int COUNTRY = 0x04;
    }

    final protected String TAG = getClass().getName();
    private boolean validateOnOut;
    private boolean withPhoneInfo;
    private boolean withLocalInfo;
    private boolean withCountryInfo;
    private int numDigitsCode;
    private boolean ignoreMaskOneTime = false;

    private int autoChangeTextLimit;
    protected String currentMask = "";
    protected String maskErrorText;

    public PhoneEditText(Context context) {
        super(context);
        this.withCountryInfo = false;
        this.withLocalInfo = true;
        this.numDigitsCode = 0;
        this.maskErrorText = getContext().getString(R.string.phoneedittext_invalid_mask_err_text);

        // Setting Listeners...
        this.initClassListeners();

        // Completing Initialization
        this.initElement();
    }

    public PhoneEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Sertting Attributes
        this.initAttributes(attrs);
        // Setting Listeners...
        this.initClassListeners();

        // Completing Initialization
        this.initElement();
    }

    public PhoneEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Sertting Attributes
        this.initAttributes(attrs);

        // Setting Listeners...
        this.initClassListeners();

        // Completing Initialization
        this.initElement();
    }

    /**
     * Generate Phone mask (8 or 9 digits) with or without DDD value
     * @param nineDigits -> Boolean indicating if generate with 9 digits or 8
     * @return -> String Mask
     */
    protected String generatePhoneMask(boolean nineDigits) {
        final StringBuilder sp = new StringBuilder();
        if ( withCountryInfo ) sp.append("+99").append((!withLocalInfo && !withPhoneInfo) ? "":" ");
        if ( withLocalInfo ) sp.append("(99)").append((!withPhoneInfo) ? "":" ");
        if ( withPhoneInfo && nineDigits ) sp.append("99999-9999");
        else if ( withPhoneInfo ) sp.append("9999-9999");
        return sp.toString();
    }

    protected void initAttributes(AttributeSet attrs) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PhoneEditText, 0, 0);
        try {
            // Validate on Out
            this.validateOnOut = a.getBoolean(R.styleable.PhoneEditText_validateOnOut, true);

            // Type of digits
            this.numDigitsCode = a.getInteger(R.styleable.PhoneEditText_digitsType, 0);

            // Error text
            this.maskErrorText = a.getString(R.styleable.PhoneEditText_maskErrorText);
            if ( this.maskErrorText == null || this.maskErrorText.length() <= 0 )
                this.maskErrorText = getContext().getString(R.string.phoneedittext_invalid_mask_err_text);

            // Setting Field Flags (default: LOCAL|PHONE)
            final int phoneFieldsFlags = a.getInt(R.styleable.PhoneEditText_fields, 0x03);
            withCountryInfo = ((Fields.COUNTRY & phoneFieldsFlags) == Fields.COUNTRY);
            withLocalInfo = ((Fields.LOCAL & phoneFieldsFlags) == Fields.LOCAL);
            withPhoneInfo = ((Fields.PHONE & phoneFieldsFlags) == Fields.PHONE);
        } finally {
            a.recycle();
        }
    }

    protected void initClassListeners() {
        this.addTextChangedListener(this);
    }

    protected void initElement() {
        // Setting attributes to Work with Phone Number
        this.currentMask = this.generatePhoneMask((this.numDigitsCode == 2));
        this.setInputType(InputType.TYPE_CLASS_PHONE);

        // Setting limits...
        this.setControlLimits();
    }

    protected void setControlLimits() {
        this.autoChangeTextLimit = (withPhoneInfo) ? 9 : 0;
        if ( this.withLocalInfo ) this.autoChangeTextLimit += 5;
        if ( this.withCountryInfo ) this.autoChangeTextLimit += 4;

        // Setting max text lenght limit
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(this.currentMask.length() + 1)});
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Checking is mask is disabled
        if ( this.currentMask == null || this.currentMask.length() <= 0 )
            return;

        final int newTextLen = (start + after) - count;
        if ( ((newTextLen > this.autoChangeTextLimit) && after > 0) && (this.currentMask.length() <= this.autoChangeTextLimit) ) {
            this.currentMask = this.generatePhoneMask((this.numDigitsCode != 1));
        } else if ( (newTextLen <= this.autoChangeTextLimit && count > 0) && (this.currentMask.length() != this.autoChangeTextLimit) ) {
            this.currentMask = this.generatePhoneMask((this.numDigitsCode == 2));
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Checking if text is empty
        if ( s == null || s.length() <= 0 )
            return;

        // Checking if mask is enable/setted or ignored
        if ( this.currentMask == null || this.currentMask.length() <= 0 || this.ignoreMaskOneTime ) {
            this.ignoreMaskOneTime = false;
            this.setSelection(getText().length()); // Moving cursor to end...
            return; // Nothing to do
        }

        // Applying mask (Generating masked text value)
        final String currentTextValue = this.getUnmaskedText().replaceAll("\\s+", "");
        String newTextValue = this.generateMaskedText(currentTextValue);

        // Setting new text value
        this.removeTextChangedListener(this); // Remove this listener to prevent freezy... :/
        this.setText(newTextValue);
        this.setSelection(newTextValue.length()); // Moving cursor to end
        this.addTextChangedListener(this); // Add this listener again. :)
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        /*
          This algorithm will validate current text value (only on when focus is losed),
          obviusly only if the option 'validateOnOut' is TRUE (default).
          If is a invalid input, all text will be cleaned and show erro message
         */
        if ( !focused && (this.validateOnOut && this.currentMask != null && this.currentMask.length() > 0) ) {
            // Validating mask (using length)
            int maskLen = this.currentMask.length();
            int textLen = getText().toString().length();
            if ( textLen > 0 && maskLen != textLen ) {
                // Cleaning text and setting error massage
                this.setText("");
                this.setError(maskErrorText);
            }
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }



    /**
     * Generate current mask inside text
     * @param unmaskedText -> Text that will be masked
     * @return -> Masked text
     */
    public String generateMaskedText(final String unmaskedText) {
        final String numberOnlyText = unmaskedText.replaceAll("[^0-9]", "");
        final int currentTextSize = numberOnlyText.length();

        // Applying mask
        String newTextValue = "";
        int mask_idx, text_idx;
        for ( mask_idx = 0, text_idx = 0; mask_idx < currentMask.length(); mask_idx++ ) {
            if ( text_idx >= currentTextSize ) {
                break; // Stopping loop
            }

            char ch = numberOnlyText.charAt(text_idx);
            char msk = this.currentMask.charAt(mask_idx);

            // Checando type of mask char (9 == number, a == Letters, # == Any or Mask Fix Char)
            if ( msk == '9' ) { // Mask to digit/number,
                if ( !Character.isDigit(ch) ) break; // Invalid char
                else { newTextValue += ch; text_idx++; }
            } else if ( msk == 'a' ) { // Mask to letter
                if ( !Character.isLetter(ch) ) break; // Invalid char
                else { newTextValue += ch; text_idx++; }
            } else if ( msk == '#' ) { // Mask to anywhere
                newTextValue += ch; text_idx++;
            } else { // Fixed mask value...
                newTextValue += msk;
            }
        }

        return newTextValue;
    }

    /**
     * Set phone fields (Country, Local and Phone)
     * @param phoneFieldsValue -> Field to view (can be combined with '|' bitwise)
     */
    public void setPhoneFields(final int phoneFieldsValue) {
        withCountryInfo = ((Fields.COUNTRY & phoneFieldsValue) == Fields.COUNTRY);
        withLocalInfo = ((Fields.LOCAL & phoneFieldsValue) == Fields.LOCAL);
        withPhoneInfo = ((Fields.PHONE & phoneFieldsValue) == Fields.PHONE);

        this.initElement();
        this.setText(this.getUnmaskedText().replaceAll("\\s+", ""));
    }

    /**
     * Get current text (UNMASKED)
     * @return -> Unmasked current Text value
     */
    public String getUnmaskedText() {
        return getText().toString().replaceAll("[.]", "").replaceAll("[+]", "")
                .replaceAll("[-]", "").replaceAll("[/]", "")
                .replaceAll("[(]", "").replaceAll("[)]", "");
    }

    /**
     * Get current text (MASKED) (same as getText)
     * @return -> Masked current Text value
     */
    public String getMaskedText() {
        return getText().toString();
    }

    /**
     * Set number of phone digits (8,9 or both with automatic change)
     * @param nd -> Enum with number digits value
     */
    public void setNumDigits(NumDigits nd) {
        this.numDigitsCode = nd.ordinal();
        this.currentMask = this.generatePhoneMask((numDigitsCode == 2));
        this.setControlLimits();
        this.setText(this.getUnmaskedText().replaceAll("\\s+", ""));
    }

    /**
     * Set a new text
     * @param text -> New text
     * @param ignoreMask -> Ignore mask (True or False)
     */
    public void setText(CharSequence text, boolean ignoreMask) {
        this.ignoreMaskOneTime = ignoreMask;
        this.setText(text);
    }

    /**
     * Set value to ValidateOnOut
     * @param v -> Boolean (True -> Validate, False -> Not Validate)
     */
    public void setValidateOnOut(boolean v) {
        this.validateOnOut = v;
    }
}
