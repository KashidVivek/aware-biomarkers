package com.aware.plugin.tone_analyser;

import android.content.Context;
import android.os.AsyncTask;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.tone_analyzer.v3.model.ToneOptions;

public class RetrieveToneTask extends AsyncTask<Void,Void,String> {
    private Context context;
    public String textToAnalyse;
    public String EMPTY_TONE = "NO_TONE_DETECTED";

    public RetrieveToneTask(Context context, String textToAnalyse){
        this.context = context;
        this.textToAnalyse = textToAnalyse;
    }
    @Override
    protected String doInBackground(Void... voids) {
        if (textToAnalyse != null) {
            IamAuthenticator authenticator = new IamAuthenticator("Ufr3FL9wHAotv_f7QItN7Q_EKvUHkpK-B88J5USZWloR");
            ToneAnalyzer toneAnalyzer = new ToneAnalyzer("2017-09-21", authenticator);
            toneAnalyzer.setServiceUrl("https://api.us-south.tone-analyzer.watson.cloud.ibm.com/instances/94baf436-8819-49d9-af67-cf6c3fc3345c");
            ToneOptions toneOptions = new ToneOptions.Builder()
                    .text(textToAnalyse)
                    .build();

            ToneAnalysis toneAnalysis = toneAnalyzer.tone(toneOptions).execute().getResult();
            if(toneAnalysis.getDocumentTone().getTones().toString().equals("[]")){
                return EMPTY_TONE;
            }
            String msg = toneAnalysis.getDocumentTone().getTones().get(0).getToneName();
            if (msg != null)
                return msg;
            else
                return EMPTY_TONE;
        }
        else{
            return EMPTY_TONE;
        }
    }

    @Override
    protected void onPostExecute(String msg) {
        super.onPostExecute(msg);
        // Toast.makeText(context, "Emotion:" + msg, Toast.LENGTH_SHORT).show();
    }
}

