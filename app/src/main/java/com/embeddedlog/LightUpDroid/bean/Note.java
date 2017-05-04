package com.embeddedlog.LightUpDroid.bean;

/**
 * Created by k54hu on 10/13/2016.
 */

import android.content.Context;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Note implements Serializable {
    private long mDateTime;
    private int noteId;
    private String noteTitle;
    private String noteContent;

    private Note noter;
    public Note()  {

    }

    public Note(String noteTitle, String noteContent) {
        this.noteTitle= noteTitle;
        this.noteContent= noteContent;
    }

    public Note(int noteId, String noteTitle, String noteContent) {
        this.noteId= noteId;
        this.noteTitle= noteTitle;
        this.noteContent= noteContent;
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }
    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }


    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }


    @Override
    public String toString()  {
        return this.noteTitle;
    }


    public String getDateTimeFormatted(Context context) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"
                , context.getResources().getConfiguration().locale);
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(new Date(mDateTime));
    }
    public void setDateTime(long dateTime) {
        mDateTime = dateTime;
    }
    public long getDateTime() {
        return mDateTime;
    }
}