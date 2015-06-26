# WearSocket
A wrapper for socket communication between Android Wear and Android Mobile
There is a lot of boiler plate to send data between Android Wear and Android Mobile devices.
This library (inspired by Socket.IO), aims to streamline both [message sending](https://developer.android.com/training/wearables/data-layer/messages.html) and [synching data updates](https://developer.android.com/training/wearables/data-layer/data-items.html).

## Installation

Add this line to your ```depedencies``` in your ```build.gradle``` file:
```
compile 'com.github.jrejaud:wear-socket:0.1.0'
```

Note: WearSocket uses jCenter repository and is not on Maven Central atm.

## Setup 

First, get an instance of the WearSocket and set it up by passing Activity Context

```java
WearSocket wearSocket = WearSocket.getInstance();
wearSocket.setupAndConnect(context);
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

Implement WearSocket.DataListener and override messageReceived to start listening for messages

```java
public class MyActivity implements WearSocket.DataListener
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
