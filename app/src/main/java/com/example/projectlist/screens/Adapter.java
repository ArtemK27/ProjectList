package com.example.projectlist.screens;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.example.projectlist.App;
import com.example.projectlist.R;
import com.example.projectlist.model.Group;
import com.example.projectlist.model.Note;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Adapter extends RecyclerView.Adapter<Adapter.NoteViewHolder> {

    private SortedList<Note> sortedList;
    private OnItemLongClickListener mListener;
    ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    public Adapter() {
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


        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    mListener.onItemLongClick(holder.getAdapterPosition(), v);
                }
                return true;
            }
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

        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                App.getInstance().getNoteDao().update(note);

            }
        });
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteText, noteAmount;
        Note note;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            noteText = itemView.findViewById(R.id.note_text);
            noteAmount = itemView.findViewById(R.id.note_amount);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    note.time = System.currentTimeMillis();
                    note.done = !note.done;
                    note.update_flag = "click_update";
                    databaseExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            App.getInstance().getNoteDao().update(note);

                        }
                    });
                    updateStrokeOut();
                    //-----------------------------------------------------
                    // НЕТ ЗАКРЫТИЯ ПОТОКА, ну и ладно че


                }
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
                noteText.setBackgroundColor(Color.argb(180,211,211,211));
                noteAmount.setPaintFlags(noteText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                noteAmount.setBackgroundColor(Color.argb(180,211,211,211));
            } else {
                noteText.setPaintFlags(noteText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                noteText.setBackgroundColor(Color.TRANSPARENT);
                noteAmount.setPaintFlags(noteText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                noteAmount.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
}
