package org.elsys.checksplitter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class FinalizeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalize);

        Intent i = getIntent();
        final Double amount = i.getExtras().getDouble("finalAmount");

        final EditText tip = (EditText) findViewById(R.id.tip_percentage);
        final String tipPercentage = tip.getText().toString();

        Double finalAmount = (amount + (amount * (Double.parseDouble(tipPercentage) / 100)));
        final TextView checkAmount = (TextView) findViewById(R.id.amount);
        checkAmount.setText(finalAmount.toString());

        tip.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    String tipPercent = tip.getText().toString();
                    if(tipPercent.equals("")) {
                        tip.setText("0");
                        tipPercent = tip.getText().toString();
                    }

                    Double finalAmount = (amount + (amount * (Double.parseDouble(tipPercent) / 100)));
                    checkAmount.setText(finalAmount.toString());

                    return true;
                }
                return false;
            }
        });

    }
}
