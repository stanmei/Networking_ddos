

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.net.*;
import java.text.*;


public class MasterBot {

    /* master attributes
     */
    static int srcPort = 0;

    ServerSocket serverSock ;
    /*HashSet : to store slave's information with key(slave hostname+src port).
     */
    HashSet <Host> slaveset = new HashSet<Host> ();

    /* main
     * Display ">" on terminal. Then
     *Input  : user cmd
     *         1) list ;
     *         2) connect ;
     *         3) disconnect;
     */
    public static void main(String[] args){
        //Receive "-p" option for master port.
        String iparam_0 = args[0] ;
        if ( !iparam_0.equalsIgnoreCase("-p") ){
            System.out.println("Invalid Parameter : " + args[0] + " ; You should input format: MaterBot -p/P <srcPort>");
            System.exit(1);
        }
        try {
            srcPort = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException Nfe){
            Nfe.printStackTrace();
            System.exit(1);
        }
        //Call method go.
        MasterBot master = new MasterBot();
        master.go();
    } // close main

    /* method : go
     * Parse incoming commands and call corresponding methods.
     * Scanner : to read in cmd line from console.
     */
    public void go() {

        //Display ">" in terminal and create server socket.
        try {
            serverSock = new ServerSocket(srcPort);
            //new thread for connection monitor
            Thread t_slave_conn = new Thread (new SlaveConnRx()  ) ;
            t_slave_conn.start();

            //new thread for console commands read
            Thread t_console = new Thread (new ConsolRead()  ) ;
            t_console.start();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }
    /*  Command monitor from console
     *
     *
     */
    public class ConsolRead implements Runnable {
        BufferedReader reader ;

        public void run (){
            String consolCmd ;
            String [] splitCmd = null;
            try {
                //Display symbol >
                System.out.print(">");
                reader = new BufferedReader(new InputStreamReader(System.in))  ;
                while ( (consolCmd= reader.readLine())!=null) {
                    //Command parse
                    //System.out.println ("user input: "+ consolCmd);
                    splitCmd = consolCmd.split(" ");

                    if ( splitCmd[0].equalsIgnoreCase("list") ) {
                        listHandler(splitCmd);
                    } else if (splitCmd[0].equalsIgnoreCase("connect") ) {
                        slaveConnTx(consolCmd,splitCmd);
                    } else if (splitCmd[0].equalsIgnoreCase("disconnect")) {
                        slaveConnTx(consolCmd,splitCmd);
                    } else {
                        System.out.println ("Error, Unsupported command types. Current supported : list,connect,disconnect! Please double check and Re-input." );
                    }
                    //New Thread to execute
                    System.out.print(">");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void listHandler (String [] splitCmd) {
            if ( splitCmd.length==1) {
                if (slaveset.size()!=0) {
                    for (Host slave :slaveset  ) {
                        System.out.println (slave.hostName+" "+slave.ipAddr+" "+slave.srcPort+" "+slave.timeStamp);
                    }
                } else {
                    System.out.println("Empty!");
                }
            } else {
                System.out.println ("Error, illegal input command format!");
            }
        }

        public void slaveConnTx (String consolCmd,String[] splitCmd) {
            try {
                String slaveIp = null ;
                String slaveName = null;
                if (!splitCmd[1].equalsIgnoreCase("all")) {
                    InetAddress slaveAddr = InetAddress.getByName(splitCmd[1]);
                    slaveIp = slaveAddr.getHostAddress();
                    slaveName = slaveAddr.getHostName();
                } else{
                    slaveIp="all" ;
                }
                System.out.println ("user input addr:  "+slaveIp+ "; slaveNmae: "+ slaveName) ;
                //int slavePort=Integer.parseInt(splitCmd[3]);
                //Socket Sock = getSock(slaveIp,slavePort);
                Socket Sock =null ;
                for ( Host slave : slaveset ) {
                    System.out.println ("Query list, ipaddr:  "+slave.ipAddr+ "; srcPort: "+slave.srcPort) ;
                    if (slaveIp.equals("all")) {
                        Sock = slave.slaveSock;
                    }else if (slave.ipAddr.equals(slaveIp)) {
                        System.out.println("Found:  " + slave.ipAddr + "; " + slave.srcPort);
                        Sock = slave.slaveSock;
                    } else {
                        Sock =null;
                    }

                    if (Sock!=null) {
                        PrintWriter writer = new PrintWriter(Sock.getOutputStream());
                        //System.out.println("Start to write cmd <" + consolCmd + ">to slave via socket:;" + slaveIp + "; ");
                        writer.println(consolCmd);
                        System.out.println("Write cmd <" + consolCmd + "> to slave:"+slaveIp+";");
                        writer.close();
                    }
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    } // close Class ConsolScan

    /*  Slave Connection handler
     */
    public class SlaveConnRx implements Runnable {
        public void run (){
            //InetAddress  slaveIP ;
            String  slaveHostname ;
            int slavePort ;

            try {
                while (true){
                    Host slave = new Host();
                    Socket slaveSock = serverSock.accept();

                    slave.slaveSock=slaveSock;
                    slave.ipAddr = slaveSock.getInetAddress().getHostAddress();
                    slave.hostName = slaveSock.getInetAddress().getHostName();
                    slave.srcPort = slaveSock.getPort();
                    slave.timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(Calendar.getInstance().getTime());
                    System.out.println ("Accepted connection from slave: "+slave.ipAddr+","+slave.hostName+",port:"+slave.srcPort +",time:"+slave.timeStamp);
                    slaveset.add(slave);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /* Sub-class :  Slaves
     * @slave host name
     * @slave IP
     * @Slave Source port
     * @Register data
     *
     * This sub class is for registered class.
     */
    class Host implements Comparable<Host>{
        String hostName ;
        //InetAddress  ipAddr ;
        String ipAddr;
        int srcPort;
        String regData;
        String timeStamp ;
        Socket slaveSock ;

        public int compareTo (Host s){
            String key = s.getKey();
            return key.compareTo(s.getKey());
        }

        public String getKey (){
            return hostName + srcPort ;
        }

    }

} //close class MaterBot
