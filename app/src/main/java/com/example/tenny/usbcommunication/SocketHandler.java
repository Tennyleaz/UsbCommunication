package com.example.tenny.usbcommunication;


import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SocketHandler {
    private static Socket socket = null;
    private static boolean isCreated = false;
    private static InputStream in = null;
    private static OutputStream out = null;
    private static String ip;
    private static int port;

    /*public static synchronized Socket getSocket(){
        if(isCreated)
            return socket;
        else
            return null;
    }*/

    public static synchronized Socket initSocket(String SERVERIP, int SERVERPORT){
        try {
            ip = SERVERIP;
            port = SERVERPORT;
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 2000);
            isCreated = true;
            //socket.setSoTimeout(2500);
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (SocketTimeoutException e)  {
            System.out.println("Error timeout: "+e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Error0: "+e.getMessage());
        } catch(IOException e) {
            System.out.println("Error1: " + e.getMessage());
        }
        return socket;
    }

    public static synchronized void setSocketTimeout(int timeout){
        try{
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.out.println("Error setSocketTimeout: "+e.getMessage());
        }
    }

    public static synchronized String getOutput(){
        if(isCreated) {
            String result = "";
            int i;
            List<Byte> buffer = new ArrayList<Byte>();
            //byte[] buffer = new byte[32768];
            byte[] readbyte = new byte[2048];
            try {
                while ((i = in.read(readbyte)) != -1) {
                    for (int j = 0; j < i; j++) {
                        buffer.add(readbyte[j]);
                    }
                    //to test if <END> received
                    String s = new String(readbyte, 0, i);
                    //readbyte = null;
                    readbyte = new byte[2048];
                    Log.d("Mylog", "i=" + i + ", s=" + s);
                    if (s.contains("<END>"))
                        break;
                }
            } catch (SocketTimeoutException e)  {
                //System.out.println("Error timeout: "+e.getMessage());
            } catch (IOException e) {
                System.out.println("Error getOutput: " + e.getMessage());
            }
            result = byteListToString(buffer);
            return result;
        }
        else {
            Log.e("Mylog", "socket not created, cant get output!");
            return null;
        }
    }

    public static synchronized void writeToSocket(String s){
        if(isCreated) {
            try {
                Log.d("Mylog", "in writeToSocket send:" + s);
                out.write(s.getBytes());
                Log.d("Mylog", "write to socket finished");
            } catch (IOException e) {
                System.out.println("Error writeToSocket: " + e.getMessage());
            }
        }
        else
            Log.e("Mylog", "socket not created, cant write!");
    }

    private static String byteListToString(List<Byte> l) {
        if (l == null) {
            return "";
        }
        byte[] array = new byte[l.size()];
        int i = 0;
        for (Byte current : l) {
            array[i] = current;
            i++;
        }
        String s = null;
        try {
            s = new String(array, "UTF-8");
        }
        catch (UnsupportedEncodingException e){
            Log.e("Mylog", "UnsupportedEncodingException:"+e);
        }
        return s;
    }

    public static synchronized void closeSocket() {
        Log.d("Mylog", "Socket closed");
        if(isCreated) {
            try {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
                isCreated = false;
            }
            catch (UnknownHostException e)
            {
                System.out.println("Error3: "+e.getMessage());
            }
            catch(IOException e)
            {
                System.out.println("Error4: " + e.getMessage());
            }
        }
    }
}
