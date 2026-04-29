package com.example.sos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class AddContect extends AppCompatActivity {

    private EditText editTextName,editTextPhone,editTextemail;
    private Button buttonSubmit;
    private DatabaseHelper databaseHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_contect);


        editTextName = findViewById(R.id.name);
        editTextPhone = findViewById(R.id.num);
        editTextemail = findViewById(R.id.email);
        buttonSubmit = findViewById(R.id.submit);


        databaseHelper = new DatabaseHelper(this);


        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString().trim();
                String phoneNum = editTextPhone.getText().toString().trim();
                String email = editTextemail.getText().toString().trim();

                if (name.isEmpty() || phoneNum.isEmpty() || email.isEmpty()) {
                    Toast.makeText(AddContect.this, "Please Fill all fields", Toast.LENGTH_SHORT).show();
                } else {

                    boolean isInserted = databaseHelper.insertContact(name,phoneNum,email);
                    if (isInserted) {
                        Toast.makeText(AddContect.this, "saved successfully", Toast.LENGTH_SHORT).show();
                        editTextName.setText("");
                        editTextPhone.setText("");
                        editTextemail.setText("");
                    } else {
                        Toast.makeText(AddContect.this, "Failed to save person", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
