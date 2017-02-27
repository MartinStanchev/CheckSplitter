package org.elsys.checksplitter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChooseProductsActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private double finalAmount = 0.0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_products);

        Intent i = getIntent();
        String resultText = i.getExtras().getString("result");
        makeClickableNumbers(resultText);

        Button done = (Button) findViewById(R.id.done_choosing);
        if (done != null) {
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finalizeCheck();
                }
            });
        }

    }

    private void finalizeCheck() {
        Intent i = new Intent(this, FinalizeActivity.class);
        i.putExtra("finalAmount", finalAmount);
        startActivity(i);
    }

    private void makeClickableNumbers(String text) {
        String regexPattern = "\\d.[^ ]\\d+";
        final Matcher matcher = Pattern.compile(regexPattern).matcher(text);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(text);
        while(matcher.find()) {
            ClickableSpan clickable = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    TextView tv = (TextView) view;
                    Spanned s = (Spanned) tv.getText();
                    int start = s.getSpanStart(this);
                    int end = s.getSpanEnd(this);
                    Toast.makeText(getApplicationContext(), s.subSequence(start, end), Toast.LENGTH_SHORT).show();
                    String val = (s.subSequence(start, end)).toString().replaceAll("[\\s : , \\t]", "");
                    finalAmount += Double.parseDouble(val);
                    Log.d(TAG, String.valueOf(finalAmount));
                }
            };
            strBuilder.setSpan(clickable, matcher.start(), matcher.end(), 0);
        }
        TextView textView = (TextView) findViewById(R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(strBuilder);
    }

}
