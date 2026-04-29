package com.example.sos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private ArrayList<Contact> contacts;
    private Context context;
    private DatabaseHelper databaseHelper;

    public ContactsAdapter(Context context, ArrayList<Contact> contacts, DatabaseHelper databaseHelper) {
        this.context = context;
        this.contacts = contacts;
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Contact contact = contacts.get(position);

        holder.nameText.setText(contact.getName());
        holder.phoneText.setText(contact.getPhone());
        holder.emailText.setText(contact.getEmail());

        holder.deleteButton.setOnClickListener(v -> {

            boolean isDeleted = databaseHelper.deleteContact(contact.getId());

            if (isDeleted) {
                contacts.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, contacts.size());

                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to delete contact", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView nameText, phoneText, emailText;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.nameText);
            phoneText = itemView.findViewById(R.id.phoneText);
            emailText = itemView.findViewById(R.id.emailText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }


    public static class Contact {

        private String id;
        private String name;
        private String phone;
        private String email;

        public Contact(String id, String name, String phone, String email) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }
    }
}
