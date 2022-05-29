package com.settop.LogisticsWoW.Utils;


public abstract class StringReferenceHolder
{
    public static StringReferenceHolder single()
    {
        return new StringReferenceHolder()
        {
            private String value;
            private boolean isDirty = false;

            public String get()
            {
                return this.value;
            }

            public void set(String value)
            {
                this.isDirty = true;
                //Java strings are immutable so no need to create a copy here
                this.value = value;
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
        };
    }

    public abstract String get();
    public abstract void set(String value);

    public abstract boolean isDirty();
    public abstract void clearDirty();
}
