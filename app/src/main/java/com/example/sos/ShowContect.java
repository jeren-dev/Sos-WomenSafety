package com.example.sos;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ShowContect extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_contect);


        recyclerView = findViewById(R.id.contactsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        databaseHelper = new DatabaseHelper(this);


        ArrayList<ContactsAdapter.Contact> contacts = databaseHelper.getAllContacts();


        adapter = new ContactsAdapter(this, contacts, databaseHelper);
        recyclerView.setAdapter(adapter);
    }
}
