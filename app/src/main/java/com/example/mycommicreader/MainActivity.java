package com.example.mycommicreader;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.mycommicreader.model.Chapter;
import com.example.mycommicreader.model.ChapterBread;
import com.example.mycommicreader.model.ChapterData;
import com.example.mycommicreader.model.Manga;
import com.example.mycommicreader.model.MangaBread;

import com.example.mycommicreader.modelview.MangaApiService;
import com.example.mycommicreader.databinding.ActivityMainBinding;
import com.example.mycommicreader.view.MangaAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements MangaAdapter.OnNoteListener {

    ProgressDialog progress;
    private ActivityMainBinding binding;
    private MangaAdapter mangaAdapter;
    List<Manga> mangaList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private Session session;
    ArrayList<String> followed;
    private static String idUser = "";
    private boolean i;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.rgb(255, 199, 249)));
        getSupportActionBar().setTitle("Following");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        mangaAdapter = new MangaAdapter(mangaList, this, this);
        setContentView(view);
        session = new Session(this);
        idUser = session.getUserName();
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        followed = new ArrayList<>();
        new MainActivity.GetManga("Follow").execute();

    }

    private class GetManga extends AsyncTask<Void, Void, Void> {
        private String title;
        public GetManga(String title) {
            this.title = title;
        }
        @Override
        protected  void onPreExecute () {
            progress = ProgressDialog.show(MainActivity.this, "Loading...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {


            try {
                Response<MangaBread> m;
                mangaList.removeAll(mangaList);
                if (title == "update") {
                    m = MangaApiService.apiService.getManga().execute();
                } else if (title == "popular") {
                    m = MangaApiService.apiService.getPopularManga().execute();
                } else if (title == "Follow") {
                    i = true;
                    getDataStore(idUser);
                    while (true) {
                        if (!i) {
                            break;
                        }
                    }
                    m = MangaApiService.apiService.getFollowedManga(followed).execute();
                } else {
                    m = MangaApiService.apiService.findManga(title).execute();
                }

                mangaList.addAll(m.body().getData());

            } catch(Exception e) {
                Log.d("DEBUG", e.toString());
            }

            return null;
        }




        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);
            binding.notify.setText("");
            if (!isNetworkAvailable()) {
                binding.notify.setText("No internet connection ￣へ￣");
            } else if (mangaList.size() == 0) {
                binding.notify.setText("No manga found w(ﾟДﾟ)w");
            }
            binding.rvMangas.setAdapter(mangaAdapter);
            binding.rvMangas.setLayoutManager(new GridLayoutManager(getApplicationContext(), 3));
            progress.dismiss();

        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    @Override
    public void onNoteClick(int position) {
        Intent intent = new Intent(MainActivity.this, MangaDetail.class);
        Manga m = mangaList.get(position);
        intent.putExtra("id", m.getID());
        intent.putExtra("name", m.getTitle());
        intent.putExtra("tag", m.getTag());
        intent.putExtra("Cover", m.getCoverFileName());
        intent.putExtra("author", m.getAuthor());
        intent.putExtra("type", m.getType());
        intent.putExtra("status", m.getStatus());
        intent.putExtra("year", m.getYear());
        intent.putExtra("UserID", idUser);
        intent.putExtra("DocumentID",m.getDocumentID());
        intent.putStringArrayListExtra("followed", followed);
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == 2) {
                if (resultCode == RESULT_OK) {
                    String id = data.getStringExtra("mangaID");
                    if (id != "" && id != null) {
                        followed.removeAll(followed);
                    }
                    getDataStore(idUser);
                }
            }

            if (requestCode == 3) {
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("userID");
                    idUser = name;
                    Log.d("DEBUG", idUser);
                    getDataStore(idUser);
                    session.setUserName(idUser);
                }
            }
        } catch (Exception e) {
            Log.d("DEBUG", e.getMessage());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                new MainActivity.GetManga(query).execute();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                new MainActivity.GetManga("update").execute();
                getSupportActionBar().setTitle("Recently updated");
                return true;
            }
        });

        MenuItem menuItem1 = menu.findItem(R.id.menu);
        menuItem1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.followed_item:
                try {
                    if (idUser == null || idUser == "") {
                        LoginAlert();
                    } else {
                        getSupportActionBar().setTitle("Following");
                        new MainActivity.GetManga("Follow").execute();
                    }
                } catch (Exception e) {

                }
                return true;
            case R.id.popular_item:
                getSupportActionBar().setTitle("Popular mangas");
                new MainActivity.GetManga("popular").execute();
                return true;
            case R.id.updated_item:
                getSupportActionBar().setTitle("Recently updated");
                new MainActivity.GetManga("update").execute();
                return true;
            case R.id.login_item:
                Intent i = new Intent(MainActivity.this,Login.class);
                startActivityForResult(i, 3);
                return true;
            case R.id.logout_item:
                idUser = "";
                session.setUserName(idUser);
                Logout();
                return true;
            case R.id.about_item:
                About();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void getDataStore(String IDUser){
        firestore.collection(IDUser)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String IDManga = document.getData().get("IDManga").toString();
                                followed.add(IDManga);
                                Log.d("DEBUG", document.getId() + " => " + document.getData().get("IDManga").toString());
                            }
                            i = false;
                            Log.d("DEBUG",  " => " + followed.size());
                        } else {
                            Log.w("DEBUG", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    void About() {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(MainActivity.this);
        dlgAlert.setTitle("About");
        String s = "Creator:\n" +
                "   Do Thanh Long\n" +
                "   Duong Xuan Ngoc Phong\n" +
                "   Nguyen Phan Minh Nhat\n\n" +
                "All manga credit belongs to MangaDex (https://mangadex.org/)";
        dlgAlert.setMessage(s);
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    void LoginAlert() {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(MainActivity.this);
        //dlgAlert.setTitle("Login");
        String s = "Login to use this feature (～￣▽￣)～";
        dlgAlert.setMessage(s);
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    void Logout() {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(MainActivity.this);
        //dlgAlert.setTitle("Login");
        String s = "You logged out (～￣▽￣)～";
        dlgAlert.setMessage(s);
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }
}