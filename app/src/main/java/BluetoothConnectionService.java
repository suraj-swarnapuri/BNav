import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "myApp";
    private static final UUID myUUID = null;
    private final BluetoothAdapter bluetoothAdapter;
    Context context;
    private AcceptThread _acceptThread;
    private ConnectThread _connectThread;
    private BluetoothDevice _device;
    private UUID _deviceUUID;
    ProgressDialog _progressDialog;
    private ConnectedThread _connectionThread;


    public BluetoothConnectionService(Context context) {
        context = this.context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /*
     * Thread listens for incomming connections*/
    private class AcceptThread extends Thread {
        //local socket
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            //create a new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);
            } catch (IOException e) {
                //handle acception
            }
            serverSocket = tmp;
        }

        public void run() {
            //run the thread
            Log.d(TAG, "run: AcceptThread Running.");
            BluetoothSocket socket = null;
            /*
             * this is a blocking call and will only terminate after a sucessful connection
             * or an exception is thrown*/
            try {
                Log.d(TAG, "run: RFCOM server socekt start....");
                socket = serverSocket.accept();
                Log.d(TAG, "run: RFCOM Server Socket accepted");
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOExeption: " + e.getMessage());
            }
            if (socket != null) {
                connected(socket, _device);
            }
            Log.i(TAG, "End AcceptThread");

        }

        public void cancel() {
            Log.d(TAG, "cancel: Cancelling AcceptThread");
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: closing AcceptThread Failed " + e.getMessage());
            }
        }
    }

    /*
     * Thread runs while attempting to make an outgoing connection witha  device
     * */
    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started");
            _device = device;
            _deviceUUID = uuid;
        }

        public void run() {
            Log.d(TAG, "RUN connectThread");
            BluetoothSocket tmp = null;
            //Get a bluetooth socket for connection with the given device
            try {
                tmp = _device.createRfcommSocketToServiceRecord(_deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: could not create RFComSock");
            }
            mSocket = tmp;
            //cancel discovery mode
            bluetoothAdapter.cancelDiscovery();
            //blocking call
            try {
                mSocket.connect();
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "connectThread run: Unable to close connection");

                }
                Log.d(TAG, "run: ConnectThread; could not connect to UUID " + myUUID);

            }
            connected(mSocket, _device);

        }
        public void cancel(){
            try{
                Log.d(TAG,"cancel: Closing Client Socket");
                mSocket.close();
            }catch(IOException e){
                Log.e(TAG,"Cancel: Close() of socket is ConnectThread faild::" + e.getMessage());

            }
        }
    }
    //start the accept thread
 public synchronized void start(){
        Log.d(TAG,"start");
        //cancel any thread attempting to make a connection
        if(_connectThread != null){
            _connectThread.cancel();
            _connectThread = null;
        }
        if(_acceptThread == null){
            _acceptThread = new AcceptThread();
            _acceptThread.start();
        }
 }
 public void startClient(BluetoothDevice device, UUID uuid){
      Log.d(TAG,"StartClient: started");
      //init process dialog
     _progressDialog = ProgressDialog.show(context, "Connecting Bluetooth","Please wait....",true);
     _connectThread = new ConnectThread(device,uuid);
     _connectThread.start();
 }

private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG,"ConnectedThread: Starting");
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the processdialog
            _progressDialog.dismiss();
            try{
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            }catch(IOException e){
                //print stacktrace
            }

            mInStream = tmpIn;
            mOutStream  = tmpOut;

        }
        public void run(){
            byte[] buffer = new byte[1024]; //buffer store for stream

            int bytes; //bytes returend from read()

            while(true) {
                try {
                    //read from instream
                    bytes = mInStream.read(buffer);
                    String incommingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incommingMessage);
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"write: error writing to outputstream");
            try{
                mOutStream.write(bytes);
            }catch (IOException e){
                //somethin
            }
        }
        public void cancel(){
            try{
                mSocket.close();
            }catch(IOException e){

            }
        }

    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice mDevice){
        Log.d(TAG,"connected Started");
        //Start the thread to manage the connection gn perform tranmisson
        _connectionThread = new ConnectedThread(mSocket);
        _connectionThread.start();
    }

    //write to the _connectThread in an unsycn manner
    public void write(byte[] out){
        //create tmp
        ConnectThread r;
        //Sychnronize a copy of connectedThread
        Log.d(TAG,"write: Write Called.");
        _connectionThread.write(out);
    }
}

