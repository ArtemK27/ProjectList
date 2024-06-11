package com.example.projectlist.screens;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.example.projectlist.App;
import com.example.projectlist.R;
import com.example.projectlist.model.Note;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Adapter extends RecyclerView.Adapter<Adapter.NoteViewHolder> {

    private SortedList<Note> sortedList;
    private OnItemLongClickListener mListener;
    private FirebaseFirestore cloud_database;

    ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public boolean is_exist (Note checkNote) {
        for (int i = 0; i < sortedList.size(); i++) {
            Note note = sortedList.get(i);
            if (!(note.uid == checkNote.uid)) continue;
            if (!note.text.equals(checkNote.text)) continue;
            if (!note.done == checkNote.done) continue;
            if (!note.amount.equals(checkNote.amount)) continue;
            if (!note.group.equals(checkNote.group)) continue;
            return true;
        }

        return false;
    };

    public Adapter() {
        cloud_database = FirebaseFirestore.getInstance();
        sortedList = new SortedList<>(Note.class, new SortedList.Callback<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                if(!o2.done && o1.done) {
                    return 1;
                }
                if(o2.done && !o1.done) {
                    return -1;
                }
                return (int) (o2.time - o1.time);

            }


            @Override
            public void onChanged(int position, int count) {
                notifyItemChanged(position, count);
            }
            @Override
            public boolean areContentsTheSame(Note oldItem, Note newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(Note item1, Note item2) {
                return item1.uid == item2.uid;
            }

            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }
        }) ;
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, View view);
    }

    // Метод для установки слушателя долгого нажатия
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mListener = listener;
    }

    public Note getNote(int pos) {
        return sortedList.get(pos);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_list, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, final int position) {
        holder.bind(sortedList.get(holder.getAdapterPosition()));


        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onItemLongClick(holder.getAdapterPosition(), v);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void setItems(List<Note> notes) {
        sortedList.replaceAll(notes);
    }
    public void updateItem(int pos, Note note ) {
        sortedList.updateItemAt(pos, note);
        databaseExecutor.execute(() -> App.getInstance().getNoteDao().update(note));
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteText, noteAmount;
        Note note;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            noteText = itemView.findViewById(R.id.note_text);
            noteAmount = itemView.findViewById(R.id.note_amount);


            itemView.setOnClickListener(v -> {
                note.time = System.currentTimeMillis();
                note.done = !note.done;
                note.update_flag = "click_update";
                note.author = MainActivity.author;
                databaseExecutor.execute(() -> {
                    App.getInstance().getNoteDao().update(note);
                    cloud_database
                            .collection("Notes")
                            .document("groups")
                            .collection(note.group)
                            .document(String.valueOf(note.uid))
                            .update("author", MainActivity.author);
                    cloud_database
                            .collection("Notes")
                            .document("groups")
                            .collection(note.group)
                            .document(String.valueOf(note.uid))
                            .update("done", note.done);
                });
                updateStrokeOut();
                //-----------------------------------------------------
                // НЕТ ЗАКРЫТИЯ ПОТОКА, ну и ладно че


            });

        }


        public void bind(Note note) {
            this.note = note;
            noteText.setText(note.text);
            noteAmount.setText(String.valueOf(note.amount));
            updateStrokeOut();
        }

        private void updateStrokeOut() {
            if (note.done) {
                noteText.setPaintFlags(noteText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                noteText.setBackgroundColor(Color.argb(180,127,127,127));
                noteAmount.setPaintFlags(noteText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                noteAmount.setBackgroundColor(Color.argb(180,127,127,127));
            } else {
                noteText.setPaintFlags(noteText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                noteText.setBackgroundResource(R.drawable.default_background);
                noteAmount.setPaintFlags(noteText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                noteAmount.setBackgroundResource(R.drawable.default_background);
            }
        }
    }
}
