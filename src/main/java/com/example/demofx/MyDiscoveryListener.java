package com.example.demofx;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MyDiscoveryListener implements DiscoveryListener {

    private static Object lock=new Object();

    @Override
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass arg1) {
        String name;
        try {
            name = btDevice.getFriendlyName(false);
        } catch (Exception e) {
            name = btDevice.getBluetoothAddress();
        }

        System.out.println("device found: " + name);

    }

    public void inquiryCompleted(int discType)
    {
        synchronized(lock){
            lock.notify();
        }
        switch (discType)
        {
            case DiscoveryListener.INQUIRY_COMPLETED :
                System.out.println("INQUIRY_COMPLETED");
                break;
            case DiscoveryListener.INQUIRY_TERMINATED :
                System.out.println("INQUIRY_TERMINATED");
                break;
            case DiscoveryListener.INQUIRY_ERROR :
                System.out.println("INQUIRY_ERROR");
                break;
            default :
                System.out.println("Unknown Response Code");
                break;
        }
    }

    @Override
    public void serviceSearchCompleted(int arg0, int arg1) {
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void servicesDiscovered(int arg0, ServiceRecord[] services) {
        for (int i = 0; i < services.length; i++) {
            String url = services[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            if (url == null) {
                continue;
            }

            DataElement serviceName = services[i].getAttributeValue(0x0100);
            if (serviceName != null) {
                System.out.println("service " + serviceName.getValue() + " found " + url);
            } else {
                System.out.println("service found " + url);
            }

            if(serviceName.getValue().equals("OBEX Object Push")){
                sendMessageToDevice(url);
            }
        }

    }

    private static void sendMessageToDevice(String serverURL){
        try{
            System.out.println("Connecting to " + serverURL);

            ClientSession clientSession = (ClientSession) Connector.open(serverURL);
            HeaderSet hsConnectReply = clientSession.connect(null);
            if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                System.out.println("Failed to connect");
                return;
            }

            HeaderSet hsOperation = clientSession.createHeaderSet();
            hsOperation.setHeader(HeaderSet.NAME, "Hello.txt");
            hsOperation.setHeader(HeaderSet.TYPE, "text");

            //Create PUT Operation
            Operation putOperation = clientSession.put(hsOperation);

            // Sending the message
            byte data[] = "Hell".getBytes(StandardCharsets.ISO_8859_1);
            OutputStream os = putOperation.openOutputStream();
            os.write(data);
            os.close();

            putOperation.close();
            clientSession.disconnect(null);
            clientSession.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
