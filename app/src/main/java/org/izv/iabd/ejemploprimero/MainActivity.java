package org.izv.iabd.ejemploprimero;

import androidx.appcompat.app.AppCompatActivity;

import org.json.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

//tts
import android.speech.tts.TextToSpeech;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//sst
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.speech.RecognizerIntent;

//dialogflow
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.common.collect.Lists;

import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_PHONE_CALL = 1;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private Button btConfirmar, btEscuchar, btLlamar, btContacto, btInternet, btCalendario;
    private EditText etFrase, etNumero, etContacto, etInternet;
    private TextView tvResultado,tvNumero;

    //tts
    private boolean ttsReady = false;
    private TextToSpeech tts;

    //stt
    private ActivityResultLauncher<Intent> sttLauncher;
    private Intent sttIntent;

    //dialogflow
    private final String uuid = UUID.randomUUID().toString();
    private boolean dialogFlowReady = false;
    private SessionsClient sessionClient;
    private SessionName sessionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //-------
        initialize();
    }

    //llamada al proceso que trata de escuchar el mensaje hablado
    private Intent getSttIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("spa", "ES"));
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Por favor, diga su mensaje:");
        return intent;
    }

    //definición del proceso que se ejecuta una vez que se ha escuchado el mensaje
    private ActivityResultLauncher<Intent> getSttLauncher() {
        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    String text = "";
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        List<String> r = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        text = r.get(0);
                    } else if(result.getResultCode() == Activity.RESULT_CANCELED) {
                        text = "message: error receiving text";
                    }
                    showResult(text);
                }
        );
    }

    private void initialize() {
        btConfirmar = findViewById(R.id.btConfirmar);
        btEscuchar = findViewById(R.id.btEscuchar);
        etFrase = findViewById(R.id.etFrase);
        tvResultado = findViewById(R.id.tvResultado);

        etNumero = findViewById(R.id.etNumero);
        btLlamar = findViewById(R.id.btLlamar);

        etContacto = findViewById(R.id.etContacto);
        btContacto = findViewById(R.id.btContacto);
        tvNumero = findViewById(R.id.tvNumero);

        etInternet = findViewById(R.id.etInternet);
        btInternet = findViewById(R.id.btInternet);

        btCalendario = findViewById(R.id.btCalendario);

        //tts
        tts = new TextToSpeech(this, this);

        //stt
        sttLauncher = getSttLauncher();
        sttIntent = getSttIntent();

        //dialogflow
        setupBot();

        //asignar un evento al botón (old)
        /*btConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickBtConfirmar();
            }
        });*/

        //lambda expression -> sustituye la creación e implementación de una interface anónima que tiene un sólo método
        btConfirmar.setOnClickListener(view -> {
            onClickBtConfirmar();
        });

        btEscuchar.setOnClickListener(view -> {
            onClickBtEscuchar();
        });

        btLlamar.setOnClickListener(view -> {
            onClickBtLlamar();
        });

        btContacto.setOnClickListener(view -> {
            onClickBtContacto();
        });

        btInternet.setOnClickListener(view -> {
            System.out.print("CLICK BTINTERNET");
            onClickBtInternet();
        });

        btCalendario.setOnClickListener(view -> {
            onClickBtCalendario();
        });
    }

    private void onClickBtConfirmar() {
        String frase = etFrase.getText().toString().trim();
        etFrase.setText("");
        //showMessage("message sent: " + frase);
        if(ttsReady && frase.length()> 0) {
            //tts.speak(frase, TextToSpeech.QUEUE_ADD, null, null);
        } else {
            //showMessage("message: error reproducing the text");
        }
        if(dialogFlowReady && frase.length()> 0) {
            System.out.println("si entro");
          sendMessageToBot(frase);
        }
    }

    private void onClickBtEscuchar() {
        sttLauncher.launch(sttIntent);
    }

    //proceso que se ejecuta como respuesta a la inicialización del tts
    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS) {
            ttsReady = true;
            tts.setLanguage(new Locale("spa", "ES"));
        } else {
            //showMessage("message: error onInit");
        }
    }

    private void showResult(String text) {
        etFrase.setText(text);
    }

    private void showMessage(String message) {
        runOnUiThread(() -> {
            SpannableString spanString = new SpannableString(message + "\n" + tvResultado.getText().toString());
            //spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
            tvResultado.setText(spanString);
        });
    }
    private void setupBot() {
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();
            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            sessionClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);
            dialogFlowReady = true;
        } catch (Exception e) {
            showMessage("message: exception in setupBot " + e.getMessage());
        }
    }

    private void sendMessageToBot(String message) {
        QueryInput input = QueryInput.newBuilder().setText(
                TextInput.newBuilder().setText(message).setLanguageCode("es-ES")).build();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    DetectIntentRequest detectIntentRequest =
                            DetectIntentRequest.newBuilder()
                                    .setSession(sessionName.toString())
                                    .setQueryInput(input)
                                    .build();
                    DetectIntentResponse detectIntentResponse = sessionClient.detectIntent(detectIntentRequest);
                    //intent, action, sentiment
                    if(detectIntentResponse != null) {
                        String action = detectIntentResponse.getQueryResult().getAction();
                        String intent = detectIntentResponse.getQueryResult().getIntent().getDisplayName();
                        //String sentiment = detectIntentResponse.getQueryResult().getSentimentAnalysisResult().toString();
                        String botReply = detectIntentResponse.getQueryResult().getFulfillmentText();

                        if(!botReply.isEmpty()) {
                            showMessage(intent);
                            /*if(intent.equalsIgnoreCase("liga")) {
                                //showMessage("Ahora mismo el Real Madrid, aunque el Manchester le metiera cuatro");
                                getUrl("https://informatica.ieszaidinvergeles.org:10099/liga.json");
                            } else {
                                showMessage("received response: " + botReply);
                            }*/
                            showMessage("received response: " + botReply);
                        } else {
                            showMessage("message: something went wrong in the response");
                        }

                        //Boolean dateResponse = detectIntentResponse.getQueryResult().getParameters().containsFields("date");
                        //Boolean timeResponse = detectIntentResponse.getQueryResult().getParameters().containsFields("time");

                        //if(dateResponse && timeResponse) {
                            //String dataResponseValue = detectIntentResponse.getQueryResult().getParameters().getAllFields().toString();
                          //  addEventCalendar(dataResponseValue);
                        //}
                    } else {
                        showMessage("message: connection failed");
                    }
                } catch (Exception e) {
                    showMessage("message: exception in sendMessageToBot thread " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void addEventCalendar(String data){
        //String dateEvent = date;
        String dateEvent = "20-05-2022";
        String title = "save in calendar";
        String location = "Granada";

        DateTimeFormatter isoDateFormatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime ldate = LocalDateTime.parse(dateEvent, isoDateFormatter);
        Date rDate = Date.from(ldate.atZone(ZoneId.of("UTC+2")).toInstant());
        long begin = rDate.getTime();

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void getUrl(String urlPath) {
        String respuesta = "Real Madrid";
        showMessage("El equipo que va primero es " + respuesta);
    }

    public void onClickBtLlamar() {
        callPhoneNumber(etNumero.getText().toString());
    }
    public void callPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        //if (intent.resolveActivity(getPackageManager()) != null) {
          //  startActivity(intent);
        //}
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE},REQUEST_PHONE_CALL);
        } else {
            startActivity(intent);
        }
    }

    public void onClickBtContacto() {
        String numero = search(etContacto.getText().toString());
        tvNumero.setText(numero);
    }
    public String search(String contactName) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            //startActivity(intent);
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String proyeccion[] = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
            String condicion = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like ?";
            String argumentos[] = new String[]{ contactName + "%" };
            String orden = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
            Cursor cursor = getContentResolver().query(uri, proyeccion, condicion, argumentos, orden);
            int columnaNombre = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int columnaNumero = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String nombre, numero = "";
            if(cursor.moveToNext()) {
                nombre = cursor.getString(columnaNombre);
                numero = cursor.getString(columnaNumero);
            }
            return numero;
        }
        return "No existe el contacto";
    }

    public void onClickBtInternet() {
        searchInWikipedia(etInternet.getText().toString());
    }
    public void searchInWikipedia(String term) {
        System.out.print("DENTRO searchInWikipedia");
        String url = "https://es.wikipedia.org/wiki/" + term;
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            System.out.print("DENTRO IF");
            startActivity(intent);
        } else {
            System.out.print("DENTRO ELSE");
        }
    }

    public void onClickBtCalendario() {
        saveInCalendar();
    }
    public void saveInCalendar() {
        System.out.print("DENTRO funcion caldndario");
        String date = "2022-05-05T12:00:00+02:00";
        String title = "save in calendar";
        String location = "Granada";

        DateTimeFormatter isoDateFormatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime ldate = LocalDateTime.parse(date, isoDateFormatter);
        Date rDate = Date.from(ldate.atZone(ZoneId.of("UTC+2")).toInstant());
        long begin = rDate.getTime();

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

}