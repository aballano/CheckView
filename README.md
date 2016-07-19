# Check View

[![Release]()]()

![Sample](https://github.com/Shyish/CheckView/blob/master/gifs/checkview.gif?raw=true)

## Usage
   
Add a CheckView to your layout
```xml
    <com.zdvdev.checkview.CheckView
        android:id="@+id/checkView"
        android:layout_width="48dp"
        android:layout_height="48dp"
        app:lineColor="#ff00" />
```

// Note that you can define the color with `lineColor`

Toggle is done **automatically by default**, if you want to change that, just:

```java
    checkView.setAutoToggle(true);
```

or by xml:

```xml
    app:autoToggle="false" />
```

You can also set a state directly:
```java
    checkView.plus();
```
```java
    checkView.check();
```

Optionally supply an animation duration in milliseconds

```java
    checkView.check(0l);
```

```java
    checkView.toggle(150l);
```

```java
    checkView.plus(200l);
```

## Download

Easy as:

```gradle
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.Shyish:checkview:v1.0.0'
}
```

## Based on

[CrossView from Collin Flynn (cdflynn)](https://github.com/cdflynn/crossview)