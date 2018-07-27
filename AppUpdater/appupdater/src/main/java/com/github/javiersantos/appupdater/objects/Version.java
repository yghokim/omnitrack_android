package com.github.javiersantos.appupdater.objects;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {

    static final String REGEX_NUMBERS = "/(\\d+)[\\.\\-\\s]?/g";
    static final String REGEX_SUFFIX = "/[\\-\\s]([a-zA-Z0-9]+)/g";

    private String version;

    private int[] numbers;
    private String suffix;

    public final String get() {
        return this.version;
    }

    public Version(String version) {
        final String TAG = "AppUpdater";
        if (version == null)
            Log.e(TAG, "Version can not be null");

        List<Integer> versionNumbers = new ArrayList<Integer>();
        Matcher m = Pattern.compile(REGEX_NUMBERS).matcher(version);
        while(m.find()){
           versionNumbers.add(Integer.parseInt(m.group()));
        }
        numbers = new int[versionNumbers.size()];
        for(int i = 0;i<numbers.length; i++){
            numbers[i] = versionNumbers.get(i);
        }

        List<String> versionSuffix = new ArrayList<String>();
        m = Pattern.compile(REGEX_SUFFIX).matcher(version);
        while(m.find()){
            versionSuffix.add(m.group());
        }

        if(versionSuffix.size() > 0)
            suffix = versionSuffix.get(0);

        /*
        version = version.replaceAll("[^0-9?!\\.]", "");
        if (!version.matches("[0-9]+(\\.[0-9]+)*"))
            Log.e(TAG, "Invalid version format");*/

        this.version = version;


    }

    @Override
    public int compareTo(@NonNull Version that) {
        int length = Math.max(this.numbers.length, that.numbers.length);
        for (int i = 0; i < length; i++) {
            int thisPart = i < this.numbers.length ?
                    this.numbers[i] : 0;
            int thatPart = i < that.numbers.length ?
                    that.numbers[i] : 0;
            if (thisPart < thatPart)
                return -1;
            if (thisPart > thatPart)
                return 1;
        }

        if(this.suffix != null && that.suffix != null){
            return this.suffix.toLowerCase().compareTo(that.suffix.toLowerCase());
        }
        else if(this.suffix == null && that.suffix != null){
            return 1;
        }else if(this.suffix != null && that.suffix == null){
            return -1;
        }else return 0;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;
        if (that == null)
            return false;
        return this.getClass() == that.getClass() && this.compareTo((Version) that) == 0;
    }

}
