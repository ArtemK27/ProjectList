package com.example.projectlist.screens;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;


import com.example.projectlist.App;
import com.example.projectlist.R;
import com.example.projectlist.data.NoteDao;
import com.example.projectlist.model.Group;
import com.example.projectlist.model.Note;
import com.example.projectlist.ui.home.HomeFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectlist.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    List<Group> allGroups;
    ArrayAdapter<String> adapterNameLists;
    private ActivityMainBinding binding;
    GroupAdapter groupAdapter;
    Toolbar toolbar;
    FloatingActionButton floatingActionButton;
    private Note note;
    private MainViewModel mainViewModel;
    ImageView imageView;
    Dialog dialog;
    TextView addList;
    ListView listViewNames;
    HashMap<String, String> namesGroupDB = new HashMap<>();
    ExecutorService databaseExecutorMain;
    Handler handler;
    String currentGroup = "";

    private FirebaseFirestore cloud_database;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        cloud_database = FirebaseFirestore.getInstance();
        setSupportActionBar(binding.appBarMain.toolbar);
        floatingActionButton = findViewById(R.id.fab);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });


//        View fragment = findViewById(R.id.nav_host_fragment_content_main);

        listViewNames = findViewById(R.id.names_list);

        NavigationView navView = findViewById(R.id.nav_view);
        //  ОБРАБОТКА НАЖАТИЯ НА КАРТИНКУ ------------------------------------------------
        ImageView imageView =  navView.getHeaderView(0).findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // действия при нажатии на картинку
            }
        });

        addList = findViewById(R.id.add_list);
        addList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAddList();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

//        mAppBarConfiguration = new AppBarConfiguration.Builder()
//                .setOpenableLayout(drawer)
//                .build();
//        NavController navController = new NavController(getApplicationContext());
//        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
//        NavigationUI.setupWithNavController(navigationView, navController);


        toolbar = findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        MainViewModel mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        registerForContextMenu(listViewNames);

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
                                            toolbar.setTitle(newGroup.group);

                                        } else {
                                            floatingActionButton.setVisibility(View.INVISIBLE);
                                            toolbar.setTitle("Создайте новый список");
                                            currentGroup = "";
                                            mainViewModel.setFilter(currentGroup);
                                        }
                                        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                                        databaseExecutor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                App.getInstance().getNoteDao().deleteName(group.uid);
                                            }
                                        });

                                        return true;
                                    }

                                    if(id == R.id.action_edit_name) {
                                        Group group = groupAdapter.getItem(position);
                                        showDialogAddList(group);

                                    }

                                    if (id == R.id.action_show_id) {
                                        Toast.makeText(MainActivity.this, String.valueOf(groupAdapter.getGroupId(position)), Toast.LENGTH_SHORT).show();
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

                    Adapter adapter = new Adapter();
                    final RecyclerView recyclerView = findViewById(R.id.main_list);

                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false);
                    recyclerView.setLayoutManager(linearLayoutManager);
                    recyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
                    recyclerView.setAdapter(adapter);



                    mainViewModel.setFilter(currentGroup);
                    LiveData<List<Note>> liveNotes = mainViewModel.getLiveDataByGroup();
                    liveNotes.observe(MainActivity.this, new Observer<List<Note>>() {
                        @Override
                        public void onChanged(List<Note> notes) {
                            adapter.setItems(notes);
                        }
                    });


                    adapter.setOnItemLongClickListener(new Adapter.OnItemLongClickListener() {
                        @Override
                        public void onItemLongClick(int position, View view) {
                            Note tmp = adapter.getNote(position);
//                            Toast.makeText(MainActivity.this, tmp.text, Toast.LENGTH_SHORT).show();


                            PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
                            popupMenu.inflate(R.menu.context_menu_item);
                            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
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
                                        showDialog(tmp);
                                        return true;
                                    }


                                    return false;
                                }
                            });
                            popupMenu.show();

                        }
                    });
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
                currentGroup = group.uid;

                mainViewModel.setFilter(currentGroup);
                toolbar.setTitle(group.group);
                drawer.close();
            }
        });

    }


    // обнловление списка списков магазинов
    public void update_lists() {

        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                allGroups = App.getInstance().getNoteDao().getAllNames();

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
    };


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
                if(id == R.id.action_sync){
                    Log.d(TAG, "sync begin");

                    Set<String> idsGroup = namesGroupDB.keySet();
                    String[] ids = idsGroup.toArray(new String[0]);

                    for(String idGroup : ids) {
                        cloud_database
                            .collection("Notes")
                            .document("groups")
                            .collection(idGroup)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                                    databaseExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            NoteDao repo = App.getInstance().getNoteDao();
                                            if (task.isSuccessful()) {
                                                repo.clearSync();
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
                                                    repo.insert(local_note);
                                                    Log.d(TAG, "succeful");
                                                    Log.d(TAG, document.getId() + " => " + document.getData().get("text"));
                                                }
                                            } else {
                                                Log.d(TAG, "Error getting documents: ", task.getException());
                                            }
                                            repo.deleteSync();
                                        };
                                    });
                                    databaseExecutor.shutdown();
                                }});

                    }
                }
                if(id == R.id.action_updateclick) {
                    ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                    databaseExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            List<Note> clickatedNotes = App.getInstance().getNoteDao().getClickatedNotes();
                            for(Note curNote : clickatedNotes) {
                                cloud_database
                                        .collection("Notes")
                                        .document("groups")
                                        .collection(curNote.group)
                                        .document(String.valueOf(curNote.uid))
                                        .update("done", curNote.done);
                            }
                            App.getInstance().getNoteDao().clearClickatedNotes();
                        }
                    });
                }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


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
                    Log.d(TAG, "last_num_pos = " + String.valueOf(j));

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
    private void showDialog(Note editNote) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);
        final EditText editText = dialog.findViewById(R.id.editTextText);
        String entry = (editNote.text + " "+ editNote.amount).trim();
        editText.setText(entry);
        FloatingActionButton button = dialog.findViewById(R.id.confirm_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!editText.equals(""))   {
                    String s = editText.getText().toString();
                    Note tmp = converterEntry(s);

                    ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
                    databaseExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            App.getInstance().getNoteDao().delete(editNote);
//                            editNote.text = tmp.text;
//                            editNote.amount = tmp.amount;
//                            App.getInstance().getNoteDao().insert(editNote);
                            Log.i("EditingNotes", "editNote: " + editNote.text + " " +editNote.amount);
                            cloud_database.collection("Notes")
                                    .document("groups")
                                    .collection(editNote.group)
                                    .document(String.valueOf(editNote.uid))
                                    .update("text", editNote.text, "amount", editNote.amount);

                        }
                    });
                    databaseExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            editNote.text = tmp.text;
                            editNote.amount = tmp.amount;
                            App.getInstance().getNoteDao().insert(editNote);
                        }
                    });
                    dialog.dismiss();
                    databaseExecutor.shutdown();

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



    private boolean is_id(String s) {
        if(s.length() < 26) return false;
        if (s.charAt(8) == '-'
                && s.charAt(13) == '-'
                && s.charAt(18) == '-') {
            Log.i("ISTHISID?", "is_id: " + "true");
            return true;
        }
        Log.i("ISTHISID?", "is_id: " + "false");
        return false;
    };

    void addNewList(Group group) {
        Log.i("cloudGroupName", "name2 = " + group.group);
        Log.i("cloudGroupName", "id2 = " + group.uid);
        floatingActionButton.setVisibility(View.VISIBLE);
        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (allGroups){

                    App.getInstance().getNoteDao().insert(group);
                    if(allGroups.isEmpty()) {
                        currentGroup = group.uid;
                        allGroups.add(group);
                        Message msg = new Message();
                        msg.arg1 = 1;
                        handler.sendMessage(msg);
                    } else {
                        allGroups.add(group);
                    }
                    namesGroupDB.put(group.uid, group.group);


                    Map<String, Object> NewGroup1 = new HashMap<>();
                    NewGroup1.put("group", group.group);
                    cloud_database.collection("Notes")
                            .document("groups")
                            .collection("names")
                            .document(group.uid)
                            .set(NewGroup1);
                }
            }
        });
        databaseExecutor.shutdown();
        groupAdapter.notifyDataSetChanged();
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
        final EditText editText = dialog.findViewById(R.id.add_new_list);
        editText.setText(group.group);
        LinearLayout linearLayout = dialog.findViewById(R.id.layout_button);
        linearLayout.setOnClickListener(new View.OnClickListener() {
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

     void showDialogAddList() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_list_layout);
        dialog.show();
        final EditText editText = dialog.findViewById(R.id.add_new_list);

        LinearLayout linearLayout = dialog.findViewById(R.id.layout_button);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String listName = editText.getText().toString();

                if(!listName.equals("") && !namesGroupDB.containsValue(listName) ) {
                    Group group = new Group();
                    if (is_id(listName)) {
                        cloud_database.collection("Notes")
                                .document("groups")
                                .collection("names")
                                .document(listName)
                                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document != null && document.exists()) {
                                                Map<String, Object> cloudGroupName = task.getResult().getData();
                                                group.group = (String) cloudGroupName.get("group");
                                                Log.i("cloudGroupName", "name0 = " + group.group);
                                                group.uid = listName;
                                            } else {
                                                Toast.makeText(MainActivity.this, "такого id нет", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.this, "такого id нет", Toast.LENGTH_SHORT).show();
                                        }
                                        addNewList(group);
                                        dialog.dismiss();
                                    }
                                });
                    } else {
                        group.uid = UUID.randomUUID().toString();
                        group.group = listName;
                        addNewList(group);
                        dialog.dismiss();

                    }
                } else {
                    Log.i("NewListCreate", "onClick: не прошло условие");
                    Toast.makeText(MainActivity.this, "такое имя уже есть", Toast.LENGTH_SHORT).show();
                }
            }

        });
    };

    @Override
    protected void onStop() {
        super.onStop();

//        ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
//        databaseExecutor.execute(new Runnable() {
//            @Override
//            public void run() {
//                List<Note> updateNotes = App.getInstance().getNoteDao().getUpdateNotes();
//                for (Note updateNote : updateNotes) {
//                    Log.d("DEL_DB", "обновляется товар " + updateNote.text);
//
//                    Map<String, Object> note1 = noteConverter(updateNote);
//                    cloud_database.collection("Notes")
//                            .whereEqualTo("uid",  updateNote.uid)
//                            .limit(1)
//                            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                                @Override
//                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                    if (task.isSuccessful()) {
//                                        for (QueryDocumentSnapshot document : task.getResult()) {
//                                            cloud_database.collection("Notes").document(document.getId()).update(note1)
//                                                    .addOnSuccessListener(aVoid -> {
//                                                        Log.d("DEL_DB", "Документ " + document.getData().get("text").toString() + " успешно обновлен");
//                                                    })
//                                                    .addOnFailureListener(e -> {
//                                                        Log.d("DEL_DB", "ОШИБКА при обновлении ДОКУМЕНТА из облака");
//                                                    });;
//                                        }
//                                    } else {
//                                        Log.d("DEL_DB", "ОШИБКА при обновлении ДАННЫХ из облака");
//                                    }
//                                }
//                            });
//                }
//            }
//        });
//
//        databaseExecutor.shutdown();


    }

    private Object hashCode(String valueOf) {
        int hash = 7;
        hash = 31 * hash + (valueOf == null ? 0 : valueOf.hashCode());
        return hash;
    }


}