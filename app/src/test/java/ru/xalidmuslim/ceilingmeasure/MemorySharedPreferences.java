package ru.xalidmuslim.ceilingmeasure;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class MemorySharedPreferences implements SharedPreferences {
    private final LinkedHashMap<String,Object> values=new LinkedHashMap<>();

    @Override public Map<String,?> getAll(){return Collections.unmodifiableMap(new LinkedHashMap<>(values));}
    @Nullable @Override public String getString(String key,@Nullable String defValue){
        Object v=values.get(key);return v instanceof String?(String)v:defValue;
    }
    @Nullable @Override public Set<String> getStringSet(String key,@Nullable Set<String> defValues){
        Object v=values.get(key);
        if(v instanceof Set)return new HashSet<>((Set<String>)v);
        return defValues;
    }
    @Override public int getInt(String key,int defValue){Object v=values.get(key);return v instanceof Number?((Number)v).intValue():defValue;}
    @Override public long getLong(String key,long defValue){Object v=values.get(key);return v instanceof Number?((Number)v).longValue():defValue;}
    @Override public float getFloat(String key,float defValue){Object v=values.get(key);return v instanceof Number?((Number)v).floatValue():defValue;}
    @Override public boolean getBoolean(String key,boolean defValue){Object v=values.get(key);return v instanceof Boolean?(Boolean)v:defValue;}
    @Override public boolean contains(String key){return values.containsKey(key);}
    @Override public Editor edit(){return new MemoryEditor();}
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}

    private final class MemoryEditor implements Editor{
        private final Map<String,Object> puts=new HashMap<>();
        private final Set<String> removes=new HashSet<>();
        private boolean clear;

        @Override public Editor putString(String key,@Nullable String value){if(value==null)return remove(key);puts.put(key,value);removes.remove(key);return this;}
        @Override public Editor putStringSet(String key,@Nullable Set<String> value){if(value==null)return remove(key);puts.put(key,new HashSet<>(value));removes.remove(key);return this;}
        @Override public Editor putInt(String key,int value){puts.put(key,value);removes.remove(key);return this;}
        @Override public Editor putLong(String key,long value){puts.put(key,value);removes.remove(key);return this;}
        @Override public Editor putFloat(String key,float value){puts.put(key,value);removes.remove(key);return this;}
        @Override public Editor putBoolean(String key,boolean value){puts.put(key,value);removes.remove(key);return this;}
        @Override public Editor remove(String key){removes.add(key);puts.remove(key);return this;}
        @Override public Editor clear(){clear=true;puts.clear();removes.clear();return this;}
        @Override public boolean commit(){applyChanges();return true;}
        @Override public void apply(){applyChanges();}

        private void applyChanges(){
            if(clear)values.clear();
            for(String key:removes)values.remove(key);
            values.putAll(puts);
        }
    }
}
