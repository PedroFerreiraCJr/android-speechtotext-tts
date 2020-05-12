package br.com.dotofcodex.speech_recognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1000;

    @BindView(R.id.bt_listen)
    protected Button listen;

    @BindView(R.id.bt_stop_listen)
    protected Button stop;

    @BindView(R.id.tv_status)
    protected TextView status;

    @BindView(R.id.bt_speech)
    protected TextView toSpeech;

    private SpeechRecognizer recognizer;
    private List<String> values;
    private Intent intent;
    private TextToSpeech tts;
    private ExecutorService executor;
    private Map<String, List<String>> aq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (values == null) {
            values = new ArrayList<>();
        }

        if (intent == null) {
            intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Prompt");
        }

        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
            recognizer.setRecognitionListener(new SimpleSpeechRecognizerListener(this));
        }

        if (tts == null) {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status == TextToSpeech.SUCCESS) {
                        int result = tts.setLanguage(Locale.getDefault());
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e(TAG, "This Language is not supported");
                            return;
                        }

                        tts.setSpeechRate(0.8f);
                    }
                }
            });
        }

        if (executor == null) {
            executor = Executors.newFixedThreadPool(2);
        }

        if (aq == null) {
            aq = new HashMap<>();
            aq.put("O preço do prato depende do sabor escolhido", Arrays.asList("qual", "preço", "massa"));
            aq.put("Eu moro no conjunto acaracuzinho", Arrays.asList("qual", "seu", "endereço"));
            aq.put("Porque você quer saber o nome da minha mãe?", Arrays.asList("qual", "nome", "sua", "mãe"));
            aq.put("A querida Elivânia Gomes", Arrays.asList("quem", "dona", "so", "macarrão"));
        }

        listen.setOnClickListener((View v) -> {
            status.setText("Listening...");
            recognizer.startListening(intent);
        });

        stop.setOnClickListener((View v) -> {
            status.setText("Ready to listen");
            recognizer.stopListening();

            if (!values.isEmpty()) {
                Log.i(TAG, Arrays.toString(values.toArray()));
                values.clear();
            }
        });

        toSpeech.setOnClickListener((View v) -> {
            status.setText("Speeching...");

            if (!values.isEmpty()) {
                // assumindo que eu esteja na tela de seleção do tipo de macarrão;
                for (String value : values) {
                    value = value.trim().toLowerCase();

                    String answer = null;
                    boolean match;
                    Iterator<Map.Entry<String, List<String>>> iterator = aq.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, List<String>> keywords = iterator.next();

                        match = true;
                        for (String keyword : keywords.getValue()) {
                            if (!value.contains(keyword)) {
                                match = false;
                                break;
                            }
                        }

                        if (match) {
                            answer = keywords.getKey();
                            break;
                        }
                    }

                    if (answer != null) {
                        tts.speak(answer, TextToSpeech.QUEUE_ADD, null);
                        break;
                    }
                    else {
                        tts.speak("Não foi possivel encontrar uma boa resposta", TextToSpeech.QUEUE_ADD, null);
                        break;
                    }
                }
                /*
                for (String value : values) {
                    tts.speak(value, TextToSpeech.QUEUE_ADD, null);
                }
                */
            }

            executor.submit(() -> {
                while (tts.isSpeaking()) {
                    try { Thread.sleep(500); } catch (InterruptedException e) {  }
                }
                runOnUiThread(() -> {
                    status.setText("Ready to listen");
                });
            });
        });

        if (!hasRecordAudioPermission()) {
            requestAudioPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(MainActivity.this::finish, 4000);
        }
    }

    private boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.RECORD_AUDIO }, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private static class SimpleSpeechRecognizerListener implements RecognitionListener {

        private MainActivity activity;

        SimpleSpeechRecognizerListener(MainActivity activity) {
            super();
            this.activity = activity;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.i(TAG, "onReadForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.i(TAG, "onBeginningOfSpeech");
            activity.status.setText("I am listening...");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Log.i(TAG, "onRmsChanged");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.i(TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.i(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            Log.i(TAG, "onError");
            Log.i(TAG, String.valueOf(error));

            String message = null;
            switch (error) {
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: {
                    message = "Network timeout";
                    break;
                }
                case SpeechRecognizer.ERROR_NETWORK: {
                    message = "Network error";
                    break;
                }
                case SpeechRecognizer.ERROR_AUDIO: {
                    message = "Audio error";
                    break;
                }
                case SpeechRecognizer.ERROR_SERVER: {
                    message = "Server error";
                    break;
                }
                case SpeechRecognizer.ERROR_CLIENT: {
                    message = "Client error";
                    break;
                }
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: {
                    message = "Speech timeout";
                    break;
                }
                case SpeechRecognizer.ERROR_NO_MATCH: {
                    message = "No match";
                    break;
                }
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: {
                    message = "Recognizer busy";
                    break;
                }
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: {
                    message = "Insufficient permission";
                    break;
                }
                default: {
                    message = "Error";
                }
            }
            Log.i(TAG, message);
            activity.status.setText(message);
        }

        @Override
        public void onResults(Bundle results) {
            Log.i(TAG, "onResults");
            List<String> voiceResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (voiceResults == null) {
                Log.i(TAG, "No voice results");
            } else {
                Log.i(TAG, "Printing matches");

                for (String match : voiceResults) {
                    activity.values.add(match);
                    Log.i(TAG, match);
                }
            }
            activity.status.setText("Ready to listen");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.i(TAG, "onPartialResults");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.i(TAG, "onEvent");
        }
    }
}
