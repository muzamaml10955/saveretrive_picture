package com.inspirationappsstudio.savingandretrivingfromfirebase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //constant to track image chooser intent
    private static final int PICK_IMAGE_REQUEST = 234;

    //view objects
    private Button buttonChoose;
    private Button buttonUpload;
    private EditText editTextName;
    private TextView textViewShow;
    private ImageView imageView;

    //uri to store file
    private Uri filePath;

    //firebase objects
    private StorageReference storageReference;
    private DatabaseReference mDatabase;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        buttonChoose = (Button) findViewById(R.id.buttonChoose);
        buttonUpload = (Button) findViewById(R.id.buttonUpload);
        imageView = (ImageView) findViewById(R.id.imageView);
        editTextName = (EditText) findViewById(R.id.editText);
        textViewShow = (TextView) findViewById(R.id.textViewShow);

        storageReference = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_PATH_UPLOADS);

        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);
        textViewShow.setOnClickListener(this);



    }
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }


    @Override
    public void onClick(View view) {
        if (view == buttonChoose) {
            showFileChooser();
        } else if (view == buttonUpload) {
            uploadFile();
        } else if (view == textViewShow) {
            Toast.makeText(this, "you click show button" , Toast.LENGTH_SHORT).show();
            Intent i = new Intent(getApplicationContext(), ShowImagesActivity.class);
            startActivity(i);

        }
    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }


    private void uploadFile() {
        //checking if file is available
        if (filePath != null) {
            //displaying progress dialog while image is uploading
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            //getting the storage reference
            StorageReference sRef = storageReference.child(Constants.STORAGE_PATH_UPLOADS + System.currentTimeMillis() + "." + getFileExtension(filePath));


///////////////////////////////////////////////////////////////////////


            //   final StorageReference ref = storageRef.child("mountains.jpg");

            UploadTask uploadTask = sRef.putFile(filePath);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }

                    // Continue with the task to get the download URL
                    return sRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {

                        progressDialog.dismiss();

                        //displaying success toast
                        Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();





                        Uri downloadUri = task.getResult();
                        Log.d("uri ", downloadUri.toString());
                        //creating the upload object to store uploaded image details
                        Upload upload = new Upload(editTextName.getText().toString().trim(),downloadUri.toString());

                        //adding an upload to firebase database
                        String uploadId = mDatabase.push().getKey();
                        mDatabase.child(uploadId).setValue(upload);

                        // author.setText(downloadUri.toString());
                        //   Log.d("DownloadURL ",downloadUri.toString());
                    } else {
                        // Handle failures
                        // ...
                    }
                }
            }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //Uri downloadUri = task.getResult();
                    // author.setText(downloadUri.toString());
                    //dismissing the progress dialog


                    Log.d("DownloadURL ", uri.toString());
                }
            });


///////////////////////////////////////////////////////////////////////





/*
            //adding the file to reference
            sRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //dismissing the progress dialog
                            progressDialog.dismiss();

                            //displaying success toast
                            Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();

                            //creating the upload object to store uploaded image details
                            Upload upload = new Upload(editTextName.getText().toString().trim(), taskSnapshot.getDownloadUrl().toString());

                            //adding an upload to firebase database
                            String uploadId = mDatabase.push().getKey();
                            mDatabase.child(uploadId).setValue(upload);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //displaying the upload progress
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        } else {
            Toast.makeText(this, "plsese select a file to upload" , Toast.LENGTH_SHORT).show(); //display an error if no file is selected
        }
    }*/


        }}}