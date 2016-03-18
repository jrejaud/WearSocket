# WearSocket

[![](https://jitpack.io/v/jrejaud/WearSocket.svg)](https://jitpack.io/#jrejaud/WearSocket)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-WearSocket-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2416)



A wrapper for socket communication between Android Wear and Android Mobile.
There is a lot of boiler plate to send data between Android Wear and Android Mobile devices.
This library (inspired by Socket.IO), aims to streamline both [message sending](https://developer.android.com/training/wearables/data-layer/messages.html) and [synching data updates](https://developer.android.com/training/wearables/data-layer/data-items.html).

## Installation

Use [JitPack](https://jitpack.io/#jrejaud/WearSocket)

```
allprojects {
	repositories {
		maven { url "https://jitpack.io" }
	}
}
```

```
dependencies {
	    compile 'com.github.jrejaud:WearSocket:1.0.5'
}
```	

## Setup 

First, get an instance of the WearSocket and set it up by passing Context and the [wear app capabilities](http://developer.android.com/training/wearables/data-layer/messages.html#SendMessage) 

```java
WearSocket wearSocket = WearSocket.getInstance();
String androidWearCapability = ...;
wearSocket.setupAndConnect(context, androidWearCapability, new onErrorListener() {
       	@Override
        public void onError(Throwable throwable) {
        	//Throws an error here if there is a problem connecting to the other device.
        }
});
```

### Advertising Capabilities (copied from official Google [documentation](http://developer.android.com/training/wearables/data-layer/messages.html#SendMessage))

Create an XML configuration file in the res/values/ directory of your project and name it wear.xml.
Add a resource named android_wear_capabilities to wear.xml.
Define capabilities that the device provides.
Note: Capabilities are custom strings that you define and must be unique within your app.

The following example shows how to add a capability named voice_transcription to wear.xml:

```
<resources>
    <string-array name="android_wear_capabilities">
        <item>voice_transcription</item>
    </string-array>
</resources>
```

## [Sending and Receiving Messages](https://developer.android.com/training/wearables/data-layer/messages.html) between Wear and Mobile

### Sending Message

Pick a path (think of it like a subject) for your message.

```java
wearSocket.sendMessage("myPath","myMessage");
```

### Receiving a Message

Start your message listener and listen for the path that you are associating with your messages. 

```java
wearSocket.startMessageListener(context,"myPath");
```

Implement WearSocket.MessageListener and override messageReceived to start listening for messages

```java
public class MyActivity implements WearSocket.MessageListener
```
```java
    @Override
    public void messageReceived(String path, String message) {
        //Do things here!
    }
```

## [Synching Data Updates](https://developer.android.com/training/wearables/data-layer/data-items.html) between Wear and Mobile

### Updating a data item

**Note**: Data Path should begin with /, thus I recommend using one different from message sending.
dataItem is an object (or collection of objects).

```java
wearSocket.updateDataItem("myDataPath","myKey",dataItem);
```

### Receiving a data item update

Start your data change listener and listen for the path that you are associating with your data changes.

```java
wearSocket.startDataListener(context,"myDataPath");
```

Additionally, you need to associate a Type with a specific Key. For example, if the key "myKey" is associated with a `List<String>` object, set that Type accordingly:

```java
wearSocket.setKeyDataType("myKey",new TypeToken<List<String>>() {}.getType());
```

Change `List<String>` to whatever class (or collection of classes) that dataItem is.

Implement WearSocket.MessageListener and override messageReceived to start listening for messages

```java
public class MyActivity implements WearSocket.MessageListener
```
```java
    @Override
    public void dataChanged(String key, Object data) {
        //Cast "data" as whatever class you sent it as
        ArrayList<String> dataItem = (ArrayList<String>) data;
        //Do things here!
    }
```

## License and Attribution

MIT License

WearSocket uses:
[Gson](https://code.google.com/p/google-gson/source/browse/trunk/gson/LICENSE?r=369), 
[Android Wearable APIs](https://developer.android.com/training/building-wearables.html), 
[Google Play Services](https://components.xamarin.com/license/googleplayservices)
