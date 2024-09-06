package com.example.jakgelapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.*;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private TextView nfcContentTextView;
    private int currentInquery;
    TextView Saldo, hinText;
    Tag nfcTag;
    Button TopUpButton;
    EditText Ammount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TopUpButton = findViewById(R.id.topBtn);
        Ammount = findViewById(R.id.ammount);
        Saldo = findViewById(R.id.Saldo);
        hinText = findViewById(R.id.hintText);

        TopUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nfcTag == null || Ammount.getText().equals("")){
                    System.out.println(nfcTag);
                }else{
                    try{
                        write(Ammount.getText().toString(), nfcTag);
                    }catch (Error | IOException | FormatException e){
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        nfcContentTextView = findViewById(R.id.nfcStatus);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_SHORT).show();
        }

        // Handle intent if app is started via NFC tag scan
        if (getIntent() != null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        IntentFilter[] filters = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    }

    private void handleIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ) {
            processNdefMessage(intent);
            nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    private void processNdefMessage(Intent intent) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages != null && rawMessages.length > 0) {
            NdefMessage ndefMessage = (NdefMessage) rawMessages[0];
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord record : records) {
                if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                        Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        String text = readTextFromNdefRecord(record);
                        currentInquery = Integer.parseInt(text);
                        nfcContentTextView.setText("NFC Content: " + text);
                        Saldo.setText("Rp "+ text);
                        hinText.setVisibility(View.INVISIBLE);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Toast.makeText(this, "No NDEF messages found!", Toast.LENGTH_SHORT).show();
        }
    }

    private String readTextFromNdefRecord(NdefRecord record) throws UnsupportedEncodingException {
        byte[] payload = record.getPayload();

        // Get the Text Encoding
        String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";

        // Get the Language Code Length
        int languageCodeLength = payload[0] & 0x3F;

        // Get the Language Code (if needed)
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        // Get the Text
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    private void write(String text, Tag tag) throws  IOException, FormatException {
        currentInquery += Integer.parseInt(text);
        NdefRecord[] records = {createRecords(String.valueOf(currentInquery))};
        NdefMessage message = new NdefMessage(records);

        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
        Saldo.setText(String.valueOf("Rp "+currentInquery));
        Ammount.setText("");
    }

    private NdefRecord createRecords(String text) throws UnsupportedEncodingException{
        String language = "en";
        byte[] textByte = text.getBytes();
        byte[] langByte =  language.getBytes(StandardCharsets.UTF_8);
        int langLength = langByte.length;
        int textLength = textByte.length;
        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        System.arraycopy(langByte, 0, payload, 1, langLength);
        System.arraycopy(textByte, 0, payload, 1+ langLength, textLength);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    public void WriteText(View view) {

    }
}
