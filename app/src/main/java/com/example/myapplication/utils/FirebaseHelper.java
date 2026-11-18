package com.example.myapplication.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {

    // Instancia de autenticación
    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    // Instancia de base de datos
    public static FirebaseFirestore getDB() {
        return FirebaseFirestore.getInstance();
    }

    // Obtener UID del usuario actual
    public static String getUserId() {
        if (getAuth().getCurrentUser() != null) {
            return getAuth().getCurrentUser().getUid();
        }

        return null;
    }
}
