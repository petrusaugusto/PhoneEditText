package com.github.petrusaugusto.phoneedittext_demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.github.petrusaugusto.phoneedittext.PhoneEditText;

public class MainActivity extends AppCompatActivity {
    protected PhoneEditText phone_mask1, phone_mask2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.phone_mask1 = (PhoneEditText)findViewById(R.id.phone_mask1);
        this.phone_mask2 = (PhoneEditText)findViewById(R.id.phone_mask2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.phone_mask2.setNumDigits(PhoneEditText.NumDigits.DIGITS_8);
        this.phone_mask2.setPhoneFields(PhoneEditText.Fields.LOCAL | PhoneEditText.Fields.PHONE);
        this.phone_mask2.setValidateOnOut(false);
    }
}
