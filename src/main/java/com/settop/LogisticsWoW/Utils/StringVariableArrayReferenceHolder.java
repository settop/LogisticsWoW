package com.settop.LogisticsWoW.Utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class StringVariableArrayReferenceHolder extends StringReferenceHolder
{
    private boolean isDirty = false;
    private final ArrayList<String> stringArray = new ArrayList<>();
    private final char separator;

    public StringVariableArrayReferenceHolder(char separator)
    {
        this.separator = separator;
    }

    @Override
    public String get()
    {
        return StringUtils.join(stringArray, separator);
    }

    @Override
    public void set(String value)
    {
        stringArray.clear();
        for(String str : StringUtils.split(value, separator))
        {
            stringArray.add(str);
        }
        isDirty = true;
    }

    public ArrayList<String> getArray()
    {
        return new ArrayList<String>(stringArray);
    }

    public void setArray(ArrayList<String> values)
    {
        stringArray.clear();
        stringArray.addAll(values);
        isDirty = true;
    }

    @Override
    public boolean isDirty()
    {
        return isDirty;
    }

    @Override
    public void clearDirty()
    {
        isDirty = false;
    }
}
