package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;


import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etCorreo, etContrasena;
    private Button btnIniciarSesion, btnRegistrarse, btnGoogle;
    private TextView tvRecuperar;

    private FirebaseAuth auth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(getBaseContext());

        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvRecuperar = findViewById(R.id.tvRecuperar);

        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());
        btnRegistrarse.setOnClickListener(v -> registrarUsuario());
        tvRecuperar.setOnClickListener(v -> recuperarContrasena());

        btnGoogle.setOnClickListener(v -> iniciarSesionGoogle());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (auth != null && auth.getCurrentUser() != null) {
            irAListaSesiones();
        }
    }

    // INICIO DE SESION CON CORREO
    private void iniciarSesion() {
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString();

        if (!validarDatos(correo, contrasena)) return;

        auth.signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "¡Bienvenido!",
                                Toast.LENGTH_SHORT).show();
                        irAListaSesiones();
                    } else {
                        Toast.makeText(this,
                                "Error al iniciar sesión: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // REGISTRO CON CORREO
    private void registrarUsuario() {
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString();

        if (!validarDatos(correo, contrasena)) return;

        auth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Cuenta creada correctamente",
                                Toast.LENGTH_SHORT).show();
                        irAListaSesiones();
                    } else {
                        Toast.makeText(this,
                                "Error al registrarse: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // RECUPERAR CONTRASEÑA
    private void recuperarContrasena() {
        String correo = etCorreo.getText().toString().trim();

        if (correo.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Ingresa un correo válido");
            etCorreo.requestFocus();
            return;
        }

        auth.sendPasswordResetEmail(correo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Te enviamos un correo para recuperar tu contraseña",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "No se pudo enviar el correo: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // INICIO DE SESION CON GOOGLE
    private void iniciarSesionGoogle() {

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(R.string.default_web_client_id))
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                getBaseContext(),
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                    @Override
                    public void onResult(@NonNull GetCredentialResponse result) {
                        handleGoogleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "No se pudo seleccionar la cuenta: "
                                                + e.getLocalizedMessage(),
                                        Toast.LENGTH_LONG).show());
                    }
                }
        );
    }

    // PROCESAR LA CUENTA DE GOOGLE
    private void handleGoogleCredential(Credential credential) {

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                try {
                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(
                                    customCredential.getData());

                    autenticarFirebaseConGoogle(
                            googleCredential.getIdToken());

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Error al procesar la cuenta de Google",
                                    Toast.LENGTH_LONG).show());
                }

            } else {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Tipo de cuenta no compatible",
                                Toast.LENGTH_SHORT).show());
            }

        } else {
            runOnUiThread(() ->
                    Toast.makeText(this,
                            "No se recibió una cuenta de Google válida",
                            Toast.LENGTH_SHORT).show());
        }
    }

    // AUTENTICAR EN FIREBASE
    private void autenticarFirebaseConGoogle(String idToken) {

        AuthCredential firebaseCredential =
                GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "¡Inicio de sesión con Google exitoso!",
                                Toast.LENGTH_SHORT).show();

                        irAListaSesiones();

                    } else {
                        Toast.makeText(this,
                                "Error con Firebase: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // VALIDAR CORREO Y CONTRASEÑA
    private boolean validarDatos(String correo, String contrasena) {

        if (correo.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Ingresa un correo válido");
            etCorreo.requestFocus();
            return false;
        }

        if (contrasena.length() < 6) {
            etContrasena.setError(
                    "La contraseña debe tener al menos 6 caracteres");
            etContrasena.requestFocus();
            return false;
        }

        return true;
    }

    // IR A LA LISTA DE SESIONES
    private void irAListaSesiones() {
        Intent intent = new Intent(
                MainActivity.this,
                ListaSesionesActivity.class);

        startActivity(intent);
        finish();
    }
}