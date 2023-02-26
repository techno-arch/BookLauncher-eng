package com.tolino.custom.booklauncher.tools;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.tolino.custom.booklauncher.LauncherActivity;
import com.tolino.custom.booklauncher.R;
import com.tolino.custom.booklauncher.utils.AndroidUtils;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FtpActivity extends Activity {
    FtpServer server = null;
    ToggleButton chkServer = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp);
        chkServer = ((ToggleButton)findViewById(R.id.chkFtpSwitch));
        try {
            setupStart();
        }catch (Exception ex){
            ex.printStackTrace();
            server = null;
            AndroidUtils.Msgbox(this,"The ftp server could not be setup","Attention","Close");
            chkServer.setEnabled(false);
        }
        chkServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    try {
                        new Thread(){
                            @Override
                            public void run() {
                                try {
                                    server.start();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView)findViewById(R.id.txtFtpAddress)).setText("FTP address：ftp://"+getIPAddress(true)+":4321/");
                                        }
                                    });
                                } catch (FtpException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AndroidUtils.Msgbox(FtpActivity.this,"The FTP server failed to open","Attention","Close");
                                            chkServer.setChecked(false);
                                        }
                                    });
                                }
                            }
                        }.start();
                        ((TextView)findViewById(R.id.txtFtpAddress)).setText("Launching...");
                    } catch (Exception e) {
                        e.printStackTrace();
                        AndroidUtils.Msgbox(FtpActivity.this,"The FTP server failed to open","Attention","Close");
                        buttonView.setChecked(false);
                    }
                }
                else{
                    if(!server.isStopped()) {
                        server.stop();
                        try {
                            setupStart();
                        } catch (Exception e) {
                            e.printStackTrace();
                            server = null;
                        }
                        ((TextView)findViewById(R.id.txtFtpAddress)).setText("FTP has been closed");
                    }
                }
            }
        });
    }

    public void onBackClick(View view) {

        finish();
    }

    public void showFtpIp(View view) {
        String message=
                "On windows download filezilla from filezilla-project.org\r\n" +
                "On linux install filezilla with your package manger of choice\r\n" +
                "Use anonymous login\r\n\r\n" +
                "FTP address：ftp://"+getIPAddress(true)+":4321/";
        AndroidUtils.Msgbox(this,message,"Usage","Understanding");
    }

    private void setupStart() throws FileNotFoundException, FtpException {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(4321);
        serverFactory.addListener("default",listenerFactory.createListener());
        BaseUser user = new BaseUser();
        user.setName("anonymous");
        user.setHomeDirectory(LauncherActivity.bookRoot);
        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        serverFactory.getUserManager().save(user);
        server = serverFactory.createServer();
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        // boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {ex.printStackTrace(); } // for now eat exceptions
        return "127.0.0.1";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null!=server && !server.isStopped()){
            server.stop();
            AndroidUtils.Toast(this,"FTP Has been closed");
        }
    }
}
