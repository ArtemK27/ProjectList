package com.example.projectlist.screens;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.projectlist.App;
import com.example.projectlist.R;
import com.example.projectlist.model.Group;
import com.example.projectlist.model.Note;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectlist.databinding.ActivityMainBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    List<Group> allGroups;
    private ActivityMainBinding binding;
    GroupAdapter groupAdapter;
    Toolbar toolbar;
    FloatingActionButton floatingActionButton;
    private Note note;
    private MainViewModel mainViewModel;
    Adapter adapter;
    TextView addList;
    ListView listViewNames;
    HashMap<String, String> namesGroupDB = new HashMap<>();
    Handler handler;
    String currentGroup = "";
    ProgressBar progressBarMain;
    List<String> allGroupNames;
    private ListenerRegistration fireStoreListener;

    private FirebaseFirestore cloud_database;
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "shopping_list_channel";
    private static final String CHANNEL_NAME = "Shopping List Updates";
    public List<Note> deleted = new LinkedList<>();
    public List<Note> changed = new LinkedList<>();
    public List<Note> added = new LinkedList<>();

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        cloud_database = FirebaseFirestore.getInstance();
        setSupportActionBar(binding.appBarMain.toolbar);
        floatingActionButton = findViewById(R.id.fab);
        binding.appBarMain.fab.setOnClickListener(view -> showDialog());
        progressBarMain = findViewById(R.id.progressBarMain);
        listViewNames = findViewById(R.id.names_list);


        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NavigationView navView = findViewById(R.id.nav_view);
        //  ОБРАБОТКА НАЖАТИЯ НА КАРТИНКУ ------------------------------------------------
        ImageView imageView =  navView.getHeaderView(0).findViewById(R.id.imageView);
        imageView.setOnClickListener(v -> {
            // действия при нажатии на картинку
        });

        addList = findViewById(R.id.add_list);
        addList.setOnClickListener(v -> showDialogAddList());

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getColor(R.color.one_more_green));

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        MainViewModel mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        registerForContextMenu(listViewNames);

        ExecutorService clearSyncExecutor = Executors.newSingleThreadExecutor();
        clearSyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                App.getInstance().getNoteDao().clearSync();
            }
        });

        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null ) {
            progressBarMain.setVisibility(View.VISIBLE);

            Animation animate;
            animate = AnimationUtils.loadAnimation(MainActivity.this, R.anim.progress_animate);
            progressBarMain.startAnimation(animate);
            String groupId = data.getQueryParameter("id");
            if (groupId != null) {
                cloud_database.collection("Notes")
                        .document("groups")
                        .collection("names")
                        .document(groupId)
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document != null && document.exists()) {

                                        Map<String, Object> cloudGroupName = task.getResult().getData();
                                        if (cloudGroupName != null) {
                                            ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                                            databaseExecutor.execute(() -> {
                                                allGroupNames = App.getInstance().getNoteDao().getAllNames();
                                            });
                                            databaseExecutor.shutdown();
                                            try {
                                                if(databaseExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                                                    if(!allGroupNames.contains(groupId)) {
                                                        showAddGroupDialog(groupId, (String) cloudGroupName.get("group"));
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "Такой список уже существует", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            } catch (InterruptedException e) {
//                                                throw new RuntimeException(e);
                                            }

                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this, "такого id нет", Toast.LENGTH_SHORT).show();
                                    }
                                    progressBarMain.clearAnimation();
                                    progressBarMain.setVisibility(View.INVISIBLE);

                                } else {
                                    Toast.makeText(MainActivity.this, "такого id нет", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
            } else {
                Toast.makeText(MainActivity.this, "Такой список уже существует", Toast.LENGTH_SHORT).show();
                progressBarMain.clearAnimation();
                progressBarMain.setVisibility(View.INVISIBLE);
            }
        }

        handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if(msg.arg1 == 1) {

//                    adapterNameLists = new ArrayAdapter<>(getApplicationContext(), R.layout.listview_names_item, names_lists);

                    groupAdapter = new GroupAdapter(getApplicationContext(), allGroups);
                    listViewNames.setAdapter(groupAdapter);
                    toolbar.setTitle(namesGroupDB.get(currentGroup));


                    listViewNames.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                            PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
                            popupMenu.inflate(R.menu.context_menu_for_lists);

                            popupMenu.setForceShowIcon(true);
                            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    int id = item.getItemId();
                                    if (id == R.id.action_delete_name) {
                                        Group group = groupAdapter.getItem(position);
                                        allGroups.remove(group);
                                        namesGroupDB.remove(group.uid);
                                        groupAdapter.notifyDataSetChanged();
                                        if (!allGroups.isEmpty()) {
                                            Group newGroup = allGroups.get(0);
                                            currentGroup = newGroup.uid;
                                            mainViewModel.setFilter(currentGroup);
                                            setFireStoreListener(currentGroup);
                                            toolbar.setTitle(newGroup.group);

                                        } else {
                                            floatingActionButton.setVisibility(View.INVISIBLE);
                                            toolbar.setTitle("Создайте новый список");
                                            currentGroup = "";
                                            mainViewModel.setFilter(currentGroup);
                                        }
                                        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                                        databaseExecutor.execute(() -> App.getInstance().getNoteDao().deleteName(group.uid));
                                        databaseExecutor.shutdown();
                                        return true;
                                    }

                                    if(id == R.id.action_edit_name) {
                                        Group group = groupAdapter.getItem(position);
                                        showDialogAddList(group);
                                        return true;
                                    }

                                    if (id == R.id.action_show_id) {
                                        Intent intent = new Intent(Intent.ACTION_SEND);
                                        intent.setType("text/plain");
                                        String link = "ftp://quickcart.com/add_group?id=" + groupAdapter.getGroupId(position);
                                        intent.putExtra(Intent.EXTRA_TEXT,link);

                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            // Проверяем есть ли устройство подходящее приложение для открытия интента.
                                            startActivity(Intent.createChooser(intent, "Поделиться группой"));
                                        }

//                                        Toast.makeText(MainActivity.this, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();
//                                        ClipboardManager clipboard = (ClipboardManager) MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
//                                        ClipData clip = ClipData.newPlainText("", String.valueOf(groupAdapter.getGroupId(position)) );
//                                        clipboard.setPrimaryClip(clip);
                                        return true;
                                    }

                                    return false;
                                }
                            });
                            popupMenu.show();
                            return true;

                        }
                    });


                    Log.d("HANDLERSEND", "get sending");

                    adapter = new Adapter();
                    final RecyclerView recyclerView = findViewById(R.id.main_list);

                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false);
                    recyclerView.setLayoutManager(linearLayoutManager);
                    DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL);
                    dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.recycleview_divider));
                    recyclerView.addItemDecoration(dividerItemDecoration);
                    recyclerView.setAdapter(adapter);



                    mainViewModel.setFilter(currentGroup);
                    setFireStoreListener(currentGroup);
                    LiveData<List<Note>> liveNotes = mainViewModel.getLiveDataByGroup();
                    liveNotes.observe(MainActivity.this, notes -> {
//                            adapter.setItems(null);
                        adapter.setItems(notes);
                    });


                    adapter.setOnItemLongClickListener((position, view) -> {
                        Note tmp = adapter.getNote(position);
//                            Toast.makeText(MainActivity.this, tmp.text, Toast.LENGTH_SHORT).show();


                        PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
                        popupMenu.inflate(R.menu.context_menu_item);
                        popupMenu.setForceShowIcon(true);
                        popupMenu.setOnMenuItemClickListener(item -> {
                            int id = item.getItemId();
                            if(id == R.id.action_delete_item) {
                                ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                                databaseExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("DEL_DB", "удаляется doneNote с товаром " + tmp.text + tmp.amount);
                                        cloud_database.collection("Notes")
                                                .document("groups")
                                                .collection(currentGroup)
                                                .document(String.valueOf(tmp.uid))
                                                .delete();
                                        App.getInstance().getNoteDao().delete(tmp);
                                    }
                                });

                                databaseExecutor.shutdown();
                                return true;
                            }
                            if(id == R.id.action_edit_item) {
                                showDialog(tmp, position);
                                return true;
                            }


                            return false;
                        });
                        popupMenu.show();

                    });
                    syncAddNotes();


                } else if(msg.arg1 == -1) {
                    floatingActionButton.setVisibility(View.INVISIBLE);
                    toolbar.setTitle("Создайте новый список");
                }

            }

        };

        update_lists();




        listViewNames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group group = groupAdapter.getItem(position);
                if (group != null) {
                    currentGroup = group.uid;
                }

                mainViewModel.setFilter(currentGroup);
                setFireStoreListener(currentGroup);
                toolbar.setTitle(group.group);
                groupAdapter.setSelectedItem(position);
                drawer.close();
            }
        });



    }

    public void showNotification(String msg) {

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,  PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.cart_icon)
                .setTicker("Новое уведомление")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle("Quick cart")
                .setContentText(msg);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }



    private void setFireStoreListener(String id) {
        if (id == "" || id == null) {
            return;
        }
        if (fireStoreListener != null) {
            fireStoreListener.remove();
        }


        fireStoreListener = cloud_database
                .collection("Notes")
                .document("groups")
                .collection(id)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w("Firestore", "Listen failed.", error);
                            return;
                        }
                        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Note item = dc.getDocument().toObject(Note.class);
                            boolean exist = adapter.is_exist(item);

                            if (!exist) {
                                switch (dc.getType()) {
                                    case ADDED:
                                        databaseExecutor.execute(() -> {
                                            App.getInstance().getNoteDao().insert(item);
                                        });
                                        showNotification("Был добавлен " + '"' + item.text + '"' + " " + item.amount);
                                        break;
                                    case MODIFIED:
                                        databaseExecutor.execute(() -> {
                                            Note previousNote = App.getInstance().getNoteDao().findById(item.uid);
                                            App.getInstance().getNoteDao().insert(item);

                                            if(item.text.equals(previousNote.text) &&
                                                    item.amount.equals(previousNote.amount)) {
                                                if(item.done) {
                                                    showNotification("Вычеркнут " + '"' + item.text + '"' + " " + item.amount);
                                                } else {
                                                    showNotification("Вернут " + '"' + item.text + '"' + " " + item.amount);
                                                }
                                            } else {
                                                showNotification("Был изменен " +
                                                        '"' + previousNote.text + '"' + " " + previousNote.amount +
                                                        " ---> " +
                                                        '"' + item.text + '"' + " " + item.amount);
                                            }

                                        });

                                        break;
                                }
                            } else {
                                switch (dc.getType()) {
                                    case REMOVED:
                                        databaseExecutor.execute(() -> {
                                            App.getInstance().getNoteDao().delete(item);
                                        });
                                        showNotification("Был удален " + '"' + item.text + '"' + " " + item.amount);
                                        break;
                                }
                            }
                        }
                    databaseExecutor.shutdown();
                    }
                });
    }

    // обновление списка списков магазинов
    public void update_lists() {

        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                allGroups = App.getInstance().getNoteDao().getAllGroups();

                if (!allGroups.isEmpty()) {
                    synchronized (currentGroup) {
                        for (Group group : allGroups) {
                            Log.i("LISTNAMES", group.group);
                            namesGroupDB.put(group.uid, group.group);
                        }
                        currentGroup = allGroups.get(0).uid;
                    }

                    toolbar.setTitle(namesGroupDB.get(currentGroup));

                    msg.arg1 = 1;
                    Log.i("HANDLERSEND", "try to send");

                } else {
                    msg.arg1 = -1;
                    Log.i("HANDLERSEND", "NO GROUPS");
                }
                handler.sendMessage(msg);

            }
        });
        databaseExecutor.shutdown();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // создание объекта для загрузки в Firebase
    Map<String, Object> noteConverter(Note note) {
        Map<String, Object> note1 = new HashMap<>();
        note1.put("uid", note.uid);
        note1.put("text", note.text);
        note1.put("time", note.time);
        note1.put("done", note.done);
        note1.put("amount", note.amount);
        note1.put("group", note.group);
        note1.put("author", note.author);

        return note1;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if(id == R.id.action_delete) {
                   ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                   databaseExecutor.execute(new Runnable() {
                       @Override
                       public void run() {
                           List<Note> doneNotes = App.getInstance().getNoteDao().getDoneNoteByGroup(currentGroup);
                            for (Note doneNote : doneNotes) {
                                Log.d("DEL_DB", "удаляется doneNote с товаром " + doneNote.text);
                                cloud_database.collection("Notes")
                                        .document("groups")
                                        .collection(currentGroup)
                                        .document(String.valueOf(doneNote.uid))
                                        .delete();
                            }
                            App.getInstance().getNoteDao().deleteDoneNoteByGroup(currentGroup);
                       }
                   });

                    databaseExecutor.shutdown();

                }
//                if(id == R.id.action_sync){
//                    syncAddNotes();
//                }
        return super.onOptionsItemSelected(item);
    }

    void syncAddNotes() {
                Log.d(TAG, "sync begin");
                for(String idGroup : namesGroupDB.keySet()) {
                    Log.i("SyncGroup", "group =  " + idGroup);
                    cloud_database
                            .collection("Notes")
                            .document("groups")
                            .collection(idGroup)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                                        databaseExecutor.execute(new Runnable() {

                                            @Override
                                            public void run() {
//                                                App.getInstance().getNoteDao().clearSync();
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    Map<String, Object> cloud_note = document.getData();
                                                    Note local_note = new Note();
                                                    local_note.uid = Long.valueOf(cloud_note.get("uid").toString()).intValue();
                                                    local_note.time = Long.valueOf(cloud_note.get("time").toString()).intValue();
                                                    local_note.text = (String) cloud_note.get("text");
                                                    local_note.done = (boolean) cloud_note.get("done");
                                                    local_note.amount = (String) cloud_note.get("amount");
                                                    local_note.group = (String) cloud_note.get("group");
                                                    local_note.author = (String) cloud_note.get("author");
                                                    local_note.sync = 1;
                                                    App.getInstance().getNoteDao().insert(local_note);
                                                    Log.d("AddDocument", "succeful");
                                                    Log.d("AddDocument", document.getId() + " => " + document.getData().get("text"));
                                                }
                                                App.getInstance().getNoteDao().deleteSyncByGroup(idGroup);
                                            }
                                        });
                                    databaseExecutor.shutdown();
                                    } else {
                                        Log.d("AddDocument", "Error getting documents: ", task.getException());
                                    }
                                }
                            });
                }

    }
//    @Override
//    public boolean onSupportNavigateUp() {
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
//                || super.onSupportNavigateUp();
//    }


    // массив единиц измерения товаров
    String[] units = {"кг", "г", "мл", "л", "шт"};

    // функция добавления элемента в список через диалог
    public Note converterEntry(String s) {
        //обработка текста добавления заметки, разбиение на amount, unit и text
        Note note2 = new Note();
        String unit = "";
        s = s.trim();
        if(s.contains(" ")){
            int last_num_pos = 0;
            for(int j = s.length()-1; j > 0; j--) {
                if (Character.isDigit(s.charAt(j))) {
                    last_num_pos = j;
                    Log.d(TAG, "last_num_pos = " + j);

                    for(String check : units) {
                        if (s.substring(last_num_pos, s.length()).toLowerCase().contains(check)) {
                            unit = check;
                            s = s.substring(0, s.lastIndexOf(check));
                            s = s.trim();
                            Log.d(TAG, "s after 1st check = " + s);
                            break;
                        }
                    }
                }
            }
        }

        try {
            note2.amount = Integer.parseInt(s.substring(s.lastIndexOf(" ")).trim()) + " " + unit;
            s = s.substring(0,s.lastIndexOf(" "));
        } catch (Exception e) {
            note2.amount = "";
        }
        note2.text = s;

        return note2;
    }
    private void showDialog(Note editNote, int pos) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);
        final EditText editText = dialog.findViewById(R.id.editTextText);
        String entry = (editNote.text + " "+ editNote.amount).trim();
        editText.setText(entry);
        FloatingActionButton button = dialog.findViewById(R.id.confirm_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!editText.getText().toString().equals(""))   {
                    String s = editText.getText().toString();
                    Note tmp = converterEntry(s);
                            editNote.text = tmp.text;
                            editNote.amount = tmp.amount;
                            adapter.updateItem(pos, editNote);
//                            mainViewModel.updateNote(editNote);
                            Log.i("EditingNotes", "editNote: " + editNote.text + " " +editNote.amount);
                            cloud_database.collection("Notes")
                                    .document("groups")
                                    .collection(editNote.group)
                                    .document(String.valueOf(editNote.uid))
                                    .update("text", editNote.text, "amount", editNote.amount);

                    dialog.dismiss();


                }

            }
        });

        // Расположение окна внизу
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        dialog.show();
        // показать клавиатуру
        editText.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }
    private void showDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);
        final EditText editText = dialog.findViewById(R.id.editTextText);

        FloatingActionButton button = dialog.findViewById(R.id.confirm_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(editText.getText().length() > 0) {
                    ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                    databaseExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            note = new Note();
                            String s = editText.getText().toString();
                            Note tmp = converterEntry(s);
                            note.text = tmp.text;
                            note.amount = tmp.amount;
                            // создание локальной заметки
                            if(!currentGroup.equals("")){
                                note.group = currentGroup;
                            } else note.group = "";
                            note.done = false;
                            note.time = System.currentTimeMillis();
                            note.author = "ghost";
                            note.uid = note.hashCode();

                            Map<String, Object> note1 = noteConverter(note);

                            cloud_database.collection("Notes")
                                    .document("groups")
                                    .collection(currentGroup)
                                    .document(String.valueOf(note.uid))
                                    .set(note1);
                            App.getInstance().getNoteDao().insert(note);
                        }
                    });
                    databaseExecutor.shutdown();
                }

                dialog.dismiss();
            }
        });



        // Расположение окна внизу
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        dialog.show();
        // показать клавиатуру
        editText.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }



    private boolean is_link(String s) {
        if(s.length() < 50) return false;
        if (s.charAt(32) == '=') {
            Log.i("IS_this_link", "на 32 позиции буква " + s.charAt(32));
            return true;
        }
        Log.i("ISTHISID?", "is_id: " + "false");
        return false;
    }

    void addNewList(Group group) {
        Log.i("cloudGroupName", "name2 = " + group.group);
        Log.i("cloudGroupName", "id2 = " + group.uid);
        boolean flag = !allGroups.isEmpty();
        floatingActionButton.setVisibility(View.VISIBLE);
        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (allGroups) {

                    App.getInstance().getNoteDao().insert(group);
                    if (allGroups.isEmpty()) {
                        currentGroup = group.uid;
                        allGroups.add(group);
                        Message msg = new Message();
                        msg.arg1 = 1;
                        handler.sendMessage(msg);
                    } else {
                        allGroups.add(group);
                    }
                    namesGroupDB.put(group.uid, group.group);
                }

                Map<String, Object> NewGroup1 = new HashMap<>();
                NewGroup1.put("group", group.group);
                cloud_database.collection("Notes")
                        .document("groups")
                        .collection("names")
                        .document(group.uid)
                        .set(NewGroup1);
            }
        });
        databaseExecutor.shutdown();

        if(flag) groupAdapter.notifyDataSetChanged();
//        try {
//            if (databaseExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
//                Log.i("editNewList1", "editNewList1: thread finish work ");
//                syncAddNotes();
//            }
//        } catch (InterruptedException e) {
//            Log.e("editNewList1", "editNewList1: time error ");
//        }
    }
    void editNewList(Group group) {
        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                    App.getInstance().getNoteDao().update(group);
                    namesGroupDB.replace(group.uid, group.group);
                    cloud_database.collection("Notes")
                            .document("groups")
                            .collection("names")
                            .document(group.uid)
                            .update("group", group.group);
            }
        });
        databaseExecutor.shutdown();
        try {
            if (databaseExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                Log.i("editNewList", "editNewList: thread finish work ");
                groupAdapter.notifyDataSetChanged();
            }
        } catch (InterruptedException e) {
            Log.e("editNewList", "editNewList: time error ");
        }
    }


    void showDialogAddList(Group group) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_list_layout);
        dialog.show();
        final TextInputEditText editText = dialog.findViewById(R.id.add_new_list);
        editText.setText(group.group);
        MaterialButton materialButton = dialog.findViewById(R.id.confirm_button);
        materialButton.setText("Сохранить");
        materialButton.setIcon(getDrawable(R.drawable.baseline_check_24));
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String listName = editText.getText().toString();
                if(!listName.equals(group.group)) {

                    allGroups.remove(group);
                    group.group = listName;
                    allGroups.add(group);
                    if(currentGroup.equals(group.uid)) {
                        toolbar.setTitle(group.group);
                    }
                    editNewList(group);
                    dialog.dismiss();

                } else {
                    Toast.makeText(MainActivity.this, "название не изменилось", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void showAddGroupDialog(final String groupId, String nameGroup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить группу");
        builder.setMessage("Вы хотите добавить новый список '" + nameGroup +"'" + "?");

        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                addGroupById(groupId);
                Toast.makeText(getApplicationContext(), "Новый список был добавлен", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
     void showDialogAddList() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_list_layout);
        dialog.show();
        final TextInputEditText editText = dialog.findViewById(R.id.add_new_list);
        final ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        final MaterialButton materialButton = dialog.findViewById(R.id.confirm_button);
        Animation animate;
        animate = AnimationUtils.loadAnimation(MainActivity.this, R.anim.progress_animate);
        progressBar.setVisibility(View.INVISIBLE);

        materialButton.setOnClickListener(v -> {
            String listName = editText.getText().toString().trim();

            if(!listName.equals("") && !namesGroupDB.containsValue(listName)) {
                Group group = new Group();
                if (is_link(listName)) {
                    String idGroup = listName.substring(33);
                    if (!namesGroupDB.containsKey(idGroup)) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.startAnimation(animate);
                        cloud_database.collection("Notes")
                                .document("groups")
                                .collection("names")
                                .document(idGroup)
                                .get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists()) {
                                            Map<String, Object> cloudGroupName = task.getResult().getData();
                                            assert cloudGroupName != null;
                                            group.group = (String) cloudGroupName.get("group");
                                            Log.i("cloudGroupName", "name0 = " + group.group);
                                            group.uid = idGroup;
                                            addNewList(group);
                                        } else {
                                            Toast.makeText(MainActivity.this, "Такого id нет", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this, "Такого id нет", Toast.LENGTH_SHORT).show();
                                    }
                                    progressBar.clearAnimation();
                                    dialog.dismiss();
                                });
                    } else {
                        Toast.makeText(MainActivity.this, "Такой список уже существует", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (listName.length() > 40) {
                        Toast.makeText(MainActivity.this, "Слишком длинное название списка", Toast.LENGTH_SHORT).show();
                    } else {
                        group.uid = UUID.randomUUID().toString();
                        group.group = listName;
                        addNewList(group);
                        dialog.dismiss();
                    }
                }
            } else {
                Log.i("NewListCreate", "onClick: не прошло условие");
                Toast.makeText(MainActivity.this, "Неправильный ввод или такое имя уже есть", Toast.LENGTH_SHORT).show();
            }

        });
    }


    void addGroupById(String id) {
        Group group = new Group();
        cloud_database.collection("Notes")
                .document("groups")
                .collection("names")
                .document(id)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> cloudGroupName = task.getResult().getData();
                            assert cloudGroupName != null;
                            group.group = (String) cloudGroupName.get("group");
                            Log.i("cloudGroupName", "name0 = " + group.group);
                            group.uid = id;
                        } else {
                            Toast.makeText(MainActivity.this, "такого id нет", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "такого id нет", Toast.LENGTH_SHORT).show();
                    }
                    addNewList(group);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private Object hashCode(String valueOf) {
        int hash = 7;
        hash = 31 * hash + (valueOf == null ? 0 : valueOf.hashCode());
        return hash;
    }


}
