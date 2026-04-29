package com.example.sos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "ContactDB";
    private static final int DATABASE_VERSION = 1;


    private static final String TABLE_NAME = "contacts";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_EMAIL = "email";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PHONE + " TEXT, " +
                COLUMN_EMAIL + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    public boolean insertContact(String name, String phoneNumber, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PHONE, phoneNumber);
        values.put(COLUMN_EMAIL, email);
        long result = db.insert(TABLE_NAME, null, values);
        db.close();
        return result != -1;
    }

    public ArrayList<ContactsAdapter.Contact> getAllContacts() {
        ArrayList<ContactsAdapter.Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name, phone, email,id FROM contacts", null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String phone = cursor.getString(1);
                String email = cursor.getString(2);
                String id = cursor.getString(3);
                contacts.add(new ContactsAdapter.Contact(id,name,phone, email));
            } while (cursor.moveToNext());
        }
        cursor.close();

        db.close();
        return contacts;
    }



    public boolean deleteContact(String phoneNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{phoneNumber});
        db.close();
        return rowsDeleted > 0;
    }
}

